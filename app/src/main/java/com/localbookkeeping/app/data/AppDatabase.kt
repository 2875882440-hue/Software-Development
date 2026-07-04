package com.localbookkeeping.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExpenseRecord::class,
        DebugNotificationLog::class,
        ClassificationRule::class,
        BackgroundStabilityLog::class,
        MerchantCategoryLearning::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun debugNotificationLogDao(): DebugNotificationLogDao
    abstract fun classificationRuleDao(): ClassificationRuleDao
    abstract fun backgroundStabilityLogDao(): BackgroundStabilityLogDao
    abstract fun merchantCategoryLearningDao(): MerchantCategoryLearningDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun create(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookkeeping-v01.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                ).build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expense_records ADD COLUMN status TEXT NOT NULL DEFAULT 'CONFIRMED'")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN sourceApp TEXT NOT NULL DEFAULT '手动'")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN rawText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN notificationFingerprint TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expense_records ADD COLUMN notificationTitle TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN notificationText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN notificationPackageName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN notificationPostedAtMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS debug_notification_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        title TEXT NOT NULL,
                        text TEXT NOT NULL,
                        subText TEXT NOT NULL,
                        bigText TEXT NOT NULL,
                        textLines TEXT NOT NULL,
                        rawText TEXT NOT NULL,
                        postTime INTEGER NOT NULL,
                        receivedAtMillis INTEGER NOT NULL,
                        parseStatus TEXT NOT NULL,
                        failureReason TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE expense_records SET status = 'PENDING_CONFIRM' WHERE status = 'PENDING'")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN isPaymentNotification INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN hasAmount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN pendingCreated INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN parseReason TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expense_records ADD COLUMN imageUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN screenshotPath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN ocrText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'notification'")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expense_records ADD COLUMN merchantName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN confidence INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN matchedRuleName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN normalizedRawText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN ruleMatched INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN matchedRuleName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN confidence INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN finalCategory TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS classification_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        keyword TEXT NOT NULL,
                        category TEXT NOT NULL,
                        type TEXT NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN isFromPaymentApp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN isPaymentRelated INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN isParsed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN failReason TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN serviceAlive INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN appInBackground INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN screenLocked INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS background_stability_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventType TEXT NOT NULL,
                        message TEXT NOT NULL,
                        detail TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expense_records ADD COLUMN categorySource TEXT NOT NULL DEFAULT 'unknown'")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN matchedKeyword TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE expense_records ADD COLUMN deletedAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE classification_rules ADD COLUMN ruleName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE classification_rules ADD COLUMN priority INTEGER NOT NULL DEFAULT 100")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expense_records ADD COLUMN learnedMerchantId INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS merchant_category_learning (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        merchantNormalized TEXT NOT NULL,
                        merchantDisplayName TEXT NOT NULL,
                        category TEXT NOT NULL,
                        sourceApp TEXT NOT NULL DEFAULT 'unknown',
                        matchKeyword TEXT NOT NULL DEFAULT '',
                        useCount INTEGER NOT NULL DEFAULT 1,
                        confidence INTEGER NOT NULL DEFAULT 50,
                        lastUsedAt INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_merchant_category_learning_merchantNormalized_category_sourceApp ON merchant_category_learning(merchantNormalized, category, sourceApp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_merchant_category_learning_merchantNormalized ON merchant_category_learning(merchantNormalized)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_merchant_category_learning_lastUsedAt ON merchant_category_learning(lastUsedAt)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN notificationKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN amountCandidates TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN selectedAmount TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE debug_notification_logs ADD COLUMN selectedReason TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
