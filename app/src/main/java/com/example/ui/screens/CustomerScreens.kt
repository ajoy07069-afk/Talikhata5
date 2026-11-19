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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Sms
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
import androidx.compose.ui.draw.clip
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
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.components.SearchBarField
import com.example.ui.theme.LocalTalikhataColors
import com.example.util.Formatters
import com.example.util.IntentHelper

enum class CustomerFilter { ALL, HAS_DUE, SETTLED }
enum class CustomerSort { NAME, HIGHEST_DUE, RECENT }

@Composable
fun CustomerListScreen(
    customers: List<CustomerEntity>,
    settings: AppSettings,
    onAddCustomer: () -> Unit,
    onCustomerClick: (CustomerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalikhataColors.current
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CustomerFilter.ALL) }
    var sort by remember { mutableStateOf(CustomerSort.RECENT) }

    val filtered = remember(customers, searchQuery, filter, sort) {
        customers
            .filter { c ->
                val matchesSearch = searchQuery.isBlank() ||
                    c.name.contains(searchQuery, ignoreCase = true) ||
                    c.phone.contains(searchQuery, ignoreCase = true) ||
                    c.customerCode.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (filter) {
                    CustomerFilter.ALL -> true
                    CustomerFilter.HAS_DUE -> c.currentDue > 0.0
                    CustomerFilter.SETTLED -> c.currentDue <= 0.0
                }
                matchesSearch && matchesFilter
            }
            .let { list ->
                when (sort) {
                    CustomerSort.NAME -> list.sortedBy { it.name.lowercase() }
                    CustomerSort.HIGHEST_DUE -> list.sortedByDescending { it.currentDue }
                    CustomerSort.RECENT -> list.sortedByDescending { it.lastTransactionAt ?: it.createdAt }
                }
            }
    }

    val totalDue = remember(customers) { customers.sumOf { it.currentDue.coerceAtLeast(0.0) } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomer,
                containerColor = colors.customerBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = "নতুন কাস্টমার")
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
            // Blue-themed Header Card
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
                                listOf(colors.deepNavy, colors.customerBlue)
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
                                text = "কাস্টমার তালিকা",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "মোট কাস্টমার: ${customers.size} জন",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDBEAFE)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "মোট পাওনা",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFED7AA)
                            )
                            Text(
                                text = Formatters.formatMoney(totalDue, settings.currencySymbol),
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
                placeholder = "কাস্টমারের নাম, আইডি বা মোবাইল খুঁজুন...",
                testTag = "customer_search_input"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == CustomerFilter.ALL,
                    onClick = { filter = CustomerFilter.ALL },
                    label = { Text("সকল (${customers.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.customerContainer,
                        selectedLabelColor = colors.customerBlue
                    )
                )
                FilterChip(
                    selected = filter == CustomerFilter.HAS_DUE,
                    onClick = { filter = CustomerFilter.HAS_DUE },
                    label = { Text("বাকি আছে") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.dueContainer,
                        selectedLabelColor = colors.dueOrange
                    )
                )
                FilterChip(
                    selected = filter == CustomerFilter.SETTLED,
                    onClick = { filter = CustomerFilter.SETTLED },
                    label = { Text("পরিশোধিত") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.paymentContainer,
                        selectedLabelColor = colors.paymentGreen
                    )
                )
                FilterChip(
                    selected = sort == CustomerSort.HIGHEST_DUE,
                    onClick = {
                        sort = if (sort == CustomerSort.HIGHEST_DUE) CustomerSort.RECENT else CustomerSort.HIGHEST_DUE
                    },
                    label = { Text("বেশি বাকি আগে") }
                )
                FilterChip(
                    selected = sort == CustomerSort.NAME,
                    onClick = {
                        sort = if (sort == CustomerSort.NAME) CustomerSort.RECENT else CustomerSort.NAME
                    },
                    label = { Text("নাম অনুযায়ী") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                EmptyStateView(
                    title = "কোনো কাস্টমার পাওয়া যায়নি",
                    description = "নতুন কাস্টমার যোগ করতে নিচের + বাটনে ট্যাপ করুন।",
                    actionLabel = "নতুন কাস্টমার যোগ করুন",
                    onActionClick = onAddCustomer,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { customer ->
                        CustomerCardItem(
                            customer = customer,
                            currencySymbol = settings.currencySymbol,
                            language = settings.language,
                            onClick = { onCustomerClick(customer) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCardItem(
    customer: CustomerEntity,
    currencySymbol: String,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val colors = LocalTalikhataColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}")
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                PartyAvatar(
                    name = customer.name,
                    photoUri = customer.photoUri,
                    accentColor = colors.customerBlue,
                    containerColor = colors.customerContainer,
                    size = 50.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (customer.phone.isNotBlank()) "${customer.customerCode} • ${customer.phone}" else customer.customerCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.customerBlue,
                        maxLines = 1
                    )
                    Text(
                        text = "শেষ লেনদেন: ${Formatters.formatDate(customer.lastTransactionAt, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (customer.currentDue > 0) "বাকি" else "পরিশোধিত",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (customer.currentDue > 0) colors.dueOrange else colors.paymentGreen
                )
                Text(
                    text = Formatters.formatMoney(customer.currentDue, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (customer.currentDue > 0) colors.dueOrange else colors.paymentGreen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    customer: CustomerEntity,
    transactionsAsc: List<LedgerTransactionEntity>,
    shopName: String,
    settings: AppSettings,
    onBack: () -> Unit,
    onAddDue: () -> Unit,
    onReceivePayment: () -> Unit,
    onEditCustomer: () -> Unit,
    onDeleteCustomer: () -> Unit,
    onOpenFullHistory: () -> Unit,
    onOpenReceipt: (LedgerTransactionEntity) -> Unit,
    onReminderLogged: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val colors = LocalTalikhataColors.current
    val currency = settings.currencySymbol
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val smsMessage = remember(customer, shopName, settings.smsTemplate, currency) {
        Formatters.formatSmsTemplate(
            template = settings.smsTemplate,
            customerName = customer.name,
            customerCode = customer.customerCode,
            dueAmount = customer.currentDue,
            shopName = shopName,
            currencySymbol = currency
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কাস্টমার প্রোফাইল") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("customer_profile_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditCustomer, modifier = Modifier.testTag("customer_edit_btn")) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Customer", tint = colors.customerBlue)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.testTag("customer_delete_btn")) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Customer", tint = colors.warningRed)
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
            // Profile Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PartyAvatar(
                                name = customer.name,
                                photoUri = customer.photoUri,
                                accentColor = colors.customerBlue,
                                containerColor = colors.customerContainer,
                                size = 68.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.customerContainer
                                ) {
                                    Text(
                                        text = customer.customerCode,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.customerBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                if (customer.phone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { IntentHelper.dialPhone(context, customer.phone) }
                                    ) {
                                        Icon(Icons.Rounded.Call, contentDescription = "Call", tint = colors.customerBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(customer.phone, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                    }
                                }
                                if (customer.address.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.LocationOn, contentDescription = "Address", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(customer.address, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Total Due Highlight
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (customer.currentDue > 0) colors.dueContainer else colors.paymentContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "মোট বকেয়া / পাওনা",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = "মোট বাকি: ${Formatters.formatMoney(customer.totalDueAdded, currency)}  •  আদায়: ${Formatters.formatMoney(customer.totalPaid, currency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                Text(
                                    text = Formatters.formatMoney(customer.currentDue, currency),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (customer.currentDue > 0) colors.dueOrange else colors.paymentGreen
                                )
                            }
                        }
                    }
                }
            }

            // Primary Transaction Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddDue,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("profile_add_due_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.dueOrange)
                    ) {
                        Icon(Icons.Rounded.AddCard, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পাওনা যোগ")
                    }
                    Button(
                        onClick = onReceivePayment,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("profile_add_payment_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.paymentGreen)
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("টাকা আদায়")
                    }
                }
            }

            // Secondary Profile Actions: History, WhatsApp, SMS Reminder
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenFullHistory,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("হিস্ট্রি", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            IntentHelper.openWhatsApp(context, customer.phone, smsMessage)
                            onReminderLogged("WhatsApp")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, tint = colors.paymentGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            IntentHelper.sendSms(context, customer.phone, smsMessage)
                            onReminderLogged("SMS")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Sms, contentDescription = null, tint = colors.customerBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS", maxLines = 1)
                    }
                }
            }

            // Transaction History Header
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
                                text = "এই কাস্টমারের এখনো কোনো লেনদেন নেই।",
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
            title = "কাস্টমার ডিলিট করবেন?",
            message = "${customer.name}-কে রিসাইকেল বিনে পাঠানো হবে। প্রয়োজনে রিসাইকেল বিন থেকে পুনরুদ্ধার করা যাবে।",
            confirmText = "ডিলিট করুন",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDeleteCustomer()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun AddEditCustomerDialog(
    initial: CustomerEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, photoUri: String?, openingDue: Double) -> Unit
) {
    val colors = LocalTalikhataColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var photoUri by remember { mutableStateOf(initial?.photoUri) }
    var openingDueText by remember { mutableStateOf("") }
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
                text = if (initial == null) "নতুন কাস্টমার যোগ" else "কাস্টমার তথ্য এডিট",
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
                        name = name.ifBlank { "C" },
                        photoUri = photoUri,
                        accentColor = colors.customerBlue,
                        containerColor = colors.customerContainer,
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
                    label = { Text("কাস্টমারের নাম *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorText = null },
                    label = { Text("মোবাইল নম্বর (ঐচ্ছিক)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (initial == null) {
                    OutlinedTextField(
                        value = openingDueText,
                        onValueChange = { openingDueText = it },
                        label = { Text("পূর্বের বকেয়া (যদি থাকে)") },
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
                        errorText = "কাস্টমারের নাম লিখুন"
                        return@Button
                    }
                    val opening = openingDueText.toDoubleOrNull() ?: 0.0
                    onSave(name, phone.trim(), address, photoUri, opening)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.customerBlue),
                modifier = Modifier.testTag("save_customer_btn")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
