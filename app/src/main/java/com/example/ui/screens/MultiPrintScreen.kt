package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettings
import com.example.data.local.CustomerEntity
import com.example.data.local.ShopEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.components.SearchBarField
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.DueOrange
import com.example.ui.theme.LocalTalikhataColors
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MultiPrintState
import com.example.util.CustomerStatementInput
import com.example.util.CustomerStatementPageModel
import com.example.util.Formatters
import com.example.util.PrintPdfHelper
import kotlinx.coroutines.launch

@Composable
fun MultiPrintScreen(
    customers: List<CustomerEntity>,
    activeShop: ShopEntity?,
    settings: AppSettings,
    state: MultiPrintState,
    onSearchChange: (String) -> Unit,
    onMinDueChange: (Double) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: (List<CustomerEntity>) -> Unit,
    onDeselectAll: () -> Unit,
    onCommonNoteChange: (String) -> Unit,
    onToggleHistory: (Boolean) -> Unit,
    onPreparePreview: (onReady: (List<CustomerStatementInput>) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalTalikhataColors.current
    var showPreviewScreen by remember { mutableStateOf(false) }
    var preparedInputs by remember { mutableStateOf<List<CustomerStatementInput>>(emptyList()) }

    val filteredCustomers = remember(customers, state.searchQuery, state.minDueFilter) {
        customers.filter { c ->
            val matchesQuery = state.searchQuery.isBlank() ||
                c.name.contains(state.searchQuery, ignoreCase = true) ||
                c.phone.contains(state.searchQuery, ignoreCase = true) ||
                c.customerCode.contains(state.searchQuery, ignoreCase = true)
            val matchesDue = when {
                state.minDueFilter < 0 -> true
                state.minDueFilter == 0.0 -> c.currentDue > 0.0
                else -> c.currentDue >= state.minDueFilter
            }
            matchesQuery && matchesDue
        }.sortedByDescending { it.currentDue }
    }

    if (showPreviewScreen) {
        MultiPrintA4PreviewScreen(
            shop = activeShop ?: ShopEntity(name = "Amol Pharmacy"),
            settings = settings,
            pages = state.previewPages,
            commonNote = state.commonNote,
            includeHistory = state.includeHistory,
            onBack = { showPreviewScreen = false },
            onPrintAll = {
                val shop = activeShop ?: ShopEntity(name = "Amol Pharmacy")
                PrintPdfHelper.printMultiCustomerStatements(
                    context = context,
                    shop = shop,
                    inputs = preparedInputs,
                    commonNote = state.commonNote,
                    includeHistory = state.includeHistory,
                    showAddress = settings.printShowAddress,
                    showPhone = settings.printShowPhone,
                    currencySymbol = settings.currencySymbol,
                    language = settings.language
                )
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        // Premium Multi Print Banner
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
                            listOf(DeepNavy, colors.reportTeal)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "মাল্টি প্রিন্ট (A4 স্টেটমেন্ট)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "প্রতিটি কাস্টমারের জন্য আলাদা A4 বিবরণী তৈরি ও ব্যাচ প্রিন্ট",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCCFBF1)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${state.selectedCustomerIds.size}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "নির্বাচিত",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCCFBF1)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Controls Card: Common Note + History Toggle
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.commonNote,
                    onValueChange = onCommonNoteChange,
                    label = { Text("সাধারণ নোট (সবার বিবরণীতে থাকবে)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("multiprint_common_note")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "লেনদেনের সম্পূর্ণ ইতিহাস দেখান",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (state.includeHistory) "চালু: প্রথম থেকে সর্বশেষ লেনদেন থাকবে" else "বন্ধ: শুধুমাত্র সর্বমোট পাওনা দেখাবে",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                    Switch(
                        checked = state.includeHistory,
                        onCheckedChange = onToggleHistory,
                        modifier = Modifier.testTag("multiprint_history_toggle")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        SearchBarField(
            query = state.searchQuery,
            onQueryChange = onSearchChange,
            placeholder = "কাস্টমার খুঁজুন (নাম, আইডি, মোবাইল)...",
            testTag = "multiprint_search"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Due Filter Chips + Select All / Deselect All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = state.minDueFilter == 0.0,
                onClick = { onMinDueChange(0.0) },
                label = { Text("বাকি আছে (>৳০)") }
            )
            FilterChip(
                selected = state.minDueFilter == 500.0,
                onClick = { onMinDueChange(500.0) },
                label = { Text("৳৫০০+") }
            )
            FilterChip(
                selected = state.minDueFilter == 1000.0,
                onClick = { onMinDueChange(1000.0) },
                label = { Text("৳১,০০০+") }
            )
            FilterChip(
                selected = state.minDueFilter < 0,
                onClick = { onMinDueChange(-1.0) },
                label = { Text("সব কাস্টমার") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onSelectAll(filteredCustomers) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("multiprint_select_all")
                ) {
                    Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সব নির্বাচন", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onDeselectAll,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("multiprint_deselect_all")
                ) {
                    Icon(Icons.Rounded.Deselect, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("বাতিল", style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                text = "মোট প্রদর্শিত: ${filteredCustomers.size}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (filteredCustomers.isEmpty()) {
            EmptyStateView(
                title = "প্রিন্টের জন্য কাস্টমার নেই",
                description = "ফিল্টার পরিবর্তন করুন অথবা নতুন কাস্টমার ও বকেয়া যুক্ত করুন।",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    val checked = customer.id in state.selectedCustomerIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleSelect(customer.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (checked) colors.customerContainer.copy(alpha = 0.5f) else colors.cardBackground
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggleSelect(customer.id) },
                                colors = CheckboxDefaults.colors(checkedColor = colors.customerBlue)
                            )
                            PartyAvatar(
                                name = customer.name,
                                photoUri = customer.photoUri,
                                accentColor = colors.customerBlue,
                                containerColor = colors.customerContainer,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (customer.phone.isNotBlank()) "${customer.customerCode} • ${customer.phone}" else customer.customerCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = Formatters.formatMoney(customer.currentDue, settings.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (customer.currentDue > 0) colors.dueOrange else colors.paymentGreen
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar: A4 Print Preview + Print All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onPreparePreview { inputs ->
                        preparedInputs = inputs
                        showPreviewScreen = true
                    }
                },
                enabled = state.selectedCustomerIds.isNotEmpty() && !state.isPreparingPreview,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("multiprint_preview_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("A4 প্রিভিউ")
            }
            Button(
                onClick = {
                    onPreparePreview { inputs ->
                        val shop = activeShop ?: ShopEntity(name = "Amol Pharmacy")
                        PrintPdfHelper.printMultiCustomerStatements(
                            context = context,
                            shop = shop,
                            inputs = inputs,
                            commonNote = state.commonNote,
                            includeHistory = state.includeHistory,
                            showAddress = settings.printShowAddress,
                            showPhone = settings.printShowPhone,
                            currencySymbol = settings.currencySymbol,
                            language = settings.language
                        )
                    }
                },
                enabled = state.selectedCustomerIds.isNotEmpty() && !state.isPreparingPreview,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("multiprint_print_all_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.reportTeal)
            ) {
                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print All (${state.selectedCustomerIds.size})")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPrintA4PreviewScreen(
    shop: ShopEntity,
    settings: AppSettings,
    pages: List<CustomerStatementPageModel>,
    commonNote: String,
    includeHistory: Boolean,
    onBack: () -> Unit,
    onPrintAll: () -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = LocalTalikhataColors.current
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("A4 প্রিন্ট প্রিভিউ", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "পৃষ্ঠা ${pagerState.currentPage + 1} / ${pages.size.coerceAtLeast(1)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("preview_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = onPrintAll,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.reportTeal),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("preview_print_all_btn")
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print All")
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
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pages.isEmpty()) {
                EmptyStateView(
                    title = "প্রিভিউ করার মতো পৃষ্ঠা নেই",
                    description = "ফিরে গিয়ে কাস্টমার নির্বাচন করুন।"
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    pageSpacing = 12.dp
                ) { idx ->
                    val pageModel = pages[idx]
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        A4PaperSheetPreview(
                            shop = shop,
                            settings = settings,
                            model = pageModel,
                            commonNote = commonNote,
                            includeHistory = includeHistory
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Text("পূর্বের পৃষ্ঠা")
                    }
                    Text(
                        text = "${pages[pagerState.currentPage].customer.name} (${pages[pagerState.currentPage].pageNumberForCustomer}/${pages[pagerState.currentPage].totalPagesForCustomer})",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = {
                            if (pagerState.currentPage < pages.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        enabled = pagerState.currentPage < pages.lastIndex
                    ) {
                        Text("পরবর্তী পৃষ্ঠা")
                    }
                }
            }
        }
    }
}

@Composable
private fun A4PaperSheetPreview(
    shop: ShopEntity,
    settings: AppSettings,
    model: CustomerStatementPageModel,
    commonNote: String,
    includeHistory: Boolean
) {
    // True A4 aspect ratio (595:842 ~ 0.707) rendered as crisp white paper with dark ink
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(595f / 842f),
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Shop Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DeepNavy)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = shop.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (model.isFirstPage) {
                            val meta = buildList {
                                if (settings.printShowAddress && shop.address.isNotBlank()) add(shop.address)
                                if (settings.printShowPhone && shop.phone.isNotBlank()) add("মোবাইল: ${shop.phone}")
                            }.joinToString(" | ")
                            if (meta.isNotBlank()) {
                                Text(meta, fontSize = 8.5.sp, color = Color(0xFFDBEAFE))
                            }
                            Text(
                                text = "তারিখ: ${Formatters.formatDate(System.currentTimeMillis(), settings.language)}",
                                fontSize = 8.5.sp,
                                color = Color(0xFFBFDBFE)
                            )
                        } else {
                            Text(
                                text = "${model.customer.name} (${model.customer.customerCode}) - চলমান",
                                fontSize = 9.sp,
                                color = Color(0xFFDBEAFE)
                            )
                        }
                    }
                    Text(
                        text = "পৃষ্ঠা ${model.pageNumberForCustomer}/${model.totalPagesForCustomer}",
                        fontSize = 8.5.sp,
                        color = Color(0xFFBFDBFE)
                    )
                }
            }

            if (model.isFirstPage) {
                // Customer Block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEFF6FF))
                        .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "কাস্টমার: ${model.customer.name}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "আইডি: ${model.customer.customerCode}",
                            fontSize = 9.sp,
                            color = Color(0xFF334155)
                        )
                    }
                    Text(
                        text = "মোবাইল: ${model.customer.phone.ifBlank { "সংরক্ষিত নেই" }}",
                        fontSize = 9.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "ঠিকানা: ${model.customer.address.ifBlank { "সংরক্ষিত নেই" }}",
                        fontSize = 9.sp,
                        color = Color(0xFF334155)
                    )
                }

                // Common Note
                if (commonNote.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = commonNote,
                            fontSize = 9.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // Total Due Box (NO separate accounting summary table!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFF7ED))
                        .border(1.2.dp, DueOrange, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সর্বমোট পাওনা:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = Formatters.formatMoney(model.customer.currentDue, settings.currencySymbol),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DueOrange
                    )
                }
            }

            // Optional Complete Transaction History (ONLY when includeHistory == true)
            if (includeHistory && model.transactionsSlice.isNotEmpty()) {
                Text(
                    text = "লেনদেনের বিবরণ (প্রথম থেকে সর্বশেষ)",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.6.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryBlue)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text("তারিখ", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1.2f))
                        Text("ধরন", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1.2f))
                        Text("নোট", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1.3f))
                        Text("পরিমাণ", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                    model.transactionsSlice.forEachIndexed { idx, txn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                Formatters.formatDate(txn.transactionDate, settings.language),
                                fontSize = 8.sp,
                                color = Color(0xFF334155),
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                Formatters.transactionTypeLabel(txn.type, settings.language),
                                fontSize = 8.sp,
                                color = Color(0xFF334155),
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                txn.note.ifBlank { "-" },
                                fontSize = 8.sp,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.3f)
                            )
                            Text(
                                Formatters.formatMoney(txn.amount, settings.currencySymbol),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Talikhata • ${shop.name}",
                fontSize = 7.5.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
