package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettings
import com.example.data.local.AuditLogEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.ShopEntity
import com.example.data.local.SupplierEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SearchBarField
import com.example.ui.components.TalikhataBrandLogo
import com.example.ui.theme.LocalTalikhataColors
import com.example.ui.theme.ThemeMode
import com.example.util.BiometricHelper
import com.example.util.DeviceDownloadResult
import com.example.util.Formatters
import com.example.util.IntentHelper

enum class MoreDestination {
    REPORTS, DUE_REMINDER, RECEIPTS, BACKUP, RECYCLE_BIN, AUDIT_HISTORY, SHOPS, SETTINGS
}

@Composable
fun MoreMenuScreen(
    activeShopName: String,
    overdueCount: Int,
    recycleBinCount: Int,
    onNavigate: (MoreDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalikhataColors.current

    val items = listOf(
        Triple(MoreDestination.REPORTS, "রিপোর্টস ও বিশ্লেষণ", Icons.Rounded.Assessment to colors.reportTeal),
        Triple(MoreDestination.DUE_REMINDER, "বাকি তাগাদা ($overdueCount)", Icons.Rounded.NotificationsActive to colors.dueOrange),
        Triple(MoreDestination.RECEIPTS, "রসিদসমূহ ও লেনদেন", Icons.AutoMirrored.Rounded.ReceiptLong to colors.customerBlue),
        Triple(MoreDestination.BACKUP, "ব্যাকআপ ও রিস্টোর", Icons.Rounded.Backup to colors.paymentGreen),
        Triple(MoreDestination.RECYCLE_BIN, "রিসাইকেল বিন ($recycleBinCount)", Icons.Rounded.DeleteSweep to colors.warningRed),
        Triple(MoreDestination.AUDIT_HISTORY, "অডিট হিস্ট্রি", Icons.Rounded.HistoryEdu to colors.supplierPurple),
        Triple(MoreDestination.SHOPS, "দোকান ব্যবস্থাপনা", Icons.Rounded.Storefront to colors.customerBlue),
        Triple(MoreDestination.SETTINGS, "সেটিংস", Icons.Rounded.Settings to colors.textPrimary)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TalikhataBrandLogo(size = 50.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Talikhata • আরও মেনু",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "সক্রিয় দোকান: $activeShopName",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.customerBlue
                        )
                    }
                }
            }
        }

        items(items) { (dest, title, iconPair) ->
            val (icon, tint) = iconPair
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("more_item_${dest.name.lowercase()}")
                    .clickable { onNavigate(dest) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tint.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    deletedCustomers: List<CustomerEntity>,
    deletedSuppliers: List<SupplierEntity>,
    deletedTransactions: List<LedgerTransactionEntity>,
    settings: AppSettings,
    onBack: () -> Unit,
    onRestoreCustomer: (Long) -> Unit,
    onPermanentDeleteCustomer: (Long) -> Unit,
    onRestoreSupplier: (Long) -> Unit,
    onPermanentDeleteSupplier: (Long) -> Unit,
    onRestoreTransaction: (Long) -> Unit,
    onPermanentDeleteTransaction: (Long) -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = LocalTalikhataColors.current
    var tabIndex by remember { mutableIntStateOf(0) }
    var confirmPermanentAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("রিসাইকেল বিন") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("recycle_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("কাস্টমার (${deletedCustomers.size})") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("সাপ্লায়ার (${deletedSuppliers.size})") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("লেনদেন (${deletedTransactions.size})") })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (tabIndex) {
                    0 -> {
                        if (deletedCustomers.isEmpty()) {
                            item { EmptyStateView("মুছে ফেলা কাস্টমার নেই", "রিসাইকেল বিন পরিষ্কার রয়েছে।") }
                        } else {
                            items(deletedCustomers, key = { it.id }) { cust ->
                                RecycleItemCard(
                                    title = "${cust.name} (${cust.customerCode})",
                                    subtitle = "বাকি: ${Formatters.formatMoney(cust.currentDue, settings.currencySymbol)} • মোছা হয়েছে: ${Formatters.formatDate(cust.deletedAt, settings.language)}",
                                    onRestore = { onRestoreCustomer(cust.id) },
                                    onPermanentDelete = {
                                        confirmPermanentAction = "${cust.name} ও এর সকল লেনদেন স্থায়ীভাবে মুছে যাবে।" to {
                                            onPermanentDeleteCustomer(cust.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    1 -> {
                        if (deletedSuppliers.isEmpty()) {
                            item { EmptyStateView("মুছে ফেলা সাপ্লায়ার নেই", "রিসাইকেল বিন পরিষ্কার রয়েছে।") }
                        } else {
                            items(deletedSuppliers, key = { it.id }) { sup ->
                                RecycleItemCard(
                                    title = "${sup.name} (${sup.supplierCode})",
                                    subtitle = "দেনা: ${Formatters.formatMoney(sup.currentPayable, settings.currencySymbol)}",
                                    onRestore = { onRestoreSupplier(sup.id) },
                                    onPermanentDelete = {
                                        confirmPermanentAction = "${sup.name} স্থায়ীভাবে মুছে যাবে।" to {
                                            onPermanentDeleteSupplier(sup.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    else -> {
                        if (deletedTransactions.isEmpty()) {
                            item { EmptyStateView("মুছে ফেলা লেনদেন নেই", "রিসাইকেল বিন পরিষ্কার রয়েছে।") }
                        } else {
                            items(deletedTransactions, key = { it.id }) { txn ->
                                RecycleItemCard(
                                    title = "${txn.partyName} • ${txn.receiptNumber}",
                                    subtitle = "${Formatters.transactionTypeLabel(txn.type, settings.language)} • ${Formatters.formatMoney(txn.amount, settings.currencySymbol)}",
                                    onRestore = { onRestoreTransaction(txn.id) },
                                    onPermanentDelete = {
                                        confirmPermanentAction = "রসিদ ${txn.receiptNumber} স্থায়ীভাবে মুছে যাবে।" to {
                                            onPermanentDeleteTransaction(txn.id)
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

    if (confirmPermanentAction != null) {
        val (msg, action) = confirmPermanentAction!!
        ConfirmActionDialog(
            title = "স্থায়ীভাবে মুছবেন?",
            message = msg,
            confirmText = "স্থায়ী ডিলিট",
            isDestructive = true,
            onConfirm = {
                confirmPermanentAction = null
                action()
            },
            onDismiss = { confirmPermanentAction = null }
        )
    }
}

@Composable
private fun RecycleItemCard(
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val colors = LocalTalikhataColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.paymentGreen)
                ) {
                    Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("পুনরুদ্ধার")
                }
                OutlinedButton(
                    onClick = onPermanentDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = colors.warningRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("স্থায়ী ডিলিট", color = colors.warningRed)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditHistoryScreen(
    logs: List<AuditLogEntity>,
    settings: AppSettings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = LocalTalikhataColors.current
    var search by remember { mutableStateOf("") }
    val filtered = remember(logs, search) {
        logs.filter {
            search.isBlank() ||
                it.action.contains(search, true) ||
                it.recordTitle.contains(search, true) ||
                it.newValue.contains(search, true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("অডিট হিস্ট্রি") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("audit_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            SearchBarField(
                query = search,
                onQueryChange = { search = it },
                placeholder = "অডিট লগ খুঁজুন...",
                testTag = "audit_search_input"
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (filtered.isEmpty()) {
                EmptyStateView(
                    title = "কোনো অডিট ইতিহাস নেই",
                    description = "যেকোনো পরিবর্তন ও লেনদেন স্বয়ংক্রিয়ভাবে এখানে নথিভুক্ত হয়।",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered, key = { it.id }) { log ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.supplierContainer
                                    ) {
                                        Text(
                                            text = log.action,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.supplierPurple,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = Formatters.formatDateTime(log.timestamp, settings.language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                Text(
                                    text = "${log.recordType}: ${log.recordTitle}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "পূর্বের মান: ${log.previousValue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "নতুন মান: ${log.newValue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.paymentGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    activeShop: ShopEntity?,
    settings: AppSettings,
    isDownloadingData: Boolean = false,
    lastDownloadResult: DeviceDownloadResult? = null,
    suggestedBackupFileName: String = "Talikhata_AllData.json",
    onDismissDownloadResult: () -> Unit = {},
    onDownloadAllData: () -> Unit = {},
    onExportAllDataToUri: (Uri) -> Unit = {},
    onRestoreAnyBackupFromUri: (Uri) -> Unit = {},
    onBack: () -> Unit,
    onUpdateShop: (ShopEntity) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onSetFingerprint: (Boolean) -> Unit,
    onSetAutoBackup: (Boolean) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenShops: () -> Unit,
    onSaveSmsTemplate: (String) -> Unit,
    onSavePrintSettings: (Boolean, Boolean, String) -> Unit,
    onSetCurrency: (String) -> Unit,
    onSetOverdueDays: (Int) -> Unit,
    onResetShopData: () -> Unit,
    onResetOnboarding: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val colors = LocalTalikhataColors.current
    val biometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }

    var showEditShop by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showSmsDialog by remember { mutableStateOf(false) }
    var showPrintDialog by remember { mutableStateOf(false) }
    var showResetDataConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val saveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportAllDataToUri(uri)
    }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    LaunchedEffect(lastDownloadResult) {
        if (lastDownloadResult?.requiresManualPicker == true) {
            val fileName = lastDownloadResult.fileName.ifBlank { suggestedBackupFileName }
            onDismissDownloadResult()
            saveFolderLauncher.launch(fileName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সেটিংস") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Shop Info
            SettingsTile(
                icon = Icons.Rounded.Storefront,
                tint = colors.customerBlue,
                title = "দোকানের তথ্য",
                subtitle = activeShop?.name ?: "Amol Pharmacy",
                onClick = { showEditShop = true }
            )

            // Theme Mode
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = colors.supplierPurple)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("থিম ও ডার্ক মোড", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { onSetThemeMode(ThemeMode.SYSTEM) },
                            label = { Text("সিস্টেম") }
                        )
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { onSetThemeMode(ThemeMode.LIGHT) },
                            label = { Text("লাইট মোড") }
                        )
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.DARK,
                            onClick = { onSetThemeMode(ThemeMode.DARK) },
                            label = { Text("ডার্ক মোড") }
                        )
                    }
                }
            }

            // Language
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = colors.reportTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("ভাষা (Language)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.language == AppLanguage.BN,
                            onClick = { onSetLanguage(AppLanguage.BN) },
                            label = { Text("বাংলা (প্রাথমিক)") }
                        )
                        FilterChip(
                            selected = settings.language == AppLanguage.EN,
                            onClick = { onSetLanguage(AppLanguage.EN) },
                            label = { Text("English") }
                        )
                    }
                }
            }

            // PIN & Fingerprint Security
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.dueOrange)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("পিন নিরাপত্তা (PIN Lock)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(if (settings.pinEnabled) "পিন সক্রিয় আছে" else "বন্ধ আছে", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            }
                        }
                        OutlinedButton(onClick = { showPinDialog = true }) {
                            Text(if (settings.pinEnabled) "পরিবর্তন" else "চালু করুন")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = colors.paymentGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("ফিঙ্গারপ্রিন্ট লক", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (!biometricAvailable) "ডিভাইসে বায়োমেট্রিক পাওয়া যায়নি" else if (!settings.pinEnabled) "আগে পিন চালু করুন" else "ফিঙ্গারপ্রিন্ট দিয়ে দ্রুত আনলক",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.fingerprintEnabled,
                            enabled = biometricAvailable && settings.pinEnabled,
                            onCheckedChange = onSetFingerprint
                        )
                    }
                }
            }

            // Full Device Data Download & Transfer Folder
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.paymentContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = colors.paymentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ডিভাইস পরিবর্তন ও সম্পূর্ণ ডাটা ডাউনলোড",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "ফোল্ডার: Download/Talikhata",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.paymentGreen
                            )
                        }
                    }

                    Text(
                        text = "অন্য ফোনে যাওয়ার জন্য সব দোকান, সকল কাস্টমার ও সাপ্লায়ারের হিসাব এবং সম্পূর্ণ লেনদেন হিস্ট্রি একটি ফাইলে সরাসরি ডাউনলোড করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Button(
                        onClick = onDownloadAllData,
                        enabled = !isDownloadingData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_download_all_data_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.paymentGreen)
                    ) {
                        if (isDownloadingData) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ডাটা ডাউনলোড হচ্ছে...")
                        } else {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("সম্পূর্ণ ডাটা ডাউনলোড (Download Data)")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { saveFolderLauncher.launch(suggestedBackupFileName) },
                            enabled = !isDownloadingData,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_save_folder_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ফোল্ডার বেছে সেভ")
                        }
                        OutlinedButton(
                            onClick = { restoreFileLauncher.launch(arrayOf("application/json", "*/*")) },
                            enabled = !isDownloadingData,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_restore_device_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ফাইল থেকে রিস্টোর")
                        }
                    }
                }
            }

            // Backup Settings
            SettingsTile(
                icon = Icons.Rounded.Backup,
                tint = colors.paymentGreen,
                title = "ব্যাকআপ সেটিংস",
                subtitle = if (settings.autoBackupEnabled) "অটো ব্যাকআপ চালু • ব্যাকআপ দেখুন" else "অটো ব্যাকআপ বন্ধ",
                onClick = onOpenBackup
            )

            // SMS Template
            SettingsTile(
                icon = Icons.Rounded.Sms,
                tint = colors.customerBlue,
                title = "SMS তাগাদা টেমপ্লেট",
                subtitle = settings.smsTemplate,
                onClick = { showSmsDialog = true }
            )

            // Print Settings
            SettingsTile(
                icon = Icons.Rounded.Print,
                tint = colors.reportTeal,
                title = "প্রিন্ট সেটিংস (A4)",
                subtitle = "ঠিকানা ও ফোন প্রদর্শন, ডিফল্ট নোট",
                onClick = { showPrintDialog = true }
            )

            // Currency & Overdue Threshold
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("মুদ্রা ও নোটিফিকেশন সীমা", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("৳", "BDT", "Tk").forEach { sym ->
                            FilterChip(
                                selected = settings.currencySymbol == sym,
                                onClick = { onSetCurrency(sym) },
                                label = { Text(sym) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = colors.dueOrange, modifier = Modifier.size(18.dp))
                        Text("তাগাদা সতর্কতা সীমা:", style = MaterialTheme.typography.bodySmall)
                        listOf(3, 7, 15, 30).forEach { days ->
                            FilterChip(
                                selected = settings.overdueDaysThreshold == days,
                                onClick = { onSetOverdueDays(days) },
                                label = { Text("$days দিন") }
                            )
                        }
                    }
                }
            }

            // Multi-Shop Settings
            SettingsTile(
                icon = Icons.Rounded.Storefront,
                tint = colors.supplierPurple,
                title = "মাল্টি-শপ সেটিংস",
                subtitle = "দোকান যোগ, পরিবর্তন ও ব্যবস্থাপনা",
                onClick = onOpenShops
            )

            // Data Management
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.warningContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ডাটা ম্যানেজমেন্ট", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colors.warningRed)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showResetDataConfirm = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("দোকানের ডাটা রিসেট", color = colors.warningRed)
                        }
                        OutlinedButton(
                            onClick = onResetOnboarding,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("অ্যাপ সেটআপ রিসেট")
                        }
                    }
                }
            }

            // About Talikhata
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.customerBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Talikhata – Amol Pharmacy", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("সংস্করণ ১.০ • সম্পূর্ণ অফলাইন ডিজিটাল হিসাব খাতা", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }

    if (showEditShop && activeShop != null) {
        AddEditShopDialog(
            initial = activeShop,
            onDismiss = { showEditShop = false },
            onSave = { name, owner, phone, addr, tagline ->
                onUpdateShop(activeShop.copy(name = name, ownerName = owner, phone = phone, address = addr, tagline = tagline))
                showEditShop = false
            }
        )
    }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        var pinErr by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("পিন নিরাপত্তা") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) { newPin = it; pinErr = null } },
                        label = { Text("নতুন ৪-সংখ্যার পিন") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    if (pinErr != null) {
                        Text(pinErr!!, color = colors.warningRed, style = MaterialTheme.typography.bodySmall)
                    }
                    if (settings.pinEnabled) {
                        OutlinedButton(onClick = {
                            onDisablePin()
                            showPinDialog = false
                        }) {
                            Text("পিন লক বন্ধ করুন", color = colors.warningRed)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPin.length != 4) {
                        pinErr = "৪ সংখ্যার পিন দিন"
                        return@Button
                    }
                    onSetPin(newPin)
                    showPinDialog = false
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPinDialog = false }) { Text("বাতিল") }
            }
        )
    }

    if (showSmsDialog) {
        var tpl by remember { mutableStateOf(settings.smsTemplate) }
        AlertDialog(
            onDismissRequest = { showSmsDialog = false },
            title = { Text("SMS টেমপ্লেট এডিট") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ব্যবহারযোগ্য ট্যাগ: {name}, {due}, {shop}, {id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.customerBlue
                    )
                    OutlinedTextField(
                        value = tpl,
                        onValueChange = { tpl = it },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tpl.isNotBlank()) onSaveSmsTemplate(tpl)
                    showSmsDialog = false
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSmsDialog = false }) { Text("বাতিল") }
            }
        )
    }

    if (showPrintDialog) {
        var showAddr by remember { mutableStateOf(settings.printShowAddress) }
        var showPhone by remember { mutableStateOf(settings.printShowPhone) }
        var defNote by remember { mutableStateOf(settings.defaultCommonNote) }
        AlertDialog(
            onDismissRequest = { showPrintDialog = false },
            title = { Text("প্রিন্ট সেটিংস (A4)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("দোকানের ঠিকানা দেখান")
                        Switch(checked = showAddr, onCheckedChange = { showAddr = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("দোকানের মোবাইল দেখান")
                        Switch(checked = showPhone, onCheckedChange = { showPhone = it })
                    }
                    OutlinedTextField(
                        value = defNote,
                        onValueChange = { defNote = it },
                        label = { Text("ডিফল্ট সাধারণ নোট") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSavePrintSettings(showAddr, showPhone, defNote)
                    showPrintDialog = false
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPrintDialog = false }) { Text("বাতিল") }
            }
        )
    }

    if (showResetDataConfirm) {
        ConfirmActionDialog(
            title = "সতর্কতা: সকল ডাটা রিসেট",
            message = "${activeShop?.name ?: "Amol Pharmacy"}-এর সকল কাস্টমার, সাপ্লায়ার ও লেনদেন স্থায়ীভাবে মুছে যাবে। আপনি কি নিশ্চিত?",
            confirmText = "হ্যাঁ, রিসেট করুন",
            isDestructive = true,
            onConfirm = {
                showResetDataConfirm = false
                onResetShopData()
            },
            onDismiss = { showResetDataConfirm = false }
        )
    }

    if (pendingRestoreUri != null) {
        val uri = pendingRestoreUri!!
        ConfirmActionDialog(
            title = "সতর্কতা: সম্পূর্ণ ডাটা রিস্টোর",
            message = "নির্বাচিত ব্যাকআপ ফাইল থেকে সব কাস্টমার, সাপ্লায়ার ও লেনদেন হিস্ট্রি রিস্টোর করলে বর্তমান ফোনের পুরনো তথ্য প্রতিস্থাপিত হবে। আপনি কি নিশ্চিত?",
            confirmText = "হ্যাঁ, রিস্টোর করুন",
            isDestructive = true,
            onConfirm = {
                pendingRestoreUri = null
                onRestoreAnyBackupFromUri(uri)
            },
            onDismiss = { pendingRestoreUri = null }
        )
    }

    if (lastDownloadResult != null && lastDownloadResult.success && !lastDownloadResult.requiresManualPicker) {
        val res = lastDownloadResult
        AlertDialog(
            onDismissRequest = onDismissDownloadResult,
            title = { Text("সম্পূর্ণ ডাটা ডাউনলোড সম্পন্ন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.paymentContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ফোল্ডার অবস্থান:",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.paymentGreen
                            )
                            Text(
                                text = res.folderDisplayPath,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                    }
                    Text(
                        text = "দোকান: ${res.shopCount}টি • কাস্টমার: ${res.customerCount} জন\nসাপ্লায়ার: ${res.supplierCount} জন • লেনদেন হিস্ট্রি: ${res.transactionCount}টি",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "নতুন ফোনে Talikhata অ্যাপ খুলে সেটিংস থেকে \"ফাইল থেকে রিস্টোর\" চাপলেই সব হিসাব ফিরে পাবেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissDownloadResult,
                    modifier = Modifier.testTag("download_done_ok_btn")
                ) {
                    Text("ঠিক আছে")
                }
            },
            dismissButton = {
                if (res.uri != null) {
                    OutlinedButton(
                        onClick = {
                            IntentHelper.shareBackupUri(context, res.uri, "Talikhata ব্যাকআপ ফাইল")
                        },
                        modifier = Modifier.testTag("download_share_file_btn")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ফাইল শেয়ার")
                    }
                }
            }
        )
    }
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalTalikhataColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2)
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textSecondary)
        }
    }
}
