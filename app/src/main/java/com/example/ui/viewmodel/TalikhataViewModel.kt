package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettings
import com.example.data.local.AuditLogEntity
import com.example.data.local.BackupRecordEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.PartyType
import com.example.data.local.PreferencesManager
import com.example.data.local.ShopEntity
import com.example.data.local.SupplierEntity
import com.example.data.local.TransactionType
import com.example.data.repository.RestoreScope
import com.example.data.repository.TalikhataRepository
import com.example.ui.theme.ThemeMode
import com.example.data.local.AppLanguage
import com.example.util.CustomerStatementInput
import com.example.util.CustomerStatementPageModel
import com.example.util.DeviceDownloadResult
import com.example.util.Formatters
import com.example.util.PrintPdfHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class MonthlyBarPoint(
    val label: String,
    val collectionAmount: Double,
    val paymentAndDueAmount: Double
)

data class DueAgeingBucket(
    val label: String,
    val customerCount: Int,
    val totalAmount: Double
)

data class DashboardMetrics(
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val todayCollection: Double = 0.0,
    val todayPayment: Double = 0.0,
    val totalCustomers: Int = 0,
    val totalSuppliers: Int = 0,
    val overdueCustomersCount: Int = 0,
    val todayTransactions: List<LedgerTransactionEntity> = emptyList(),
    val recentCustomers: List<CustomerEntity> = emptyList(),
    val recentSuppliers: List<SupplierEntity> = emptyList(),
    val monthlySeries: List<MonthlyBarPoint> = emptyList(),
    val ageingBuckets: List<DueAgeingBucket> = emptyList()
)

data class MainUiState(
    val isReady: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val activeShop: ShopEntity? = null,
    val allShops: List<ShopEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val transactions: List<LedgerTransactionEntity> = emptyList(),
    val deletedCustomers: List<CustomerEntity> = emptyList(),
    val deletedSuppliers: List<SupplierEntity> = emptyList(),
    val deletedTransactions: List<LedgerTransactionEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val backupRecords: List<BackupRecordEntity> = emptyList(),
    val metrics: DashboardMetrics = DashboardMetrics()
)

data class MultiPrintState(
    val searchQuery: String = "",
    val minDueFilter: Double = 0.0,
    val selectedCustomerIds: Set<Long> = emptySet(),
    val commonNote: String = "",
    val includeHistory: Boolean = true,
    val previewPages: List<CustomerStatementPageModel> = emptyList(),
    val isPreparingPreview: Boolean = false
)

sealed class UiFeedback {
    data class Message(val text: String) : UiFeedback()
    data class TransactionSaved(val transaction: LedgerTransactionEntity) : UiFeedback()
    data class DeviceDownloadFinished(val result: DeviceDownloadResult) : UiFeedback()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TalikhataViewModel(
    private val repository: TalikhataRepository
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isDownloadingBackup = MutableStateFlow(false)
    val isDownloadingBackup: StateFlow<Boolean> = _isDownloadingBackup.asStateFlow()

    private val _multiPrintState = MutableStateFlow(MultiPrintState())
    val multiPrintState: StateFlow<MultiPrintState> = _multiPrintState.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 8)
    val feedback: SharedFlow<UiFeedback> = _feedback.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.initializeApp()
            repository.settingsFlow.collect { s ->
                if (_multiPrintState.value.commonNote.isEmpty() && s.defaultCommonNote.isNotEmpty()) {
                    _multiPrintState.value = _multiPrintState.value.copy(commonNote = s.defaultCommonNote)
                }
                if (!s.pinEnabled && !s.fingerprintEnabled) {
                    _isUnlocked.value = true
                }
            }
        }
    }

    private val activeShopIdFlow = repository.activeShopFlow.map { it?.id }

    private val customersFlow = activeShopIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeCustomers(id)
    }

    private val suppliersFlow = activeShopIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeSuppliers(id)
    }

    private val transactionsFlow = activeShopIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeTransactions(id)
    }

    private val deletedCustomersFlow = activeShopIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeDeletedCustomers(id)
    }

    private val deletedSuppliersFlow = activeShopIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeDeletedSuppliers(id)
    }

    private val deletedTransactionsFlow = activeShopIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeDeletedTransactions(id)
    }

    private val auditLogsFlow = activeShopIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeAuditLogs(id)
    }

    private val backupRecordsFlow = activeShopIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeBackupRecords(id)
    }

    private val baseShopTuple = combine(
        repository.settingsFlow,
        repository.activeShopFlow,
        repository.allShopsFlow
    ) { settings, activeShop, allShops ->
        Triple(settings, activeShop, allShops)
    }

    private val activeDataTuple = combine(
        customersFlow,
        suppliersFlow,
        transactionsFlow
    ) { customers, suppliers, txns ->
        Triple(customers, suppliers, txns)
    }

    private val recycleTuple = combine(
        deletedCustomersFlow,
        deletedSuppliersFlow,
        deletedTransactionsFlow
    ) { delCust, delSup, delTxn ->
        Triple(delCust, delSup, delTxn)
    }

    private val governanceTuple = combine(
        auditLogsFlow,
        backupRecordsFlow
    ) { logs, backups ->
        logs to backups
    }

    val uiState: StateFlow<MainUiState> = combine(
        baseShopTuple,
        activeDataTuple,
        recycleTuple,
        governanceTuple
    ) { base, activeData, recycle, governance ->
        val (settings, activeShop, allShops) = base
        val (customers, suppliers, txns) = activeData
        val (delCust, delSup, delTxn) = recycle
        val (logs, backups) = governance

        val metrics = computeMetrics(
            customers = customers,
            suppliers = suppliers,
            transactions = txns,
            overdueThreshold = settings.overdueDaysThreshold,
            language = settings.language
        )

        MainUiState(
            isReady = activeShop != null,
            settings = settings,
            activeShop = activeShop,
            allShops = allShops,
            customers = customers,
            suppliers = suppliers,
            transactions = txns,
            deletedCustomers = delCust,
            deletedSuppliers = delSup,
            deletedTransactions = delTxn,
            auditLogs = logs,
            backupRecords = backups,
            metrics = metrics
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    private fun computeMetrics(
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        transactions: List<LedgerTransactionEntity>,
        overdueThreshold: Int,
        language: AppLanguage
    ): DashboardMetrics {
        val totalReceivable = customers.sumOf { it.currentDue.coerceAtLeast(0.0) }
        val totalPayable = suppliers.sumOf { it.currentPayable.coerceAtLeast(0.0) }

        val todayStart = Formatters.startOfTodayMillis()
        val todayEnd = Formatters.endOfDayMillis(todayStart)
        val todayTxns = transactions.filter { it.transactionDate in todayStart..todayEnd }
        val todayCollection = todayTxns
            .filter { it.type == TransactionType.PAYMENT_RECEIVED }
            .sumOf { it.amount }
        val todayPayment = todayTxns
            .filter { it.type == TransactionType.SUPPLIER_PAYMENT }
            .sumOf { it.amount }

        val overdueCount = customers.count { c ->
            c.currentDue > 0.0 && Formatters.daysSince(c.lastPaymentAt ?: c.lastTransactionAt ?: c.createdAt) >= overdueThreshold
        }

        val recentCustomers = customers
            .sortedByDescending { it.lastTransactionAt ?: it.createdAt }
            .take(6)
        val recentSuppliers = suppliers
            .sortedByDescending { it.lastTransactionAt ?: it.createdAt }
            .take(6)

        // Monthly 6-month series from actual DB transactions
        val cal = Calendar.getInstance()
        val monthPoints = mutableListOf<MonthlyBarPoint>()
        for (offset in 5 downTo 0) {
            val mCal = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, -offset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = mCal.timeInMillis
            val endCal = (mCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }
            val end = endCal.timeInMillis
            val monthTxns = transactions.filter { it.transactionDate in start..end }
            val collected = monthTxns.filter { it.type == TransactionType.PAYMENT_RECEIVED }.sumOf { it.amount }
            val disbursed = monthTxns.filter {
                it.type == TransactionType.SUPPLIER_PAYMENT || it.type == TransactionType.DUE_ADDED
            }.sumOf { it.amount }

            val monthFormat = java.text.SimpleDateFormat(
                "MMM",
                if (language == AppLanguage.BN) Locale("bn", "BD") else Locale.ENGLISH
            )
            monthPoints.add(
                MonthlyBarPoint(
                    label = monthFormat.format(mCal.time),
                    collectionAmount = collected,
                    paymentAndDueAmount = disbursed
                )
            )
        }

        // Due Ageing Report Buckets
        val dueCustomers = customers.filter { it.currentDue > 0.0 }
        val b0 = dueCustomers.filter { Formatters.daysSince(it.lastPaymentAt ?: it.lastTransactionAt ?: it.createdAt) <= 7 }
        val b1 = dueCustomers.filter { Formatters.daysSince(it.lastPaymentAt ?: it.lastTransactionAt ?: it.createdAt) in 8..30 }
        val b2 = dueCustomers.filter { Formatters.daysSince(it.lastPaymentAt ?: it.lastTransactionAt ?: it.createdAt) in 31..60 }
        val b3 = dueCustomers.filter { Formatters.daysSince(it.lastPaymentAt ?: it.lastTransactionAt ?: it.createdAt) > 60 }

        val ageingBuckets = listOf(
            DueAgeingBucket("০–৭ দিন", b0.size, b0.sumOf { it.currentDue }),
            DueAgeingBucket("৮–৩০ দিন", b1.size, b1.sumOf { it.currentDue }),
            DueAgeingBucket("৩১–৬০ দিন", b2.size, b2.sumOf { it.currentDue }),
            DueAgeingBucket("৬০+ দিন", b3.size, b3.sumOf { it.currentDue })
        )

        return DashboardMetrics(
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            todayCollection = todayCollection,
            todayPayment = todayPayment,
            totalCustomers = customers.size,
            totalSuppliers = suppliers.size,
            overdueCustomersCount = overdueCount,
            todayTransactions = todayTxns,
            recentCustomers = recentCustomers,
            recentSuppliers = recentSuppliers,
            monthlySeries = monthPoints,
            ageingBuckets = ageingBuckets
        )
    }

    // --- Auth & Onboarding ---
    fun verifyPin(pin: String): Boolean {
        val storedHash = uiState.value.settings.pinHash
        val ok = storedHash.isNotBlank() && PreferencesManager.hashPin(pin) == storedHash
        if (ok) _isUnlocked.value = true
        return ok
    }

    fun unlockWithBiometric() {
        _isUnlocked.value = true
    }

    fun completeOnboarding(
        shopName: String,
        ownerName: String,
        phone: String,
        address: String,
        pin: String?,
        fingerprintEnabled: Boolean
    ) {
        viewModelScope.launch {
            val activeShop = uiState.value.activeShop
            if (activeShop != null) {
                repository.updateShopInfo(
                    activeShop.copy(
                        name = shopName.trim().ifBlank { "Amol Pharmacy" },
                        ownerName = ownerName.trim(),
                        phone = phone.trim(),
                        address = address.trim()
                    )
                )
            }
            if (!pin.isNullOrBlank() && pin.length == 4) {
                repository.setPin(pin)
                repository.setFingerprintEnabled(fingerprintEnabled)
            } else {
                repository.disablePin()
            }
            repository.completeOnboarding()
            _isUnlocked.value = true
        }
    }

    fun observeCustomer(id: Long) = repository.observeCustomerById(id)
    fun observeSupplier(id: Long) = repository.observeSupplierById(id)
    fun observePartyTransactions(partyType: PartyType, partyId: Long): kotlinx.coroutines.flow.Flow<List<LedgerTransactionEntity>> {
        val shopId = uiState.value.activeShop?.id ?: return flowOf(emptyList())
        return repository.observePartyTransactionsAsc(shopId, partyType, partyId)
    }

    // --- Customers ---
    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        photoUri: String?,
        openingDue: Double
    ) {
        val shopId = uiState.value.activeShop?.id ?: return
        viewModelScope.launch {
            repository.addCustomer(shopId, name, phone, address, photoUri, openingDue)
            _feedback.tryEmit(UiFeedback.Message("নতুন কাস্টমার যোগ হয়েছে"))
        }
    }

    fun updateCustomer(customerId: Long, name: String, phone: String, address: String, photoUri: String?) {
        viewModelScope.launch {
            repository.updateCustomer(customerId, name, phone, address, photoUri)
            _feedback.tryEmit(UiFeedback.Message("কাস্টমার তথ্য আপডেট হয়েছে"))
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.softDeleteCustomer(customerId)
            _feedback.tryEmit(UiFeedback.Message("রিসাইকেল বিনে পাঠানো হয়েছে"))
        }
    }

    // --- Suppliers ---
    fun addSupplier(
        name: String,
        phone: String,
        address: String,
        photoUri: String?,
        openingPayable: Double
    ) {
        val shopId = uiState.value.activeShop?.id ?: return
        viewModelScope.launch {
            repository.addSupplier(shopId, name, phone, address, photoUri, openingPayable)
            _feedback.tryEmit(UiFeedback.Message("নতুন সাপ্লায়ার যোগ হয়েছে"))
        }
    }

    fun updateSupplier(supplierId: Long, name: String, phone: String, address: String, photoUri: String?) {
        viewModelScope.launch {
            repository.updateSupplier(supplierId, name, phone, address, photoUri)
            _feedback.tryEmit(UiFeedback.Message("সাপ্লায়ার তথ্য আপডেট হয়েছে"))
        }
    }

    fun deleteSupplier(supplierId: Long) {
        viewModelScope.launch {
            repository.softDeleteSupplier(supplierId)
            _feedback.tryEmit(UiFeedback.Message("রিসাইকেল বিনে পাঠানো হয়েছে"))
        }
    }

    // --- Transactions ---
    fun recordTransaction(
        partyType: PartyType,
        partyId: Long,
        type: TransactionType,
        amount: Double,
        note: String,
        transactionDate: Long,
        idempotencyKey: String
    ) {
        val shopId = uiState.value.activeShop?.id ?: return
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val saved = repository.recordTransaction(
                    shopId = shopId,
                    partyType = partyType,
                    partyId = partyId,
                    type = type,
                    amount = amount,
                    note = note,
                    transactionDate = transactionDate,
                    idempotencyKey = idempotencyKey
                )
                if (saved != null) {
                    _feedback.tryEmit(UiFeedback.TransactionSaved(saved))
                    if (uiState.value.settings.autoBackupEnabled) {
                        val last = uiState.value.settings.lastAutoBackupAt
                        if (System.currentTimeMillis() - last > 12 * 60 * 60 * 1000L) {
                            repository.createLocalBackup(shopId, "AUTO")
                        }
                    }
                } else {
                    _feedback.tryEmit(UiFeedback.Message("লেনদেন সংরক্ষণ করা যায়নি"))
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun softDeleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.softDeleteTransaction(transactionId)
            _feedback.tryEmit(UiFeedback.Message("লেনদেন রিসাইকেল বিনে পাঠানো হয়েছে"))
        }
    }

    // --- Recycle Bin ---
    fun restoreCustomer(id: Long) = viewModelScope.launch {
        repository.restoreCustomer(id)
        _feedback.tryEmit(UiFeedback.Message("কাস্টমার পুনরুদ্ধার হয়েছে"))
    }

    fun permanentDeleteCustomer(id: Long) = viewModelScope.launch {
        repository.permanentlyDeleteCustomer(id)
        _feedback.tryEmit(UiFeedback.Message("স্থায়ীভাবে মুছে ফেলা হয়েছে"))
    }

    fun restoreSupplier(id: Long) = viewModelScope.launch {
        repository.restoreSupplier(id)
        _feedback.tryEmit(UiFeedback.Message("সাপ্লায়ার পুনরুদ্ধার হয়েছে"))
    }

    fun permanentDeleteSupplier(id: Long) = viewModelScope.launch {
        repository.permanentlyDeleteSupplier(id)
        _feedback.tryEmit(UiFeedback.Message("স্থায়ীভাবে মুছে ফেলা হয়েছে"))
    }

    fun restoreTransaction(id: Long) = viewModelScope.launch {
        repository.restoreTransaction(id)
        _feedback.tryEmit(UiFeedback.Message("লেনদেন পুনরুদ্ধার ও ব্যালেন্স সমন্বয় হয়েছে"))
    }

    fun permanentDeleteTransaction(id: Long) = viewModelScope.launch {
        repository.permanentlyDeleteTransaction(id)
        _feedback.tryEmit(UiFeedback.Message("লেনদেন স্থায়ীভাবে মুছে ফেলা হয়েছে"))
    }

    // --- Due Reminder ---
    fun markReminderSent(customerId: Long, channel: String) {
        viewModelScope.launch {
            repository.markReminderSent(customerId, channel)
            _feedback.tryEmit(UiFeedback.Message("তাগাদা স্ট্যাটাস আপডেট হয়েছে"))
        }
    }

    // --- Multi Print ---
    fun updateMultiPrintSearch(query: String) {
        _multiPrintState.value = _multiPrintState.value.copy(searchQuery = query)
    }

    fun updateMultiPrintMinDue(minDue: Double) {
        _multiPrintState.value = _multiPrintState.value.copy(minDueFilter = minDue)
    }

    fun toggleCustomerSelection(customerId: Long) {
        val current = _multiPrintState.value.selectedCustomerIds
        val updated = if (customerId in current) current - customerId else current + customerId
        _multiPrintState.value = _multiPrintState.value.copy(selectedCustomerIds = updated)
    }

    fun selectAllFiltered(filteredCustomers: List<CustomerEntity>) {
        _multiPrintState.value = _multiPrintState.value.copy(
            selectedCustomerIds = filteredCustomers.map { it.id }.toSet()
        )
    }

    fun deselectAllCustomers() {
        _multiPrintState.value = _multiPrintState.value.copy(selectedCustomerIds = emptySet())
    }

    fun updateMultiPrintNote(note: String) {
        _multiPrintState.value = _multiPrintState.value.copy(commonNote = note)
    }

    fun toggleMultiPrintHistory(enabled: Boolean) {
        _multiPrintState.value = _multiPrintState.value.copy(includeHistory = enabled)
    }

    fun prepareMultiPrintPreview(onReady: (List<CustomerStatementInput>) -> Unit = {}) {
        val shopId = uiState.value.activeShop?.id ?: return
        val selectedIds = _multiPrintState.value.selectedCustomerIds
        val selectedCustomers = uiState.value.customers.filter { it.id in selectedIds }
        if (selectedCustomers.isEmpty()) {
            _feedback.tryEmit(UiFeedback.Message("অনুগ্রহ করে অন্তত একজন কাস্টমার নির্বাচন করুন"))
            return
        }
        viewModelScope.launch {
            _multiPrintState.value = _multiPrintState.value.copy(isPreparingPreview = true)
            val inputs = selectedCustomers.map { cust ->
                val txns = if (_multiPrintState.value.includeHistory) {
                    repository.getPartyTransactionsAscOnce(shopId, PartyType.CUSTOMER, cust.id)
                } else {
                    emptyList()
                }
                CustomerStatementInput(cust, txns)
            }
            val pages = PrintPdfHelper.buildMultiPrintPages(
                inputs = inputs,
                commonNote = _multiPrintState.value.commonNote,
                includeHistory = _multiPrintState.value.includeHistory
            )
            _multiPrintState.value = _multiPrintState.value.copy(
                previewPages = pages,
                isPreparingPreview = false
            )
            onReady(inputs)
        }
    }

    // --- Backup & Restore ---
    fun createManualBackup() {
        val shopId = uiState.value.activeShop?.id ?: return
        viewModelScope.launch {
            val rec = repository.createLocalBackup(shopId, "MANUAL")
            if (rec != null) {
                _feedback.tryEmit(UiFeedback.Message("ব্যাকআপ তৈরি সম্পন্ন: ${rec.fileName}"))
            }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        val shopId = uiState.value.activeShop?.id ?: return
        viewModelScope.launch {
            val ok = repository.exportBackupToUri(shopId, uri)
            _feedback.tryEmit(
                UiFeedback.Message(if (ok) "ব্যাকআপ ফাইল এক্সপোর্ট সফল হয়েছে" else "এক্সপোর্ট ব্যর্থ হয়েছে")
            )
        }
    }

    fun restoreFromRecord(record: BackupRecordEntity) {
        val shopId = uiState.value.activeShop?.id ?: return
        viewModelScope.launch {
            val ok = repository.restoreFromBackupFile(shopId, record.filePath)
            _feedback.tryEmit(
                UiFeedback.Message(if (ok) "ব্যাকআপ রিস্টোর সফল হয়েছে" else "রিস্টোর করা যায়নি")
            )
        }
    }

    fun restoreFromUri(uri: Uri) {
        val shopId = uiState.value.activeShop?.id ?: 0L
        viewModelScope.launch {
            val outcome = repository.restoreAnyBackupFromUri(shopId, uri)
            val msg = when {
                !outcome.success -> "অবৈধ বা ত্রুটিপূর্ণ ব্যাকআপ ফাইল"
                outcome.scope == RestoreScope.FULL_DEVICE ->
                    "সম্পূর্ণ ডিভাইস ব্যাকআপ রিস্টোর সফল (${outcome.shopCount} দোকান, ${outcome.customerCount} কাস্টমার, ${outcome.supplierCount} সাপ্লায়ার, ${outcome.transactionCount} লেনদেন)"
                else ->
                    "ফাইল থেকে রিস্টোর সফল হয়েছে (${outcome.customerCount} কাস্টমার, ${outcome.supplierCount} সাপ্লায়ার, ${outcome.transactionCount} লেনদেন)"
            }
            _feedback.tryEmit(UiFeedback.Message(msg))
        }
    }

    fun suggestedFullBackupFileName(): String = repository.suggestedFullBackupFileName()

    fun downloadAllDataToPhoneFolder() {
        if (_isDownloadingBackup.value) return
        viewModelScope.launch {
            _isDownloadingBackup.value = true
            try {
                val result = repository.downloadAllDataToPhoneFolder(uiState.value.settings)
                _feedback.tryEmit(UiFeedback.DeviceDownloadFinished(result))
            } finally {
                _isDownloadingBackup.value = false
            }
        }
    }

    fun exportAllDataToUri(uri: Uri) {
        if (_isDownloadingBackup.value) return
        viewModelScope.launch {
            _isDownloadingBackup.value = true
            try {
                val result = repository.exportAllDataToUri(uri, uiState.value.settings)
                if (result.success) {
                    _feedback.tryEmit(UiFeedback.DeviceDownloadFinished(result))
                } else {
                    _feedback.tryEmit(UiFeedback.Message("নির্বাচিত ফোল্ডারে ফাইল সংরক্ষণ করা যায়নি"))
                }
            } finally {
                _isDownloadingBackup.value = false
            }
        }
    }

    fun deleteBackupRecord(record: BackupRecordEntity) {
        viewModelScope.launch {
            repository.deleteBackupRecord(record)
            _feedback.tryEmit(UiFeedback.Message("ব্যাকআপ মুছে ফেলা হয়েছে"))
        }
    }

    // --- Multi-Shop Management ---
    fun addShop(name: String, ownerName: String, phone: String, address: String, tagline: String) {
        viewModelScope.launch {
            repository.createShopAndSwitch(name, ownerName, phone, address, tagline)
            _multiPrintState.value = _multiPrintState.value.copy(selectedCustomerIds = emptySet())
            _feedback.tryEmit(UiFeedback.Message("নতুন দোকান যুক্ত ও সক্রিয় করা হয়েছে"))
        }
    }

    fun updateShop(shop: ShopEntity) {
        viewModelScope.launch {
            repository.updateShopInfo(shop)
            _feedback.tryEmit(UiFeedback.Message("দোকানের তথ্য আপডেট হয়েছে"))
        }
    }

    fun switchShop(shopId: Long) {
        viewModelScope.launch {
            repository.switchShop(shopId)
            _multiPrintState.value = _multiPrintState.value.copy(selectedCustomerIds = emptySet())
            _feedback.tryEmit(UiFeedback.Message("দোকান পরিবর্তন করা হয়েছে"))
        }
    }

    fun deleteShop(shopId: Long) {
        viewModelScope.launch {
            val ok = repository.deleteShop(shopId)
            _feedback.tryEmit(
                UiFeedback.Message(if (ok) "দোকান মুছে ফেলা হয়েছে" else "অন্তত একটি দোকান থাকতে হবে")
            )
        }
    }

    // --- Settings ---
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { repository.setLanguage(lang) }
    fun setPin(pin: String) = viewModelScope.launch {
        repository.setPin(pin)
        _feedback.tryEmit(UiFeedback.Message("পিন লক সেট করা হয়েছে"))
    }
    fun disablePin() = viewModelScope.launch {
        repository.disablePin()
        _feedback.tryEmit(UiFeedback.Message("পিন লক বন্ধ করা হয়েছে"))
    }
    fun setFingerprintEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setFingerprintEnabled(enabled)
    }
    fun setAutoBackupEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setAutoBackupEnabled(enabled)
    }
    fun setSmsTemplate(template: String) = viewModelScope.launch {
        repository.setSmsTemplate(template)
        _feedback.tryEmit(UiFeedback.Message("SMS টেমপ্লেট সংরক্ষিত হয়েছে"))
    }
    fun setPrintSettings(showAddress: Boolean, showPhone: Boolean, defaultNote: String) = viewModelScope.launch {
        repository.setPrintSettings(showAddress, showPhone, defaultNote)
        _multiPrintState.value = _multiPrintState.value.copy(commonNote = defaultNote)
        _feedback.tryEmit(UiFeedback.Message("প্রিন্ট সেটিংস সংরক্ষিত হয়েছে"))
    }
    fun setCurrencySymbol(symbol: String) = viewModelScope.launch { repository.setCurrencySymbol(symbol) }
    fun setOverdueThreshold(days: Int) = viewModelScope.launch { repository.setOverdueDaysThreshold(days) }
    fun resetActiveShopData() {
        val shopId = uiState.value.activeShop?.id ?: return
        viewModelScope.launch {
            repository.resetActiveShopData(shopId)
            _feedback.tryEmit(UiFeedback.Message("দোকানের সকল ডাটা রিসেট হয়েছে"))
        }
    }
    fun resetOnboardingAndApp() = viewModelScope.launch {
        repository.resetOnboarding()
    }
}

class TalikhataViewModelFactory(
    private val repository: TalikhataRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TalikhataViewModel::class.java)) {
            return TalikhataViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
