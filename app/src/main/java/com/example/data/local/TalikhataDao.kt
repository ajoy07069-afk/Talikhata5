package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Dao
interface TalikhataDao {

    // --- SHOPS ---
    @Query("SELECT * FROM shops ORDER BY createdAt ASC")
    fun observeAllShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE isActive = 1 LIMIT 1")
    fun observeActiveShop(): Flow<ShopEntity?>

    @Query("SELECT * FROM shops WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveShopOnce(): ShopEntity?

    @Query("SELECT * FROM shops ORDER BY createdAt ASC")
    suspend fun getAllShopsOnce(): List<ShopEntity>

    @Query("SELECT * FROM shops WHERE id = :shopId LIMIT 1")
    suspend fun getShopById(shopId: Long): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity): Long

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Query("UPDATE shops SET isActive = CASE WHEN id = :shopId THEN 1 ELSE 0 END")
    suspend fun setActiveShopFlag(shopId: Long)

    @Query("DELETE FROM shops WHERE id = :shopId")
    suspend fun deleteShopById(shopId: Long)

    @Query("DELETE FROM customers WHERE shopId = :shopId")
    suspend fun deleteAllCustomersForShop(shopId: Long)

    @Query("DELETE FROM suppliers WHERE shopId = :shopId")
    suspend fun deleteAllSuppliersForShop(shopId: Long)

    @Query("DELETE FROM ledger_transactions WHERE shopId = :shopId")
    suspend fun deleteAllTransactionsForShop(shopId: Long)

    @Query("DELETE FROM audit_logs WHERE shopId = :shopId")
    suspend fun deleteAllAuditLogsForShop(shopId: Long)

    @Query("DELETE FROM backup_records WHERE shopId = :shopId")
    suspend fun deleteAllBackupRecordsForShop(shopId: Long)

    // --- CUSTOMERS ---
    @Query("SELECT * FROM customers WHERE shopId = :shopId AND isDeleted = 0 ORDER BY name ASC")
    fun observeCustomers(shopId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE shopId = :shopId AND isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeDeletedCustomers(shopId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    fun observeCustomerById(customerId: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE shopId = :shopId AND isDeleted = 0")
    suspend fun getActiveCustomersOnce(shopId: Long): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE shopId = :shopId")
    suspend fun getAllCustomersForShopOnce(shopId: Long): List<CustomerEntity>

    @Query("SELECT COUNT(*) FROM customers WHERE shopId = :shopId")
    suspend fun countAllCustomersForShop(shopId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun hardDeleteCustomer(customerId: Long)

    // --- SUPPLIERS ---
    @Query("SELECT * FROM suppliers WHERE shopId = :shopId AND isDeleted = 0 ORDER BY name ASC")
    fun observeSuppliers(shopId: Long): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE shopId = :shopId AND isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeDeletedSuppliers(shopId: Long): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :supplierId LIMIT 1")
    fun observeSupplierById(supplierId: Long): Flow<SupplierEntity?>

    @Query("SELECT * FROM suppliers WHERE id = :supplierId LIMIT 1")
    suspend fun getSupplierById(supplierId: Long): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE shopId = :shopId")
    suspend fun getAllSuppliersForShopOnce(shopId: Long): List<SupplierEntity>

    @Query("SELECT COUNT(*) FROM suppliers WHERE shopId = :shopId")
    suspend fun countAllSuppliersForShop(shopId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers WHERE id = :supplierId")
    suspend fun hardDeleteSupplier(supplierId: Long)

    // --- TRANSACTIONS ---
    @Query("SELECT * FROM ledger_transactions WHERE shopId = :shopId AND isDeleted = 0 ORDER BY transactionDate DESC, id DESC")
    fun observeTransactions(shopId: Long): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE shopId = :shopId AND isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeDeletedTransactions(shopId: Long): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE shopId = :shopId AND partyType = :partyType AND partyId = :partyId AND isDeleted = 0 ORDER BY transactionDate ASC, id ASC")
    fun observePartyTransactionsAsc(shopId: Long, partyType: PartyType, partyId: Long): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE shopId = :shopId AND partyType = :partyType AND partyId = :partyId AND isDeleted = 0 ORDER BY transactionDate ASC, id ASC")
    suspend fun getPartyTransactionsAscOnce(shopId: Long, partyType: PartyType, partyId: Long): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE shopId = :shopId AND isDeleted = 0")
    suspend fun getActiveTransactionsOnce(shopId: Long): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE shopId = :shopId")
    suspend fun getAllTransactionsForShopOnce(shopId: Long): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: Long): LedgerTransactionEntity?

    @Query("SELECT COUNT(*) FROM ledger_transactions WHERE idempotencyKey = :key")
    suspend fun countByIdempotencyKey(key: String): Int

    @Query("SELECT COUNT(*) FROM ledger_transactions WHERE shopId = :shopId")
    suspend fun countAllTransactionsForShop(shopId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: LedgerTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<LedgerTransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: LedgerTransactionEntity)

    @Query("UPDATE ledger_transactions SET partyName = :name, partyPhone = :phone WHERE shopId = :shopId AND partyType = :partyType AND partyId = :partyId")
    suspend fun syncPartyMetadataOnTransactions(shopId: Long, partyType: PartyType, partyId: Long, name: String, phone: String)

    @Query("DELETE FROM ledger_transactions WHERE id = :transactionId")
    suspend fun hardDeleteTransaction(transactionId: Long)

    @Query("DELETE FROM ledger_transactions WHERE shopId = :shopId AND partyType = :partyType AND partyId = :partyId")
    suspend fun hardDeleteTransactionsForParty(shopId: Long, partyType: PartyType, partyId: Long)

    // --- AUDIT LOGS ---
    @Query("SELECT * FROM audit_logs WHERE shopId = :shopId ORDER BY timestamp DESC")
    fun observeAuditLogs(shopId: Long): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogsAcrossAllShopsOnce(): List<AuditLogEntity>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersAcrossAllShopsOnce(): List<CustomerEntity>

    @Query("SELECT * FROM suppliers")
    suspend fun getAllSuppliersAcrossAllShopsOnce(): List<SupplierEntity>

    @Query("SELECT * FROM ledger_transactions")
    suspend fun getAllTransactionsAcrossAllShopsOnce(): List<LedgerTransactionEntity>

    @Query("DELETE FROM ledger_transactions")
    suspend fun deleteAllTransactionsAllShops()

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomersAllShops()

    @Query("DELETE FROM suppliers")
    suspend fun deleteAllSuppliersAllShops()

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAllAuditLogsAllShops()

    @Query("DELETE FROM backup_records")
    suspend fun deleteAllBackupRecordsAllShops()

    @Query("DELETE FROM shops")
    suspend fun deleteAllShops()

    @Insert
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Insert
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)

    // --- BACKUP RECORDS ---
    @Query("SELECT * FROM backup_records WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observeBackupRecords(shopId: Long): Flow<List<BackupRecordEntity>>

    @Insert
    suspend fun insertBackupRecord(record: BackupRecordEntity): Long

    @Query("DELETE FROM backup_records WHERE id = :id")
    suspend fun deleteBackupRecord(id: Long)

    // --- ATOMIC BUSINESS OPERATIONS ---

    @Transaction
    suspend fun ensureDefaultShop(): Long {
        val existing = getAllShopsOnce()
        if (existing.isNotEmpty()) {
            val active = existing.firstOrNull { it.isActive }
            if (active != null) return active.id
            setActiveShopFlag(existing.first().id)
            return existing.first().id
        }
        val shopId = insertShop(
            ShopEntity(
                name = "Amol Pharmacy",
                ownerName = "",
                phone = "",
                address = "",
                tagline = "বিশ্বস্ত ফার্মেসি ও হিসাব ব্যবস্থা",
                isActive = true
            )
        )
        insertAuditLog(
            AuditLogEntity(
                shopId = shopId,
                action = "SHOP_CREATED",
                recordType = "Shop",
                recordTitle = "Amol Pharmacy",
                previousValue = "-",
                newValue = "ডিফল্ট দোকান তৈরি হয়েছে"
            )
        )
        return shopId
    }

    @Transaction
    suspend fun createShopAndSwitch(
        name: String,
        ownerName: String,
        phone: String,
        address: String,
        tagline: String
    ): Long {
        val newId = insertShop(
            ShopEntity(
                name = name.trim(),
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                address = address.trim(),
                tagline = tagline.trim().ifEmpty { "বিশ্বস্ত ফার্মেসি ও হিসাব ব্যবস্থা" },
                isActive = true
            )
        )
        setActiveShopFlag(newId)
        insertAuditLog(
            AuditLogEntity(
                shopId = newId,
                action = "SHOP_CREATED",
                recordType = "Shop",
                recordTitle = name.trim(),
                previousValue = "-",
                newValue = "নতুন দোকান তৈরি ও সক্রিয় করা হয়েছে"
            )
        )
        return newId
    }

    @Transaction
    suspend fun switchActiveShop(shopId: Long) {
        val target = getShopById(shopId) ?: return
        setActiveShopFlag(shopId)
        insertAuditLog(
            AuditLogEntity(
                shopId = shopId,
                action = "SHOP_SWITCHED",
                recordType = "Shop",
                recordTitle = target.name,
                previousValue = "-",
                newValue = "সক্রিয় দোকান পরিবর্তন"
            )
        )
    }

    @Transaction
    suspend fun deleteShopSafely(shopId: Long): Boolean {
        val allShops = getAllShopsOnce()
        if (allShops.size <= 1) return false
        val deleting = allShops.firstOrNull { it.id == shopId } ?: return false
        deleteAllTransactionsForShop(shopId)
        deleteAllCustomersForShop(shopId)
        deleteAllSuppliersForShop(shopId)
        deleteAllAuditLogsForShop(shopId)
        deleteAllBackupRecordsForShop(shopId)
        deleteShopById(shopId)
        if (deleting.isActive) {
            val next = getAllShopsOnce().firstOrNull()
            if (next != null) setActiveShopFlag(next.id)
        }
        return true
    }

    @Transaction
    suspend fun addCustomerWithOpeningDue(
        shopId: Long,
        name: String,
        phone: String,
        address: String,
        photoUri: String?,
        openingDue: Double
    ): Long {
        val count = countAllCustomersForShop(shopId) + 1
        val code = "CUST-${count.toString().padStart(4, '0')}"
        val now = System.currentTimeMillis()
        val initialDue = openingDue.coerceAtLeast(0.0)
        val customerId = insertCustomer(
            CustomerEntity(
                shopId = shopId,
                customerCode = code,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                photoUri = photoUri,
                currentDue = 0.0,
                totalDueAdded = 0.0,
                totalPaid = 0.0,
                lastTransactionAt = if (initialDue > 0) now else null,
                createdAt = now
            )
        )
        insertAuditLog(
            AuditLogEntity(
                shopId = shopId,
                action = "CUSTOMER_ADDED",
                recordType = "Customer",
                recordTitle = "${name.trim()} ($code)",
                previousValue = "-",
                newValue = "মোবাইল: ${phone.trim()}"
            )
        )
        if (initialDue > 0.0) {
            recordCustomerTransaction(
                shopId = shopId,
                customerId = customerId,
                type = TransactionType.DUE_ADDED,
                amount = initialDue,
                note = "পূর্বের জের (Opening Due)",
                transactionDate = now,
                idempotencyKey = "opening-cust-$customerId-${UUID.randomUUID()}"
            )
        }
        return customerId
    }

    @Transaction
    suspend fun updateCustomerProfile(
        customerId: Long,
        name: String,
        phone: String,
        address: String,
        photoUri: String?
    ) {
        val existing = getCustomerById(customerId) ?: return
        val updated = existing.copy(
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            photoUri = photoUri
        )
        updateCustomer(updated)
        syncPartyMetadataOnTransactions(
            shopId = existing.shopId,
            partyType = PartyType.CUSTOMER,
            partyId = customerId,
            name = updated.name,
            phone = updated.phone
        )
        insertAuditLog(
            AuditLogEntity(
                shopId = existing.shopId,
                action = "CUSTOMER_EDITED",
                recordType = "Customer",
                recordTitle = "${updated.name} (${updated.customerCode})",
                previousValue = "${existing.name} | ${existing.phone}",
                newValue = "${updated.name} | ${updated.phone}"
            )
        )
    }

    @Transaction
    suspend fun recordCustomerTransaction(
        shopId: Long,
        customerId: Long,
        type: TransactionType,
        amount: Double,
        note: String,
        transactionDate: Long,
        idempotencyKey: String
    ): LedgerTransactionEntity? {
        if (type != TransactionType.DUE_ADDED && type != TransactionType.PAYMENT_RECEIVED) return null
        if (amount <= 0.0) return null
        if (countByIdempotencyKey(idempotencyKey) > 0) return null

        val customer = getCustomerById(customerId) ?: return null
        if (customer.shopId != shopId) return null

        val seq = countAllTransactionsForShop(shopId) + 1
        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(transactionDate))
        val receiptNo = "TXN-$dateStamp-${seq.toString().padStart(4, '0')}"

        val insertedId = insertTransaction(
            LedgerTransactionEntity(
                shopId = shopId,
                receiptNumber = receiptNo,
                partyType = PartyType.CUSTOMER,
                partyId = customerId,
                partyCode = customer.customerCode,
                partyName = customer.name,
                partyPhone = customer.phone,
                type = type,
                amount = amount,
                previousBalance = customer.currentDue,
                newBalance = customer.currentDue,
                note = note.trim(),
                transactionDate = transactionDate,
                createdAt = System.currentTimeMillis(),
                idempotencyKey = idempotencyKey
            )
        )

        recalculateCustomerBalances(shopId, customerId)

        val saved = getTransactionById(insertedId)
        insertAuditLog(
            AuditLogEntity(
                shopId = shopId,
                action = if (type == TransactionType.DUE_ADDED) "DUE_ADDED" else "PAYMENT_RECEIVED",
                recordType = "Transaction",
                recordTitle = "${customer.name} ($receiptNo)",
                previousValue = "৳ ${saved?.previousBalance ?: customer.currentDue}",
                newValue = "৳ ${saved?.newBalance ?: customer.currentDue} (পরিমাণ: ৳ $amount)"
            )
        )
        return saved
    }

    @Transaction
    suspend fun addSupplierWithOpeningPayable(
        shopId: Long,
        name: String,
        phone: String,
        address: String,
        photoUri: String?,
        openingPayable: Double
    ): Long {
        val count = countAllSuppliersForShop(shopId) + 1
        val code = "SUP-${count.toString().padStart(4, '0')}"
        val now = System.currentTimeMillis()
        val initialPayable = openingPayable.coerceAtLeast(0.0)
        val supplierId = insertSupplier(
            SupplierEntity(
                shopId = shopId,
                supplierCode = code,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                photoUri = photoUri,
                currentPayable = 0.0,
                totalGoodsReceived = 0.0,
                totalPaid = 0.0,
                lastTransactionAt = if (initialPayable > 0) now else null,
                createdAt = now
            )
        )
        insertAuditLog(
            AuditLogEntity(
                shopId = shopId,
                action = "SUPPLIER_ADDED",
                recordType = "Supplier",
                recordTitle = "${name.trim()} ($code)",
                previousValue = "-",
                newValue = "মোবাইল: ${phone.trim()}"
            )
        )
        if (initialPayable > 0.0) {
            recordSupplierTransaction(
                shopId = shopId,
                supplierId = supplierId,
                type = TransactionType.GOODS_RECEIVED,
                amount = initialPayable,
                note = "পূর্বের বকেয়া (Opening Payable)",
                transactionDate = now,
                idempotencyKey = "opening-sup-$supplierId-${UUID.randomUUID()}"
            )
        }
        return supplierId
    }

    @Transaction
    suspend fun updateSupplierProfile(
        supplierId: Long,
        name: String,
        phone: String,
        address: String,
        photoUri: String?
    ) {
        val existing = getSupplierById(supplierId) ?: return
        val updated = existing.copy(
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            photoUri = photoUri
        )
        updateSupplier(updated)
        syncPartyMetadataOnTransactions(
            shopId = existing.shopId,
            partyType = PartyType.SUPPLIER,
            partyId = supplierId,
            name = updated.name,
            phone = updated.phone
        )
        insertAuditLog(
            AuditLogEntity(
                shopId = existing.shopId,
                action = "SUPPLIER_EDITED",
                recordType = "Supplier",
                recordTitle = "${updated.name} (${updated.supplierCode})",
                previousValue = "${existing.name} | ${existing.phone}",
                newValue = "${updated.name} | ${updated.phone}"
            )
        )
    }

    @Transaction
    suspend fun recordSupplierTransaction(
        shopId: Long,
        supplierId: Long,
        type: TransactionType,
        amount: Double,
        note: String,
        transactionDate: Long,
        idempotencyKey: String
    ): LedgerTransactionEntity? {
        if (type != TransactionType.GOODS_RECEIVED && type != TransactionType.SUPPLIER_PAYMENT) return null
        if (amount <= 0.0) return null
        if (countByIdempotencyKey(idempotencyKey) > 0) return null

        val supplier = getSupplierById(supplierId) ?: return null
        if (supplier.shopId != shopId) return null

        val seq = countAllTransactionsForShop(shopId) + 1
        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(transactionDate))
        val receiptNo = "SUP-$dateStamp-${seq.toString().padStart(4, '0')}"

        val insertedId = insertTransaction(
            LedgerTransactionEntity(
                shopId = shopId,
                receiptNumber = receiptNo,
                partyType = PartyType.SUPPLIER,
                partyId = supplierId,
                partyCode = supplier.supplierCode,
                partyName = supplier.name,
                partyPhone = supplier.phone,
                type = type,
                amount = amount,
                previousBalance = supplier.currentPayable,
                newBalance = supplier.currentPayable,
                note = note.trim(),
                transactionDate = transactionDate,
                createdAt = System.currentTimeMillis(),
                idempotencyKey = idempotencyKey
            )
        )

        recalculateSupplierBalances(shopId, supplierId)

        val saved = getTransactionById(insertedId)
        insertAuditLog(
            AuditLogEntity(
                shopId = shopId,
                action = if (type == TransactionType.GOODS_RECEIVED) "GOODS_RECEIVED" else "SUPPLIER_PAYMENT",
                recordType = "Transaction",
                recordTitle = "${supplier.name} ($receiptNo)",
                previousValue = "৳ ${saved?.previousBalance ?: supplier.currentPayable}",
                newValue = "৳ ${saved?.newBalance ?: supplier.currentPayable} (পরিমাণ: ৳ $amount)"
            )
        )
        return saved
    }

    @Transaction
    suspend fun recalculateCustomerBalances(shopId: Long, customerId: Long) {
        val customer = getCustomerById(customerId) ?: return
        val txns = getPartyTransactionsAscOnce(shopId, PartyType.CUSTOMER, customerId)
        var runningBalance = 0.0
        var totalDue = 0.0
        var totalPaid = 0.0
        var lastTxnAt: Long? = null
        var lastPayAt: Long? = null

        for (txn in txns) {
            val prev = runningBalance
            val next = when (txn.type) {
                TransactionType.DUE_ADDED -> {
                    totalDue += txn.amount
                    prev + txn.amount
                }
                TransactionType.PAYMENT_RECEIVED -> {
                    totalPaid += txn.amount
                    lastPayAt = txn.transactionDate
                    prev - txn.amount
                }
                else -> prev
            }
            runningBalance = next
            lastTxnAt = txn.transactionDate
            if (txn.previousBalance != prev || txn.newBalance != next) {
                updateTransaction(txn.copy(previousBalance = prev, newBalance = next))
            }
        }
        updateCustomer(
            customer.copy(
                currentDue = runningBalance,
                totalDueAdded = totalDue,
                totalPaid = totalPaid,
                lastTransactionAt = lastTxnAt,
                lastPaymentAt = lastPayAt
            )
        )
    }

    @Transaction
    suspend fun recalculateSupplierBalances(shopId: Long, supplierId: Long) {
        val supplier = getSupplierById(supplierId) ?: return
        val txns = getPartyTransactionsAscOnce(shopId, PartyType.SUPPLIER, supplierId)
        var runningBalance = 0.0
        var totalGoods = 0.0
        var totalPaid = 0.0
        var lastTxnAt: Long? = null

        for (txn in txns) {
            val prev = runningBalance
            val next = when (txn.type) {
                TransactionType.GOODS_RECEIVED -> {
                    totalGoods += txn.amount
                    prev + txn.amount
                }
                TransactionType.SUPPLIER_PAYMENT -> {
                    totalPaid += txn.amount
                    prev - txn.amount
                }
                else -> prev
            }
            runningBalance = next
            lastTxnAt = txn.transactionDate
            if (txn.previousBalance != prev || txn.newBalance != next) {
                updateTransaction(txn.copy(previousBalance = prev, newBalance = next))
            }
        }
        updateSupplier(
            supplier.copy(
                currentPayable = runningBalance,
                totalGoodsReceived = totalGoods,
                totalPaid = totalPaid,
                lastTransactionAt = lastTxnAt
            )
        )
    }

    @Transaction
    suspend fun softDeleteCustomer(customerId: Long) {
        val customer = getCustomerById(customerId) ?: return
        val now = System.currentTimeMillis()
        updateCustomer(customer.copy(isDeleted = true, deletedAt = now))
        insertAuditLog(
            AuditLogEntity(
                shopId = customer.shopId,
                action = "CUSTOMER_DELETED",
                recordType = "Customer",
                recordTitle = "${customer.name} (${customer.customerCode})",
                previousValue = "সক্রিয় (বাকি: ৳ ${customer.currentDue})",
                newValue = "রিসাইকেল বিনে স্থানান্তর"
            )
        )
    }

    @Transaction
    suspend fun restoreCustomer(customerId: Long) {
        val customer = getCustomerById(customerId) ?: return
        updateCustomer(customer.copy(isDeleted = false, deletedAt = null))
        insertAuditLog(
            AuditLogEntity(
                shopId = customer.shopId,
                action = "CUSTOMER_RESTORED",
                recordType = "Customer",
                recordTitle = "${customer.name} (${customer.customerCode})",
                previousValue = "রিসাইকেল বিন",
                newValue = "পুনরুদ্ধার করা হয়েছে"
            )
        )
    }

    @Transaction
    suspend fun permanentlyDeleteCustomer(customerId: Long) {
        val customer = getCustomerById(customerId) ?: return
        hardDeleteTransactionsForParty(customer.shopId, PartyType.CUSTOMER, customerId)
        hardDeleteCustomer(customerId)
        insertAuditLog(
            AuditLogEntity(
                shopId = customer.shopId,
                action = "CUSTOMER_PERMANENT_DELETE",
                recordType = "Customer",
                recordTitle = "${customer.name} (${customer.customerCode})",
                previousValue = "রিসাইকেল বিন",
                newValue = "স্থায়ীভাবে মুছে ফেলা হয়েছে"
            )
        )
    }

    @Transaction
    suspend fun softDeleteSupplier(supplierId: Long) {
        val supplier = getSupplierById(supplierId) ?: return
        val now = System.currentTimeMillis()
        updateSupplier(supplier.copy(isDeleted = true, deletedAt = now))
        insertAuditLog(
            AuditLogEntity(
                shopId = supplier.shopId,
                action = "SUPPLIER_DELETED",
                recordType = "Supplier",
                recordTitle = "${supplier.name} (${supplier.supplierCode})",
                previousValue = "সক্রিয় (দেনা: ৳ ${supplier.currentPayable})",
                newValue = "রিসাইকেল বিনে স্থানান্তর"
            )
        )
    }

    @Transaction
    suspend fun restoreSupplier(supplierId: Long) {
        val supplier = getSupplierById(supplierId) ?: return
        updateSupplier(supplier.copy(isDeleted = false, deletedAt = null))
        insertAuditLog(
            AuditLogEntity(
                shopId = supplier.shopId,
                action = "SUPPLIER_RESTORED",
                recordType = "Supplier",
                recordTitle = "${supplier.name} (${supplier.supplierCode})",
                previousValue = "রিসাইকেল বিন",
                newValue = "পুনরুদ্ধার করা হয়েছে"
            )
        )
    }

    @Transaction
    suspend fun permanentlyDeleteSupplier(supplierId: Long) {
        val supplier = getSupplierById(supplierId) ?: return
        hardDeleteTransactionsForParty(supplier.shopId, PartyType.SUPPLIER, supplierId)
        hardDeleteSupplier(supplierId)
        insertAuditLog(
            AuditLogEntity(
                shopId = supplier.shopId,
                action = "SUPPLIER_PERMANENT_DELETE",
                recordType = "Supplier",
                recordTitle = "${supplier.name} (${supplier.supplierCode})",
                previousValue = "রিসাইকেল বিন",
                newValue = "স্থায়ীভাবে মুছে ফেলা হয়েছে"
            )
        )
    }

    @Transaction
    suspend fun softDeleteTransaction(transactionId: Long) {
        val txn = getTransactionById(transactionId) ?: return
        updateTransaction(txn.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
        if (txn.partyType == PartyType.CUSTOMER) {
            recalculateCustomerBalances(txn.shopId, txn.partyId)
        } else {
            recalculateSupplierBalances(txn.shopId, txn.partyId)
        }
        insertAuditLog(
            AuditLogEntity(
                shopId = txn.shopId,
                action = "TRANSACTION_DELETED",
                recordType = "Transaction",
                recordTitle = "${txn.partyName} (${txn.receiptNumber})",
                previousValue = "৳ ${txn.amount}",
                newValue = "রিসাইকেল বিনে স্থানান্তর ও ব্যালেন্স সমন্বয়"
            )
        )
    }

    @Transaction
    suspend fun restoreTransaction(transactionId: Long) {
        val txn = getTransactionById(transactionId) ?: return
        updateTransaction(txn.copy(isDeleted = false, deletedAt = null))
        if (txn.partyType == PartyType.CUSTOMER) {
            recalculateCustomerBalances(txn.shopId, txn.partyId)
        } else {
            recalculateSupplierBalances(txn.shopId, txn.partyId)
        }
        insertAuditLog(
            AuditLogEntity(
                shopId = txn.shopId,
                action = "TRANSACTION_RESTORED",
                recordType = "Transaction",
                recordTitle = "${txn.partyName} (${txn.receiptNumber})",
                previousValue = "রিসাইকেল বিন",
                newValue = "পুনরুদ্ধার ও ব্যালেন্স সমন্বয় (৳ ${txn.amount})"
            )
        )
    }

    @Transaction
    suspend fun permanentlyDeleteTransaction(transactionId: Long) {
        val txn = getTransactionById(transactionId) ?: return
        hardDeleteTransaction(transactionId)
        insertAuditLog(
            AuditLogEntity(
                shopId = txn.shopId,
                action = "TRANSACTION_PERMANENT_DELETE",
                recordType = "Transaction",
                recordTitle = "${txn.partyName} (${txn.receiptNumber})",
                previousValue = "৳ ${txn.amount}",
                newValue = "স্থায়ীভাবে মুছে ফেলা হয়েছে"
            )
        )
    }

    @Transaction
    suspend fun markReminderSent(customerId: Long, channel: String) {
        val customer = getCustomerById(customerId) ?: return
        val now = System.currentTimeMillis()
        updateCustomer(
            customer.copy(
                lastReminderSentAt = now,
                reminderCount = customer.reminderCount + 1
            )
        )
        insertAuditLog(
            AuditLogEntity(
                shopId = customer.shopId,
                action = "REMINDER_SENT",
                recordType = "Customer",
                recordTitle = "${customer.name} (${customer.customerCode})",
                previousValue = "তাগাদা সংখ্যা: ${customer.reminderCount}",
                newValue = "$channel তাগাদা পাঠানো হয়েছে (বাকি: ৳ ${customer.currentDue})"
            )
        )
    }

    @Transaction
    suspend fun replaceShopDataFromBackup(
        shopId: Long,
        restoredShopInfo: ShopEntity?,
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        transactions: List<LedgerTransactionEntity>
    ) {
        val currentShop = getShopById(shopId) ?: return
        if (restoredShopInfo != null) {
            updateShop(
                currentShop.copy(
                    name = restoredShopInfo.name,
                    ownerName = restoredShopInfo.ownerName,
                    phone = restoredShopInfo.phone,
                    address = restoredShopInfo.address,
                    tagline = restoredShopInfo.tagline
                )
            )
        }
        deleteAllTransactionsForShop(shopId)
        deleteAllCustomersForShop(shopId)
        deleteAllSuppliersForShop(shopId)

        // Remap old IDs cleanly to new IDs under target shopId
        val customerIdMap = mutableMapOf<Long, Long>()
        for (cust in customers) {
            val newCustId = insertCustomer(cust.copy(id = 0, shopId = shopId))
            customerIdMap[cust.id] = newCustId
        }
        val supplierIdMap = mutableMapOf<Long, Long>()
        for (sup in suppliers) {
            val newSupId = insertSupplier(sup.copy(id = 0, shopId = shopId))
            supplierIdMap[sup.id] = newSupId
        }
        val remappedTxns = transactions.mapNotNull { txn ->
            val newPartyId = if (txn.partyType == PartyType.CUSTOMER) {
                customerIdMap[txn.partyId]
            } else {
                supplierIdMap[txn.partyId]
            } ?: return@mapNotNull null
            txn.copy(
                id = 0,
                shopId = shopId,
                partyId = newPartyId,
                idempotencyKey = "${txn.idempotencyKey}-restored-${UUID.randomUUID()}"
            )
        }
        if (remappedTxns.isNotEmpty()) {
            insertTransactions(remappedTxns)
        }

        insertAuditLog(
            AuditLogEntity(
                shopId = shopId,
                action = "BACKUP_RESTORED",
                recordType = "Backup",
                recordTitle = currentShop.name,
                previousValue = "পূর্বের তথ্য প্রতিস্থাপিত",
                newValue = "কাস্টমার: ${customers.size}, সাপ্লায়ার: ${suppliers.size}, লেনদেন: ${remappedTxns.size}"
            )
        )
    }

    @Transaction
    suspend fun replaceAllDataFromFullBackup(
        shops: List<ShopEntity>,
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        transactions: List<LedgerTransactionEntity>,
        auditLogs: List<AuditLogEntity>
    ): Long {
        if (shops.isEmpty()) return ensureDefaultShop()

        deleteAllTransactionsAllShops()
        deleteAllCustomersAllShops()
        deleteAllSuppliersAllShops()
        deleteAllAuditLogsAllShops()
        deleteAllBackupRecordsAllShops()
        deleteAllShops()

        val shopIdMap = mutableMapOf<Long, Long>()
        var activeShopNewId: Long? = null
        for (shop in shops) {
            val newShopId = insertShop(shop.copy(id = 0))
            shopIdMap[shop.id] = newShopId
            if (shop.isActive && activeShopNewId == null) {
                activeShopNewId = newShopId
            }
        }
        val finalActiveShopId = activeShopNewId ?: shopIdMap.values.first()
        setActiveShopFlag(finalActiveShopId)

        val customerIdMap = mutableMapOf<Long, Long>()
        for (cust in customers) {
            val mappedShopId = shopIdMap[cust.shopId] ?: finalActiveShopId
            val newCustId = insertCustomer(cust.copy(id = 0, shopId = mappedShopId))
            customerIdMap[cust.id] = newCustId
        }

        val supplierIdMap = mutableMapOf<Long, Long>()
        for (sup in suppliers) {
            val mappedShopId = shopIdMap[sup.shopId] ?: finalActiveShopId
            val newSupId = insertSupplier(sup.copy(id = 0, shopId = mappedShopId))
            supplierIdMap[sup.id] = newSupId
        }

        val remappedTxns = transactions.mapNotNull { txn ->
            val mappedShopId = shopIdMap[txn.shopId] ?: finalActiveShopId
            val newPartyId = if (txn.partyType == PartyType.CUSTOMER) {
                customerIdMap[txn.partyId]
            } else {
                supplierIdMap[txn.partyId]
            } ?: return@mapNotNull null
            txn.copy(
                id = 0,
                shopId = mappedShopId,
                partyId = newPartyId,
                idempotencyKey = "${txn.idempotencyKey}-full-${UUID.randomUUID()}"
            )
        }
        if (remappedTxns.isNotEmpty()) {
            insertTransactions(remappedTxns)
        }

        val remappedLogs = auditLogs.map { log ->
            log.copy(
                id = 0,
                shopId = shopIdMap[log.shopId] ?: finalActiveShopId
            )
        }
        if (remappedLogs.isNotEmpty()) {
            insertAuditLogs(remappedLogs)
        }

        insertAuditLog(
            AuditLogEntity(
                shopId = finalActiveShopId,
                action = "FULL_DEVICE_RESTORED",
                recordType = "Backup",
                recordTitle = "সম্পূর্ণ ডিভাইস ব্যাকআপ",
                previousValue = "পূর্বের সব ডাটা প্রতিস্থাপিত",
                newValue = "দোকান: ${shops.size}, কাস্টমার: ${customers.size}, সাপ্লায়ার: ${suppliers.size}, লেনদেন: ${remappedTxns.size}"
            )
        )
        return finalActiveShopId
    }
}
