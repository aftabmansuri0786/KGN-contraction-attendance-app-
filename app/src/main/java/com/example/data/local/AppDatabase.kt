package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AdvanceEntity
import com.example.data.model.AmavasyaConfigEntity
import com.example.data.model.AmavasyaSettlementEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.MonthlyPdfEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.SettlementAuditEntity
import com.example.data.model.SiteEntity
import com.example.data.model.WorkerEntity

@Database(
    entities = [
        WorkerEntity::class,
        SiteEntity::class,
        AttendanceEntity::class,
        AdvanceEntity::class,
        PaymentEntity::class,
        AmavasyaConfigEntity::class,
        AmavasyaSettlementEntity::class,
        SettlementAuditEntity::class,
        MonthlyPdfEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun siteDao(): SiteDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun advanceDao(): AdvanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun amavasyaConfigDao(): AmavasyaConfigDao
    abstract fun amavasyaSettlementDao(): AmavasyaSettlementDao
    abstract fun settlementAuditDao(): SettlementAuditDao
    abstract fun monthlyPdfDao(): MonthlyPdfDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `amavasya_config` (
                        `id` TEXT NOT NULL,
                        `selectedSettlementDate` TEXT NOT NULL,
                        `isManualDate` INTEGER NOT NULL,
                        `reminderDaysBefore` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `amavasya_settlements` (
                        `id` TEXT NOT NULL,
                        `workerId` TEXT NOT NULL,
                        `workerName` TEXT NOT NULL,
                        `periodStartDate` TEXT NOT NULL,
                        `periodEndDate` TEXT NOT NULL,
                        `presentDays` INTEGER NOT NULL,
                        `halfDays` INTEGER NOT NULL,
                        `absentDays` INTEGER NOT NULL,
                        `paidLeaveDays` INTEGER NOT NULL,
                        `payableManDays` REAL NOT NULL,
                        `totalWages` REAL NOT NULL,
                        `overtimeHours` REAL NOT NULL,
                        `overtimeAmount` REAL NOT NULL,
                        `totalEarnings` REAL NOT NULL,
                        `previousBalance` REAL NOT NULL,
                        `advancesTaken` REAL NOT NULL,
                        `paymentsMade` REAL NOT NULL,
                        `netBalance` REAL NOT NULL,
                        `payableAmount` REAL NOT NULL,
                        `outstandingAdvanceAmount` REAL NOT NULL,
                        `carryForwardBalance` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `isLocked` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `settledAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_amavasya_settlements_workerId` ON `amavasya_settlements` (`workerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_amavasya_settlements_periodEndDate` ON `amavasya_settlements` (`periodEndDate`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_amavasya_settlements_workerId_periodEndDate` ON `amavasya_settlements` (`workerId`, `periodEndDate`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `settlement_audits` (
                        `id` TEXT NOT NULL,
                        `settlementId` TEXT NOT NULL,
                        `workerId` TEXT NOT NULL,
                        `workerName` TEXT NOT NULL,
                        `action` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `previousNetBalance` REAL NOT NULL,
                        `newNetBalance` REAL NOT NULL,
                        `performedBy` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlement_audits_settlementId` ON `settlement_audits` (`settlementId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlement_audits_workerId` ON `settlement_audits` (`workerId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workers` ADD COLUMN `photoUrl` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `workers` ADD COLUMN `photoStoragePath` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `workers` ADD COLUMN `localPhotoPath` TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monthly_hisab_pdfs` (
                        `id` TEXT NOT NULL,
                        `workerId` TEXT NOT NULL,
                        `workerName` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `monthLabel` TEXT NOT NULL,
                        `periodStartDate` TEXT NOT NULL,
                        `periodEndDate` TEXT NOT NULL,
                        `isCombinedReport` INTEGER NOT NULL,
                        `localFilePath` TEXT NOT NULL,
                        `storagePath` TEXT NOT NULL,
                        `downloadUrl` TEXT NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `generatedAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_monthly_hisab_pdfs_year_month` ON `monthly_hisab_pdfs` (`year`, `month`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_monthly_hisab_pdfs_workerId_periodEndDate` ON `monthly_hisab_pdfs` (`workerId`, `periodEndDate`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kgn_construction.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

