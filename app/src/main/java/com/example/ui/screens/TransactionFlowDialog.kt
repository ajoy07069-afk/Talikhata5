package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.local.CustomerEntity
import com.example.data.local.PartyType
import com.example.data.local.SupplierEntity
import com.example.data.local.TransactionType
import com.example.ui.theme.LocalTalikhataColors
import com.example.util.Formatters
import java.util.UUID

data class TransactionDraftRequest(
    val partyType: PartyType,
    val initialPartyId: Long? = null,
    val transactionType: TransactionType
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryDialog(
    request: TransactionDraftRequest,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    currencySymbol: String,
    language: AppLanguage,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirmSave: (
        partyType: PartyType,
        partyId: Long,
        type: TransactionType,
        amount: Double,
        note: String,
        dateMillis: Long,
        idempotencyKey: String
    ) -> Unit
) {
    val colors = LocalTalikhataColors.current
    var selectedPartyId by remember(request) {
        mutableStateOf(
            request.initialPartyId ?: if (request.partyType == PartyType.CUSTOMER) {
                customers.firstOrNull()?.id
            } else {
                suppliers.firstOrNull()?.id
            }
        )
    }
    var partyDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCustomer = customers.firstOrNull { it.id == selectedPartyId }
    val selectedSupplier = suppliers.firstOrNull { it.id == selectedPartyId }

    val previousBalance = if (request.partyType == PartyType.CUSTOMER) {
        selectedCustomer?.currentDue ?: 0.0
    } else {
        selectedSupplier?.currentPayable ?: 0.0
    }

    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showConfirmStep by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val idempotencyKey = remember(request) { UUID.randomUUID().toString() }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val newBalance = when (request.transactionType) {
        TransactionType.DUE_ADDED, TransactionType.GOODS_RECEIVED -> previousBalance + parsedAmount
        TransactionType.PAYMENT_RECEIVED, TransactionType.SUPPLIER_PAYMENT -> previousBalance - parsedAmount
    }

    val accentColor = when (request.transactionType) {
        TransactionType.DUE_ADDED -> colors.dueOrange
        TransactionType.PAYMENT_RECEIVED -> colors.paymentGreen
        TransactionType.GOODS_RECEIVED -> colors.supplierPurple
        TransactionType.SUPPLIER_PAYMENT -> colors.paymentGreen
    }

    if (showConfirmStep) {
        val partyName = if (request.partyType == PartyType.CUSTOMER) {
            selectedCustomer?.name ?: ""
        } else {
            selectedSupplier?.name ?: ""
        }
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showConfirmStep = false },
            title = { Text("লেনদেন নিশ্চিত করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${Formatters.transactionTypeLabel(request.transactionType, language)} • $partyName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.subtleDivider.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("পূর্বের ব্যালেন্স:", color = colors.textSecondary)
                                Text(Formatters.formatMoney(previousBalance, currencySymbol))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("পরিমাণ:", color = colors.textSecondary)
                                Text(
                                    Formatters.formatMoney(parsedAmount, currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("নতুন ব্যালেন্স:", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    Formatters.formatMoney(newBalance, currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                    if (newBalance < 0.0) {
                        Text(
                            text = "সতর্কতা: প্রদত্ত পরিমাণ বর্তমান বকেয়ার চেয়ে বেশি (অগ্রিম হিসাব হবে)।",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.dueOrange
                        )
                    }
                    if (noteText.isNotBlank()) {
                        Text("নোট: $noteText", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        val pId = selectedPartyId ?: return@Button
                        onConfirmSave(
                            request.partyType,
                            pId,
                            request.transactionType,
                            parsedAmount,
                            noteText,
                            selectedDate,
                            idempotencyKey
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.testTag("confirm_save_transaction_btn")
                ) {
                    Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "নিশ্চিত করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmStep = false }, enabled = !isSubmitting) {
                    Text("ফিরে যান")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Formatters.transactionTypeLabel(request.transactionType, language),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Party selector if not fixed or allow switching when opened from Quick Action
                val partyLabel = if (request.partyType == PartyType.CUSTOMER) {
                    selectedCustomer?.let { "${it.name} (${it.customerCode})" } ?: "কাস্টমার নির্বাচন করুন"
                } else {
                    selectedSupplier?.let { "${it.name} (${it.supplierCode})" } ?: "সাপ্লায়ার নির্বাচন করুন"
                }

                if (request.initialPartyId == null) {
                    Box {
                        OutlinedTextField(
                            value = partyLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(if (request.partyType == PartyType.CUSTOMER) "কাস্টমার" else "সাপ্লায়ার")
                            },
                            trailingIcon = { Icon(Icons.Rounded.ExpandMore, contentDescription = "Select") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { partyDropdownExpanded = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { partyDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = partyDropdownExpanded,
                            onDismissRequest = { partyDropdownExpanded = false }
                        ) {
                            if (request.partyType == PartyType.CUSTOMER) {
                                customers.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.name} (${Formatters.formatMoney(c.currentDue, currencySymbol)})") },
                                        onClick = {
                                            selectedPartyId = c.id
                                            partyDropdownExpanded = false
                                        }
                                    )
                                }
                            } else {
                                suppliers.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.name} (${Formatters.formatMoney(s.currentPayable, currencySymbol)})") },
                                        onClick = {
                                            selectedPartyId = s.id
                                            partyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = partyLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                }

                // Live Previous & New Balance Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.subtleDivider.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("পূর্বের ব্যালেন্স", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(
                                Formatters.formatMoney(previousBalance, currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("নতুন ব্যালেন্স", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(
                                Formatters.formatMoney(newBalance, currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text("টাকার পরিমাণ ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txn_amount_input")
                )

                // Partial / Full Payment quick chips when receiving or paying
                if ((request.transactionType == TransactionType.PAYMENT_RECEIVED ||
                        request.transactionType == TransactionType.SUPPLIER_PAYMENT) &&
                    previousBalance > 0.0
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.25 to "২৫%", 0.50 to "৫০%", 1.0 to "পূর্ণ ১০০%").forEach { (ratio, label) ->
                            AssistChip(
                                onClick = {
                                    val valAmt = (previousBalance * ratio)
                                    amountText = if (valAmt % 1.0 == 0.0) valAmt.toLong().toString() else String.format(java.util.Locale.US, "%.2f", valAmt)
                                    errorMsg = null
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Date Selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("তারিখ: ${Formatters.formatDate(selectedDate, language)}")
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("নোট / বিবরণ (ঐচ্ছিক)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txn_note_input")
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.warningRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPartyId == null) {
                        errorMsg = if (request.partyType == PartyType.CUSTOMER) {
                            "আগে একজন কাস্টমার যোগ/নির্বাচন করুন"
                        } else {
                            "আগে একজন সাপ্লায়ার যোগ/নির্বাচন করুন"
                        }
                        return@Button
                    }
                    if (parsedAmount <= 0.0) {
                        errorMsg = "সঠিক টাকার পরিমাণ লিখুন"
                        return@Button
                    }
                    showConfirmStep = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.testTag("proceed_txn_confirm_btn")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("ঠিক আছে") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("বাতিল") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
