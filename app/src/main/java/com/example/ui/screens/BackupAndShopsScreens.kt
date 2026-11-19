package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettings
import com.example.data.local.BackupRecordEntity
import com.example.data.local.ShopEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.LocalTalikhataColors
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    activeShop: ShopEntity?,
    backupRecords: List<BackupRecordEntity>,
    settings: AppSettings,
    onBack: () -> Unit,
    onCreateManualBackup: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onExportBackupToUri: (Uri) -> Unit,
    onRestoreFromRecord: (BackupRecordEntity) -> Unit,
    onRestoreFromUri: (Uri) -> Unit,
    onDeleteRecord: (BackupRecordEntity) -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = LocalTalikhataColors.current
    var showBackupConfirm by remember { mutableStateOf(false) }
    var pendingRestoreRecord by remember { mutableStateOf<BackupRecordEntity?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingDeleteRecord by remember { mutableStateOf<BackupRecordEntity?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupToUri(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ব্যাকআপ ও রিস্টোর") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("backup_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "সক্রিয় দোকান: ${activeShop?.name ?: "Amol Pharmacy"}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.customerBlue
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("স্বয়ংক্রিয় ব্যাকআপ (Auto Backup)", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "সর্বশেষ অটো ব্যাকআপ: ${if (settings.lastAutoBackupAt > 0) Formatters.formatDateTime(settings.lastAutoBackupAt, settings.language) else "এখনো হয়নি"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                            Switch(
                                checked = settings.autoBackupEnabled,
                                onCheckedChange = onToggleAutoBackup
                            )
                        }

                        Button(
                            onClick = { showBackupConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("create_manual_backup_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.customerBlue)
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ম্যানুয়াল ব্যাকআপ তৈরি করুন")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val name = "talikhata_${(activeShop?.name ?: "shop").replace(" ", "_")}.json"
                                    exportLauncher.launch(name)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("এক্সপোর্ট")
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ফাইল রিস্টোর")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ব্যাকআপ ইতিহাস (${backupRecords.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }

            if (backupRecords.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "কোনো লোকাল ব্যাকআপ নেই",
                        description = "উপরের বাটন থেকে আপনার দোকানের তথ্যের নিরাপদ ব্যাকআপ তৈরি করুন।"
                    )
                }
            } else {
                items(backupRecords, key = { it.id }) { record ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.fileName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${record.backupType} • ${Formatters.formatDateTime(record.createdAt, settings.language)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                IconButton(onClick = { pendingDeleteRecord = record }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete backup", tint = colors.warningRed)
                                }
                            }
                            Text(
                                text = "কাস্টমার: ${record.customerCount} • সাপ্লায়ার: ${record.supplierCount} • লেনদেন: ${record.transactionCount} • সাইজ: ${(record.sizeBytes / 1024.0).let { String.format(java.util.Locale.US, "%.1f KB", maxOf(it, 0.1)) }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            OutlinedButton(
                                onClick = { pendingRestoreRecord = record },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("এই ব্যাকআপ থেকে রিস্টোর করুন")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBackupConfirm) {
        ConfirmActionDialog(
            title = "ব্যাকআপ তৈরি করবেন?",
            message = "${activeShop?.name ?: "Amol Pharmacy"}-এর সকল কাস্টমার, সাপ্লায়ার এবং লেনদেনের লোকাল ব্যাকআপ তৈরি হবে।",
            confirmText = "ব্যাকআপ করুন",
            onConfirm = {
                showBackupConfirm = false
                onCreateManualBackup()
            },
            onDismiss = { showBackupConfirm = false }
        )
    }

    if (pendingRestoreRecord != null) {
        val rec = pendingRestoreRecord!!
        ConfirmActionDialog(
            title = "সতর্কতা: ডাটা রিস্টোর",
            message = "রিস্টোর করলে বর্তমান দোকানের বিদ্যমান সকল কাস্টমার, সাপ্লায়ার ও লেনদেন মুছে গিয়ে ব্যাকআপ (${rec.fileName}) দ্বারা প্রতিস্থাপিত হবে। আপনি কি নিশ্চিত?",
            confirmText = "হ্যাঁ, রিস্টোর করুন",
            isDestructive = true,
            onConfirm = {
                pendingRestoreRecord = null
                onRestoreFromRecord(rec)
            },
            onDismiss = { pendingRestoreRecord = null }
        )
    }

    if (pendingRestoreUri != null) {
        val uri = pendingRestoreUri!!
        ConfirmActionDialog(
            title = "সতর্কতা: ফাইল থেকে রিস্টোর",
            message = "নির্বাচিত ব্যাকআপ ফাইল দিয়ে বর্তমান দোকানের সব ডাটা প্রতিস্থাপিত হবে। পূর্বের ডাটা হারিয়ে যাবে। চালিয়ে যেতে চান?",
            confirmText = "হ্যাঁ, রিস্টোর করুন",
            isDestructive = true,
            onConfirm = {
                pendingRestoreUri = null
                onRestoreFromUri(uri)
            },
            onDismiss = { pendingRestoreUri = null }
        )
    }

    if (pendingDeleteRecord != null) {
        val rec = pendingDeleteRecord!!
        ConfirmActionDialog(
            title = "ব্যাকআপ মুছে ফেলবেন?",
            message = "${rec.fileName} স্থায়ীভাবে মুছে যাবে।",
            confirmText = "মুছুন",
            isDestructive = true,
            onConfirm = {
                pendingDeleteRecord = null
                onDeleteRecord(rec)
            },
            onDismiss = { pendingDeleteRecord = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopManagementScreen(
    shops: List<ShopEntity>,
    onBack: () -> Unit,
    onAddShop: (name: String, owner: String, phone: String, address: String, tagline: String) -> Unit,
    onUpdateShop: (ShopEntity) -> Unit,
    onSwitchShop: (Long) -> Unit,
    onDeleteShop: (Long) -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = LocalTalikhataColors.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingShop by remember { mutableStateOf<ShopEntity?>(null) }
    var deletingShop by remember { mutableStateOf<ShopEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("দোকান ব্যবস্থাপনা (Multi-Shop)") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("shops_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.customerBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_shop_fab")
            ) {
                Icon(Icons.Rounded.AddBusiness, contentDescription = "নতুন দোকান")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.customerContainer
                ) {
                    Text(
                        text = "প্রতিটি দোকানের কাস্টমার, সাপ্লায়ার, লেনদেন, রিপোর্ট ও ব্যাকআপ সম্পূর্ণ আলাদা রাখা হয়।",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.customerBlue,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            items(shops, key = { it.id }) { shop ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!shop.isActive) onSwitchShop(shop.id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (shop.isActive) colors.paymentContainer.copy(alpha = 0.45f) else colors.cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Rounded.Storefront,
                                    contentDescription = null,
                                    tint = if (shop.isActive) colors.paymentGreen else colors.customerBlue
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = shop.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    if (shop.phone.isNotBlank()) {
                                        Text(shop.phone, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    }
                                    if (shop.address.isNotBlank()) {
                                        Text(shop.address, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    }
                                }
                            }
                            if (shop.isActive) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = colors.paymentGreen
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("সক্রিয়", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!shop.isActive) {
                                Button(
                                    onClick = { onSwitchShop(shop.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("চালু করুন")
                                }
                            }
                            OutlinedButton(
                                onClick = { editingShop = shop },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("এডিট")
                            }
                            if (shops.size > 1) {
                                IconButton(onClick = { deletingShop = shop }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Shop", tint = colors.warningRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingShop != null) {
        AddEditShopDialog(
            initial = editingShop,
            onDismiss = { showAddDialog = false; editingShop = null },
            onSave = { name, owner, phone, addr, tagline ->
                if (editingShop == null) {
                    onAddShop(name, owner, phone, addr, tagline)
                } else {
                    onUpdateShop(
                        editingShop!!.copy(
                            name = name.trim(),
                            ownerName = owner.trim(),
                            phone = phone.trim(),
                            address = addr.trim(),
                            tagline = tagline.trim()
                        )
                    )
                }
                showAddDialog = false
                editingShop = null
            }
        )
    }

    if (deletingShop != null) {
        val target = deletingShop!!
        ConfirmActionDialog(
            title = "দোকান মুছে ফেলবেন?",
            message = "${target.name} এবং এর সকল কাস্টমার, সাপ্লায়ার ও লেনদেন স্থায়ীভাবে মুছে যাবে।",
            confirmText = "মুছুন",
            isDestructive = true,
            onConfirm = {
                deletingShop = null
                onDeleteShop(target.id)
            },
            onDismiss = { deletingShop = null }
        )
    }
}

@Composable
fun AddEditShopDialog(
    initial: ShopEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, owner: String, phone: String, address: String, tagline: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var owner by remember { mutableStateOf(initial?.ownerName ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var tagline by remember { mutableStateOf(initial?.tagline ?: "বিশ্বস্ত ফার্মেসি ও হিসাব ব্যবস্থা") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "নতুন দোকান যোগ" else "দোকানের তথ্য এডিট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("দোকানের নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("মালিকের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("মোবাইল নম্বর") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("ট্যাগলাইন") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = LocalTalikhataColors.current.warningRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) {
                    error = "দোকানের নাম লিখুন"
                    return@Button
                }
                onSave(name, owner, phone, address, tagline)
            }) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
