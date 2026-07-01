package com.localbookkeeping.app.notification

import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import kotlin.math.abs

data class ParsedNotificationBill(
    val amountCents: Long?,
    val type: TransactionType,
    val status: RecordStatus,
    val sourceApp: String,
    val note: String,
    val rawText: String,
    val merchantName: String,
    val notificationTitle: String,
    val notificationText: String,
    val notificationPackageName: String,
    val fingerprint: String,
    val hasAmount: Boolean,
    val isPaymentNotification: Boolean,
    val parseStatus: String,
    val parseReason: String
)

data class AmountCandidate(
    val text: String,
    val amountCents: Long,
    val score: Int,
    val reason: String,
    val startIndex: Int
)

data class NotificationParseDiagnostics(
    val rawText: String = "",
    val matchedKeywords: List<String> = emptyList(),
    val amountCandidates: List<AmountCandidate> = emptyList(),
    val selectedAmount: AmountCandidate? = null,
    val selectedReason: String = selectedAmount?.reason.orEmpty(),
    val type: TransactionType = TransactionType.UNKNOWN,
    val typeReason: String = "未命中类型关键词",
    val merchantName: String = "",
    val merchantReason: String = "未识别商户",
    val pendingCreatedExpected: Boolean = false,
    val failReason: String = ""
)

data class NotificationParseResult(
    val bill: ParsedNotificationBill? = null,
    val failureReason: String = "",
    val isPaymentNotification: Boolean = false,
    val hasAmount: Boolean = false,
    val parseStatus: String = "ignored",
    val parseReason: String = failureReason,
    val diagnostics: NotificationParseDiagnostics = NotificationParseDiagnostics(failReason = failureReason)
)

class NotificationBillParser {
    fun parse(packageName: String, title: String, text: String, postTimeMillis: Long = 0L): NotificationParseResult {
        val sourceApp = sourceName(packageName)
        val notificationTitle = title.trim()
        val notificationText = text.trim()
        val normalized = normalize(listOf(notificationTitle, notificationText))

        if (normalized.isBlank()) {
            return ignored("rawText为空", rawText = normalized)
        }
        if (sourceApp == null) {
            return ignored("非微信/支付宝通知", rawText = normalized)
        }

        val matchedKeywords = paymentKeywords.filter { normalized.contains(it) }
        val amountCandidates = findAmountCandidates(normalized)
        val selectedAmount = amountCandidates.firstOrNull()
        val diagnosticsBase = buildDiagnostics(
            rawText = normalized,
            matchedKeywords = matchedKeywords,
            amountCandidates = amountCandidates,
            selectedAmount = selectedAmount
        )

        if (matchedKeywords.isEmpty()) {
            val reason = "未识别为支付相关通知"
            return ignored(
                reason = reason,
                rawText = normalized,
                hasAmount = selectedAmount != null,
                diagnostics = diagnosticsBase.copy(failReason = reason)
            )
        }

        val hasAmount = selectedAmount != null
        val status = if (hasAmount) RecordStatus.PENDING_CONFIRM else RecordStatus.NEED_AMOUNT
        val parseStatus = if (hasAmount) "pending_confirm" else "need_amount"
        val parseReason = if (hasAmount) {
            "识别到支付关键词和金额，生成待确认账单"
        } else {
            "识别到支付关键词但没有可靠金额，生成待补金额账单"
        }
        val merchantName = diagnosticsBase.merchantName
        val amountCents = selectedAmount?.amountCents

        return NotificationParseResult(
            bill = ParsedNotificationBill(
                amountCents = amountCents,
                type = diagnosticsBase.type,
                status = status,
                sourceApp = sourceApp,
                note = buildNote(sourceApp, merchantName, normalized),
                rawText = normalized,
                merchantName = merchantName,
                notificationTitle = notificationTitle,
                notificationText = notificationText,
                notificationPackageName = packageName,
                fingerprint = "$packageName|${postTimeMillis / DUPLICATE_WINDOW_MILLIS}|${amountCents ?: 0}|${normalizeForFingerprint(normalized)}".sha1(),
                hasAmount = hasAmount,
                isPaymentNotification = true,
                parseStatus = parseStatus,
                parseReason = parseReason
            ),
            isPaymentNotification = true,
            hasAmount = hasAmount,
            parseStatus = parseStatus,
            parseReason = parseReason,
            diagnostics = diagnosticsBase.copy(
                pendingCreatedExpected = true,
                failReason = ""
            )
        )
    }

    private fun buildDiagnostics(
        rawText: String,
        matchedKeywords: List<String>,
        amountCandidates: List<AmountCandidate>,
        selectedAmount: AmountCandidate?
    ): NotificationParseDiagnostics {
        val typeDetection = detectType(rawText, selectedAmount)
        val merchantDetection = detectMerchant(rawText)
        return NotificationParseDiagnostics(
            rawText = rawText,
            matchedKeywords = matchedKeywords,
            amountCandidates = amountCandidates,
            selectedAmount = selectedAmount,
            type = typeDetection.type,
            typeReason = typeDetection.reason,
            merchantName = merchantDetection.name,
            merchantReason = merchantDetection.reason,
            pendingCreatedExpected = matchedKeywords.isNotEmpty()
        )
    }

    private fun sourceName(packageName: String): String? = when (packageName) {
        WECHAT_PACKAGE -> "微信"
        ALIPAY_PACKAGE -> "支付宝"
        else -> null
    }

    private fun normalize(parts: List<String>): String =
        parts.joinToString("\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")

    private fun findAmountCandidates(text: String): List<AmountCandidate> {
        val candidates = mutableListOf<AmountCandidate>()
        amountPhraseRegex.findAll(text).forEach { match ->
            val rawNumber = match.groups["amount"]?.value ?: return@forEach
            val start = match.groups["amount"]?.range?.first ?: match.range.first
            val end = match.groups["amount"]?.range?.last ?: match.range.last
            val phrase = match.value
            val context = text.contextAround(start, end, 16)
            if (!phrase.hasAmountSignal() && !context.hasAmountSignal()) return@forEach
            if (looksLikeDateOrTime(text, start, end) || looksLikeLongIdentifier(rawNumber, text, start, end)) return@forEach
            val amountCents = rawNumber.toAmountCents() ?: return@forEach
            if (amountCents <= 0L) return@forEach

            val reasons = mutableListOf("金额短语")
            var score = 90
            if (phrase.any { it == '¥' || it == '￥' }) {
                score += 35
                reasons += "包含¥/￥"
            }
            if (phrase.contains("元")) {
                score += 25
                reasons += "包含元"
            }
            amountKeywordWeights.forEach { (keyword, weight) ->
                if (phrase.contains(keyword) || context.contains(keyword)) {
                    score += weight
                    reasons += "靠近$keyword"
                }
            }
            if (rawNumber.contains(".")) {
                score += 15
                reasons += "小数金额"
            }
            candidates += AmountCandidate(
                text = rawNumber.trim(),
                amountCents = amountCents,
                score = score,
                reason = reasons.distinct().joinToString("、"),
                startIndex = start
            )
        }

        splitDigitAmountRegex.findAll(text).forEach { match ->
            val keyword = match.groups["keyword"]?.value.orEmpty()
            val digits = match.groups["digits"]?.value?.filter { it.isDigit() }.orEmpty()
            if (digits.length < 2) return@forEach
            val amountCents = digits.toAmountCents() ?: return@forEach
            candidates += AmountCandidate(
                text = digits,
                amountCents = amountCents,
                score = 220,
                reason = "修复分隔数字金额、靠近$keyword",
                startIndex = match.range.first
            )
        }

        numberRegex.findAll(text).forEach { match ->
            val rawNumber = match.value
            val start = match.range.first
            val end = match.range.last
            if (looksLikeDateOrTime(text, start, end) || looksLikeLongIdentifier(rawNumber, text, start, end)) {
                return@forEach
            }
            val context = text.contextAround(start, end, 12)
            val amountCents = rawNumber.toAmountCents()
                ?: return@forEach
            if (amountCents <= 0L) return@forEach

            val reasons = mutableListOf<String>()
            var score = 0
            if (rawNumber.startsWith("-")) {
                score += 25
                reasons += "负数支出"
            }
            if (context.hasCurrencyBefore(start, text) || context.hasCurrencyAfter()) {
                score += 60
                reasons += "靠近¥/￥"
            }
            if (context.contains("元")) {
                score += 45
                reasons += "靠近元"
            }
            amountKeywordWeights.forEach { (keyword, weight) ->
                if (context.contains(keyword)) {
                    score += weight
                    reasons += "靠近$keyword"
                }
            }
            if (rawNumber.contains(".")) {
                score += 15
                reasons += "小数金额"
            }
            if (score >= MIN_AMOUNT_SCORE) {
                candidates += AmountCandidate(
                    text = rawNumber,
                    amountCents = amountCents,
                    score = score,
                    reason = reasons.distinct().joinToString("、").ifBlank { "疑似金额" },
                    startIndex = start
                )
            }
        }
        return candidates
            .distinctBy { it.startIndex to it.amountCents }
            .sortedWith(compareByDescending<AmountCandidate> { it.score }.thenBy { it.startIndex })
    }

    private fun String.toAmountCents(): Long? =
        replace("¥", "")
            .replace("￥", "")
            .replace("元", "")
            .replace(",", "")
            .trim()
            .toBigDecimalOrNull()
            ?.abs()
            ?.multiply(BigDecimal(100))
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toLong()

    private fun String.hasAmountSignal(): Boolean =
        any { it == '¥' || it == '￥' } ||
            contains("元") ||
            amountKeywordWeights.keys.any { contains(it) }

    private fun String.contextAround(start: Int, end: Int, radius: Int): String {
        val from = (start - radius).coerceAtLeast(0)
        val to = (end + radius + 1).coerceAtMost(length)
        return substring(from, to)
    }

    private fun String.hasCurrencyBefore(numberStart: Int, fullText: String): Boolean {
        val from = (numberStart - 3).coerceAtLeast(0)
        return fullText.substring(from, numberStart).any { it == '¥' || it == '￥' }
    }

    private fun String.hasCurrencyAfter(): Boolean = any { it == '¥' || it == '￥' }

    private fun looksLikeDateOrTime(text: String, start: Int, end: Int): Boolean {
        val before = text.getOrNull(start - 1)
        val after = text.getOrNull(end + 1)
        if (before in dateTimeSeparators || after in dateTimeSeparators) return true
        val context = text.contextAround(start, end, 4)
        return dateTimeContextRegex.containsMatchIn(context)
    }

    private fun looksLikeLongIdentifier(rawNumber: String, text: String, start: Int, end: Int): Boolean {
        val digits = rawNumber.filter { it.isDigit() }
        if (digits.length < 6) return false
        val context = text.contextAround(start, end, 8)
        val hasAmountSignal = amountKeywordWeights.keys.any { context.contains(it) } ||
            context.contains("¥") ||
            context.contains("￥") ||
            context.contains("元")
        return !hasAmountSignal
    }

    private fun detectType(text: String, selectedAmount: AmountCandidate?): TypeDetection {
        val content = text.replace("微信支付", "").replace("支付宝", "")
        val incomeMatches = incomeKeywords.filter { content.contains(it) }
        val expenseMatches = expenseKeywords.filter { content.contains(it) }
        if (incomeMatches.isEmpty() && expenseMatches.isEmpty()) {
            return TypeDetection(TransactionType.UNKNOWN, "未命中收入/支出关键词")
        }
        if (incomeMatches.isNotEmpty() && expenseMatches.isEmpty()) {
            return TypeDetection(TransactionType.INCOME, "命中收入关键词：${incomeMatches.joinToString("、")}")
        }
        if (expenseMatches.isNotEmpty() && incomeMatches.isEmpty()) {
            return TypeDetection(TransactionType.EXPENSE, "命中支出关键词：${expenseMatches.joinToString("、")}")
        }

        val amountIndex = selectedAmount?.startIndex ?: -1
        if (amountIndex >= 0) {
            val nearestIncome = nearestKeywordDistance(content, amountIndex, incomeMatches)
            val nearestExpense = nearestKeywordDistance(content, amountIndex, expenseMatches)
            if (nearestIncome < nearestExpense) {
                return TypeDetection(TransactionType.INCOME, "收入关键词更靠近金额：${incomeMatches.joinToString("、")}")
            }
            if (nearestExpense < nearestIncome) {
                return TypeDetection(TransactionType.EXPENSE, "支出关键词更靠近金额：${expenseMatches.joinToString("、")}")
            }
        }

        return TypeDetection(TransactionType.UNKNOWN, "收入和支出关键词同时出现，需用户确认")
    }

    private fun nearestKeywordDistance(text: String, amountIndex: Int, keywords: List<String>): Int =
        keywords.flatMap { keyword ->
            Regex(Regex.escape(keyword)).findAll(text).map { abs(it.range.first - amountIndex) }.toList()
        }.minOrNull() ?: Int.MAX_VALUE

    private fun detectMerchant(text: String): MerchantDetection {
        merchantPatterns.forEach { (pattern, reason) ->
            val match = pattern.find(text)
            val merchant = match?.groups?.get(1)?.value?.cleanMerchant()
            if (!merchant.isNullOrBlank()) {
                return MerchantDetection(merchant.take(40), reason)
            }
        }
        return MerchantDetection("", "未识别商户")
    }

    private fun String.cleanMerchant(): String =
        replace("\n", " ")
            .trim(' ', '：', ':', '，', ',', '。', '；', ';')
            .replace(Regex("""\s+"""), " ")

    private fun buildNote(sourceApp: String, merchantName: String, text: String): String {
        if (merchantName.isNotBlank()) return "${sourceApp}通知：$merchantName".take(80)
        val firstUsefulLine = text.lines().firstOrNull { line ->
            line.length >= 2 &&
                sourceNoise.none { line.contains(it) } &&
                !numberRegex.containsMatchIn(line)
        }
        return "${sourceApp}通知：${firstUsefulLine ?: "待确认账单"}".take(80)
    }

    private fun normalizeForFingerprint(text: String): String =
        text.lowercase()
            .replace(amountFingerprintRegex, "")
            .filter { it.isLetter() || it.isDigit() }

    private fun String.sha1(): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun ignored(
        reason: String,
        rawText: String,
        isPaymentNotification: Boolean = false,
        hasAmount: Boolean = false,
        diagnostics: NotificationParseDiagnostics = NotificationParseDiagnostics(rawText = rawText, failReason = reason)
    ): NotificationParseResult =
        NotificationParseResult(
            failureReason = reason,
            isPaymentNotification = isPaymentNotification,
            hasAmount = hasAmount,
            parseStatus = "ignored",
            parseReason = reason,
            diagnostics = diagnostics.copy(failReason = reason)
        )

    private data class TypeDetection(val type: TransactionType, val reason: String)
    private data class MerchantDetection(val name: String, val reason: String)

    companion object {
        private const val WECHAT_PACKAGE = "com.tencent.mm"
        private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
        private const val DUPLICATE_WINDOW_MILLIS = 60_000L
        private const val MIN_AMOUNT_SCORE = 40

        private val paymentKeywords = listOf(
            "支付成功",
            "付款成功",
            "交易成功",
            "已付款",
            "已支付",
            "扣款",
            "消费",
            "收款",
            "到账",
            "转账",
            "微信支付",
            "支付宝"
        )
        private val incomeKeywords = listOf("收款", "到账", "入账", "收到转账", "已收款")
        private val expenseKeywords = listOf("付款", "支付", "扣款", "消费", "已付款", "支付成功", "实付", "支出")
        private val sourceNoise = listOf("微信支付", "微信", "支付宝", "服务通知", "支付助手", "交易提醒")
        private val amountKeywordWeights = linkedMapOf(
            "付款" to 45,
            "支付" to 45,
            "实付" to 55,
            "交易金额" to 65,
            "金额" to 50,
            "扣款" to 45,
            "消费" to 45,
            "支出" to 45,
            "收款" to 35,
            "到账" to 35
        )
        private val amountPhraseRegex = Regex(
            """(?:(?:支付|付款|实付|消费|支出|扣款|交易金额|金额)\s*[:：]?\s*)?(?<amount>[¥￥]?\s*-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?\s*元?)"""
        )
        private val splitDigitAmountRegex = Regex(
            """(?<keyword>支付|付款|实付|消费|支出|扣款|交易金额|金额)\s*(?:成功|完成)?\s*[:：]?\s*(?<digits>[0-9](?:\s+[0-9]){1,3})\s*元?"""
        )
        private val numberRegex = Regex("""-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?""")
        private val amountFingerprintRegex = Regex("""[¥￥]?\s*-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?\s*元?""")
        private val dateTimeSeparators = setOf('-', '/', ':', '年', '月', '日')
        private val dateTimeContextRegex = Regex("""\d{1,4}[-/:年月日]\d{1,2}|\d{1,2}[-/:年月日]\d{1,4}""")
        private val merchantPatterns = listOf(
            Regex("""向\s*([^\n，,。；;]{2,40}?)\s*付款""") to "命中模式：向 XXX 付款",
            Regex("""付款给\s*([^\n，,。；;]{2,40})""") to "命中模式：付款给 XXX",
            Regex("""商户\s*[:：]\s*([^\n，,。；;]{2,40})""") to "命中模式：商户：XXX",
            Regex("""收款方\s*[:：]\s*([^\n，,。；;]{2,40})""") to "命中模式：收款方：XXX",
            Regex("""对方账户\s*[:：]\s*([^\n，,。；;]{2,40})""") to "命中模式：对方账户：XXX",
            Regex("""([^\n，,。；;]{2,40}?)\s*收款成功""") to "命中模式：XXX 收款成功",
            Regex("""你在\s*([^\n，,。；;]{2,40}?)\s*有一笔""") to "命中模式：你在 XXX 有一笔"
        )
    }
}
