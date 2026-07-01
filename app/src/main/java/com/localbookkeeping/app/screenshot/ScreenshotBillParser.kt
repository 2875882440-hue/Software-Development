package com.localbookkeeping.app.screenshot

import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ParsedScreenshotBill(
    val amountCents: Long?,
    val type: TransactionType,
    val status: RecordStatus,
    val sourceType: String,
    val sourceApp: String,
    val merchant: String,
    val paidAtMillis: Long?,
    val rawText: String,
    val fingerprint: String,
    val hasAmount: Boolean,
    val isPaymentScreenshot: Boolean,
    val parseStatus: String,
    val parseReason: String
)

data class ScreenshotParseResult(
    val bill: ParsedScreenshotBill? = null,
    val failureReason: String = "",
    val sourceType: String = "unknown_screenshot",
    val isPaymentScreenshot: Boolean = false,
    val hasAmount: Boolean = false,
    val parseStatus: String = "screenshot_ignored",
    val parseReason: String = failureReason
)

class ScreenshotBillParser {
    fun parse(rawText: String, imageUri: String = "", nowMillis: Long = System.currentTimeMillis()): ScreenshotParseResult {
        val normalized = normalize(rawText)
        if (normalized.isBlank()) {
            return ignored("OCR原文为空")
        }

        val platform = detectPlatform(normalized)
        val sourceType = platform?.sourceType ?: "unknown_screenshot"
        val sourceApp = platform?.sourceApp ?: "未知截图"
        val amountCents = findAmountCents(normalized)
        val hasAmount = amountCents != null
        val isPaymentScreenshot = platform != null || paymentKeywords.any { normalized.contains(it) } || hasAmount

        if (!isPaymentScreenshot) {
            return ignored(
                reason = "未识别到有效账单信息",
                sourceType = sourceType
            )
        }

        val status = if (hasAmount) RecordStatus.PENDING_CONFIRM else RecordStatus.NEED_AMOUNT
        val parseStatus = if (hasAmount) "screenshot_pending_confirm" else "screenshot_need_amount"
        val parseReason = if (hasAmount) {
            "截图OCR已识别金额，待确认"
        } else {
            "截图疑似支付账单，但未识别金额"
        }

        return ScreenshotParseResult(
            bill = ParsedScreenshotBill(
                amountCents = amountCents,
                type = detectType(normalized),
                status = status,
                sourceType = sourceType,
                sourceApp = sourceApp,
                merchant = detectMerchant(normalized),
                paidAtMillis = detectTimeMillis(normalized),
                rawText = normalized,
                fingerprint = "$sourceType|$imageUri|${normalized.take(500)}".sha1(),
                hasAmount = hasAmount,
                isPaymentScreenshot = true,
                parseStatus = parseStatus,
                parseReason = parseReason
            ),
            sourceType = sourceType,
            isPaymentScreenshot = true,
            hasAmount = hasAmount,
            parseStatus = parseStatus,
            parseReason = parseReason
        )
    }

    private fun detectPlatform(text: String): ScreenshotPlatform? = when {
        alipayKeywords.any { text.contains(it) } -> ScreenshotPlatform("alipay_screenshot", "支付宝截图")
        wechatKeywords.any { text.contains(it) } -> ScreenshotPlatform("wechat_screenshot", "微信截图")
        else -> null
    }

    private fun findAmountCents(text: String): Long? {
        val amountText = amountPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groups?.get(1)?.value?.replace(",", "")
        }
        return amountText
            ?.toBigDecimalOrNull()
            ?.abs()
            ?.multiply(BigDecimal(100))
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toLong()
    }

    private fun detectType(text: String): TransactionType {
        val income = incomeKeywords.any { text.contains(it) }
        val expense = expenseKeywords.any { text.contains(it) } || text.contains(Regex("""-\s*\d"""))
        return when {
            income && !expense -> TransactionType.INCOME
            expense && !income -> TransactionType.EXPENSE
            income && expense -> TransactionType.EXPENSE
            else -> TransactionType.UNKNOWN
        }
    }

    private fun detectMerchant(text: String): String {
        merchantPatterns.forEach { pattern ->
            val merchant = pattern.find(text)?.groups?.get(1)?.value?.trim()
            if (!merchant.isNullOrBlank()) return merchant.take(40)
        }
        return text.lines()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length in 2..40 &&
                    amountPatterns.none { it.containsMatchIn(line) } &&
                    platformNoise.none { line.contains(it) } &&
                    timePatterns.none { it.containsMatchIn(line) }
            }
            .orEmpty()
    }

    private fun detectTimeMillis(text: String): Long? {
        timePatterns.forEach { pattern ->
            val value = pattern.find(text)?.value ?: return@forEach
            parseDateTime(value)?.let { return it }
        }
        return null
    }

    private fun parseDateTime(value: String): Long? {
        val normalized = value.replace("/", "-").trim()
        val zone = ZoneId.systemDefault()
        dateTimeFormatters.forEach { formatter ->
            runCatching {
                return LocalDateTime.parse(normalized, formatter)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }
        }
        dateFormatters.forEach { formatter ->
            runCatching {
                return LocalDate.parse(normalized, formatter)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            }
        }
        return null
    }

    private fun normalize(text: String): String =
        text.replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")

    private fun ignored(reason: String, sourceType: String = "unknown_screenshot"): ScreenshotParseResult =
        ScreenshotParseResult(
            failureReason = reason,
            sourceType = sourceType,
            parseReason = reason
        )

    private fun String.sha1(): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private data class ScreenshotPlatform(val sourceType: String, val sourceApp: String)

    companion object {
        private val wechatKeywords = listOf("微信支付", "微信", "支付成功")
        private val alipayKeywords = listOf("支付宝", "交易成功", "账单详情")
        private val paymentKeywords = listOf("支付", "付款", "扣款", "消费", "交易", "收款", "到账", "转账", "红包", "扫码", "商户", "实付")
        private val incomeKeywords = listOf("收款", "到账", "收到", "入账")
        private val expenseKeywords = listOf("支付", "付款", "扣款", "消费", "交易成功", "实付")
        private val platformNoise = listOf("微信支付", "微信", "支付宝", "交易成功", "账单详情", "支付成功", "付款成功")
        private val amountPatterns = listOf(
            Regex("""[¥￥]\s*(-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)"""),
            Regex("""(-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*元"""),
            Regex("""(?:支付金额|付款|实付)\s*[:：]?\s*[¥￥]?\s*(-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)"""),
            Regex("""(?:^|\s)(-[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)(?:$|\s)""")
        )
        private val merchantPatterns = listOf(
            Regex("""(?:商户|商家|收款方|对方|付款给|转账给|商品)\s*[:：]?\s*([^\n]+)""")
        )
        private val timePatterns = listOf(
            Regex("""\d{4}[-/]\d{1,2}[-/]\d{1,2}\s+\d{1,2}:\d{2}(?::\d{2})?"""),
            Regex("""\d{4}[-/]\d{1,2}[-/]\d{1,2}""")
        )
        private val dateTimeFormatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm")
        )
        private val dateFormatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-M-d")
        )
    }
}
