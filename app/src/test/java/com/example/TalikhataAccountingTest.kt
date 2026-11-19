package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettings
import com.example.data.local.PartyType
import com.example.data.local.TalikhataDao
import com.example.data.local.TalikhataDatabase
import com.example.data.local.TransactionType
import com.example.ui.theme.ThemeMode
import com.example.util.BackupManager
import com.example.util.CustomerStatementInput
import com.example.util.PrintPdfHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TalikhataAccountingTest {

    private lateinit var context: Context
    private lateinit var db: TalikhataDatabase
    private lateinit var dao: TalikhataDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TalikhataDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.talikhataDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun defaultShop_isAmolPharmacy() = runTest {
        val shopId = dao.ensureDefaultShop()
        val shop = dao.getShopById(shopId)
        assertNotNull(shop)
        assertEquals("Amol Pharmacy", shop?.name)
        assertTrue(shop?.isActive == true)
    }

    @Test
    fun customerAndSupplierLedgers_areIsolatedAndAccurate() = runTest {
        val shopId = dao.ensureDefaultShop()
        val custId = dao.addCustomerWithOpeningDue(
            shopId = shopId,
            name = "রহিম মিয়া",
            phone = "01711000001",
            address = "ঢাকা",
            photoUri = null,
            openingDue = 500.0
        )
        val supId = dao.addSupplierWithOpeningPayable(
            shopId = shopId,
            name = "স্কয়ার ফার্মা",
            phone = "01711000002",
            address = "ঢাকা",
            photoUri = null,
            openingPayable = 2000.0
        )

        // Add due and receive partial payment on Customer
        dao.recordCustomerTransaction(
            shopId = shopId,
            customerId = custId,
            type = TransactionType.DUE_ADDED,
            amount = 300.0,
            note = "ওষুধ বাকি",
            transactionDate = System.currentTimeMillis(),
            idempotencyKey = "k-1"
        )
        // Duplicate submission with same idempotencyKey must be ignored
        val dup = dao.recordCustomerTransaction(
            shopId = shopId,
            customerId = custId,
            type = TransactionType.DUE_ADDED,
            amount = 300.0,
            note = "ওষুধ বাকি",
            transactionDate = System.currentTimeMillis(),
            idempotencyKey = "k-1"
        )
        assertNull(dup)

        val paymentTxn = dao.recordCustomerTransaction(
            shopId = shopId,
            customerId = custId,
            type = TransactionType.PAYMENT_RECEIVED,
            amount = 250.0,
            note = "আংশিক আদায়",
            transactionDate = System.currentTimeMillis(),
            idempotencyKey = "k-2"
        )
        assertNotNull(paymentTxn)
        assertEquals(800.0, paymentTxn!!.previousBalance, 0.01)
        assertEquals(550.0, paymentTxn.newBalance, 0.01)

        // Supplier payment must not affect customer
        dao.recordSupplierTransaction(
            shopId = shopId,
            supplierId = supId,
            type = TransactionType.SUPPLIER_PAYMENT,
            amount = 600.0,
            note = "ব্যাংক প্রদান",
            transactionDate = System.currentTimeMillis(),
            idempotencyKey = "s-1"
        )

        val customer = dao.getCustomerById(custId)!!
        val supplier = dao.getSupplierById(supId)!!
        assertEquals(550.0, customer.currentDue, 0.01)
        assertEquals(800.0, customer.totalDueAdded, 0.01)
        assertEquals(250.0, customer.totalPaid, 0.01)

        assertEquals(1400.0, supplier.currentPayable, 0.01)
        assertEquals(2000.0, supplier.totalGoodsReceived, 0.01)
        assertEquals(600.0, supplier.totalPaid, 0.01)

        // Soft delete and restore transaction adjusts balance cleanly
        dao.softDeleteTransaction(paymentTxn.id)
        assertEquals(800.0, dao.getCustomerById(custId)!!.currentDue, 0.01)
        dao.restoreTransaction(paymentTxn.id)
        assertEquals(550.0, dao.getCustomerById(custId)!!.currentDue, 0.01)
    }

    @Test
    fun multiShop_isolatesCustomersAndSuppliers() = runTest {
        val shop1 = dao.ensureDefaultShop()
        dao.addCustomerWithOpeningDue(shop1, "কাস্টমার ১", "01700000001", "", null, 100.0)

        val shop2 = dao.createShopAndSwitch("Branch Pharmacy", "", "01800000002", "", "")
        dao.addCustomerWithOpeningDue(shop2, "কাস্টমার ২", "01800000003", "", null, 400.0)

        val shop1Customers = dao.observeCustomers(shop1).first()
        val shop2Customers = dao.observeCustomers(shop2).first()
        assertEquals(1, shop1Customers.size)
        assertEquals("কাস্টমার ১", shop1Customers[0].name)
        assertEquals(1, shop2Customers.size)
        assertEquals("কাস্টমার ২", shop2Customers[0].name)
    }

    @Test
    fun multiPrintPagination_isolatesCustomersAndHandlesLongHistories() = runTest {
        val shopId = dao.ensureDefaultShop()
        val c1 = dao.addCustomerWithOpeningDue(shopId, "কাস্টমার A", "01711111111", "ঢাকা", null, 100.0)
        val c2 = dao.addCustomerWithOpeningDue(shopId, "কাস্টমার B", "01722222222", "চট্টগ্রাম", null, 200.0)

        repeat(24) { idx ->
            dao.recordCustomerTransaction(
                shopId = shopId,
                customerId = c1,
                type = TransactionType.DUE_ADDED,
                amount = 50.0,
                note = "আইটেম #$idx",
                transactionDate = System.currentTimeMillis() + idx * 1000L,
                idempotencyKey = "mp-$idx"
            )
        }

        val cust1 = dao.getCustomerById(c1)!!
        val cust2 = dao.getCustomerById(c2)!!
        val txns1 = dao.getPartyTransactionsAscOnce(shopId, PartyType.CUSTOMER, c1)
        val txns2 = dao.getPartyTransactionsAscOnce(shopId, PartyType.CUSTOMER, c2)

        // With history ON: customer 1 has 25 txns -> 2 pages, customer 2 has 1 txn -> 1 page
        val pagesWithHistory = PrintPdfHelper.buildMultiPrintPages(
            inputs = listOf(CustomerStatementInput(cust1, txns1), CustomerStatementInput(cust2, txns2)),
            commonNote = "মাসিক তাগাদা",
            includeHistory = true
        )
        assertEquals(3, pagesWithHistory.size)
        assertEquals(cust1.id, pagesWithHistory[0].customer.id)
        assertEquals(cust1.id, pagesWithHistory[1].customer.id)
        assertEquals(cust2.id, pagesWithHistory[2].customer.id)
        assertTrue(pagesWithHistory[0].isFirstPage)
        assertFalse(pagesWithHistory[1].isFirstPage)
        assertTrue(pagesWithHistory[2].isFirstPage)

        // With history OFF: 1 page per customer, 0 transaction rows
        val pagesNoHistory = PrintPdfHelper.buildMultiPrintPages(
            inputs = listOf(CustomerStatementInput(cust1, txns1), CustomerStatementInput(cust2, txns2)),
            commonNote = "মাসিক তাগাদা",
            includeHistory = false
        )
        assertEquals(2, pagesNoHistory.size)
        assertTrue(pagesNoHistory.all { it.transactionsSlice.isEmpty() })
    }

    @Test
    fun backupManager_roundTripsShopData() = runTest {
        val shopId = dao.ensureDefaultShop()
        dao.addCustomerWithOpeningDue(shopId, "ব্যাকআপ টেস্ট", "01900000000", "সিলেট", null, 750.0)
        val shop = dao.getShopById(shopId)!!
        val customers = dao.getAllCustomersForShopOnce(shopId)
        val suppliers = dao.getAllSuppliersForShopOnce(shopId)
        val txns = dao.getAllTransactionsForShopOnce(shopId)

        val backupManager = BackupManager(context)
        val file = backupManager.writeLocalBackupFile(shop, customers, suppliers, txns, "test")
        assertTrue(file.exists())

        val parsed = backupManager.parseBackupFile(file.absolutePath)
        assertNotNull(parsed)
        assertEquals("Amol Pharmacy", parsed!!.shop.name)
        assertEquals(1, parsed.customers.size)
        assertEquals(1, parsed.transactions.size)
        file.delete()
    }

    @Test
    fun formatReceiptSmsMessage_includesCustomerAndSupplierDetails() = runTest {
        val shopId = dao.ensureDefaultShop()
        val custId = dao.addCustomerWithOpeningDue(shopId, "আব্দুল করিম", "01711112222", "ঢাকা", null, 500.0)
        val custTxn = dao.recordCustomerTransaction(
            shopId = shopId,
            customerId = custId,
            type = TransactionType.PAYMENT_RECEIVED,
            amount = 200.0,
            note = "নগদ জমা",
            transactionDate = System.currentTimeMillis(),
            idempotencyKey = "sms-cust-1"
        )!!
        val custMsg = com.example.util.Formatters.formatReceiptSmsMessage("Amol Pharmacy", custTxn, "৳")
        assertTrue(custMsg.contains("Amol Pharmacy"))
        assertTrue(custMsg.contains("কাস্টমার: আব্দুল করিম"))
        assertTrue(custMsg.contains("টাকা আদায়"))
        assertTrue(custMsg.contains("বর্তমান বকেয়া: ৳ 300"))
        assertTrue(custMsg.contains("নোট: নগদ জমা"))

        val supId = dao.addSupplierWithOpeningPayable(shopId, "ইনসেপ্টা ফার্মা", "01811112222", "ঢাকা", null, 1000.0)
        val supTxn = dao.recordSupplierTransaction(
            shopId = shopId,
            supplierId = supId,
            type = TransactionType.SUPPLIER_PAYMENT,
            amount = 400.0,
            note = "",
            transactionDate = System.currentTimeMillis(),
            idempotencyKey = "sms-sup-1"
        )!!
        val supMsg = com.example.util.Formatters.formatReceiptSmsMessage("Amol Pharmacy", supTxn, "৳")
        assertTrue(supMsg.contains("সাপ্লায়ার: ইনসেপ্টা ফার্মা"))
        assertTrue(supMsg.contains("সাপ্লায়ার প্রদান"))
        assertTrue(supMsg.contains("অবশিষ্ট দেনা: ৳ 600"))
    }

    @Test
    fun fullDeviceBackup_exportsAndRestoresAllShopsCustomersSuppliersAndHistory() = runTest {
        val shop1 = dao.ensureDefaultShop()
        val c1 = dao.addCustomerWithOpeningDue(shop1, "কাস্টমার এক", "01710000001", "ঢাকা", null, 1200.0)
        dao.recordCustomerTransaction(
            shopId = shop1,
            customerId = c1,
            type = TransactionType.PAYMENT_RECEIVED,
            amount = 400.0,
            note = "আংশিক পরিশোধ",
            transactionDate = System.currentTimeMillis(),
            idempotencyKey = "full-c1-pay"
        )
        val s1 = dao.addSupplierWithOpeningPayable(shop1, "সাপ্লায়ার এক", "01810000001", "ঢাকা", null, 5000.0)
        dao.recordSupplierTransaction(
            shopId = shop1,
            supplierId = s1,
            type = TransactionType.SUPPLIER_PAYMENT,
            amount = 1500.0,
            note = "ব্যাংক পেমেন্ট",
            transactionDate = System.currentTimeMillis(),
            idempotencyKey = "full-s1-pay"
        )

        val shop2 = dao.createShopAndSwitch("Branch Amol", "অমল", "01910000002", "গাজীপুর", "শাখা")
        dao.addCustomerWithOpeningDue(shop2, "শাখা কাস্টমার", "01720000002", "গাজীপুর", null, 950.0)

        val backupManager = BackupManager(context)
        val json = backupManager.buildFullDeviceBackupJson(
            shops = dao.getAllShopsOnce(),
            customers = dao.getAllCustomersAcrossAllShopsOnce(),
            suppliers = dao.getAllSuppliersAcrossAllShopsOnce(),
            transactions = dao.getAllTransactionsAcrossAllShopsOnce(),
            auditLogs = dao.getAllAuditLogsAcrossAllShopsOnce(),
            settings = AppSettings(themeMode = ThemeMode.DARK, language = AppLanguage.BN, currencySymbol = "৳")
        )

        val parsed = backupManager.parseFullDeviceBackupJson(json)
        assertNotNull(parsed)
        assertEquals("FULL_DEVICE", parsed!!.backupScope)
        assertEquals(2, parsed.shops.size)
        assertEquals(2, parsed.customers.size)
        assertEquals(1, parsed.suppliers.size)
        assertEquals(5, parsed.transactions.size)
        assertEquals(ThemeMode.DARK.name, parsed.settings?.themeMode)

        // Wipe and restore onto a clean database instance to simulate device switch
        val targetDb = Room.inMemoryDatabaseBuilder(context, TalikhataDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val targetDao = targetDb.talikhataDao()
        try {
            val bundle = backupManager.toFullDeviceEntities(parsed)
            val restoredActiveShopId = targetDao.replaceAllDataFromFullBackup(
                shops = bundle.shops,
                customers = bundle.customers,
                suppliers = bundle.suppliers,
                transactions = bundle.transactions,
                auditLogs = bundle.auditLogs
            )

            val restoredShops = targetDao.getAllShopsOnce()
            val restoredCustomers = targetDao.getAllCustomersAcrossAllShopsOnce()
            val restoredSuppliers = targetDao.getAllSuppliersAcrossAllShopsOnce()
            val restoredTxns = targetDao.getAllTransactionsAcrossAllShopsOnce()

            assertEquals(2, restoredShops.size)
            assertEquals(2, restoredCustomers.size)
            assertEquals(1, restoredSuppliers.size)
            assertEquals(5, restoredTxns.size)

            val restoredActiveShop = targetDao.getShopById(restoredActiveShopId)
            assertEquals("Branch Amol", restoredActiveShop?.name)

            val restoredCust1 = restoredCustomers.first { it.name == "কাস্টমার এক" }
            assertEquals(800.0, restoredCust1.currentDue, 0.01)
            val restoredCust1Txns = targetDao.getPartyTransactionsAscOnce(
                restoredCust1.shopId,
                PartyType.CUSTOMER,
                restoredCust1.id
            )
            assertEquals(2, restoredCust1Txns.size)
            assertEquals("আংশিক পরিশোধ", restoredCust1Txns[1].note)

            val restoredSup1 = restoredSuppliers.first { it.name == "সাপ্লায়ার এক" }
            assertEquals(3500.0, restoredSup1.currentPayable, 0.01)
        } finally {
            targetDb.close()
        }
    }

    @Test
    fun customer_canBeCreatedAndTransactedWithoutPhone() = runTest {
        val shopId = dao.ensureDefaultShop()
        val custId = dao.addCustomerWithOpeningDue(
            shopId = shopId,
            name = "ফোন ছাড়া কাস্টমার",
            phone = "",
            address = "বাজার রোড",
            photoUri = null,
            openingDue = 350.0
        )
        val customer = dao.getCustomerById(custId)
        assertNotNull(customer)
        assertEquals("", customer!!.phone)
        assertEquals(350.0, customer.currentDue, 0.01)

        val txn = dao.recordCustomerTransaction(
            shopId = shopId,
            customerId = custId,
            type = TransactionType.PAYMENT_RECEIVED,
            amount = 100.0,
            note = "নগদ জমা",
            transactionDate = System.currentTimeMillis(),
            idempotencyKey = "no-phone-pay-1"
        )
        assertNotNull(txn)
        assertEquals("", txn!!.partyPhone)
        assertEquals(250.0, dao.getCustomerById(custId)!!.currentDue, 0.01)
    }
}
