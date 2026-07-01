package com.localbookkeeping.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classification_rules")
data class ClassificationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleName: String = "",
    val keyword: String,
    val category: String,
    val type: TransactionType,
    val enabled: Boolean = true,
    val priority: Int = 100,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
