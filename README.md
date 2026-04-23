# PersonalAccounting_ANDROID

![版本](https://img.shields.io/badge/版本-0.2.0-blue)
![DB](https://img.shields.io/badge/DB%20schema-v2-orange)
![平台](https://img.shields.io/badge/平台-Android%2026%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4)

基於 `PLANS/ANDROID` 規劃文件開發的 **Android 離線個人記帳 App**，採 Clean Architecture（三層式）+ Jetpack Compose + Room + Hilt。UI 以 Web 桌面版為設計藍本（漸層卡片、橫式交易清冊、主資料清冊管理）。

---

## 功能總覽

### ✅ 已完成
- **專案骨架**：Kotlin + Jetpack Compose (Material3) + Room (v2) + Hilt + Navigation Compose。
- **資料庫**：11 個 Entity、Schema 已匯出至 `app/schemas/`；首次啟動自動植入種子資料（幣別、類別）。
- **帳戶**：CRUD、依類型分群列表、詳細餘額計算。
- **交易**：新增 / 編輯 / 刪除；**轉帳**（雙筆連結）、**分割交易**（多分類明細）。
- **交易清冊（Web 版橫式排版）**：
  - 頂部漸層卡片（帳戶名 / 類型 / 當前餘額）。
  - 表頭 `日期 | 收/付款人 | 類別 | 標籤 | 備註 | 支出 | 收入 | 餘額 | 操作`。
  - 支出紅、收入綠、轉帳藍；**累計餘額**逐列計算。
  - 表頭 + 每列共用同一 `ScrollState`，水平滑動同步。
- **Dashboard（主畫面）**：仿 Web 桌面版——頂部淨值卡、依 `AccountType` 分群帳戶清冊、本月收支摘要、最近交易；底部導覽列精簡為「首頁 / 報表 / 設定」。
- **月報表**：分類圓餅 / 長條、月度摘要。
- **主資料管理模組**（對齊 Web 版的清冊畫面）：
  - **類別管理**：依類型篩選（全部 / 支出 / 收入 / 投資）、搜尋、隱藏切換、增刪改。
  - **付款人管理**：交易次數 + 絕對值總金額、搜尋、隱藏切換、增刪改。
  - **標籤管理**：類型分類、描述、搜尋、隱藏切換、增刪改。
  - 全部採 **禁止硬刪除已引用項目** 策略，改以「隱藏」處理。
- **幣別管理**：設定本位幣、更新匯率。
- **月結快照**：單月增量 / 全部重建。
- **設定**：資料庫備份 / 還原、初始化資料庫、重建快照、主資料管理入口。

### 🚧 進行中（Phase 5）
- 交易清冊的**行內編輯**、`SelectCategoryDialog`、`SplitTransactionDialog`。
- 交易表單下拉自動過濾隱藏項目（編輯既有交易保留已選）。

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
| `presentation/dashboard/` | 主畫面（帳戶清冊 + 摘要 + 最近交易） |
| `presentation/account/` | 帳戶列表 / 表單 |
| `presentation/accountregister/` | 交易清冊（Web 版橫式排版） |
| `presentation/transaction/` | 交易表單（新增 / 編輯 / 轉帳 / 分割） |
| `presentation/report/` | 月報表 / 圖表 |
| `presentation/category/` | 類別管理 |
| `presentation/payee/` | 付款人管理 |
| `presentation/tag/` | 標籤管理 |
| `presentation/currency/` | 幣別管理 |
| `presentation/settings/` | 設定 |

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

若平板 / 手機仍執行舊版 APK 導致 `IllegalStateException: A migration from 1 to 2 was required...`，請：
1. 解除安裝舊版 App。
2. 重新安裝最新 APK（或 `adb install`）。

---

## 導覽路由

定義於 `presentation/navigation/Screen.kt`：

- 頂層：`Dashboard` / `Reports` / `Settings`
- 子路由：`Accounts` / `AccountRegister/{accountId}` / `AccountForm/{accountId?}` / `TransactionForm/{accountId}/{transactionId?}`
- 主資料：`CategoryManagement` / `PayeeManagement` / `TagManagement` / `Currencies`

底部導覽列僅顯示 **首頁 / 報表 / 設定** 三項；類別 / 付款人 / 標籤 / 幣別管理一律由「設定」進入。

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
