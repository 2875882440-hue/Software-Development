package com.localbookkeeping.app.backup

import android.content.Context
import android.net.Uri
import com.localbookkeeping.app.data.ExpenseRecord
import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object LocalBackupManager {
    private const val PREFS_NAME = "local_backup_state"
    private const val KEY_LAST_BACKUP_AT = "last_backup_at"
    private const val BACKUP_SCHEMA_VERSION = 1

    fun buildJsonBackup(records: List<ExpenseRecord>, exportedAtMillis: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
            .put("schemaVersion", BACKUP_SCHEMA_VERSION)
            .put("app", "LocalBookkeeping")
            .put("exportedAtMillis", exportedAtMillis)
            .put("recordCount", records.size)
        val items = JSONArray()
        records.forEach { items.put(it.toJson()) }
        root.put("records", items)
        return root.toString(2)
    }

    fun buildCsv(records: List<ExpenseRecord>): String {
        val headers = listOf(
            "id",
            "amountYuan",
            "type",
            "status",
            "category",
            "merchantName",
            "note",
            "paidAt",
            "sourceApp",
            "sourceType",
            "isDeleted",
            "rawText"
        )
        return buildString {
            appendLine(headers.joinToString(","))
            records.forEach { record ->
                appendLine(
                    listOf(
                        record.id.toString(),
                        formatYuan(record.amountCents),
                        record.type.name,
                        record.status.name,
                        record.category,
                        record.merchantName,
                        record.note,
                        formatMillis(record.paidAtMillis),
                        record.sourceApp,
                        record.sourceType,
                        record.isDeleted.toString(),
                        record.rawText
                    ).joinToString(",") { csvCell(it) }
                )
            }
        }
    }

    fun parseJsonBackup(text: String): List<ExpenseRecord> {
        val root = JSONObject(text)
        val records = root.optJSONArray("records") ?: JSONArray()
        return buildList {
            for (index in 0 until records.length()) {
                val item = records.optJSONObject(index) ?: continue
                add(item.toExpenseRecord())
            }
        }
    }

    fun writeText(context: Context, uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("无法打开备份文件")
    }

    fun readText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("无法读取备份文件")

    fun markBackupCompleted(context: Context, timestampMillis: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP_AT, timestampMillis)
            .apply()
    }

    fun lastBackupAt(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP_AT, 0L)

    fun defaultJsonFileName(nowMillis: Long = System.currentTimeMillis()): String =
        "LocalBookkeeping-backup-${fileTimestamp(nowMillis)}.json"

    fun defaultCsvFileName(nowMillis: Long = System.currentTimeMillis()): String =
        "LocalBookkeeping-records-${fileTimestamp(nowMillis)}.csv"

    private fun ExpenseRecord.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("amountCents", amountCents)
            .put("type", type.name)
            .put("status", status.name)
            .put("category", category)
            .put("note", note)
            .put("paidAtMillis", paidAtMillis)
            .put("sourceApp", sourceApp)
            .put("rawText", rawText)
            .put("notificationTitle", notificationTitle)
            .put("notificationText", notificationText)
            .put("notificationPackageName", notificationPackageName)
            .put("notificationPostedAtMillis", notificationPostedAtMillis)
            .put("notificationFingerprint", notificationFingerprint)
            .put("merchantName", merchantName)
            .put("confidence", confidence)
            .put("matchedRuleName", matchedRuleName)
            .put("categorySource", categorySource)
            .put("matchedKeyword", matchedKeyword)
            .put("learnedMerchantId", learnedMerchantId)
            .put("normalizedRawText", normalizedRawText)
            .put("imageUri", imageUri)
            .put("screenshotPath", screenshotPath)
            .put("ocrText", ocrText)
            .put("sourceType", sourceType)
            .put("isDeleted", isDeleted)
            .put("deletedAtMillis", deletedAtMillis)
            .put("createdAtMillis", createdAtMillis)
            .put("updatedAtMillis", updatedAtMillis)

    private fun formatYuan(amountCents: Long): String =
        java.lang.String.format(java.util.Locale.US, "%.2f", amountCents / 100.0)

    private fun JSONObject.toExpenseRecord(): ExpenseRecord {
        val now = System.currentTimeMillis()
        return ExpenseRecord(
            id = 0L,
            amountCents = optLong("amountCents", 0L),
            type = enumValueOrDefault(optString("type"), TransactionType.UNKNOWN),
            status = enumValueOrDefault(optString("status"), RecordStatus.CONFIRMED),
            category = optString("category"),
            note = optString("note"),
            paidAtMillis = optLong("paidAtMillis", now),
            sourceApp = optString("sourceApp", "手动"),
            rawText = optString("rawText"),
            notificationTitle = optString("notificationTitle"),
            notificationText = optString("notificationText"),
            notificationPackageName = optString("notificationPackageName"),
            notificationPostedAtMillis = optLong("notificationPostedAtMillis", 0L),
            notificationFingerprint = optString("notificationFingerprint"),
            merchantName = optString("merchantName"),
            confidence = optInt("confidence", 0),
            matchedRuleName = optString("matchedRuleName"),
            categorySource = optString("categorySource", "unknown"),
            matchedKeyword = optString("matchedKeyword"),
            learnedMerchantId = optLong("learnedMerchantId", 0L),
            normalizedRawText = optString("normalizedRawText"),
            imageUri = optString("imageUri"),
            screenshotPath = optString("screenshotPath"),
            ocrText = optString("ocrText"),
            sourceType = optString("sourceType", "notification"),
            isDeleted = optBoolean("isDeleted", false),
            deletedAtMillis = optLong("deletedAtMillis", 0L),
            createdAtMillis = optLong("createdAtMillis", now).takeIf { it > 0L } ?: now,
            updatedAtMillis = optLong("updatedAtMillis", now).takeIf { it > 0L } ?: now
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    private fun csvCell(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace("\r", "\n")
        return if (normalized.any { it == ',' || it == '"' || it == '\n' }) {
            "\"${normalized.replace("\"", "\"\"")}\""
        } else {
            normalized
        }
    }

    private fun formatMillis(millis: Long): String =
        if (millis <= 0L) "" else Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    private fun fileTimestamp(millis: Long): String =
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
}
