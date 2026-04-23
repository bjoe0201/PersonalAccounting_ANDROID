# PersonalAccounting_ANDROID

![版本](https://img.shields.io/badge/版本-0.3.0-blue)
![DB](https://img.shields.io/badge/DB%20schema-v2-orange)
![平台](https://img.shields.io/badge/平台-Android%2026%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4)

基於 `PLANS/ANDROID` 規劃文件開發的 **Android 離線個人記帳 App**，採 Clean Architecture（三層式）+ Jetpack Compose + Room + Hilt。UI 已從 Web 桌面版橫式表格全面重新設計為**行動裝置友善的卡片式介面**。

---

## 功能總覽

### ✅ 已完成
- **專案骨架**：Kotlin + Jetpack Compose (Material3) + Room (v2) + Hilt + Navigation Compose。
- **資料庫**：11 個 Entity、Schema 已匯出至 `app/schemas/`；首次啟動自動植入種子資料（幣別、類別）。
- **帳戶**：CRUD、依資產/負債分組列表、詳細餘額計算。
- **交易**：新增 / 編輯 / 刪除；**轉帳**（雙筆連結）、**分割交易**（多分類明細）。
- **全面 UI 重新設計（v0.3.0）**：
  - **5 分頁底部導覽列**：首頁、帳戶、記帳（漸層特殊按鈕）、報表、設定。
  - **Dashboard**：淨資產英雄卡片（漸層 + 三欄收支摘要）、橫向帳戶小卡片、最近交易卡片列表。
  - **帳戶管理**：依資產/負債分組、漸層新增按鈕、淨資產總覽卡片。
  - **帳戶登記簿**：從橫向捲動表格改為**垂直卡片式清單**，按日期分組，清算狀態圓點、分類 Emoji 徽章、刪除確認 BottomSheet。
  - **報表**：環形圖（DonutChart）+ 圖例 + 進度條分類明細、彩色收支摘要卡片。
  - **設定**：分組 ListItem 風格、Emoji 圖示、危險操作紅色標示。
  - **共用元件庫**：`CategoryBadge`、`AmountText`、`AccountTypeIconCircle`、`FinanceChip`、`FinanceDivider`、`SectionHeader`。
  - **色彩系統**：完整設計 Token（`primary`、`incomeGreen`/`Light`、`expenseRed`/`Light`、`transferBlue`/`Light`、`surfaceElevated`、`danger`、`warning`）。
  - **帳戶類型圖示**：正確對應 Material Icons（`AccountBalance`、`Payments`、`CreditCard`、`TrendingUp`）+ 類型專屬背景色。
- **月報表**：分類圓餅 / 長條、月度摘要。
- **主資料管理模組**：
  - **類別管理**：依類型篩選（全部 / 支出 / 收入 / 投資）、搜尋、隱藏切換、增刪改。
  - **付款人管理**：交易次數 + 絕對值總金額、搜尋、隱藏切換、增刪改。
  - **標籤管理**：類型分類、描述、搜尋、隱藏切換、增刪改。
  - 全部採 **禁止硬刪除已引用項目** 策略，改以「隱藏」處理。
- **幣別管理**：設定本位幣、更新匯率。
- **月結快照**：單月增量 / 全部重建。
- **設定**：資料庫備份 / 還原、初始化資料庫、重建快照、主資料管理入口。

### 🚧 進行中
- 快速記帳 BottomSheet 完整表單（目前為帳戶選擇 → 導向交易表單）。
- 交易表單 `DatePickerDialog`、`SegmentedButton` 類型切換。

---

## 架構概觀（Clean Architecture）

```
presentation/   Jetpack Compose 畫面 + Hilt ViewModel（StateFlow）
domain/         純 Kotlin：領域模型、Repository 介面、Use Case
data/           Room 資料庫、DAO 實作、Mapper（Entity ↔ Domain）
di/             Hilt 模組：DatabaseModule / RepositoryModule
```

依賴方向：**Presentation → Domain ← Data**。

### 主要目錄

| 路徑 | 說明 |
|---|---|
| `presentation/dashboard/` | 首頁（英雄卡片 + 橫向帳戶 + 最近交易） |
| `presentation/account/` | 帳戶列表（分組）/ 表單 |
| `presentation/accountregister/` | 帳戶登記簿（卡片式，按日期分組） |
| `presentation/transaction/` | 交易表單（新增 / 編輯 / 轉帳 / 分割） |
| `presentation/quickadd/` | 快速記帳 BottomSheet |
| `presentation/report/` | 報表（環形圖 + 進度條明細） |
| `presentation/category/` | 類別管理 |
| `presentation/payee/` | 付款人管理 |
| `presentation/tag/` | 標籤管理 |
| `presentation/currency/` | 幣別管理 |
| `presentation/settings/` | 設定（分組 ListItem 風格） |
| `presentation/components/` | 共用元件（`AccountTypeIcon`、`SharedComponents`） |
| `presentation/navigation/` | 導覽（`AppNavGraph`、`Screen`、5 分頁底部導覽列） |

---

## 建置與執行

需 Android Studio 或 JDK 17 + Android SDK（API 26+）。Gradle Wrapper 已附於專案。

```bash
# Debug APK
./gradlew assembleDebug

# 單元測試
./gradlew test

# 儀器化測試（需連接裝置或模擬器）
./gradlew connectedAndroidTest

# 完整檢查
./gradlew check
```

產出 APK 路徑：`app/build/outputs/apk/debug/app-debug.apk`。

### ⚠️ 升級注意事項（DB schema v1 → v2）

`CategoryEntity` / `PayeeEntity` / `TagEntity` 擴充了 `is_hidden` / `description` / `display_order` / `tag_type` 等欄位。`DatabaseModule` 已配置 `fallbackToDestructiveMigration`，舊版 App 更新時舊 DB 會**自動銷毀並重建**，種子資料重新植入；帳戶與交易需重新輸入。

---

## 導覽路由

定義於 `presentation/navigation/Screen.kt`：

- 頂層（底部導覽列）：`Dashboard` / `Accounts` / `Reports` / `Settings`
- 特殊：底部導覽「記帳」按鈕 → 彈出 `QuickAddBottomSheet`
- 子路由：`AccountRegister/{accountId}` / `AccountForm/{accountId?}` / `TransactionForm/{accountId}/{transactionId?}`
- 主資料：`CategoryManagement` / `PayeeManagement` / `TagManagement` / `Currencies`

---

## 資料庫

- 名稱：`finance_manager.db`，版本 `2`，`exportSchema = true`（輸出至 `app/schemas/`）。
- 11 個 Entity：
  - 核心：`AccountEntity`、`TransactionEntity`、`TransactionSplitEntity`、`AccountMonthlyBalanceEntity`
  - 主資料：`CategoryEntity`、`PayeeEntity`、`TagEntity`、`CurrencyEntity`
  - 關聯：`TransactionTagEntity`、`TransactionSplitTagEntity`
  - 其他：`AppSettingEntity`
- 所有 DAO 集中於 `data/local/dao/Daos.kt`；所有 Entity 集中於 `data/local/entity/Entities.kt`。
- **快照機制**：`AccountMonthlyBalanceEntity` 快取每帳戶每月結餘；由 `UpdateSnapshotUseCase` / `RebuildAllSnapshotsUseCase` 維護。
- **轉帳交易**：建立兩筆以 `linked_transaction_id` 串聯的 `TransactionEntity`。

---

## 測試

- **單元測試**（`app/src/test/`）：MockK、`kotlinx-coroutines-test`、Turbine、`InstantTaskExecutorRule`。
- **儀器化測試**（`app/src/androidTest/`）：`Room.inMemoryDatabaseBuilder`，每個 DAO 一個測試類別。

---

## 語言

UI 字串與錯誤訊息均以 **繁體中文（台灣）** 撰寫。

---

## 相關文件

- `PLANS/ANDROID/` — 原始需求規格與遷移參考。
- `PLANS/08-類別付款人標籤管理功能計畫書.md` — 類別 / 付款人 / 標籤管理 + UI 重構計畫。
- `CLAUDE.md` — AI 輔助開發（Claude Code / GitHub Copilot 代理）在本專案的運作指引。
