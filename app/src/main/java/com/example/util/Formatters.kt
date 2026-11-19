package com.example.util

import com.example.data.local.AppLanguage
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.PartyType
import com.example.data.local.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {

    private val numberFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun formatMoney(amount: Double, symbol: String = "৳"): String {
        val absVal = abs(amount)
        val formatted = if (absVal % 1.0 == 0.0) {
            numberFormat.format(absVal.toLong())
        } else {
            String.format(Locale.US, "%,.2f", absVal)
        }
        return if (amount < 0) "-$symbol $formatted" else "$symbol $formatted"
    }

    fun formatDate(timestamp: Long?, language: AppLanguage = AppLanguage.BN): String {
        if (timestamp == null || timestamp <= 0L) return if (language == AppLanguage.BN) "নেই" else "None"
        val locale = if (language == AppLanguage.BN) Locale("bn", "BD") else Locale.ENGLISH
        return SimpleDateFormat("dd MMM yyyy", locale).format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long, language: AppLanguage = AppLanguage.BN): String {
        val locale = if (language == AppLanguage.BN) Locale("bn", "BD") else Locale.ENGLISH
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", locale).format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long, language: AppLanguage = AppLanguage.BN): String {
        val locale = if (language == AppLanguage.BN) Locale("bn", "BD") else Locale.ENGLISH
        return SimpleDateFormat("MMM yyyy", locale).format(Date(timestamp))
    }

    fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfDayMillis(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun daysSince(timestamp: Long?): Int {
        if (timestamp == null || timestamp <= 0L) return 0
        val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
        return (diff / (1000L * 60L * 60L * 24L)).toInt()
    }

    fun formatSmsTemplate(
        template: String,
        customerName: String,
        customerCode: String,
        dueAmount: Double,
        shopName: String,
        currencySymbol: String
    ): String {
        return template
            .replace("{name}", customerName)
            .replace("{id}", customerCode)
            .replace("{due}", formatMoney(dueAmount, currencySymbol))
            .replace("{shop}", shopName)
    }

    fun transactionTypeLabel(type: TransactionType, language: AppLanguage = AppLanguage.BN): String {
        return if (language == AppLanguage.BN) {
            when (type) {
                TransactionType.DUE_ADDED -> "পাওনা যোগ"
                TransactionType.PAYMENT_RECEIVED -> "টাকা আদায়"
                TransactionType.GOODS_RECEIVED -> "মালামাল গ্রহণ"
                TransactionType.SUPPLIER_PAYMENT -> "সাপ্লায়ার প্রদান"
            }
        } else {
            when (type) {
                TransactionType.DUE_ADDED -> "Due Added"
                TransactionType.PAYMENT_RECEIVED -> "Payment Received"
                TransactionType.GOODS_RECEIVED -> "Goods Received"
                TransactionType.SUPPLIER_PAYMENT -> "Supplier Payment"
            }
        }
    }

    fun formatReceiptSmsMessage(
        shopName: String,
        txn: LedgerTransactionEntity,
        currencySymbol: String,
        language: AppLanguage = AppLanguage.BN
    ): String {
        val partyRole = if (txn.partyType == PartyType.CUSTOMER) {
            if (language == AppLanguage.BN) "কাস্টমার" else "Customer"
        } else {
            if (language == AppLanguage.BN) "সাপ্লায়ার" else "Supplier"
        }
        val balanceLabel = if (txn.partyType == PartyType.CUSTOMER) {
            if (language == AppLanguage.BN) "বর্তমান বকেয়া" else "Current Due"
        } else {
            if (language == AppLanguage.BN) "অবশিষ্ট দেনা" else "Current Payable"
        }
        val typeText = transactionTypeLabel(txn.type, language)
        val dateText = formatDate(txn.transactionDate, language)
        val prevText = formatMoney(txn.previousBalance, currencySymbol)
        val amtText = formatMoney(txn.amount, currencySymbol)
        val newText = formatMoney(txn.newBalance, currencySymbol)

        return buildString {
            appendLine(shopName)
            if (language == AppLanguage.BN) {
                appendLine("রসিদ: ${txn.receiptNumber}")
                appendLine("$partyRole: ${txn.partyName} (${txn.partyCode})")
                appendLine("তারিখ: $dateText")
                appendLine("ধরন: $typeText")
                appendLine("পূর্বের জের: $prevText")
                appendLine("পরিমাণ: $amtText")
                append("$balanceLabel: $newText")
                if (txn.note.isNotBlank()) {
                    append("\nনোট: ${txn.note}")
                }
            } else {
                appendLine("Receipt: ${txn.receiptNumber}")
                appendLine("$partyRole: ${txn.partyName} (${txn.partyCode})")
                appendLine("Date: $dateText")
                appendLine("Type: $typeText")
                appendLine("Prev Balance: $prevText")
                appendLine("Amount: $amtText")
                append("$balanceLabel: $newText")
                if (txn.note.isNotBlank()) {
                    append("\nNote: ${txn.note}")
                }
            }
        }
    }
}
