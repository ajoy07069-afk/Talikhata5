package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ShopEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        LedgerTransactionEntity::class,
        AuditLogEntity::class,
        BackupRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TalikhataDatabase : RoomDatabase() {
    abstract fun talikhataDao(): TalikhataDao

    companion object {
        @Volatile
        private var INSTANCE: TalikhataDatabase? = null

        fun getInstance(context: Context): TalikhataDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TalikhataDatabase::class.java,
                    "talikhata_amol_pharmacy.db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
