package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettings
import com.example.data.local.AuditLogEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.PartyType
import com.example.data.local.ShopEntity
import com.example.data.local.SupplierEntity
import com.example.data.local.TransactionType
import com.example.ui.theme.ThemeMode
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class BackupShopDto(
    val name: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val tagline: String
)

@JsonClass(generateAdapter = true)
data class BackupCustomerDto(
    val id: Long,
    val customerCode: String,
    val name: String,
    val phone: String,
    val address: String,
    val photoUri: String?,
    val currentDue: Double,
    val totalDueAdded: Double,
    val totalPaid: Double,
    val lastTransactionAt: Long?,
    val lastPaymentAt: Long?,
    val lastReminderSentAt: Long?,
    val reminderCount: Int,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class BackupSupplierDto(
    val id: Long,
    val supplierCode: String,
    val name: String,
    val phone: String,
    val address: String,
    val photoUri: String?,
    val currentPayable: Double,
    val totalGoodsReceived: Double,
    val totalPaid: Double,
    val lastTransactionAt: Long?,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class BackupTransactionDto(
    val receiptNumber: String,
    val partyType: String,
    val partyId: Long,
    val partyCode: String,
    val partyName: String,
    val partyPhone: String,
    val type: String,
    val amount: Double,
    val previousBalance: Double,
    val newBalance: Double,
    val note: String,
    val transactionDate: Long,
    val createdAt: Long,
    val idempotencyKey: String,
    val isDeleted: Boolean,
    val deletedAt: Long?
)

@JsonClass(generateAdapter = true)
data class TalikhataBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val shop: BackupShopDto,
    val customers: List<BackupCustomerDto>,
    val suppliers: List<BackupSupplierDto>,
    val transactions: List<BackupTransactionDto>
)

@JsonClass(generateAdapter = true)
data class FullBackupShopDto(
    val id: Long,
    val name: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val tagline: String,
    val isActive: Boolean,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class FullBackupCustomerDto(
    val id: Long,
    val shopId: Long,
    val customerCode: String,
    val name: String,
    val phone: String,
    val address: String,
    val photoUri: String?,
    val currentDue: Double,
    val totalDueAdded: Double,
    val totalPaid: Double,
    val lastTransactionAt: Long?,
    val lastPaymentAt: Long?,
    val lastReminderSentAt: Long?,
    val reminderCount: Int,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class FullBackupSupplierDto(
    val id: Long,
    val shopId: Long,
    val supplierCode: String,
    val name: String,
    val phone: String,
    val address: String,
    val photoUri: String?,
    val currentPayable: Double,
    val totalGoodsReceived: Double,
    val totalPaid: Double,
    val lastTransactionAt: Long?,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class FullBackupTransactionDto(
    val shopId: Long,
    val receiptNumber: String,
    val partyType: String,
    val partyId: Long,
    val partyCode: String,
    val partyName: String,
    val partyPhone: String,
    val type: String,
    val amount: Double,
    val previousBalance: Double,
    val newBalance: Double,
    val note: String,
    val transactionDate: Long,
    val createdAt: Long,
    val idempotencyKey: String,
    val isDeleted: Boolean,
    val deletedAt: Long?
)

@JsonClass(generateAdapter = true)
data class FullBackupAuditLogDto(
    val shopId: Long,
    val action: String,
    val recordType: String,
    val recordTitle: String,
    val previousValue: String,
    val newValue: String,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class FullBackupSettingsDto(
    val themeMode: String,
    val language: String,
    val autoBackupEnabled: Boolean,
    val smsTemplate: String,
    val printShowAddress: Boolean,
    val printShowPhone: Boolean,
    val defaultCommonNote: String,
    val currencySymbol: String,
    val overdueDaysThreshold: Int
)

@JsonClass(generateAdapter = true)
data class FullDeviceBackupPayload(
    val version: Int = 2,
    val backupScope: String = "FULL_DEVICE",
    val exportedAt: Long = System.currentTimeMillis(),
    val shops: List<FullBackupShopDto>,
    val customers: List<FullBackupCustomerDto>,
    val suppliers: List<FullBackupSupplierDto>,
    val transactions: List<FullBackupTransactionDto>,
    val auditLogs: List<FullBackupAuditLogDto> = emptyList(),
    val settings: FullBackupSettingsDto? = null
)

data class DeviceDownloadResult(
    val success: Boolean,
    val requiresManualPicker: Boolean = false,
    val fileName: String = "",
    val folderDisplayPath: String = "Download/Talikhata",
    val uri: Uri? = null,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val transactionCount: Int = 0,
    val shopCount: Int = 0
)

class BackupManager(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(TalikhataBackupPayload::class.java).indent("  ")
    private val fullAdapter = moshi.adapter(FullDeviceBackupPayload::class.java).indent("  ")

    private fun backupDirectory(): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun buildPayloadJson(
        shop: ShopEntity,
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        transactions: List<LedgerTransactionEntity>
    ): String {
        val payload = TalikhataBackupPayload(
            shop = BackupShopDto(
                name = shop.name,
                ownerName = shop.ownerName,
                phone = shop.phone,
                address = shop.address,
                tagline = shop.tagline
            ),
            customers = customers.map {
                BackupCustomerDto(
                    id = it.id,
                    customerCode = it.customerCode,
                    name = it.name,
                    phone = it.phone,
                    address = it.address,
                    photoUri = it.photoUri,
                    currentDue = it.currentDue,
                    totalDueAdded = it.totalDueAdded,
                    totalPaid = it.totalPaid,
                    lastTransactionAt = it.lastTransactionAt,
                    lastPaymentAt = it.lastPaymentAt,
                    lastReminderSentAt = it.lastReminderSentAt,
                    reminderCount = it.reminderCount,
                    isDeleted = it.isDeleted,
                    deletedAt = it.deletedAt,
                    createdAt = it.createdAt
                )
            },
            suppliers = suppliers.map {
                BackupSupplierDto(
                    id = it.id,
                    supplierCode = it.supplierCode,
                    name = it.name,
                    phone = it.phone,
                    address = it.address,
                    photoUri = it.photoUri,
                    currentPayable = it.currentPayable,
                    totalGoodsReceived = it.totalGoodsReceived,
                    totalPaid = it.totalPaid,
                    lastTransactionAt = it.lastTransactionAt,
                    isDeleted = it.isDeleted,
                    deletedAt = it.deletedAt,
                    createdAt = it.createdAt
                )
            },
            transactions = transactions.map {
                BackupTransactionDto(
                    receiptNumber = it.receiptNumber,
                    partyType = it.partyType.name,
                    partyId = it.partyId,
                    partyCode = it.partyCode,
                    partyName = it.partyName,
                    partyPhone = it.partyPhone,
                    type = it.type.name,
                    amount = it.amount,
                    previousBalance = it.previousBalance,
                    newBalance = it.newBalance,
                    note = it.note,
                    transactionDate = it.transactionDate,
                    createdAt = it.createdAt,
                    idempotencyKey = it.idempotencyKey,
                    isDeleted = it.isDeleted,
                    deletedAt = it.deletedAt
                )
            }
        )
        return adapter.toJson(payload)
    }

    fun writeLocalBackupFile(
        shop: ShopEntity,
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        transactions: List<LedgerTransactionEntity>,
        prefix: String = "manual"
    ): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeShop = shop.name.replace(Regex("[^A-Za-z0-9]"), "_").take(20).ifBlank { "shop" }
        val file = File(backupDirectory(), "talikhata_${safeShop}_${prefix}_$stamp.json")
        val json = buildPayloadJson(shop, customers, suppliers, transactions)
        file.writeText(json, Charsets.UTF_8)
        return file
    }

    fun exportToUri(
        uri: Uri,
        shop: ShopEntity,
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        transactions: List<LedgerTransactionEntity>
    ): Boolean {
        return runCatching {
            val json = buildPayloadJson(shop, customers, suppliers, transactions)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        }.getOrDefault(false)
    }

    fun parseBackupFile(filePath: String): TalikhataBackupPayload? {
        return runCatching {
            val file = File(filePath)
            if (!file.exists()) return null
            adapter.fromJson(file.readText(Charsets.UTF_8))
        }.getOrNull()
    }

    fun parseBackupUri(uri: Uri): TalikhataBackupPayload? {
        return runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: return null
            adapter.fromJson(text)
        }.getOrNull()
    }

    fun toEntities(
        payload: TalikhataBackupPayload,
        targetShopId: Long
    ): Triple<List<CustomerEntity>, List<SupplierEntity>, List<LedgerTransactionEntity>> {
        val customers = payload.customers.map {
            CustomerEntity(
                id = it.id,
                shopId = targetShopId,
                customerCode = it.customerCode,
                name = it.name,
                phone = it.phone,
                address = it.address,
                photoUri = it.photoUri,
                currentDue = it.currentDue,
                totalDueAdded = it.totalDueAdded,
                totalPaid = it.totalPaid,
                lastTransactionAt = it.lastTransactionAt,
                lastPaymentAt = it.lastPaymentAt,
                lastReminderSentAt = it.lastReminderSentAt,
                reminderCount = it.reminderCount,
                isDeleted = it.isDeleted,
                deletedAt = it.deletedAt,
                createdAt = it.createdAt
            )
        }
        val suppliers = payload.suppliers.map {
            SupplierEntity(
                id = it.id,
                shopId = targetShopId,
                supplierCode = it.supplierCode,
                name = it.name,
                phone = it.phone,
                address = it.address,
                photoUri = it.photoUri,
                currentPayable = it.currentPayable,
                totalGoodsReceived = it.totalGoodsReceived,
                totalPaid = it.totalPaid,
                lastTransactionAt = it.lastTransactionAt,
                isDeleted = it.isDeleted,
                deletedAt = it.deletedAt,
                createdAt = it.createdAt
            )
        }
        val txns = payload.transactions.map {
            LedgerTransactionEntity(
                id = 0,
                shopId = targetShopId,
                receiptNumber = it.receiptNumber,
                partyType = PartyType.valueOf(it.partyType),
                partyId = it.partyId,
                partyCode = it.partyCode,
                partyName = it.partyName,
                partyPhone = it.partyPhone,
                type = TransactionType.valueOf(it.type),
                amount = it.amount,
                previousBalance = it.previousBalance,
                newBalance = it.newBalance,
                note = it.note,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt,
                idempotencyKey = it.idempotencyKey,
                isDeleted = it.isDeleted,
                deletedAt = it.deletedAt
            )
        }
        return Triple(customers, suppliers, txns)
    }

    fun deleteBackupFile(filePath: String) {
        runCatching { File(filePath).delete() }
    }

    fun buildFullDeviceBackupJson(
        shops: List<ShopEntity>,
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        transactions: List<LedgerTransactionEntity>,
        auditLogs: List<AuditLogEntity>,
        settings: AppSettings
    ): String {
        val payload = FullDeviceBackupPayload(
            shops = shops.map {
                FullBackupShopDto(
                    id = it.id,
                    name = it.name,
                    ownerName = it.ownerName,
                    phone = it.phone,
                    address = it.address,
                    tagline = it.tagline,
                    isActive = it.isActive,
                    createdAt = it.createdAt
                )
            },
            customers = customers.map {
                FullBackupCustomerDto(
                    id = it.id,
                    shopId = it.shopId,
                    customerCode = it.customerCode,
                    name = it.name,
                    phone = it.phone,
                    address = it.address,
                    photoUri = it.photoUri,
                    currentDue = it.currentDue,
                    totalDueAdded = it.totalDueAdded,
                    totalPaid = it.totalPaid,
                    lastTransactionAt = it.lastTransactionAt,
                    lastPaymentAt = it.lastPaymentAt,
                    lastReminderSentAt = it.lastReminderSentAt,
                    reminderCount = it.reminderCount,
                    isDeleted = it.isDeleted,
                    deletedAt = it.deletedAt,
                    createdAt = it.createdAt
                )
            },
            suppliers = suppliers.map {
                FullBackupSupplierDto(
                    id = it.id,
                    shopId = it.shopId,
                    supplierCode = it.supplierCode,
                    name = it.name,
                    phone = it.phone,
                    address = it.address,
                    photoUri = it.photoUri,
                    currentPayable = it.currentPayable,
                    totalGoodsReceived = it.totalGoodsReceived,
                    totalPaid = it.totalPaid,
                    lastTransactionAt = it.lastTransactionAt,
                    isDeleted = it.isDeleted,
                    deletedAt = it.deletedAt,
                    createdAt = it.createdAt
                )
            },
            transactions = transactions.map {
                FullBackupTransactionDto(
                    shopId = it.shopId,
                    receiptNumber = it.receiptNumber,
                    partyType = it.partyType.name,
                    partyId = it.partyId,
                    partyCode = it.partyCode,
                    partyName = it.partyName,
                    partyPhone = it.partyPhone,
                    type = it.type.name,
                    amount = it.amount,
                    previousBalance = it.previousBalance,
                    newBalance = it.newBalance,
                    note = it.note,
                    transactionDate = it.transactionDate,
                    createdAt = it.createdAt,
                    idempotencyKey = it.idempotencyKey,
                    isDeleted = it.isDeleted,
                    deletedAt = it.deletedAt
                )
            },
            auditLogs = auditLogs.map {
                FullBackupAuditLogDto(
                    shopId = it.shopId,
                    action = it.action,
                    recordType = it.recordType,
                    recordTitle = it.recordTitle,
                    previousValue = it.previousValue,
                    newValue = it.newValue,
                    timestamp = it.timestamp
                )
            },
            settings = FullBackupSettingsDto(
                themeMode = settings.themeMode.name,
                language = settings.language.name,
                autoBackupEnabled = settings.autoBackupEnabled,
                smsTemplate = settings.smsTemplate,
                printShowAddress = settings.printShowAddress,
                printShowPhone = settings.printShowPhone,
                defaultCommonNote = settings.defaultCommonNote,
                currencySymbol = settings.currencySymbol,
                overdueDaysThreshold = settings.overdueDaysThreshold
            )
        )
        return fullAdapter.toJson(payload)
    }

    fun suggestedFullBackupFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "Talikhata_AllData_$stamp.json"
    }

    fun downloadFullDeviceBackupToDownloadsFolder(
        shops: List<ShopEntity>,
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        transactions: List<LedgerTransactionEntity>,
        auditLogs: List<AuditLogEntity>,
        settings: AppSettings
    ): DeviceDownloadResult {
        val fileName = suggestedFullBackupFileName()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return DeviceDownloadResult(
                success = false,
                requiresManualPicker = true,
                fileName = fileName,
                shopCount = shops.size,
                customerCount = customers.count { !it.isDeleted },
                supplierCount = suppliers.count { !it.isDeleted },
                transactionCount = transactions.count { !it.isDeleted }
            )
        }

        val json = buildFullDeviceBackupJson(shops, customers, suppliers, transactions, auditLogs, settings)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Talikhata")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return DeviceDownloadResult(
                success = false,
                requiresManualPicker = true,
                fileName = fileName
            )

        return try {
            resolver.openOutputStream(itemUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Cannot open output stream")

            val doneValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(itemUri, doneValues, null, null)

            DeviceDownloadResult(
                success = true,
                requiresManualPicker = false,
                fileName = fileName,
                folderDisplayPath = "Download/Talikhata/$fileName",
                uri = itemUri,
                shopCount = shops.size,
                customerCount = customers.count { !it.isDeleted },
                supplierCount = suppliers.count { !it.isDeleted },
                transactionCount = transactions.count { !it.isDeleted }
            )
        } catch (_: Exception) {
            runCatching { resolver.delete(itemUri, null, null) }
            DeviceDownloadResult(
                success = false,
                requiresManualPicker = true,
                fileName = fileName
            )
        }
    }

    fun exportFullDeviceBackupToUri(
        uri: Uri,
        shops: List<ShopEntity>,
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        transactions: List<LedgerTransactionEntity>,
        auditLogs: List<AuditLogEntity>,
        settings: AppSettings
    ): Boolean {
        return runCatching {
            val json = buildFullDeviceBackupJson(shops, customers, suppliers, transactions, auditLogs, settings)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        }.getOrDefault(false)
    }

    fun readTextFromUri(uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }
        }.getOrNull()
    }

    fun parseFullDeviceBackupJson(json: String): FullDeviceBackupPayload? {
        return runCatching {
            val parsed = fullAdapter.fromJson(json) ?: return null
            if (parsed.backupScope == "FULL_DEVICE" && parsed.shops.isNotEmpty()) parsed else null
        }.getOrNull()
    }

    fun parseSingleShopBackupJson(json: String): TalikhataBackupPayload? {
        return runCatching {
            adapter.fromJson(json)
        }.getOrNull()
    }

    fun toFullDeviceEntities(payload: FullDeviceBackupPayload): FullDeviceEntitiesBundle {
        val shops = payload.shops.map {
            ShopEntity(
                id = it.id,
                name = it.name,
                ownerName = it.ownerName,
                phone = it.phone,
                address = it.address,
                tagline = it.tagline,
                isActive = it.isActive,
                createdAt = it.createdAt
            )
        }
        val customers = payload.customers.map {
            CustomerEntity(
                id = it.id,
                shopId = it.shopId,
                customerCode = it.customerCode,
                name = it.name,
                phone = it.phone,
                address = it.address,
                photoUri = it.photoUri,
                currentDue = it.currentDue,
                totalDueAdded = it.totalDueAdded,
                totalPaid = it.totalPaid,
                lastTransactionAt = it.lastTransactionAt,
                lastPaymentAt = it.lastPaymentAt,
                lastReminderSentAt = it.lastReminderSentAt,
                reminderCount = it.reminderCount,
                isDeleted = it.isDeleted,
                deletedAt = it.deletedAt,
                createdAt = it.createdAt
            )
        }
        val suppliers = payload.suppliers.map {
            SupplierEntity(
                id = it.id,
                shopId = it.shopId,
                supplierCode = it.supplierCode,
                name = it.name,
                phone = it.phone,
                address = it.address,
                photoUri = it.photoUri,
                currentPayable = it.currentPayable,
                totalGoodsReceived = it.totalGoodsReceived,
                totalPaid = it.totalPaid,
                lastTransactionAt = it.lastTransactionAt,
                isDeleted = it.isDeleted,
                deletedAt = it.deletedAt,
                createdAt = it.createdAt
            )
        }
        val transactions = payload.transactions.map {
            LedgerTransactionEntity(
                id = 0,
                shopId = it.shopId,
                receiptNumber = it.receiptNumber,
                partyType = PartyType.valueOf(it.partyType),
                partyId = it.partyId,
                partyCode = it.partyCode,
                partyName = it.partyName,
                partyPhone = it.partyPhone,
                type = TransactionType.valueOf(it.type),
                amount = it.amount,
                previousBalance = it.previousBalance,
                newBalance = it.newBalance,
                note = it.note,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt,
                idempotencyKey = it.idempotencyKey,
                isDeleted = it.isDeleted,
                deletedAt = it.deletedAt
            )
        }
        val auditLogs = payload.auditLogs.map {
            AuditLogEntity(
                id = 0,
                shopId = it.shopId,
                action = it.action,
                recordType = it.recordType,
                recordTitle = it.recordTitle,
                previousValue = it.previousValue,
                newValue = it.newValue,
                timestamp = it.timestamp
            )
        }
        return FullDeviceEntitiesBundle(
            shops = shops,
            customers = customers,
            suppliers = suppliers,
            transactions = transactions,
            auditLogs = auditLogs,
            settings = payload.settings
        )
    }

    data class FullDeviceEntitiesBundle(
        val shops: List<ShopEntity>,
        val customers: List<CustomerEntity>,
        val suppliers: List<SupplierEntity>,
        val transactions: List<LedgerTransactionEntity>,
        val auditLogs: List<AuditLogEntity>,
        val settings: FullBackupSettingsDto?
    )
}
