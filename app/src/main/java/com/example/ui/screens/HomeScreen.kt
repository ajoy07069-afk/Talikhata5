package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.SupplierEntity
import com.example.data.local.TransactionType
import com.example.ui.components.MonthlyPerformanceChartCard
import com.example.ui.components.PartyAvatar
import com.example.ui.components.QuickActionTile
import com.example.ui.components.SearchBarField
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatKpiCard
import com.example.ui.components.TalikhataBrandLogo
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LocalTalikhataColors
import com.example.ui.viewmodel.MainUiState
import com.example.util.Formatters

@Composable
fun HomeDashboardScreen(
    uiState: MainUiState,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenShops: () -> Unit,
    onOpenAddCustomer: () -> Unit,
    onOpenAddSupplier: () -> Unit,
    onQuickCustomerTxn: (TransactionType) -> Unit,
    onQuickSupplierTxn: (TransactionType) -> Unit,
    onOpenMultiPrint: () -> Unit,
    onOpenCustomerProfile: (Long) -> Unit,
    onOpenSupplierProfile: (Long) -> Unit,
    onOpenReceipt: (LedgerTransactionEntity) -> Unit,
    onOpenAllCustomers: () -> Unit,
    onOpenAllSuppliers: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalikhataColors.current
    val currency = uiState.settings.currencySymbol
    val lang = uiState.settings.language
    val activeShop = uiState.activeShop
    val metrics = uiState.metrics

    var quickSearch by remember { mutableStateOf("") }

    val matchingCustomers = remember(quickSearch, uiState.customers) {
        if (quickSearch.isBlank()) emptyList()
        else uiState.customers.filter {
            it.name.contains(quickSearch, ignoreCase = true) ||
                it.phone.contains(quickSearch, ignoreCase = true) ||
                it.customerCode.contains(quickSearch, ignoreCase = true)
        }.take(4)
    }

    val matchingSuppliers = remember(quickSearch, uiState.suppliers) {
        if (quickSearch.isBlank()) emptyList()
        else uiState.suppliers.filter {
            it.name.contains(quickSearch, ignoreCase = true) ||
                it.phone.contains(quickSearch, ignoreCase = true) ||
                it.supplierCode.contains(quickSearch, ignoreCase = true)
        }.take(4)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Branded Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenShops)
                        .padding(4.dp)
                ) {
                    TalikhataBrandLogo(size = 46.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Talikhata",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Storefront,
                                contentDescription = "Active shop",
                                tint = colors.customerBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeShop?.name ?: "Amol Pharmacy",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.customerBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (metrics.overdueCustomersCount > 0) {
                                    Badge(containerColor = colors.warningRed) {
                                        Text(metrics.overdueCustomersCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "Due Reminders",
                                tint = colors.textPrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = colors.textPrimary
                        )
                    }
                }
            }
        }

        // Hero Pharmacy Summary Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pharmacy_banner),
                        contentDescription = "Amol Pharmacy Overview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        DeepNavy.copy(alpha = 0.92f),
                                        DeepNavy.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activeShop?.name ?: "Amol Pharmacy",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = Formatters.formatDate(System.currentTimeMillis(), lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFBFDBFE)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "অফলাইন সক্রিয়",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "মোট পাওনা (কাস্টমার)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFED7AA)
                                )
                                Text(
                                    text = Formatters.formatMoney(metrics.totalReceivable, currency),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "মোট দেনা (সাপ্লায়ার)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDDD6FE)
                                )
                                Text(
                                    text = Formatters.formatMoney(metrics.totalPayable, currency),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Search
        item {
            SearchBarField(
                query = quickSearch,
                onQueryChange = { quickSearch = it },
                placeholder = "দ্রুত খুঁজুন: কাস্টমার বা সাপ্লায়ারের নাম / মোবাইল...",
                testTag = "home_quick_search"
            )
            if (quickSearch.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (matchingCustomers.isEmpty() && matchingSuppliers.isEmpty()) {
                            Text(
                                text = "কোনো মিল পাওয়া যায়নি",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        } else {
                            matchingCustomers.forEach { c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenCustomerProfile(c.id) }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${c.name} (${c.customerCode})", style = MaterialTheme.typography.titleSmall)
                                        Text("কাস্টমার • ${c.phone}", style = MaterialTheme.typography.bodySmall, color = colors.customerBlue)
                                    }
                                    Text(
                                        Formatters.formatMoney(c.currentDue, currency),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.dueOrange
                                    )
                                }
                            }
                            matchingSuppliers.forEach { s ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenSupplierProfile(s.id) }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${s.name} (${s.supplierCode})", style = MaterialTheme.typography.titleSmall)
                                        Text("সাপ্লায়ার • ${s.phone}", style = MaterialTheme.typography.bodySmall, color = colors.supplierPurple)
                                    }
                                    Text(
                                        Formatters.formatMoney(s.currentPayable, currency),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.supplierPurple
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6 KPI Cards (2 rows x 3 cols or 3 rows x 2 cols for clean readability)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatKpiCard(
                        title = "মোট পাওনা",
                        value = Formatters.formatMoney(metrics.totalReceivable, currency),
                        icon = Icons.Rounded.TrendingUp,
                        accentColor = colors.dueOrange,
                        containerColor = colors.dueContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAllCustomers
                    )
                    StatKpiCard(
                        title = "মোট দেনা",
                        value = Formatters.formatMoney(metrics.totalPayable, currency),
                        icon = Icons.Rounded.TrendingDown,
                        accentColor = colors.supplierPurple,
                        containerColor = colors.supplierContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAllSuppliers
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatKpiCard(
                        title = "আজকের আদায়",
                        value = Formatters.formatMoney(metrics.todayCollection, currency),
                        icon = Icons.Rounded.Payments,
                        accentColor = colors.paymentGreen,
                        containerColor = colors.paymentContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHistory
                    )
                    StatKpiCard(
                        title = "আজকের প্রদান",
                        value = Formatters.formatMoney(metrics.todayPayment, currency),
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = colors.warningRed,
                        containerColor = colors.warningContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHistory
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatKpiCard(
                        title = "মোট Customer",
                        value = metrics.totalCustomers.toString(),
                        icon = Icons.Rounded.Groups,
                        accentColor = colors.customerBlue,
                        containerColor = colors.customerContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAllCustomers
                    )
                    StatKpiCard(
                        title = "মোট Supplier",
                        value = metrics.totalSuppliers.toString(),
                        icon = Icons.Rounded.LocalShipping,
                        accentColor = colors.supplierPurple,
                        containerColor = colors.supplierContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAllSuppliers
                    )
                }
            }
        }

        // Quick Action Buttons (7 Actions)
        item {
            SectionHeader(title = "দ্রুত কার্যক্রম")
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile(
                        label = "নতুন Customer",
                        icon = Icons.Rounded.PersonAdd,
                        accentColor = colors.customerBlue,
                        containerColor = colors.customerContainer,
                        testTag = "qa_new_customer",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAddCustomer
                    )
                    QuickActionTile(
                        label = "নতুন Supplier",
                        icon = Icons.Rounded.LocalShipping,
                        accentColor = colors.supplierPurple,
                        containerColor = colors.supplierContainer,
                        testTag = "qa_new_supplier",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAddSupplier
                    )
                    QuickActionTile(
                        label = "পাওনা যোগ",
                        icon = Icons.Rounded.AddCard,
                        accentColor = colors.dueOrange,
                        containerColor = colors.dueContainer,
                        testTag = "qa_add_due",
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickCustomerTxn(TransactionType.DUE_ADDED) }
                    )
                    QuickActionTile(
                        label = "টাকা আদায়",
                        icon = Icons.Rounded.Payments,
                        accentColor = colors.paymentGreen,
                        containerColor = colors.paymentContainer,
                        testTag = "qa_receive_payment",
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickCustomerTxn(TransactionType.PAYMENT_RECEIVED) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile(
                        label = "মালামাল গ্রহণ",
                        icon = Icons.Rounded.Inventory2,
                        accentColor = colors.supplierPurple,
                        containerColor = colors.supplierContainer,
                        testTag = "qa_goods_received",
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickSupplierTxn(TransactionType.GOODS_RECEIVED) }
                    )
                    QuickActionTile(
                        label = "Supplier Payment",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = colors.paymentGreen,
                        containerColor = colors.paymentContainer,
                        testTag = "qa_supplier_payment",
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickSupplierTxn(TransactionType.SUPPLIER_PAYMENT) }
                    )
                    QuickActionTile(
                        label = "Multi Print",
                        icon = Icons.Rounded.Print,
                        accentColor = colors.reportTeal,
                        containerColor = colors.reportContainer,
                        testTag = "qa_multi_print",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenMultiPrint
                    )
                }
            }
        }

        // Due Reminder Banner
        if (metrics.overdueCustomersCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenNotifications),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.dueContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Rounded.WarningAmber,
                                contentDescription = "Overdue alert",
                                tint = colors.dueOrange,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "বকেয়া তাগাদা সতর্কতা",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${metrics.overdueCustomersCount} জন কাস্টমারের বকেয়া নির্ধারিত সময়ের বেশি হয়েছে",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Text(
                            text = "দেখুন",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.dueOrange
                        )
                    }
                }
            }
        }

        // Monthly Performance Chart
        item {
            MonthlyPerformanceChartCard(
                series = metrics.monthlySeries,
                currencySymbol = currency
            )
        }

        // Today's Transactions
        item {
            SectionHeader(
                title = "আজকের লেনদেন (${metrics.todayTransactions.size})",
                actionLabel = "সব দেখুন",
                onActionClick = onOpenHistory
            )
        }
        if (metrics.todayTransactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "আজ এখনো কোনো লেনদেন যুক্ত হয়নি। উপরের দ্রুত কার্যক্রম থেকে শুরু করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        } else {
            items(metrics.todayTransactions.take(5), key = { it.id }) { txn ->
                TransactionRowCard(
                    txn = txn,
                    currencySymbol = currency,
                    language = lang,
                    onClick = { onOpenReceipt(txn) }
                )
            }
        }

        // Recent Customers
        if (metrics.recentCustomers.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "সাম্প্রতিক কাস্টমার",
                    actionLabel = "সব",
                    onActionClick = onOpenAllCustomers
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(metrics.recentCustomers, key = { it.id }) { cust ->
                        RecentPartyChip(
                            name = cust.name,
                            photoUri = cust.photoUri,
                            amountText = Formatters.formatMoney(cust.currentDue, currency),
                            accentColor = colors.customerBlue,
                            containerColor = colors.customerContainer,
                            amountColor = if (cust.currentDue > 0) colors.dueOrange else colors.paymentGreen,
                            onClick = { onOpenCustomerProfile(cust.id) }
                        )
                    }
                }
            }
        }

        // Recent Suppliers
        if (metrics.recentSuppliers.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "সাম্প্রতিক সাপ্লায়ার",
                    actionLabel = "সব",
                    onActionClick = onOpenAllSuppliers
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(metrics.recentSuppliers, key = { it.id }) { sup ->
                        RecentPartyChip(
                            name = sup.name,
                            photoUri = sup.photoUri,
                            amountText = Formatters.formatMoney(sup.currentPayable, currency),
                            accentColor = colors.supplierPurple,
                            containerColor = colors.supplierContainer,
                            amountColor = colors.supplierPurple,
                            onClick = { onOpenSupplierProfile(sup.id) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun RecentPartyChip(
    name: String,
    photoUri: String?,
    amountText: String,
    accentColor: Color,
    containerColor: Color,
    amountColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalTalikhataColors.current
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PartyAvatar(
                name = name,
                photoUri = photoUri,
                accentColor = accentColor,
                containerColor = containerColor,
                size = 44.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amountText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    txn: LedgerTransactionEntity,
    currencySymbol: String,
    language: com.example.data.local.AppLanguage,
    onClick: () -> Unit
) {
    val colors = LocalTalikhataColors.current
    val isInflow = txn.type == TransactionType.PAYMENT_RECEIVED
    val isDueOrGoods = txn.type == TransactionType.DUE_ADDED || txn.type == TransactionType.GOODS_RECEIVED
    val accent = when (txn.type) {
        TransactionType.PAYMENT_RECEIVED -> colors.paymentGreen
        TransactionType.DUE_ADDED -> colors.dueOrange
        TransactionType.GOODS_RECEIVED -> colors.supplierPurple
        TransactionType.SUPPLIER_PAYMENT -> colors.customerBlue
    }
    val container = when (txn.type) {
        TransactionType.PAYMENT_RECEIVED -> colors.paymentContainer
        TransactionType.DUE_ADDED -> colors.dueContainer
        TransactionType.GOODS_RECEIVED -> colors.supplierContainer
        TransactionType.SUPPLIER_PAYMENT -> colors.customerContainer
    }

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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isInflow) Icons.Rounded.Payments else if (isDueOrGoods) Icons.Rounded.AddCard else Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = txn.partyName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Formatters.transactionTypeLabel(txn.type, language)} • ${Formatters.formatDate(txn.transactionDate, language)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    if (txn.note.isNotBlank()) {
                        Text(
                            text = txn.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatMoney(txn.amount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
                Text(
                    text = "জের: ${Formatters.formatMoney(txn.newBalance, currencySymbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
