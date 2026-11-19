package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettings
import com.example.data.local.CustomerEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.theme.LocalTalikhataColors
import com.example.util.Formatters
import com.example.util.IntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueReminderScreen(
    customers: List<CustomerEntity>,
    shopName: String,
    settings: AppSettings,
    onBack: () -> Unit,
    onViewProfile: (Long) -> Unit,
    onMarkReminderSent: (Long, String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val colors = LocalTalikhataColors.current
    val dueCustomers = remember(customers) {
        customers
            .filter { it.currentDue > 0.0 }
            .sortedByDescending { Formatters.daysSince(it.lastPaymentAt ?: it.lastTransactionAt ?: it.createdAt) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বাকি তাগাদা (Due Reminder)") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("due_reminder_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (dueCustomers.isEmpty()) {
            EmptyStateView(
                title = "কোনো বকেয়া তাগাদা বাকি নেই",
                description = "সব কাস্টমারের হিসাব পরিশোধিত রয়েছে।",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dueCustomers, key = { it.id }) { customer ->
                    val daysOverdue = Formatters.daysSince(
                        customer.lastPaymentAt ?: customer.lastTransactionAt ?: customer.createdAt
                    )
                    val isOverdue = daysOverdue >= settings.overdueDaysThreshold
                    val message = Formatters.formatSmsTemplate(
                        template = settings.smsTemplate,
                        customerName = customer.name,
                        customerCode = customer.customerCode,
                        dueAmount = customer.currentDue,
                        shopName = shopName,
                        currencySymbol = settings.currencySymbol
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    PartyAvatar(
                                        name = customer.name,
                                        photoUri = customer.photoUri,
                                        accentColor = colors.customerBlue,
                                        containerColor = colors.customerContainer,
                                        size = 44.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "শেষ পেমেন্ট: ${Formatters.formatDate(customer.lastPaymentAt, settings.language)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Formatters.formatMoney(customer.currentDue, settings.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.dueOrange
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isOverdue) colors.warningContainer else colors.dueContainer
                                    ) {
                                        Text(
                                            text = "$daysOverdue দিন বকেয়া",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOverdue) colors.warningRed else colors.dueOrange,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Reminder status strip
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (customer.lastReminderSentAt != null) colors.paymentContainer else colors.subtleDivider.copy(alpha = 0.35f)
                            ) {
                                Text(
                                    text = if (customer.lastReminderSentAt != null) {
                                        "সর্বশেষ তাগাদা: ${Formatters.formatDateTime(customer.lastReminderSentAt, settings.language)} (${customer.reminderCount} বার)"
                                    } else {
                                        "স্ট্যাটাস: এখনো তাগাদা পাঠানো হয়নি"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (customer.lastReminderSentAt != null) colors.paymentGreen else colors.textSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        IntentHelper.sendSms(context, customer.phone, message)
                                        onMarkReminderSent(customer.id, "SMS")
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.customerBlue)
                                ) {
                                    Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SMS", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = {
                                        IntentHelper.openWhatsApp(context, customer.phone, message)
                                        onMarkReminderSent(customer.id, "WhatsApp")
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.paymentGreen)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onViewProfile(customer.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("প্রোফাইল", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { onMarkReminderSent(customer.id, "Manual") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("পাঠানো চিহ্নিত", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
