package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import com.example.data.local.ShopEntity
import com.example.data.local.TransactionType
import com.example.ui.components.DueAgeingDonutCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonthlyPerformanceChartCard
import com.example.ui.components.SearchBarField
import com.example.ui.theme.LocalTalikhataColors
import com.example.ui.viewmodel.MainUiState
import com.example.util.Formatters
import com.example.util.PrintPdfHelper

enum class ReportTab(val label: String) {
    DAILY("দৈনিক সারসংক্ষেপ"),
    MONTHLY("মাসিক রিপোর্ট"),
    CUSTOMER_DUE("কাস্টমার পাওনা"),
    SUPPLIER_PAYABLE("সাপ্লায়ার দেনা"),
    DATE_RANGE("তারিখ ভিত্তিক"),
    TRANSACTIONS("লেনদেন রিপোর্ট"),
    DUE_AGEING("বকেয়া বয়স")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    uiState: MainUiState,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val colors = LocalTalikhataColors.current
    val currency = uiState.settings.currencySymbol
    val lang = uiState.settings.language
    val shop = uiState.activeShop ?: ShopEntity(name = "Amol Pharmacy")

    var selectedTab by remember { mutableStateOf(ReportTab.DAILY) }
    var search by remember { mutableStateOf("") }
    var startDate by remember { mutableLongStateOf(Formatters.startOfTodayMillis() - 30L * 24 * 60 * 60 * 1000) }
    var endDate by remember { mutableLongStateOf(Formatters.endOfDayMillis(System.currentTimeMillis())) }
    var pickingStart by remember { mutableStateOf<Boolean?>(null) }

    val reportRows: List<Pair<String, String>> = remember(selectedTab, uiState, search, startDate, endDate) {
        when (selectedTab) {
            ReportTab.DAILY -> {
                val todayTxns = uiState.metrics.todayTransactions
                val dueAdded = todayTxns.filter { it.type == TransactionType.DUE_ADDED }.sumOf { it.amount }
                val collected = todayTxns.filter { it.type == TransactionType.PAYMENT_RECEIVED }.sumOf { it.amount }
                val goodsRec = todayTxns.filter { it.type == TransactionType.GOODS_RECEIVED }.sumOf { it.amount }
                val supPaid = todayTxns.filter { it.type == TransactionType.SUPPLIER_PAYMENT }.sumOf { it.amount }
                listOf(
                    "আজকের কাস্টমার পাওনা যোগ" to Formatters.formatMoney(dueAdded, currency),
                    "আজকের কাস্টমার আদায়" to Formatters.formatMoney(collected, currency),
                    "আজকের সাপ্লায়ার মালামাল গ্রহণ" to Formatters.formatMoney(goodsRec, currency),
                    "আজকের সাপ্লায়ার প্রদান" to Formatters.formatMoney(supPaid, currency),
                    "আজকের মোট লেনদেন সংখ্যা" to "${todayTxns.size} টি"
                )
            }
            ReportTab.MONTHLY -> uiState.metrics.monthlySeries.map { m ->
                m.label to "আদায়: ${Formatters.formatMoney(m.collectionAmount, currency)} | বাকি/প্রদান: ${Formatters.formatMoney(m.paymentAndDueAmount, currency)}"
            }
            ReportTab.CUSTOMER_DUE -> uiState.customers
                .filter {
                    it.currentDue > 0.0 && (search.isBlank() || it.name.contains(search, true) || it.phone.contains(search, true))
                }
                .sortedByDescending { it.currentDue }
                .map { "${it.name} (${it.customerCode})" to Formatters.formatMoney(it.currentDue, currency) }
            ReportTab.SUPPLIER_PAYABLE -> uiState.suppliers
                .filter {
                    it.currentPayable > 0.0 && (search.isBlank() || it.name.contains(search, true) || it.phone.contains(search, true))
                }
                .sortedByDescending { it.currentPayable }
                .map { "${it.name} (${it.supplierCode})" to Formatters.formatMoney(it.currentPayable, currency) }
            ReportTab.DATE_RANGE -> {
                val s = Formatters.startOfTodayMillis().let { minOf(startDate, endDate) }
                val e = Formatters.endOfDayMillis(maxOf(startDate, endDate))
                val rangeTxns = uiState.transactions.filter {
                    it.transactionDate in s..e && (search.isBlank() || it.partyName.contains(search, true))
                }
                rangeTxns.map {
                    "${it.partyName} (${Formatters.formatDate(it.transactionDate, lang)})" to
                        "${Formatters.transactionTypeLabel(it.type, lang)} • ${Formatters.formatMoney(it.amount, currency)}"
                }
            }
            ReportTab.TRANSACTIONS -> uiState.transactions
                .filter { search.isBlank() || it.partyName.contains(search, true) || it.receiptNumber.contains(search, true) }
                .take(100)
                .map {
                    "${it.receiptNumber} • ${it.partyName}" to
                        "${Formatters.transactionTypeLabel(it.type, lang)}: ${Formatters.formatMoney(it.amount, currency)}"
                }
            ReportTab.DUE_AGEING -> uiState.metrics.ageingBuckets.map { b ->
                "${b.label} (${b.customerCount} জন)" to Formatters.formatMoney(b.totalAmount, currency)
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                val csv = buildString {
                    appendLine("বিবরণ,মান")
                    reportRows.forEach { (k, v) ->
                        appendLine("\"${k.replace("\"", "\"\"")}\",\"${v.replace("\"", "\"\"")}\"")
                    }
                }
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                true
            }.getOrDefault(false)
            Toast.makeText(context, if (ok) "রিপোর্ট এক্সপোর্ট সম্পন্ন" else "এক্সপোর্ট ব্যর্থ", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("রিপোর্টস ও বিশ্লেষণ") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reports_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            exportCsvLauncher.launch("Talikhata_${selectedTab.name.lowercase()}.csv")
                        },
                        modifier = Modifier.testTag("reports_export_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = "Export CSV", tint = colors.reportTeal)
                    }
                    IconButton(
                        onClick = {
                            PrintPdfHelper.printReportSummary(context, shop, selectedTab.label, reportRows)
                        },
                        modifier = Modifier.testTag("reports_print_btn")
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = "Print Report", tint = colors.reportTeal)
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
            // Teal Header Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(colors.deepNavy, colors.reportTeal))
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = selectedTab.label,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${shop.name} • লাইভ ডাটাবেস রিপোর্ট",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCCFBF1)
                            )
                        }
                    }
                }
            }

            // Report Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.reportContainer,
                                selectedLabelColor = colors.reportTeal
                            )
                        )
                    }
                }
            }

            // Search + Date Range Controls
            if (selectedTab == ReportTab.DATE_RANGE) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickingStart = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শুরু: ${Formatters.formatDate(startDate, lang)}", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = { pickingStart = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেষ: ${Formatters.formatDate(endDate, lang)}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (selectedTab in listOf(ReportTab.CUSTOMER_DUE, ReportTab.SUPPLIER_PAYABLE, ReportTab.DATE_RANGE, ReportTab.TRANSACTIONS)) {
                item {
                    SearchBarField(
                        query = search,
                        onQueryChange = { search = it },
                        placeholder = "রিপোর্টে খুঁজুন...",
                        testTag = "reports_search_input"
                    )
                }
            }

            // Embedded Charts for Monthly & Due Ageing
            if (selectedTab == ReportTab.MONTHLY) {
                item {
                    MonthlyPerformanceChartCard(
                        series = uiState.metrics.monthlySeries,
                        currencySymbol = currency
                    )
                }
            }
            if (selectedTab == ReportTab.DUE_AGEING) {
                item {
                    DueAgeingDonutCard(
                        buckets = uiState.metrics.ageingBuckets,
                        currencySymbol = currency
                    )
                }
            }

            if (reportRows.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "রিপোর্টে কোনো তথ্য নেই",
                        description = "লেনদেন যুক্ত হলে এখানে বিস্তারিত তালিকা দেখা যাবে।"
                    )
                }
            } else {
                items(reportRows) { (title, value) ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.reportTeal
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickingStart != null) {
        val isStart = pickingStart == true
        val dpState = rememberDatePickerState(initialSelectedDateMillis = if (isStart) startDate else endDate)
        DatePickerDialog(
            onDismissRequest = { pickingStart = null },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { picked ->
                        if (isStart) startDate = picked else endDate = Formatters.endOfDayMillis(picked)
                    }
                    pickingStart = null
                }) { Text("ঠিক আছে") }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = null }) { Text("বাতিল") }
            }
        ) {
            DatePicker(state = dpState)
        }
    }
}
