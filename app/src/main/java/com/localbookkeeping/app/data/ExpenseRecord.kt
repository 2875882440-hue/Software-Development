package com.localbookkeeping.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_records")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val type: TransactionType,
    val status: RecordStatus = RecordStatus.CONFIRMED,
    val category: String,
    val note: String,
    val paidAtMillis: Long,
    val sourceApp: String = "手动",
    val rawText: String = "",
    val notificationTitle: String = "",
    val notificationText: String = "",
    val notificationPackageName: String = "",
    val notificationPostedAtMillis: Long = 0L,
    val notificationFingerprint: String = "",
    val merchantName: String = "",
    val confidence: Int = 0,
    val matchedRuleName: String = "",
    val categorySource: String = "unknown",
    val matchedKeyword: String = "",
    val learnedMerchantId: Long = 0L,
    val normalizedRawText: String = "",
    val imageUri: String = "",
    val screenshotPath: String = "",
    val ocrText: String = "",
    val sourceType: String = "notification",
    val isDeleted: Boolean = false,
    val deletedAtMillis: Long = 0L,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

enum class TransactionType {
    EXPENSE,
    INCOME,
    UNKNOWN
}

enum class RecordStatus {
    PENDING_CONFIRM,
    NEED_AMOUNT,
    PENDING,
    CONFIRMED,
    IGNORED
}

object ExpenseCategories {
    val expense = listOf("餐饮", "交通", "购物", "娱乐", "生活", "医疗", "住房", "其他", "未分类")
    val income = listOf("收入", "工资", "兼职", "理财", "转账", "红包", "退款", "其他")
}
