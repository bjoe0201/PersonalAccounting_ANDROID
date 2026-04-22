package com.finance.manager.android.data.local

import androidx.room.withTransaction
import com.finance.manager.android.data.local.entity.AccountEntity
import com.finance.manager.android.data.local.entity.AppSettingEntity
import com.finance.manager.android.data.local.entity.CategoryEntity
import com.finance.manager.android.data.local.entity.CurrencyEntity
import com.finance.manager.android.data.local.entity.PayeeEntity
import com.finance.manager.android.data.local.entity.TransactionEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val db: AppDatabase,
) {
    private val now: String
        get() = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    suspend fun initializeIfNeeded() {
        if (db.currencyDao().getCount() > 0) return
        doSeed()
    }

    /** 清空所有資料表並重新植入初始資料（在同一個 transaction 內完成） */
    suspend fun forceInitialize() {
        db.withTransaction {
            db.transactionSplitTagDao().deleteAll()
            db.transactionTagDao().deleteAll()
            db.transactionSplitDao().deleteAll()
            db.accountMonthlyBalanceDao().deleteAll()
            db.transactionDao().deleteAll()
            db.accountDao().deleteAll()
            db.categoryDao().deleteAll()
            db.payeeDao().deleteAll()
            db.tagDao().deleteAll()
            db.currencyDao().deleteAll()
            db.settingsDao().deleteAll()
        }
        doSeed()
    }

    private suspend fun doSeed() {
        db.withTransaction {
            insertCurrencies()
            val catIds = insertCategories()
            val payeeIds = insertPayees()
            insertSettings()
            insertSampleAccountsAndTransactions(catIds, payeeIds)
        }
    }

    // ── 幣別 ─────────────────────────────────────────
    private suspend fun insertCurrencies() {
        db.currencyDao().insertAll(listOf(
            CurrencyEntity(currencyName = "Taiwan Dollar", currencySymbol = "T\$", currencyCode = "TWD",
                shortcutLetter = "T", rateToHome = 1.0, rateFromHome = 1.0, isHomeCurrency = true,
                displayOrder = 1, createdAt = now),
            CurrencyEntity(currencyName = "Japanese Yen", currencySymbol = "¥", currencyCode = "JPY",
                shortcutLetter = "Y", rateToHome = 5.0, rateFromHome = 0.2,
                displayOrder = 2, createdAt = now),
            CurrencyEntity(currencyName = "U.S. Dollar", currencySymbol = "\$", currencyCode = "USD",
                shortcutLetter = "D", rateToHome = 0.03019, rateFromHome = 33.12355,
                displayOrder = 3, createdAt = now),
        ))
    }

    // ── 分類（回傳常用分類 ID 供範例交易使用）─────────────
    private data class CategoryIds(
        val breakfast: Int, val lunch: Int, val dinner: Int,
        val mrt: Int,
        val rent: Int, val electricity: Int, val internet: Int,
        val clothes: Int,
        val salary: Int,
    )

    private suspend fun insertCategories(): CategoryIds {
        val dao = db.categoryDao()
        var breakfastId = 0; var lunchId = 0; var dinnerId = 0
        var mrtId = 0
        var rentId = 0; var electricityId = 0; var internetId = 0
        var clothesId = 0
        var salaryId = 0

        // ── 支出分類 ──
        val expenseGroups = listOf(
            "飲食" to listOf("早餐", "午餐", "晚餐", "飲料", "零食", "外送"),
            "交通" to listOf("捷運", "公車", "計程車", "油資", "停車費", "機票"),
            "住屋" to listOf("房租", "水費", "電費", "瓦斯費", "網路費", "電話費", "管理費"),
            "購物" to listOf("服飾", "3C 電子", "日用品", "家電", "書籍文具"),
            "娛樂" to listOf("電影", "遊戲", "旅遊", "健身", "音樂"),
            "醫療健康" to listOf("診療費", "藥品", "健保費", "保險費"),
            "教育" to listOf("學費", "書籍", "補習費", "線上課程"),
            "金融" to listOf("利息支出", "手續費", "信用卡費", "罰款"),
            "其他支出" to listOf("雜費", "捐款", "禮金"),
        )
        expenseGroups.forEachIndexed { pi, (parent, children) ->
            val pid = dao.insert(CategoryEntity(categoryName = parent, categoryType = "E",
                displayOrder = pi + 1, createdAt = now)).toInt()
            children.forEachIndexed { ci, child ->
                val cid = dao.insert(CategoryEntity(categoryName = child, categoryType = "E",
                    parentCategoryId = pid, displayOrder = ci + 1, createdAt = now)).toInt()
                when (parent to child) {
                    "飲食" to "早餐" -> breakfastId = cid
                    "飲食" to "午餐" -> lunchId = cid
                    "飲食" to "晚餐" -> dinnerId = cid
                    "交通" to "捷運" -> mrtId = cid
                    "住屋" to "房租" -> rentId = cid
                    "住屋" to "電費" -> electricityId = cid
                    "住屋" to "網路費" -> internetId = cid
                    "購物" to "服飾" -> clothesId = cid
                }
            }
        }

        // ── 收入分類 ──
        val incomeGroups = listOf(
            "薪資" to listOf("薪水", "獎金", "年終獎金", "加班費"),
            "投資" to listOf("股票獲利", "基金配息", "利息收入", "租金收入"),
            "副業" to listOf("接案收入", "兼職收入", "版稅", "稿費"),
            "其他收入" to listOf("退稅", "保險理賠", "禮金收入", "雜項收入"),
        )
        incomeGroups.forEachIndexed { pi, (parent, children) ->
            val pid = dao.insert(CategoryEntity(categoryName = parent, categoryType = "I",
                displayOrder = pi + 1, createdAt = now)).toInt()
            children.forEachIndexed { ci, child ->
                val cid = dao.insert(CategoryEntity(categoryName = child, categoryType = "I",
                    parentCategoryId = pid, displayOrder = ci + 1, createdAt = now)).toInt()
                if (parent == "薪資" && child == "薪水") salaryId = cid
            }
        }

        // ── 投資分類 ──
        val investGroups = listOf(
            "股票" to listOf("買入", "賣出", "股息"),
            "基金" to listOf("申購", "贖回", "配息"),
            "其他投資" to listOf("買入", "賣出"),
        )
        investGroups.forEachIndexed { pi, (parent, children) ->
            val pid = dao.insert(CategoryEntity(categoryName = parent, categoryType = "T",
                displayOrder = pi + 1, createdAt = now)).toInt()
            children.forEachIndexed { ci, child ->
                dao.insert(CategoryEntity(categoryName = child, categoryType = "T",
                    parentCategoryId = pid, displayOrder = ci + 1, createdAt = now))
            }
        }

        return CategoryIds(breakfastId, lunchId, dinnerId, mrtId, rentId, electricityId, internetId, clothesId, salaryId)
    }

    // ── 付款人（回傳常用 ID）────────────────────────────
    private data class PayeeIds(
        val sevenEleven: Int, val familyMart: Int,
        val mrt: Int, val salary: Int,
        val electricity: Int, val telecom: Int,
    )

    private suspend fun insertPayees(): PayeeIds {
        val dao = db.payeeDao()
        val names = listOf(
            "7-11", "全家", "萊爾富", "OK 超商",
            "全聯", "家樂福", "大潤發",
            "台灣鐵路", "高鐵", "台北捷運",
            "台灣電力公司", "台灣自來水公司",
            "中華電信", "台灣大哥大",
            "健保署", "台灣銀行", "郵局", "薪資來源",
        )
        val idMap = mutableMapOf<String, Int>()
        names.forEach { name ->
            idMap[name] = dao.insert(PayeeEntity(payeeName = name, createdAt = now)).toInt()
        }
        return PayeeIds(
            sevenEleven  = idMap["7-11"]!!,
            familyMart   = idMap["全家"]!!,
            mrt          = idMap["台北捷運"]!!,
            salary       = idMap["薪資來源"]!!,
            electricity  = idMap["台灣電力公司"]!!,
            telecom      = idMap["中華電信"]!!,
        )
    }

    // ── 設定 ────────────────────────────────────────
    private suspend fun insertSettings() {
        listOf(
            "app_pin_enabled"    to "false",
            "biometric_enabled"  to "false",
            "display_name"       to "我的帳本",
            "db_version"         to "1",
            "default_account_id" to "",
            "last_backup_at"     to "",
        ).forEach { (key, value) ->
            db.settingsDao().upsert(AppSettingEntity(key = key, value = value, updatedAt = now))
        }
    }

    // ── 範例帳戶 & 交易 ──────────────────────────────
    private suspend fun insertSampleAccountsAndTransactions(catIds: CategoryIds, payeeIds: PayeeIds) {
        val today = LocalDate.now()
        val y = today.year; val m = today.monthValue

        // 日期格式
        fun date(minusDays: Long) = today.minusDays(minusDays).toString()  // yyyy-MM-dd

        // ── 帳戶 ──
        val bankId = db.accountDao().insert(AccountEntity(
            accountName = "台灣銀行", accountType = "Bank",
            initialBalance = 50_000.0, currentBalance = 50_000.0,
            displayOrder = 1, createdAt = now,
        )).toInt()

        val creditId = db.accountDao().insert(AccountEntity(
            accountName = "台新信用卡", accountType = "CCard",
            initialBalance = 0.0, currentBalance = 0.0,
            displayOrder = 2, createdAt = now,
        )).toInt()

        val cashId = db.accountDao().insert(AccountEntity(
            accountName = "現金", accountType = "Cash",
            initialBalance = 3_000.0, currentBalance = 3_000.0,
            displayOrder = 3, createdAt = now,
        )).toInt()

        // ── 交易輔助 ──
        suspend fun addTx(accountId: Int, amount: Double, date: String,
                          categoryId: Int? = null, payeeId: Int? = null,
                          memo: String? = null, cleared: String = "C") {
            db.transactionDao().insert(TransactionEntity(
                accountId = accountId, transactionDate = date, amount = amount,
                categoryId = categoryId, payeeId = payeeId, memo = memo,
                clearedStatus = cleared, createdAt = now,
            ))
            db.accountDao().incrementCurrentBalance(accountId, amount)
        }

        // ── 台灣銀行交易 ──
        addTx(bankId,  50_000.0, date(22), catIds.salary, payeeIds.salary, "四月份薪資")
        addTx(bankId, -15_000.0, date(18), catIds.rent,   null,            "四月房租")
        addTx(bankId,  -1_200.0, date(18), catIds.internet, payeeIds.telecom, "網路費")
        addTx(bankId,    -800.0, date(15), catIds.electricity, payeeIds.electricity, "電費")
        addTx(bankId,    -500.0, date(10), catIds.mrt,    payeeIds.mrt,    "悠遊卡加值")

        // ── 台新信用卡交易 ──
        addTx(creditId, -3_500.0, date(12), catIds.clothes, null, "春季新品")
        addTx(creditId,   -350.0, date(8),  catIds.dinner,  null, "朋友聚餐")
        addTx(creditId,   -120.0, date(5),  catIds.lunch,   payeeIds.familyMart, "午餐")

        // ── 現金交易 ──
        addTx(cashId, -85.0,  date(1),  catIds.breakfast, payeeIds.sevenEleven, "早餐")
        addTx(cashId, -120.0, date(2),  catIds.lunch,     payeeIds.familyMart,  "午餐")
        addTx(cashId, -20.0,  date(3),  catIds.mrt,       payeeIds.mrt,         "捷運")
        addTx(cashId, -90.0,  date(4),  catIds.dinner,    null,                 "晚餐")
    }
}
