package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.PartyType
import com.example.data.local.ShopEntity
import com.example.data.local.SupplierEntity
import com.example.data.local.TransactionType
import com.example.ui.screens.AddEditCustomerDialog
import com.example.ui.screens.AddEditSupplierDialog
import com.example.ui.screens.AuditHistoryScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CustomerListScreen
import com.example.ui.screens.CustomerProfileScreen
import com.example.ui.screens.DueReminderScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MoreDestination
import com.example.ui.screens.MoreMenuScreen
import com.example.ui.screens.MultiPrintScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.ReceiptDetailScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopManagementScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SupplierListScreen
import com.example.ui.screens.SupplierProfileScreen
import com.example.ui.screens.TransactionDraftRequest
import com.example.ui.screens.TransactionEntryDialog
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.theme.LocalTalikhataColors
import com.example.ui.theme.TalikhataTheme
import com.example.ui.viewmodel.TalikhataViewModel
import com.example.ui.viewmodel.TalikhataViewModelFactory
import com.example.ui.viewmodel.UiFeedback
import com.example.util.DeviceDownloadResult
import com.example.util.Formatters
import com.example.util.IntentHelper

enum class BottomTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("হোম", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    CUSTOMER("কাস্টমার", Icons.Rounded.Groups, Icons.Outlined.Groups, "nav_customer"),
    SUPPLIER("সাপ্লায়ার", Icons.Rounded.LocalShipping, Icons.Outlined.LocalShipping, "nav_supplier"),
    MULTI_PRINT("মাল্টি প্রিন্ট", Icons.Rounded.Print, Icons.Outlined.Print, "nav_multiprint"),
    MORE("আরও", Icons.Rounded.GridView, Icons.Outlined.GridView, "nav_more")
}

sealed interface AppSubScreen {
    data class CustomerProfile(val customerId: Long) : AppSubScreen
    data class SupplierProfile(val supplierId: Long) : AppSubScreen
    data class History(val partyType: PartyType? = null, val partyId: Long? = null) : AppSubScreen
    data class Receipt(val transaction: LedgerTransactionEntity) : AppSubScreen
    data object Reports : AppSubScreen
    data object DueReminder : AppSubScreen
    data object BackupRestore : AppSubScreen
    data object RecycleBin : AppSubScreen
    data object AuditHistory : AppSubScreen
    data object ShopManagement : AppSubScreen
    data object Settings : AppSubScreen
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TalikhataApplication
        setContent {
            val viewModel: TalikhataViewModel = viewModel(
                factory = TalikhataViewModelFactory(app.repository)
            )
            TalikhataRoot(viewModel = viewModel)
        }
    }
}

@Composable
fun TalikhataRoot(viewModel: TalikhataViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val isDownloadingBackup by viewModel.isDownloadingBackup.collectAsStateWithLifecycle()
    val multiPrintState by viewModel.multiPrintState.collectAsStateWithLifecycle()

    var splashDone by remember { mutableStateOf(false) }

    TalikhataTheme(themeMode = uiState.settings.themeMode) {
        when {
            !splashDone || !uiState.isReady -> {
                SplashScreen(
                    shopName = uiState.activeShop?.name ?: "Amol Pharmacy",
                    onFinished = { splashDone = true }
                )
            }
            !uiState.settings.onboardingCompleted -> {
                OnboardingScreen(
                    initialShopName = uiState.activeShop?.name ?: "Amol Pharmacy",
                    onComplete = { shopName, owner, phone, addr, pin, fp ->
                        viewModel.completeOnboarding(shopName, owner, phone, addr, pin, fp)
                    }
                )
            }
            uiState.settings.pinEnabled && !isUnlocked -> {
                PinLockScreen(
                    shopName = uiState.activeShop?.name ?: "Amol Pharmacy",
                    fingerprintEnabled = uiState.settings.fingerprintEnabled,
                    onVerifyPin = { viewModel.verifyPin(it) },
                    onBiometricSuccess = { viewModel.unlockWithBiometric() }
                )
            }
            else -> {
                TalikhataMainShell(
                    viewModel = viewModel,
                    uiState = uiState,
                    isSubmitting = isSubmitting,
                    isDownloadingBackup = isDownloadingBackup,
                    multiPrintState = multiPrintState
                )
            }
        }
    }
}

@Composable
private fun TalikhataMainShell(
    viewModel: TalikhataViewModel,
    uiState: com.example.ui.viewmodel.MainUiState,
    isSubmitting: Boolean,
    isDownloadingBackup: Boolean,
    multiPrintState: com.example.ui.viewmodel.MultiPrintState
) {
    val colors = LocalTalikhataColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    var subScreenStack by remember { mutableStateOf<List<AppSubScreen>>(emptyList()) }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var transactionDraft by remember { mutableStateOf<TransactionDraftRequest?>(null) }
    var savedReceiptPrompt by remember { mutableStateOf<LedgerTransactionEntity?>(null) }
    var deviceDownloadResult by remember { mutableStateOf<DeviceDownloadResult?>(null) }

    val pushSubScreen: (AppSubScreen) -> Unit = { screen ->
        subScreenStack = subScreenStack + screen
    }
    val popSubScreen: () -> Unit = {
        if (subScreenStack.isNotEmpty()) {
            subScreenStack = subScreenStack.dropLast(1)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { event ->
            when (event) {
                is UiFeedback.Message -> snackbarHostState.showSnackbar(event.text)
                is UiFeedback.TransactionSaved -> {
                    transactionDraft = null
                    savedReceiptPrompt = event.transaction
                }
                is UiFeedback.DeviceDownloadFinished -> {
                    deviceDownloadResult = event.result
                }
            }
        }
    }

    val activeSubScreen = subScreenStack.lastOrNull()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        if (activeSubScreen != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                when (activeSubScreen) {
                    is AppSubScreen.CustomerProfile -> {
                        val cust by viewModel.observeCustomer(activeSubScreen.customerId)
                            .collectAsStateWithLifecycle(initialValue = uiState.customers.firstOrNull { it.id == activeSubScreen.customerId })
                        val txns by viewModel.observePartyTransactions(PartyType.CUSTOMER, activeSubScreen.customerId)
                            .collectAsStateWithLifecycle(initialValue = emptyList())
                        if (cust != null) {
                            CustomerProfileScreen(
                                customer = cust!!,
                                transactionsAsc = txns,
                                shopName = uiState.activeShop?.name ?: "Amol Pharmacy",
                                settings = uiState.settings,
                                onBack = popSubScreen,
                                onAddDue = {
                                    transactionDraft = TransactionDraftRequest(
                                        partyType = PartyType.CUSTOMER,
                                        initialPartyId = cust!!.id,
                                        transactionType = TransactionType.DUE_ADDED
                                    )
                                },
                                onReceivePayment = {
                                    transactionDraft = TransactionDraftRequest(
                                        partyType = PartyType.CUSTOMER,
                                        initialPartyId = cust!!.id,
                                        transactionType = TransactionType.PAYMENT_RECEIVED
                                    )
                                },
                                onEditCustomer = { editingCustomer = cust },
                                onDeleteCustomer = {
                                    viewModel.deleteCustomer(cust!!.id)
                                    popSubScreen()
                                },
                                onOpenFullHistory = {
                                    pushSubScreen(AppSubScreen.History(PartyType.CUSTOMER, cust!!.id))
                                },
                                onOpenReceipt = { txn -> pushSubScreen(AppSubScreen.Receipt(txn)) },
                                onReminderLogged = { channel -> viewModel.markReminderSent(cust!!.id, channel) }
                            )
                        }
                    }

                    is AppSubScreen.SupplierProfile -> {
                        val sup by viewModel.observeSupplier(activeSubScreen.supplierId)
                            .collectAsStateWithLifecycle(initialValue = uiState.suppliers.firstOrNull { it.id == activeSubScreen.supplierId })
                        val txns by viewModel.observePartyTransactions(PartyType.SUPPLIER, activeSubScreen.supplierId)
                            .collectAsStateWithLifecycle(initialValue = emptyList())
                        if (sup != null) {
                            SupplierProfileScreen(
                                supplier = sup!!,
                                transactionsAsc = txns,
                                settings = uiState.settings,
                                onBack = popSubScreen,
                                onAddGoodsReceived = {
                                    transactionDraft = TransactionDraftRequest(
                                        partyType = PartyType.SUPPLIER,
                                        initialPartyId = sup!!.id,
                                        transactionType = TransactionType.GOODS_RECEIVED
                                    )
                                },
                                onAddPayment = {
                                    transactionDraft = TransactionDraftRequest(
                                        partyType = PartyType.SUPPLIER,
                                        initialPartyId = sup!!.id,
                                        transactionType = TransactionType.SUPPLIER_PAYMENT
                                    )
                                },
                                onEditSupplier = { editingSupplier = sup },
                                onDeleteSupplier = {
                                    viewModel.deleteSupplier(sup!!.id)
                                    popSubScreen()
                                },
                                onOpenFullHistory = {
                                    pushSubScreen(AppSubScreen.History(PartyType.SUPPLIER, sup!!.id))
                                },
                                onOpenReceipt = { txn -> pushSubScreen(AppSubScreen.Receipt(txn)) }
                            )
                        }
                    }

                    is AppSubScreen.History -> {
                        TransactionHistoryScreen(
                            transactions = uiState.transactions,
                            settings = uiState.settings,
                            initialPartyFilter = activeSubScreen.partyType,
                            initialPartyId = activeSubScreen.partyId,
                            onBack = popSubScreen,
                            onOpenReceipt = { txn -> pushSubScreen(AppSubScreen.Receipt(txn)) }
                        )
                    }

                    is AppSubScreen.Receipt -> {
                        ReceiptDetailScreen(
                            txn = activeSubScreen.transaction,
                            shop = uiState.activeShop ?: ShopEntity(name = "Amol Pharmacy"),
                            settings = uiState.settings,
                            onBack = popSubScreen,
                            onDeleteTransaction = { viewModel.softDeleteTransaction(it) }
                        )
                    }

                    AppSubScreen.Reports -> {
                        ReportsScreen(
                            uiState = uiState,
                            onBack = popSubScreen
                        )
                    }

                    AppSubScreen.DueReminder -> {
                        DueReminderScreen(
                            customers = uiState.customers,
                            shopName = uiState.activeShop?.name ?: "Amol Pharmacy",
                            settings = uiState.settings,
                            onBack = popSubScreen,
                            onViewProfile = { id -> pushSubScreen(AppSubScreen.CustomerProfile(id)) },
                            onMarkReminderSent = { id, ch -> viewModel.markReminderSent(id, ch) }
                        )
                    }

                    AppSubScreen.BackupRestore -> {
                        BackupRestoreScreen(
                            activeShop = uiState.activeShop,
                            backupRecords = uiState.backupRecords,
                            settings = uiState.settings,
                            onBack = popSubScreen,
                            onCreateManualBackup = { viewModel.createManualBackup() },
                            onToggleAutoBackup = { viewModel.setAutoBackupEnabled(it) },
                            onExportBackupToUri = { viewModel.exportBackupToUri(it) },
                            onRestoreFromRecord = { viewModel.restoreFromRecord(it) },
                            onRestoreFromUri = { viewModel.restoreFromUri(it) },
                            onDeleteRecord = { viewModel.deleteBackupRecord(it) }
                        )
                    }

                    AppSubScreen.RecycleBin -> {
                        RecycleBinScreen(
                            deletedCustomers = uiState.deletedCustomers,
                            deletedSuppliers = uiState.deletedSuppliers,
                            deletedTransactions = uiState.deletedTransactions,
                            settings = uiState.settings,
                            onBack = popSubScreen,
                            onRestoreCustomer = { viewModel.restoreCustomer(it) },
                            onPermanentDeleteCustomer = { viewModel.permanentDeleteCustomer(it) },
                            onRestoreSupplier = { viewModel.restoreSupplier(it) },
                            onPermanentDeleteSupplier = { viewModel.permanentDeleteSupplier(it) },
                            onRestoreTransaction = { viewModel.restoreTransaction(it) },
                            onPermanentDeleteTransaction = { viewModel.permanentDeleteTransaction(it) }
                        )
                    }

                    AppSubScreen.AuditHistory -> {
                        AuditHistoryScreen(
                            logs = uiState.auditLogs,
                            settings = uiState.settings,
                            onBack = popSubScreen
                        )
                    }

                    AppSubScreen.ShopManagement -> {
                        ShopManagementScreen(
                            shops = uiState.allShops,
                            onBack = popSubScreen,
                            onAddShop = { n, o, p, a, t -> viewModel.addShop(n, o, p, a, t) },
                            onUpdateShop = { viewModel.updateShop(it) },
                            onSwitchShop = { viewModel.switchShop(it) },
                            onDeleteShop = { viewModel.deleteShop(it) }
                        )
                    }

                    AppSubScreen.Settings -> {
                        SettingsScreen(
                            activeShop = uiState.activeShop,
                            settings = uiState.settings,
                            isDownloadingData = isDownloadingBackup,
                            lastDownloadResult = deviceDownloadResult,
                            suggestedBackupFileName = remember(uiState.transactions.size) {
                                viewModel.suggestedFullBackupFileName()
                            },
                            onDismissDownloadResult = { deviceDownloadResult = null },
                            onDownloadAllData = { viewModel.downloadAllDataToPhoneFolder() },
                            onExportAllDataToUri = { uri -> viewModel.exportAllDataToUri(uri) },
                            onRestoreAnyBackupFromUri = { uri -> viewModel.restoreFromUri(uri) },
                            onBack = popSubScreen,
                            onUpdateShop = { viewModel.updateShop(it) },
                            onSetThemeMode = { viewModel.setThemeMode(it) },
                            onSetLanguage = { viewModel.setLanguage(it) },
                            onSetPin = { viewModel.setPin(it) },
                            onDisablePin = { viewModel.disablePin() },
                            onSetFingerprint = { viewModel.setFingerprintEnabled(it) },
                            onSetAutoBackup = { viewModel.setAutoBackupEnabled(it) },
                            onOpenBackup = { pushSubScreen(AppSubScreen.BackupRestore) },
                            onOpenShops = { pushSubScreen(AppSubScreen.ShopManagement) },
                            onSaveSmsTemplate = { viewModel.setSmsTemplate(it) },
                            onSavePrintSettings = { a, p, n -> viewModel.setPrintSettings(a, p, n) },
                            onSetCurrency = { viewModel.setCurrencySymbol(it) },
                            onSetOverdueDays = { viewModel.setOverdueThreshold(it) },
                            onResetShopData = { viewModel.resetActiveShopData() },
                            onResetOnboarding = {
                                viewModel.resetOnboardingAndApp()
                                subScreenStack = emptyList()
                            }
                        )
                    }
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = colors.cardBackground,
                            tonalElevation = 4.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            BottomTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.customerBlue,
                                        selectedTextColor = colors.customerBlue,
                                        indicatorColor = colors.customerContainer
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = colors.cardBackground
                        ) {
                            BottomTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            BottomTab.HOME -> {
                                HomeDashboardScreen(
                                    uiState = uiState,
                                    onOpenNotifications = { pushSubScreen(AppSubScreen.DueReminder) },
                                    onOpenSettings = { pushSubScreen(AppSubScreen.Settings) },
                                    onOpenShops = { pushSubScreen(AppSubScreen.ShopManagement) },
                                    onOpenAddCustomer = { showAddCustomerDialog = true },
                                    onOpenAddSupplier = { showAddSupplierDialog = true },
                                    onQuickCustomerTxn = { type ->
                                        transactionDraft = TransactionDraftRequest(PartyType.CUSTOMER, null, type)
                                    },
                                    onQuickSupplierTxn = { type ->
                                        transactionDraft = TransactionDraftRequest(PartyType.SUPPLIER, null, type)
                                    },
                                    onOpenMultiPrint = { currentTab = BottomTab.MULTI_PRINT },
                                    onOpenCustomerProfile = { id -> pushSubScreen(AppSubScreen.CustomerProfile(id)) },
                                    onOpenSupplierProfile = { id -> pushSubScreen(AppSubScreen.SupplierProfile(id)) },
                                    onOpenReceipt = { txn -> pushSubScreen(AppSubScreen.Receipt(txn)) },
                                    onOpenAllCustomers = { currentTab = BottomTab.CUSTOMER },
                                    onOpenAllSuppliers = { currentTab = BottomTab.SUPPLIER },
                                    onOpenHistory = { pushSubScreen(AppSubScreen.History()) }
                                )
                            }

                            BottomTab.CUSTOMER -> {
                                CustomerListScreen(
                                    customers = uiState.customers,
                                    settings = uiState.settings,
                                    onAddCustomer = { showAddCustomerDialog = true },
                                    onCustomerClick = { c -> pushSubScreen(AppSubScreen.CustomerProfile(c.id)) }
                                )
                            }

                            BottomTab.SUPPLIER -> {
                                SupplierListScreen(
                                    suppliers = uiState.suppliers,
                                    settings = uiState.settings,
                                    onAddSupplier = { showAddSupplierDialog = true },
                                    onSupplierClick = { s -> pushSubScreen(AppSubScreen.SupplierProfile(s.id)) }
                                )
                            }

                            BottomTab.MULTI_PRINT -> {
                                MultiPrintScreen(
                                    customers = uiState.customers,
                                    activeShop = uiState.activeShop,
                                    settings = uiState.settings,
                                    state = multiPrintState,
                                    onSearchChange = { viewModel.updateMultiPrintSearch(it) },
                                    onMinDueChange = { viewModel.updateMultiPrintMinDue(it) },
                                    onToggleSelect = { viewModel.toggleCustomerSelection(it) },
                                    onSelectAll = { viewModel.selectAllFiltered(it) },
                                    onDeselectAll = { viewModel.deselectAllCustomers() },
                                    onCommonNoteChange = { viewModel.updateMultiPrintNote(it) },
                                    onToggleHistory = { viewModel.toggleMultiPrintHistory(it) },
                                    onPreparePreview = { cb -> viewModel.prepareMultiPrintPreview(cb) }
                                )
                            }

                            BottomTab.MORE -> {
                                val recycleCount = uiState.deletedCustomers.size +
                                    uiState.deletedSuppliers.size +
                                    uiState.deletedTransactions.size
                                MoreMenuScreen(
                                    activeShopName = uiState.activeShop?.name ?: "Amol Pharmacy",
                                    overdueCount = uiState.metrics.overdueCustomersCount,
                                    recycleBinCount = recycleCount,
                                    onNavigate = { dest ->
                                        when (dest) {
                                            MoreDestination.REPORTS -> pushSubScreen(AppSubScreen.Reports)
                                            MoreDestination.DUE_REMINDER -> pushSubScreen(AppSubScreen.DueReminder)
                                            MoreDestination.RECEIPTS -> pushSubScreen(AppSubScreen.History())
                                            MoreDestination.BACKUP -> pushSubScreen(AppSubScreen.BackupRestore)
                                            MoreDestination.RECYCLE_BIN -> pushSubScreen(AppSubScreen.RecycleBin)
                                            MoreDestination.AUDIT_HISTORY -> pushSubScreen(AppSubScreen.AuditHistory)
                                            MoreDestination.SHOPS -> pushSubScreen(AppSubScreen.ShopManagement)
                                            MoreDestination.SETTINGS -> pushSubScreen(AppSubScreen.Settings)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Global Dialogs (Customer Add/Edit, Supplier Add/Edit, Transaction Entry, Receipt Success)
    if (showAddCustomerDialog || editingCustomer != null) {
        AddEditCustomerDialog(
            initial = editingCustomer,
            onDismiss = { showAddCustomerDialog = false; editingCustomer = null },
            onSave = { name, phone, address, photoUri, openingDue ->
                if (editingCustomer == null) {
                    viewModel.addCustomer(name, phone, address, photoUri, openingDue)
                } else {
                    viewModel.updateCustomer(editingCustomer!!.id, name, phone, address, photoUri)
                }
                showAddCustomerDialog = false
                editingCustomer = null
            }
        )
    }

    if (showAddSupplierDialog || editingSupplier != null) {
        AddEditSupplierDialog(
            initial = editingSupplier,
            onDismiss = { showAddSupplierDialog = false; editingSupplier = null },
            onSave = { name, phone, address, photoUri, openingPayable ->
                if (editingSupplier == null) {
                    viewModel.addSupplier(name, phone, address, photoUri, openingPayable)
                } else {
                    viewModel.updateSupplier(editingSupplier!!.id, name, phone, address, photoUri)
                }
                showAddSupplierDialog = false
                editingSupplier = null
            }
        )
    }

    if (transactionDraft != null) {
        TransactionEntryDialog(
            request = transactionDraft!!,
            customers = uiState.customers,
            suppliers = uiState.suppliers,
            currencySymbol = uiState.settings.currencySymbol,
            language = uiState.settings.language,
            isSubmitting = isSubmitting,
            onDismiss = { if (!isSubmitting) transactionDraft = null },
            onConfirmSave = { pType, pId, tType, amt, note, date, key ->
                viewModel.recordTransaction(pType, pId, tType, amt, note, date, key)
            }
        )
    }

    if (savedReceiptPrompt != null) {
        val txn = savedReceiptPrompt!!
        val context = LocalContext.current
        val activeShopName = uiState.activeShop?.name ?: "Amol Pharmacy"
        AlertDialog(
            onDismissRequest = { savedReceiptPrompt = null },
            title = { Text("লেনদেন সফলভাবে সংরক্ষিত হয়েছে") },
            text = {
                Text(
                    "${txn.partyName}-এর জন্য ${Formatters.transactionTypeLabel(txn.type, uiState.settings.language)} (${Formatters.formatMoney(txn.amount, uiState.settings.currencySymbol)}) সম্পন্ন হয়েছে।\nরসিদ নং: ${txn.receiptNumber}"
                )
            },
            confirmButton = {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val smsText = Formatters.formatReceiptSmsMessage(
                                shopName = activeShopName,
                                txn = txn,
                                currencySymbol = uiState.settings.currencySymbol,
                                language = uiState.settings.language
                            )
                            IntentHelper.sendSms(context, txn.partyPhone, smsText)
                            savedReceiptPrompt = null
                        },
                        modifier = Modifier.testTag("saved_receipt_sms_btn")
                    ) {
                        Text("SMS পাঠান")
                    }
                    Button(
                        onClick = {
                            savedReceiptPrompt = null
                            pushSubScreen(AppSubScreen.Receipt(txn))
                        },
                        modifier = Modifier.testTag("saved_receipt_view_btn")
                    ) {
                        Text("রসিদ দেখুন")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { savedReceiptPrompt = null }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}
