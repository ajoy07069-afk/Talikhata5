package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettings
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.PartyType
import com.example.data.local.ShopEntity
import com.example.data.local.TransactionType
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SearchBarField
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LocalTalikhataColors
import com.example.util.Formatters
import com.example.util.IntentHelper
import com.example.util.PrintPdfHelper

enum class HistoryPartyFilter { ALL, CUSTOMER, SUPPLIER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    transactions: List<LedgerTransactionEntity>,
    settings: AppSettings,
    initialPartyFilter: PartyType? = null,
    initialPartyId: Long? = null,
    onBack: () -> Unit,
    onOpenReceipt: (LedgerTransactionEntity) -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = LocalTalikhataColors.current
    var search by remember { mutableStateOf("") }
    var partyFilter by remember {
        mutableStateOf(
            when (initialPartyFilter) {
                PartyType.CUSTOMER -> HistoryPartyFilter.CUSTOMER
                PartyType.SUPPLIER -> HistoryPartyFilter.SUPPLIER
                null -> HistoryPartyFilter.ALL
            }
        )
    }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }

    val filtered = remember(transactions, search, partyFilter, typeFilter, initialPartyId) {
        transactions.filter { t ->
            val matchesFixedParty = initialPartyId == null || t.partyId == initialPartyId
            val matchesPartyType = when (partyFilter) {
                HistoryPartyFilter.ALL -> true
                HistoryPartyFilter.CUSTOMER -> t.partyType == PartyType.CUSTOMER
                HistoryPartyFilter.SUPPLIER -> t.partyType == PartyType.SUPPLIER
            }
            val matchesType = typeFilter == null || t.type == typeFilter
            val matchesSearch = search.isBlank() ||
                t.partyName.contains(search, ignoreCase = true) ||
                t.receiptNumber.contains(search, ignoreCase = true) ||
                t.note.contains(search, ignoreCase = true)
            matchesFixedParty && matchesPartyType && matchesType && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("লেনদেনের ইতিহাস ও রসিদ") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_btn")) {
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
                placeholder = "রসিদ নম্বর, নাম বা নোট দিয়ে খুঁজুন...",
                testTag = "history_search_input"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = partyFilter == HistoryPartyFilter.ALL && typeFilter == null,
                    onClick = { partyFilter = HistoryPartyFilter.ALL; typeFilter = null },
                    label = { Text("সকল") }
                )
                FilterChip(
                    selected = partyFilter == HistoryPartyFilter.CUSTOMER,
                    onClick = { partyFilter = HistoryPartyFilter.CUSTOMER; typeFilter = null },
                    label = { Text("কাস্টমার") }
                )
                FilterChip(
                    selected = partyFilter == HistoryPartyFilter.SUPPLIER,
                    onClick = { partyFilter = HistoryPartyFilter.SUPPLIER; typeFilter = null },
                    label = { Text("সাপ্লায়ার") }
                )
                FilterChip(
                    selected = typeFilter == TransactionType.DUE_ADDED,
                    onClick = { typeFilter = if (typeFilter == TransactionType.DUE_ADDED) null else TransactionType.DUE_ADDED },
                    label = { Text("পাওনা যোগ") }
                )
                FilterChip(
                    selected = typeFilter == TransactionType.PAYMENT_RECEIVED,
                    onClick = { typeFilter = if (typeFilter == TransactionType.PAYMENT_RECEIVED) null else TransactionType.PAYMENT_RECEIVED },
                    label = { Text("টাকা আদায়") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                EmptyStateView(
                    title = "কোনো লেনদেন পাওয়া যায়নি",
                    description = "ফিল্টার পরিবর্তন করে আবার চেষ্টা করুন।",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered, key = { it.id }) { txn ->
                        TransactionRowCard(
                            txn = txn,
                            currencySymbol = settings.currencySymbol,
                            language = settings.language,
                            onClick = { onOpenReceipt(txn) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    txn: LedgerTransactionEntity,
    shop: ShopEntity,
    settings: AppSettings,
    onBack: () -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val colors = LocalTalikhataColors.current
    val currency = settings.currencySymbol
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            PrintPdfHelper.exportReceiptPdfToUri(context, uri, shop, txn, currency, settings.language)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ডিজিটাল রসিদ") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("receipt_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Transaction", tint = colors.warningRed)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Branded Top Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                            .background(DeepNavy)
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = shop.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "রসিদ নং: ${txn.receiptNumber}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFBFDBFE)
                            )
                            if (shop.phone.isNotBlank()) {
                                Text(
                                    text = "ফোন: ${shop.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFDBEAFE)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReceiptInfoRow("আইডি", txn.partyCode)
                        ReceiptInfoRow("নাম", txn.partyName)
                        ReceiptInfoRow("মোবাইল", txn.partyPhone.ifBlank { "-" })
                        ReceiptInfoRow("তারিখ", Formatters.formatDateTime(txn.transactionDate, settings.language))
                        ReceiptInfoRow("লেনদেনের ধরন", Formatters.transactionTypeLabel(txn.type, settings.language))
                        HorizontalDivider(color = colors.subtleDivider)
                        ReceiptInfoRow("পূর্বের ব্যালেন্স", Formatters.formatMoney(txn.previousBalance, currency))
                        ReceiptInfoRow(
                            "পরিমাণ",
                            Formatters.formatMoney(txn.amount, currency),
                            highlightColor = colors.paymentGreen
                        )
                        ReceiptInfoRow(
                            "নতুন ব্যালেন্স",
                            Formatters.formatMoney(txn.newBalance, currency),
                            highlightColor = colors.dueOrange
                        )
                        if (txn.note.isNotBlank()) {
                            HorizontalDivider(color = colors.subtleDivider)
                            Text("নোট:", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                            Text(txn.note, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }
                    }
                }
            }

            // Receipt Actions: Print, PDF Export, SMS, WhatsApp/PDF Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        PrintPdfHelper.printSingleReceipt(context, shop, txn, currency, settings.language)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("receipt_print_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.reportTeal)
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("প্রিন্ট")
                }
                OutlinedButton(
                    onClick = {
                        exportPdfLauncher.launch("${txn.receiptNumber}.pdf")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("receipt_pdf_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF")
                }
            }

            val smsColor = if (txn.partyType == PartyType.CUSTOMER) colors.customerBlue else colors.supplierPurple
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val smsText = Formatters.formatReceiptSmsMessage(
                            shopName = shop.name,
                            txn = txn,
                            currencySymbol = currency,
                            language = settings.language
                        )
                        IntentHelper.sendSms(context, txn.partyPhone, smsText)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("receipt_sms_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = smsColor)
                ) {
                    Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SMS পাঠান")
                }

                Button(
                    onClick = {
                        val tempFile = PrintPdfHelper.createTempReceiptPdfForShare(
                            context, shop, txn, currency, settings.language
                        )
                        if (tempFile != null) {
                            IntentHelper.shareTempPdfFile(context, tempFile, "${shop.name} - ${txn.receiptNumber}")
                        } else {
                            val msg = Formatters.formatReceiptSmsMessage(
                                shopName = shop.name,
                                txn = txn,
                                currencySymbol = currency,
                                language = settings.language
                            )
                            IntentHelper.openWhatsApp(context, txn.partyPhone, msg)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("receipt_whatsapp_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.paymentGreen)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp / শেয়ার")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmActionDialog(
            title = "লেনদেন ডিলিট করবেন?",
            message = "এই লেনদেন রিসাইকেল বিনে পাঠানো হবে এবং ব্যালেন্স স্বয়ংক্রিয়ভাবে সমন্বয় হবে।",
            confirmText = "ডিলিট করুন",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDeleteTransaction(txn.id)
                onBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String,
    highlightColor: Color? = null
) {
    val colors = LocalTalikhataColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = highlightColor ?: colors.textPrimary
        )
    }
}
