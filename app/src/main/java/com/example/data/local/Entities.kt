package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PartyType {
    CUSTOMER,
    SUPPLIER
}

enum class TransactionType {
    DUE_ADDED,         // Customer: পাওনা যোগ (increases receivable)
    PAYMENT_RECEIVED,  // Customer: টাকা আদায় (decreases receivable)
    GOODS_RECEIVED,    // Supplier: মালামাল/বাকি গ্রহণ (increases payable)
    SUPPLIER_PAYMENT   // Supplier: সাপ্লায়ার প্রদান (decreases payable)
}

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val tagline: String = "বিশ্বস্ত ফার্মেসি ও হিসাব ব্যবস্থা",
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [
        Index("shopId"),
        Index("isDeleted"),
        Index("customerCode")
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val customerCode: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val photoUri: String? = null,
    val currentDue: Double = 0.0,
    val totalDueAdded: Double = 0.0,
    val totalPaid: Double = 0.0,
    val lastTransactionAt: Long? = null,
    val lastPaymentAt: Long? = null,
    val lastReminderSentAt: Long? = null,
    val reminderCount: Int = 0,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "suppliers",
    indices = [
        Index("shopId"),
        Index("isDeleted"),
        Index("supplierCode")
    ]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val supplierCode: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val photoUri: String? = null,
    val currentPayable: Double = 0.0,
    val totalGoodsReceived: Double = 0.0,
    val totalPaid: Double = 0.0,
    val lastTransactionAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ledger_transactions",
    indices = [
        Index("shopId"),
        Index("partyType"),
        Index("partyId"),
        Index("transactionDate"),
        Index("isDeleted"),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class LedgerTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val receiptNumber: String,
    val partyType: PartyType,
    val partyId: Long,
    val partyCode: String,
    val partyName: String,
    val partyPhone: String,
    val type: TransactionType,
    val amount: Double,
    val previousBalance: Double,
    val newBalance: Double,
    val note: String = "",
    val transactionDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "audit_logs",
    indices = [Index("shopId"), Index("timestamp")]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val action: String,
    val recordType: String,
    val recordTitle: String,
    val previousValue: String,
    val newValue: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "backup_records",
    indices = [Index("shopId"), Index("createdAt")]
)
data class BackupRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val shopName: String,
    val fileName: String,
    val filePath: String,
    val backupType: String, // "MANUAL" or "AUTO"
    val customerCount: Int,
    val supplierCount: Int,
    val transactionCount: Int,
    val sizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)
