package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettings
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.SupplierEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.components.SearchBarField
import com.example.ui.theme.LocalTalikhataColors
import com.example.util.Formatters
import com.example.util.IntentHelper

enum class SupplierFilter { ALL, HAS_PAYABLE, SETTLED }

@Composable
fun SupplierListScreen(
    suppliers: List<SupplierEntity>,
    settings: AppSettings,
    onAddSupplier: () -> Unit,
    onSupplierClick: (SupplierEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalikhataColors.current
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SupplierFilter.ALL) }

    val filtered = remember(suppliers, searchQuery, filter) {
        suppliers.filter { s ->
            val matchesSearch = searchQuery.isBlank() ||
                s.name.contains(searchQuery, ignoreCase = true) ||
                s.phone.contains(searchQuery, ignoreCase = true) ||
                s.supplierCode.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filter) {
                SupplierFilter.ALL -> true
                SupplierFilter.HAS_PAYABLE -> s.currentPayable > 0.0
                SupplierFilter.SETTLED -> s.currentPayable <= 0.0
            }
            matchesSearch && matchesFilter
        }.sortedByDescending { it.lastTransactionAt ?: it.createdAt }
    }

    val totalPayable = remember(suppliers) { suppliers.sumOf { it.currentPayable.coerceAtLeast(0.0) } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSupplier,
                containerColor = colors.supplierPurple,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_supplier_fab")
            ) {
                Icon(Icons.Rounded.LocalShipping, contentDescription = "নতুন সাপ্লায়ার")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            // Purple-themed Supplier Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF4C1D95), colors.supplierPurple)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "সাপ্লায়ার তালিকা",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "মোট সাপ্লায়ার: ${suppliers.size} জন",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEDE9FE)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "মোট দেনা",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDDD6FE)
                            )
                            Text(
                                text = Formatters.formatMoney(totalPayable, settings.currencySymbol),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SearchBarField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "সাপ্লায়ারের নাম, আইডি বা মোবাইল খুঁজুন...",
                testTag = "supplier_search_input"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == SupplierFilter.ALL,
                    onClick = { filter = SupplierFilter.ALL },
                    label = { Text("সকল (${suppliers.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.supplierContainer,
                        selectedLabelColor = colors.supplierPurple
                    )
                )
                FilterChip(
                    selected = filter == SupplierFilter.HAS_PAYABLE,
                    onClick = { filter = SupplierFilter.HAS_PAYABLE },
                    label = { Text("দেনা আছে") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.supplierContainer,
                        selectedLabelColor = colors.supplierPurple
                    )
                )
                FilterChip(
                    selected = filter == SupplierFilter.SETTLED,
                    onClick = { filter = SupplierFilter.SETTLED },
                    label = { Text("পরিশোধিত") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.paymentContainer,
                        selectedLabelColor = colors.paymentGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                EmptyStateView(
                    title = "কোনো সাপ্লায়ার পাওয়া যায়নি",
                    description = "নতুন সাপ্লায়ার বা কোম্পানি যোগ করতে + বাটনে ট্যাপ করুন।",
                    actionLabel = "নতুন সাপ্লায়ার যোগ করুন",
                    onActionClick = onAddSupplier,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { supplier ->
                        SupplierCardItem(
                            supplier = supplier,
                            currencySymbol = settings.currencySymbol,
                            language = settings.language,
                            onClick = { onSupplierClick(supplier) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierCardItem(
    supplier: SupplierEntity,
    currencySymbol: String,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val colors = LocalTalikhataColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supplier_card_${supplier.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                PartyAvatar(
                    name = supplier.name,
                    photoUri = supplier.photoUri,
                    accentColor = colors.supplierPurple,
                    containerColor = colors.supplierContainer,
                    size = 50.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = supplier.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${supplier.supplierCode} • ${supplier.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.supplierPurple
                    )
                    Text(
                        text = "শেষ লেনদেন: ${Formatters.formatDate(supplier.lastTransactionAt, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (supplier.currentPayable > 0) "প্রদেয় দেনা" else "পরিশোধিত",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (supplier.currentPayable > 0) colors.supplierPurple else colors.paymentGreen
                )
                Text(
                    text = Formatters.formatMoney(supplier.currentPayable, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (supplier.currentPayable > 0) colors.supplierPurple else colors.paymentGreen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierProfileScreen(
    supplier: SupplierEntity,
    transactionsAsc: List<LedgerTransactionEntity>,
    settings: AppSettings,
    onBack: () -> Unit,
    onAddGoodsReceived: () -> Unit,
    onAddPayment: () -> Unit,
    onEditSupplier: () -> Unit,
    onDeleteSupplier: () -> Unit,
    onOpenFullHistory: () -> Unit,
    onOpenReceipt: (LedgerTransactionEntity) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val colors = LocalTalikhataColors.current
    val currency = settings.currencySymbol
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সাপ্লায়ার প্রোফাইল") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("supplier_profile_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditSupplier, modifier = Modifier.testTag("supplier_edit_btn")) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Supplier", tint = colors.supplierPurple)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.testTag("supplier_delete_btn")) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Supplier", tint = colors.warningRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PartyAvatar(
                                name = supplier.name,
                                photoUri = supplier.photoUri,
                                accentColor = colors.supplierPurple,
                                containerColor = colors.supplierContainer,
                                size = 68.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = supplier.name,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.supplierContainer
                                ) {
                                    Text(
                                        text = supplier.supplierCode,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.supplierPurple,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { IntentHelper.dialPhone(context, supplier.phone) }
                                ) {
                                    Icon(Icons.Rounded.Call, contentDescription = "Call", tint = colors.supplierPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(supplier.phone, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                }
                                if (supplier.address.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.LocationOn, contentDescription = "Address", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(supplier.address, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3-Column Supplier Accounting Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.supplierContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("মোট মালামাল", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    Text(
                                        Formatters.formatMoney(supplier.totalGoodsReceived, currency),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.supplierPurple
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.paymentContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("মোট পরিশোধ", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    Text(
                                        Formatters.formatMoney(supplier.totalPaid, currency),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.paymentGreen
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.dueContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("অবশিষ্ট দেনা", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    Text(
                                        Formatters.formatMoney(supplier.currentPayable, currency),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.dueOrange
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddGoodsReceived,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("supplier_add_goods_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.supplierPurple)
                    ) {
                        Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("মালামাল গ্রহণ")
                    }
                    Button(
                        onClick = onAddPayment,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("supplier_add_payment_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.paymentGreen)
                    ) {
                        Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পেমেন্ট প্রদান")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onOpenFullHistory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সম্পূর্ণ লেনদেন হিস্ট্রি দেখুন")
                }
            }

            item {
                Text(
                    text = "লেনদেনের বিবরণ (${transactionsAsc.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }

            if (transactionsAsc.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = colors.textSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "এই সাপ্লায়ারের এখনো কোনো লেনদেন নেই।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                items(transactionsAsc.reversed(), key = { it.id }) { txn ->
                    TransactionRowCard(
                        txn = txn,
                        currencySymbol = currency,
                        language = settings.language,
                        onClick = { onOpenReceipt(txn) }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmActionDialog(
            title = "সাপ্লায়ার ডিলিট করবেন?",
            message = "${supplier.name}-কে রিসাইকেল বিনে পাঠানো হবে।",
            confirmText = "ডিলিট করুন",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDeleteSupplier()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun AddEditSupplierDialog(
    initial: SupplierEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, photoUri: String?, openingPayable: Double) -> Unit
) {
    val colors = LocalTalikhataColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var photoUri by remember { mutableStateOf(initial?.photoUri) }
    var openingPayableText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "নতুন সাপ্লায়ার যোগ" else "সাপ্লায়ার তথ্য এডিট",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PartyAvatar(
                        name = name.ifBlank { "S" },
                        photoUri = photoUri,
                        accentColor = colors.supplierPurple,
                        containerColor = colors.supplierContainer,
                        size = 56.dp
                    )
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ছবি নির্বাচন করুন")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("সাপ্লায়ার / কোম্পানির নাম *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorText = null },
                    label = { Text("মোবাইল নম্বর *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_phone_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (initial == null) {
                    OutlinedTextField(
                        value = openingPayableText,
                        onValueChange = { openingPayableText = it },
                        label = { Text("পূর্বের দেনা (যদি থাকে)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.warningRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorText = "সাপ্লায়ারের নাম লিখুন"
                        return@Button
                    }
                    if (phone.isBlank()) {
                        errorText = "মোবাইল নম্বর লিখুন"
                        return@Button
                    }
                    val opening = openingPayableText.toDoubleOrNull() ?: 0.0
                    onSave(name, phone, address, photoUri, opening)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.supplierPurple),
                modifier = Modifier.testTag("save_supplier_btn")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
