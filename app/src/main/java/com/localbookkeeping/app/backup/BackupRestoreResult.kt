package com.localbookkeeping.app.backup

data class BackupRestoreResult(
    val totalCount: Int,
    val importedCount: Int,
    val skippedCount: Int
)
