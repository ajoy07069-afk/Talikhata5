package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettings
import com.example.data.local.AuditLogEntity
import com.example.data.local.BackupRecordEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.PartyType
import com.example.data.local.PreferencesManager
import com.example.data.local.ShopEntity
import com.example.data.local.SupplierEntity
import com.example.data.local.TalikhataDao
import com.example.data.local.TransactionType
import com.example.ui.theme.ThemeMode
import com.example.util.BackupManager
import com.example.util.DeviceDownloadResult
import com.example.util.PrintPdfHelper
import kotlinx.coroutines.flow.Flow

enum class RestoreScope {
    FULL_DEVICE,
    SINGLE_SHOP
}

data class RestoreOutcome(
    val success: Boolean,
    val scope: RestoreScope = RestoreScope.SINGLE_SHOP,
    val shopCount: Int = 0,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val transactionCount: Int = 0
)

class TalikhataRepository(
    private val context: Context,
    private val dao: TalikhataDao,
    private val prefs: PreferencesManager,
    private val backupManager: BackupManager
) {

    val settingsFlow: Flow<AppSettings> = prefs.settingsFlow
    val allShopsFlow: Flow<List<ShopEntity>> = dao.observeAllShops()
    val activeShopFlow: Flow<ShopEntity?> = dao.observeActiveShop()

    suspend fun initializeApp() {
        dao.ensureDefaultShop()
        PrintPdfHelper.cleanTempPdfCache(context)
    }

    fun observeCustomers(shopId: Long): Flow<List<CustomerEntity>> = dao.observeCustomers(shopId)
    fun observeDeletedCustomers(shopId: Long): Flow<List<CustomerEntity>> = dao.observeDeletedCustomers(shopId)
    fun observeCustomerById(customerId: Long): Flow<CustomerEntity?> = dao.observeCustomerById(customerId)

    fun observeSuppliers(shopId: Long): Flow<List<SupplierEntity>> = dao.observeSuppliers(shopId)
    fun observeDeletedSuppliers(shopId: Long): Flow<List<SupplierEntity>> = dao.observeDeletedSuppliers(shopId)
    fun observeSupplierById(supplierId: Long): Flow<SupplierEntity?> = dao.observeSupplierById(supplierId)

    fun observeTransactions(shopId: Long): Flow<List<LedgerTransactionEntity>> = dao.observeTransactions(shopId)
    fun observeDeletedTransactions(shopId: Long): Flow<List<LedgerTransactionEntity>> = dao.observeDeletedTransactions(shopId)
    fun observePartyTransactionsAsc(
        shopId: Long,
        partyType: PartyType,
        partyId: Long
    ): Flow<List<LedgerTransactionEntity>> = dao.observePartyTransactionsAsc(shopId, partyType, partyId)

    suspend fun getPartyTransactionsAscOnce(
        shopId: Long,
        partyType: PartyType,
        partyId: Long
    ): List<LedgerTransactionEntity> = dao.getPartyTransactionsAscOnce(shopId, partyType, partyId)

    fun observeAuditLogs(shopId: Long): Flow<List<AuditLogEntity>> = dao.observeAuditLogs(shopId)
    fun observeBackupRecords(shopId: Long): Flow<List<BackupRecordEntity>> = dao.observeBackupRecords(shopId)

    // --- Shop Management ---
    suspend fun updateShopInfo(shop: ShopEntity) {
        val old = dao.getShopById(shop.id)
        dao.updateShop(shop)
        dao.insertAuditLog(
            AuditLogEntity(
                shopId = shop.id,
                action = "SHOP_UPDATED",
                recordType = "Shop",
                recordTitle = shop.name,
                previousValue = old?.name ?: "-",
                newValue = "${shop.name} | ${shop.phone}"
            )
        )
    }

    suspend fun createShopAndSwitch(
        name: String,
        ownerName: String,
        phone: String,
        address: String,
        tagline: String
    ): Long = dao.createShopAndSwitch(name, ownerName, phone, address, tagline)

    suspend fun switchShop(shopId: Long) = dao.switchActiveShop(shopId)

    suspend fun deleteShop(shopId: Long): Boolean = dao.deleteShopSafely(shopId)

    // --- Customers ---
    suspend fun addCustomer(
        shopId: Long,
        name: String,
        phone: String,
        address: String,
        photoUri: String?,
        openingDue: Double
    ): Long = dao.addCustomerWithOpeningDue(shopId, name, phone, address, photoUri, openingDue)

    suspend fun updateCustomer(
        customerId: Long,
        name: String,
        phone: String,
        address: String,
        photoUri: String?
    ) = dao.updateCustomerProfile(customerId, name, phone, address, photoUri)

    suspend fun softDeleteCustomer(customerId: Long) = dao.softDeleteCustomer(customerId)
    suspend fun restoreCustomer(customerId: Long) = dao.restoreCustomer(customerId)
    suspend fun permanentlyDeleteCustomer(customerId: Long) = dao.permanentlyDeleteCustomer(customerId)

    // --- Suppliers ---
    suspend fun addSupplier(
        shopId: Long,
        name: String,
        phone: String,
        address: String,
        photoUri: String?,
        openingPayable: Double
    ): Long = dao.addSupplierWithOpeningPayable(shopId, name, phone, address, photoUri, openingPayable)

    suspend fun updateSupplier(
        supplierId: Long,
        name: String,
        phone: String,
        address: String,
        photoUri: String?
    ) = dao.updateSupplierProfile(supplierId, name, phone, address, photoUri)

    suspend fun softDeleteSupplier(supplierId: Long) = dao.softDeleteSupplier(supplierId)
    suspend fun restoreSupplier(supplierId: Long) = dao.restoreSupplier(supplierId)
    suspend fun permanentlyDeleteSupplier(supplierId: Long) = dao.permanentlyDeleteSupplier(supplierId)

    // --- Transactions ---
    suspend fun recordTransaction(
        shopId: Long,
        partyType: PartyType,
        partyId: Long,
        type: TransactionType,
        amount: Double,
        note: String,
        transactionDate: Long,
        idempotencyKey: String
    ): LedgerTransactionEntity? {
        return if (partyType == PartyType.CUSTOMER) {
            dao.recordCustomerTransaction(shopId, partyId, type, amount, note, transactionDate, idempotencyKey)
        } else {
            dao.recordSupplierTransaction(shopId, partyId, type, amount, note, transactionDate, idempotencyKey)
        }
    }

    suspend fun softDeleteTransaction(transactionId: Long) = dao.softDeleteTransaction(transactionId)
    suspend fun restoreTransaction(transactionId: Long) = dao.restoreTransaction(transactionId)
    suspend fun permanentlyDeleteTransaction(transactionId: Long) = dao.permanentlyDeleteTransaction(transactionId)

    suspend fun markReminderSent(customerId: Long, channel: String) =
        dao.markReminderSent(customerId, channel)

    // --- Backup & Restore ---
    suspend fun createLocalBackup(shopId: Long, type: String = "MANUAL"): BackupRecordEntity? {
        val shop = dao.getShopById(shopId) ?: return null
        val customers = dao.getAllCustomersForShopOnce(shopId)
        val suppliers = dao.getAllSuppliersForShopOnce(shopId)
        val txns = dao.getAllTransactionsForShopOnce(shopId)

        val file = backupManager.writeLocalBackupFile(
            shop = shop,
            customers = customers,
            suppliers = suppliers,
            transactions = txns,
            prefix = type.lowercase()
        )
        val record = BackupRecordEntity(
            shopId = shopId,
            shopName = shop.name,
            fileName = file.name,
            filePath = file.absolutePath,
            backupType = type,
            customerCount = customers.count { !it.isDeleted },
            supplierCount = suppliers.count { !it.isDeleted },
            transactionCount = txns.count { !it.isDeleted },
            sizeBytes = file.length()
        )
        val id = dao.insertBackupRecord(record)
        dao.insertAuditLog(
            AuditLogEntity(
                shopId = shopId,
                action = "BACKUP_CREATED",
                recordType = "Backup",
                recordTitle = file.name,
                previousValue = "-",
                newValue = "$type ব্যাকআপ (${record.customerCount} কাস্টমার, ${record.transactionCount} লেনদেন)"
            )
        )
        if (type == "AUTO") {
            prefs.setLastAutoBackupAt(System.currentTimeMillis())
        }
        return record.copy(id = id)
    }

    suspend fun exportBackupToUri(shopId: Long, uri: Uri): Boolean {
        val shop = dao.getShopById(shopId) ?: return false
        val customers = dao.getAllCustomersForShopOnce(shopId)
        val suppliers = dao.getAllSuppliersForShopOnce(shopId)
        val txns = dao.getAllTransactionsForShopOnce(shopId)
        return backupManager.exportToUri(uri, shop, customers, suppliers, txns)
    }

    suspend fun restoreFromBackupFile(shopId: Long, filePath: String): Boolean {
        val payload = backupManager.parseBackupFile(filePath) ?: return false
        val (customers, suppliers, txns) = backupManager.toEntities(payload, shopId)
        val current = dao.getShopById(shopId)
        val shopInfo = current?.copy(
            ownerName = payload.shop.ownerName,
            phone = payload.shop.phone,
            address = payload.shop.address,
            tagline = payload.shop.tagline
        )
        dao.replaceShopDataFromBackup(shopId, shopInfo, customers, suppliers, txns)
        return true
    }

    suspend fun restoreFromBackupUri(shopId: Long, uri: Uri): Boolean {
        return restoreAnyBackupFromUri(shopId, uri).success
    }

    fun suggestedFullBackupFileName(): String = backupManager.suggestedFullBackupFileName()

    suspend fun downloadAllDataToPhoneFolder(settings: AppSettings): DeviceDownloadResult {
        val shops = dao.getAllShopsOnce()
        val customers = dao.getAllCustomersAcrossAllShopsOnce()
        val suppliers = dao.getAllSuppliersAcrossAllShopsOnce()
        val txns = dao.getAllTransactionsAcrossAllShopsOnce()
        val auditLogs = dao.getAllAuditLogsAcrossAllShopsOnce()

        val result = backupManager.downloadFullDeviceBackupToDownloadsFolder(
            shops = shops,
            customers = customers,
            suppliers = suppliers,
            transactions = txns,
            auditLogs = auditLogs,
            settings = settings
        )
        if (result.success) {
            val activeShopId = shops.firstOrNull { it.isActive }?.id ?: shops.firstOrNull()?.id
            if (activeShopId != null) {
                dao.insertAuditLog(
                    AuditLogEntity(
                        shopId = activeShopId,
                        action = "FULL_DEVICE_EXPORT",
                        recordType = "Backup",
                        recordTitle = result.fileName,
                        previousValue = "-",
                        newValue = "ডাউনলোড ফোল্ডার: ${result.folderDisplayPath} (${result.customerCount} কাস্টমার, ${result.supplierCount} সাপ্লায়ার, ${result.transactionCount} লেনদেন)"
                    )
                )
            }
        }
        return result
    }

    suspend fun exportAllDataToUri(uri: Uri, settings: AppSettings): DeviceDownloadResult {
        val shops = dao.getAllShopsOnce()
        val customers = dao.getAllCustomersAcrossAllShopsOnce()
        val suppliers = dao.getAllSuppliersAcrossAllShopsOnce()
        val txns = dao.getAllTransactionsAcrossAllShopsOnce()
        val auditLogs = dao.getAllAuditLogsAcrossAllShopsOnce()
        val fileName = backupManager.suggestedFullBackupFileName()

        val ok = backupManager.exportFullDeviceBackupToUri(
            uri = uri,
            shops = shops,
            customers = customers,
            suppliers = suppliers,
            transactions = txns,
            auditLogs = auditLogs,
            settings = settings
        )
        if (!ok) {
            return DeviceDownloadResult(success = false, fileName = fileName)
        }
        val customerCount = customers.count { !it.isDeleted }
        val supplierCount = suppliers.count { !it.isDeleted }
        val txnCount = txns.count { !it.isDeleted }
        val activeShopId = shops.firstOrNull { it.isActive }?.id ?: shops.firstOrNull()?.id
        if (activeShopId != null) {
            dao.insertAuditLog(
                AuditLogEntity(
                    shopId = activeShopId,
                    action = "FULL_DEVICE_EXPORT",
                    recordType = "Backup",
                    recordTitle = fileName,
                    previousValue = "-",
                    newValue = "নির্বাচিত ফোল্ডারে সম্পূর্ণ ডাটা সেভ ($customerCount কাস্টমার, $supplierCount সাপ্লায়ার, $txnCount লেনদেন)"
                )
            )
        }
        return DeviceDownloadResult(
            success = true,
            requiresManualPicker = false,
            fileName = fileName,
            folderDisplayPath = "নির্বাচিত ফোল্ডার/$fileName",
            uri = uri,
            shopCount = shops.size,
            customerCount = customerCount,
            supplierCount = supplierCount,
            transactionCount = txnCount
        )
    }

    suspend fun restoreAnyBackupFromUri(shopId: Long, uri: Uri): RestoreOutcome {
        val text = backupManager.readTextFromUri(uri) ?: return RestoreOutcome(false)

        val fullPayload = backupManager.parseFullDeviceBackupJson(text)
        if (fullPayload != null) {
            val bundle = backupManager.toFullDeviceEntities(fullPayload)
            dao.replaceAllDataFromFullBackup(
                shops = bundle.shops,
                customers = bundle.customers,
                suppliers = bundle.suppliers,
                transactions = bundle.transactions,
                auditLogs = bundle.auditLogs
            )
            bundle.settings?.let { s ->
                runCatching { ThemeMode.valueOf(s.themeMode) }.getOrNull()?.let { prefs.setThemeMode(it) }
                runCatching { AppLanguage.valueOf(s.language) }.getOrNull()?.let { prefs.setLanguage(it) }
                prefs.setAutoBackupEnabled(s.autoBackupEnabled)
                if (s.smsTemplate.isNotBlank()) prefs.setSmsTemplate(s.smsTemplate)
                prefs.setPrintSettings(s.printShowAddress, s.printShowPhone, s.defaultCommonNote)
                if (s.currencySymbol.isNotBlank()) prefs.setCurrencySymbol(s.currencySymbol)
                prefs.setOverdueDaysThreshold(s.overdueDaysThreshold)
            }
            return RestoreOutcome(
                success = true,
                scope = RestoreScope.FULL_DEVICE,
                shopCount = bundle.shops.size,
                customerCount = bundle.customers.count { !it.isDeleted },
                supplierCount = bundle.suppliers.count { !it.isDeleted },
                transactionCount = bundle.transactions.count { !it.isDeleted }
            )
        }

        val singlePayload = backupManager.parseSingleShopBackupJson(text) ?: return RestoreOutcome(false)
        val (customers, suppliers, txns) = backupManager.toEntities(singlePayload, shopId)
        val current = dao.getShopById(shopId)
        val shopInfo = current?.copy(
            ownerName = singlePayload.shop.ownerName,
            phone = singlePayload.shop.phone,
            address = singlePayload.shop.address,
            tagline = singlePayload.shop.tagline
        )
        dao.replaceShopDataFromBackup(shopId, shopInfo, customers, suppliers, txns)
        return RestoreOutcome(
            success = true,
            scope = RestoreScope.SINGLE_SHOP,
            shopCount = 1,
            customerCount = customers.count { !it.isDeleted },
            supplierCount = suppliers.count { !it.isDeleted },
            transactionCount = txns.count { !it.isDeleted }
        )
    }

    suspend fun deleteBackupRecord(record: BackupRecordEntity) {
        backupManager.deleteBackupFile(record.filePath)
        dao.deleteBackupRecord(record.id)
    }

    // --- Settings ---
    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)
    suspend fun setLanguage(language: AppLanguage) = prefs.setLanguage(language)
    suspend fun completeOnboarding() = prefs.completeOnboarding()
    suspend fun resetOnboarding() = prefs.resetOnboarding()
    suspend fun setPin(pin: String) = prefs.setPin(pin)
    suspend fun disablePin() = prefs.disablePin()
    suspend fun setFingerprintEnabled(enabled: Boolean) = prefs.setFingerprintEnabled(enabled)
    suspend fun setAutoBackupEnabled(enabled: Boolean) = prefs.setAutoBackupEnabled(enabled)
    suspend fun setSmsTemplate(template: String) = prefs.setSmsTemplate(template)
    suspend fun setPrintSettings(showAddress: Boolean, showPhone: Boolean, defaultNote: String) =
        prefs.setPrintSettings(showAddress, showPhone, defaultNote)
    suspend fun setCurrencySymbol(symbol: String) = prefs.setCurrencySymbol(symbol)
    suspend fun setOverdueDaysThreshold(days: Int) = prefs.setOverdueDaysThreshold(days)

    suspend fun resetActiveShopData(shopId: Long) {
        dao.deleteAllTransactionsForShop(shopId)
        dao.deleteAllCustomersForShop(shopId)
        dao.deleteAllSuppliersForShop(shopId)
        dao.insertAuditLog(
            AuditLogEntity(
                shopId = shopId,
                action = "SHOP_DATA_RESET",
                recordType = "Shop",
                recordTitle = "সকল ডাটা রিসেট",
                previousValue = "সক্রিয় তথ্য",
                newValue = "দোকানের সকল লেনদেন, কাস্টমার ও সাপ্লায়ার মুছে ফেলা হয়েছে"
            )
        )
    }
}
