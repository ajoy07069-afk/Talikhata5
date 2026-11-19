package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLEncoder

object IntentHelper {

    fun sendSms(context: Context, phone: String, message: String) {
        val cleanPhone = phone.filter { it.isDigit() || it == '+' }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanPhone")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "SMS অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        val digits = phone.filter { it.isDigit() }
        val bdPhone = when {
            digits.startsWith("880") -> digits
            digits.startsWith("01") && digits.length == 11 -> "88$digits"
            else -> digits
        }
        val encoded = runCatching { URLEncoder.encode(message, "UTF-8") }.getOrDefault(message)
        val url = if (bdPhone.isNotBlank()) {
            "https://wa.me/$bdPhone?text=$encoded"
        } else {
            "https://wa.me/?text=$encoded"
        }
        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(waIntent)
        }.onFailure {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                context.startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure {
                Toast.makeText(context, "WhatsApp বা শেয়ারিং অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun dialPhone(context: Context, phone: String) {
        val cleanPhone = phone.filter { it.isDigit() || it == '+' }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "ডায়ালার পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareTempPdfFile(context: Context, pdfFile: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            Toast.makeText(context, "PDF শেয়ার করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareBackupUri(context: Context, uri: Uri, title: String = "ব্যাকআপ ফাইল শেয়ার") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(shareIntent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(context, "ফাইল শেয়ার করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        }
    }
}
