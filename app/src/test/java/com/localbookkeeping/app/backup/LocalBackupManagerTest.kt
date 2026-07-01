package com.localbookkeeping.app.backup

import com.localbookkeeping.app.data.ExpenseRecord
import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBackupManagerTest {
    @Test
    fun jsonBackupRoundTrip_preservesRecordFieldsAndClearsIds() {
        val source = sampleRecord(
            id = 42L,
            amountCents = 1280L,
            merchantName = "Meituan",
            notificationFingerprint = "fp-1"
        )

        val json = LocalBackupManager.buildJsonBackup(listOf(source), exportedAtMillis = 1_700_000_000_000L)
        val restored = LocalBackupManager.parseJsonBackup(json)

        assertEquals(1, restored.size)
        assertEquals(0L, restored.first().id)
        assertEquals(source.amountCents, restored.first().amountCents)
        assertEquals(source.type, restored.first().type)
        assertEquals(source.category, restored.first().category)
        assertEquals(source.merchantName, restored.first().merchantName)
        assertEquals(source.notificationFingerprint, restored.first().notificationFingerprint)
    }

    @Test
    fun csvBackup_quotesCommasQuotesAndLineBreaks() {
        val record = sampleRecord(
            note = "coffee, \"latte\"\nsecond line",
            rawText = "paid, ok"
        )

        val csv = LocalBackupManager.buildCsv(listOf(record))

        assertTrue(csv.startsWith("id,amountYuan,type,status"))
        assertTrue(csv.contains("\"coffee, \"\"latte\"\"\nsecond line\""))
        assertTrue(csv.contains("\"paid, ok\""))
    }

    @Test
    fun parseJsonBackup_ignoresMissingRecordsArray() {
        val restored = LocalBackupManager.parseJsonBackup("""{"schemaVersion":1}""")

        assertEquals(emptyList<ExpenseRecord>(), restored)
    }

    private fun sampleRecord(
        id: Long = 1L,
        amountCents: Long = 990L,
        note: String = "Lunch",
        rawText: String = "Payment 9.90",
        merchantName: String = "Shop",
        notificationFingerprint: String = ""
    ): ExpenseRecord {
        return ExpenseRecord(
            id = id,
            amountCents = amountCents,
            type = TransactionType.EXPENSE,
            status = RecordStatus.CONFIRMED,
            category = "Food",
            note = note,
            paidAtMillis = 1_700_000_000_000L,
            sourceApp = "WeChat",
            rawText = rawText,
            notificationTitle = "Payment",
            notificationText = "Paid",
            notificationPackageName = "com.tencent.mm",
            notificationPostedAtMillis = 1_700_000_000_000L,
            notificationFingerprint = notificationFingerprint,
            merchantName = merchantName,
            confidence = 90,
            matchedRuleName = "Food default",
            categorySource = "rule",
            matchedKeyword = "Meituan",
            normalizedRawText = rawText.lowercase(),
            sourceType = "notification",
            createdAtMillis = 1_700_000_000_000L,
            updatedAtMillis = 1_700_000_000_000L
        )
    }
}
