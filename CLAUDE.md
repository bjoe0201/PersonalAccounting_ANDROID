# CLAUDE.md

此檔案提供 Claude Code / GitHub Copilot 等 AI 代理在本儲存庫中運作時所需的指引。

## 建置與測試指令

需 Android Studio 或 JDK 17 + Android SDK（API 26+）。Gradle Wrapper 已包含於專案中（`gradlew` / `gradlew.bat`）。

```bash
# 建置 Debug APK
./gradlew assembleDebug

# 執行單元測試（JVM，不需裝置）
./gradlew test

# 執行單一測試類別
./gradlew test --tests "com.finance.manager.android.domain.usecase.account.CreateAccountUseCaseTest"

# 執行儀器化測試（Room，需連接裝置或模擬器）
./gradlew connectedAndroidTest

# 靜態分析
./gradlew lint

# 完整檢查（含測試 + lint）
./gradlew check
```

APK 路徑：`app/build/outputs/apk/debug/app-debug.apk`。

## 架構

採用三層式 Clean Architecture，依賴關係只能由外向內流動（Presentation → Domain ← Data）。

```
presentation/   Jetpack Compose 畫面 + Hilt ViewModel（StateFlow）
domain/         純 Kotlin：領域模型、Repository 介面、Use Case
data/           Room 資料庫、DAO 實作、Mapper（Entity ↔ Domain）
di/             Hilt 模組：DatabaseModule（provides）、RepositoryModule（binds）
```

**核心契約：**
- `AppResult<T>` — sealed interface（`Success` / `Error`），所有變更型 Use Case 均回傳此型別。
- Repository 介面放在 `domain/repository/`；實作放在 `data/repository/`。
- 所有 DAO 集中宣告於單一檔案：`data/local/dao/Daos.kt`。
- 所有 Room Entity 集中於單一檔案：`data/local/entity/Entities.kt`。
- 統計用 POJO（含 JOIN、使用次數等）集中於 `data/local/dao/QueryModels.kt`。

## 依賴注入（Hilt）

| 標註 | 使用位置 |
|---|---|
| `@HiltAndroidApp` | `FinanceManagerApp` |
| `@AndroidEntryPoint` | `MainActivity` |
| `@HiltViewModel` | 所有 ViewModel |
| `DatabaseModule` | `@Provides` 提供 `AppDatabase` 及各 DAO |
| `RepositoryModule` | `@Binds` 將實作綁定至介面（abstract module） |

## 導覽

導覽定義於 `AppNavGraph.kt`，路由透過 `Screen.kt` 的 `Screen` sealed class 型別化。

- **底部導覽分頁**（5 分頁）：`Dashboard`（首頁）、`Accounts`（帳戶）、**記帳**（QuickAdd，彈出 BottomSheet）、`Reports`（報表）、`Settings`（設定）。
- **子路由**：`AccountRegister/{accountId}`、`AccountForm/{accountId?}`、`TransactionForm/{accountId}/{transactionId?}`。
- **主資料管理**：`CategoryManagement`、`PayeeManagement`、`TagManagement`、`Currencies`（一律由「設定」頁進入）。
- 畫面以 lambda 參數接收導覽回呼，**不持有** `NavController`。
- 頁面切換動畫：`slideInHorizontally` + `fadeIn` / `slideOutHorizontally` + `fadeOut`。

## 資料庫

- Room 資料庫名稱：`finance_manager.db`，**版本 2**，`exportSchema = true`（Schema 匯出至 `app/schemas/`）。
- 共 11 個 Entity，核心為：`AccountEntity`、`TransactionEntity`、`TransactionSplitEntity`、`AccountMonthlyBalanceEntity`、`CategoryEntity`、`PayeeEntity`、`TagEntity`、`CurrencyEntity`。
- **DB schema 變更策略（開發期）**：
  - `DatabaseModule` 已配置 `fallbackToDestructiveMigration` + `fallbackToDestructiveMigrationOnDowngrade` + `fallbackToDestructiveMigrationFrom(1)`。
  - 修改 Entity 欄位時請同步 **升版 `AppDatabase.version`**，否則舊裝置會觸發 `IllegalStateException: A migration ... was required but not found`。
- **種子資料**：首次啟動時由 `DatabaseInitializer` 植入（以 `currencyDao().getCount() > 0` 判斷）。`forceInitialize()` 清空全部資料表後重新植入，用於「設定 → 初始化資料庫」。
- **快照機制**：`AccountMonthlyBalanceEntity` 快取每個帳戶的月結餘額。`UpdateSnapshotUseCase` 更新單一月份；`RebuildAllSnapshotsUseCase` 重建全部快照。
- **轉帳交易**：會建立**兩筆**相互連結的 `TransactionEntity`（各屬一個帳戶），以 `linked_transaction_id` 串聯。
- **主資料隱藏欄位**：`CategoryEntity.isHidden`、`PayeeEntity.isHidden`、`TagEntity.isHidden` 用於管理頁的隱藏切換；交易下拉應只顯示未隱藏項目，但編輯既有交易時需保留已選的隱藏項目。

## 狀態管理模式

```kotlin
// ViewModel
private val _uiState = MutableStateFlow(XxxUiState())
val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()

// Screen
val uiState by viewModel.uiState.collectAsState()
```

ViewModel 在 `init {}` 或 `load()` 中訂閱 Repository 的 Flow。變更操作呼叫 Use Case 後，將結果（成功訊息 / 錯誤訊息）透過同一個 `uiState` 傳遞給畫面並以 Snackbar 顯示。

## Use Case 慣例

- 每個 Use Case 只有一個公開的 `operator fun invoke(...)`。
- 變更型操作以 `suspend` 回傳 `AppResult<T>`；讀取型操作直接回傳 `Flow<T>`。
- 驗證邏輯（名稱不可空白、重複名稱檢查、金額不為零、分割合計需符合）在 Use Case 內、呼叫 Repository 之前完成。
- 跨多資料表的變更（建立交易、轉帳、刪除）以 `db.withTransaction { }` 包裹。
- **刪除策略**：Category / Payee / Tag 若 `countUsage > 0`（或 Category 有子分類），一律回 `AppResult.Error("已有 X 筆引用，無法刪除；請改為隱藏")`；只允許硬刪除完全未被引用者。

## UI 設計語言（行動裝置優先卡片式設計）

### 設計原則
- **行動裝置友善**：所有畫面採垂直卡片式佈局，不使用橫向捲動表格。
- **高保真設計稿**：設計參考存放於 `D:\TEMP\FinanceManagerAndroid-UI-DESIGN\design_handoff_ui_redesign\`。
- **僅修改 Presentation 層**：UI 重構只變更 Screen Composable，Domain / Data / ViewModel UiState 介面不變。

### 色彩系統（Design Tokens）

| Token | 亮色值 | 用途 |
|-------|--------|------|
| `primary` | `#006D77` | 主要按鈕、強調色 |
| `gradientStart` → `gradientEnd` | `#006D77` → `#4ECDC4` | 英雄卡片、FAB、漸層按鈕 |
| `incomeGreen` / `incomeGreenLight` | `#2E7D32` / `#E8F5E9` | 收入金額 / 收入背景 |
| `expenseRed` / `expenseRedLight` | `#C62828` / `#FFEBEE` | 支出金額 / 支出背景 |
| `transferBlue` / `transferBlueLight` | `#1565C0` / `#E3F2FD` | 轉帳金額 / 轉帳背景 |
| `surface` | `#FAFDFD` | 頁面背景 |
| `surfaceVariant` | `#DAE4E5` | 次要背景（Chip、未選中 Tab） |
| `surfaceElevated` | `#EEF4F5` | 卡片/輸入欄背景 |
| `onSurface` | `#191C1C` | 主要文字 |
| `onSurfaceVariant` | `#4F6367` | 次要文字 |
| `outlineVariant` | `#BFC8C9` | 淡邊框/分隔線 |
| `danger` | `#BA1A1A` | 危險操作（刪除） |
| `warning` | `#E65100` | 警告（未清算） |

使用方式：`MaterialTheme.extendedColors.incomeGreen`、`MaterialTheme.extendedColors.surfaceElevated` 等。

### 帳戶類型圖示與色彩

| 類型 | Icon | 背景色 | Icon 色 |
|------|------|--------|---------|
| Bank | `Icons.Filled.AccountBalance` | `#E3F2FD` | `#1565C0` |
| Cash | `Icons.Filled.Payments` | `#E8F5E9` | `#2E7D32` |
| CCard | `Icons.Filled.CreditCard` | `#FBE9E7` | `#E65100` |
| Invst | `Icons.AutoMirrored.Filled.TrendingUp` | `#F3E5F5` | `#6A1B9A` |

使用 `AccountTypeIconCircle(type, size)` 元件，定義於 `presentation/components/AccountTypeIcon.kt`。

### 分類 Emoji 與顏色

定義於 `presentation/components/SharedComponents.kt` 的 `CATEGORY_ICONS`：

| 分類 | Emoji | 背景色 |
|------|-------|--------|
| 飲食 | 🍔 | `#FFF3E0` |
| 交通 | 🚇 | `#E3F2FD` |
| 住屋 | 🏠 | `#E8F5E9` |
| 娛樂 | 🎮 | `#F3E5F5` |
| 醫療 | 💊 | `#FFEBEE` |
| 購物 | 🛍 | `#FBE9E7` |
| 薪資 | 💰 | `#E8F5E9` |
| 投資 | 📈 | `#E0F2F1` |
| 轉帳 | 🔄 | `#E3F2FD` |

使用 `CategoryBadge(categoryName, size)` 元件。

### 共用元件庫（`presentation/components/`）

| 元件 | 檔案 | 說明 |
|------|------|------|
| `AccountTypeIconCircle` | `AccountTypeIcon.kt` | 圓形背景 + 帳戶類型 Icon |
| `accountTypeStyle()` | `AccountTypeIcon.kt` | 取得帳戶類型的 icon/背景色/icon色 |
| `accountTypeIcon()` | `AccountTypeIcon.kt` | 僅取得 ImageVector（向下相容） |
| `CategoryBadge` | `SharedComponents.kt` | Emoji 圓形徽章 |
| `AmountText` | `SharedComponents.kt` | 帶 +/- 前綴、顏色依類型（收入綠/支出紅/轉帳藍） |
| `formatAmount()` | `SharedComponents.kt` | 金額格式化（整數無小數、否則兩位小數、千分位） |
| `SectionHeader` | `SharedComponents.kt` | 區段標題（大寫標籤 + 可選動作連結） |
| `FinanceChip` | `SharedComponents.kt` | Pill 形 Chip（圓角 100dp） |
| `FinanceDivider` | `SharedComponents.kt` | 1dp 分隔線（`outlineVariant@60%`） |

### 間距與圓角慣例

| Token | 值 | 用途 |
|-------|-----|------|
| 頁面水平邊距 | 16dp（Dashboard）/ 14dp（Register） | 主要內容 |
| 英雄卡片圓角 | 20dp | 淨資產卡片 |
| 卡片圓角 | 14–16dp | 交易卡片、帳戶卡片、設定分組 |
| Chip 圓角 | 100dp | 全圓 pill |
| 按鈕圓角 | 12–14dp | 主要按鈕 |
| FAB 圓角 | 26dp | 圓形 FAB（52dp） |
| 卡片間距 | 10–12dp | 垂直列表間距 |
| 分隔線 | 1dp, `outlineVariant@60%` | 列表分隔 |
| 卡片邊框 | 1dp, `outlineVariant@40%` | 白色卡片容器 |

### 各畫面設計規格

#### Dashboard（首頁）
- **TopBar**：左「財務總覽」+ 年月副標，右通知鈴鐺（36dp 圓形，`primary@12%`）。
- **淨資產英雄卡片**：`Brush.linearGradient(GradientStart, GradientEnd)`、`radius 20dp`、`shadow 4dp`。上半部「淨資產」大數字（32sp Bold）；下半部三欄「本月收入/支出/結餘」，白色分隔線。
- **帳戶小卡片**：`LazyRow`、每張 `120dp 寬`、`radius 14dp`、帳戶類型圓形 icon（32dp）、帳戶名、餘額（負數紅色）。
- **最近交易**：白色卡片容器（`radius 16dp`、`border outlineVariant@40%`）；每列 `CategoryBadge(38dp)` + 主副文字 + `AmountText` + 日期。

#### 帳戶管理（AccountListScreen）
- **TopBar**：標題「帳戶管理」+ 右側漸層「+ 新增帳戶」按鈕。
- 依「資產帳戶」/「負債帳戶」分組，各組白色卡片容器。
- 每列：`AccountTypeIconCircle(42dp)` + 名稱 + 類型 Chip + 餘額 + `ChevronRight`。
- 底部淨資產總覽卡片（漸層淡背景 `primary@8%~12%`）。

#### 帳戶登記簿（AccountRegisterScreen）
- **TopBar**：返回箭頭 + 帳戶名/類型副標 + Filter/Calendar Icon。
- **帳戶摘要卡片**：漸層背景、`radius 16dp`。左「目前餘額」大金額（26sp Bold），右「本月收入/支出」。
- **月份分頁列**：`LazyRow` Chip（`radius 20dp`），選中 `primary` 背景白字。
- **交易列表**：按日期分組。
  - `DateGroupHeader`：日期 + 彈性分隔線 + 當日淨額（綠/紅）。
  - `TransactionCardRow`：清算圓點（7dp）+ `CategoryBadge(36dp)` + 付款人/分類Chip/備註 + 金額（依類型著色）+ 未清算 Chip + 編輯/刪除按鈕。
- **刪除確認**：`ModalBottomSheet`（非 AlertDialog）。
- **FAB**：52dp 漸層圓形。

#### 報表（ReportScreen）
- 標題「報表分析」，Tab 為 pill 樣式。
- 收支摘要：兩張彩色卡片（`incomeGreenLight` / `expenseRedLight`）。
- **環形圖（DonutChart）**：160dp、stroke 22dp、中心「總支出」+ 金額，右側圖例（色點 + 分類名 + 百分比）。
- **分類明細**：每列色點 + 分類名 + 金額 + 進度條（4dp 高）+ 百分比。

#### 設定（SettingsScreen）
- 標題「設定」，分組 ListItem 風格。
- 分組：「帳目管理」、「資料管理」、「關於」，各組白色卡片容器。
- 每列：emoji icon（20dp, 28dp 寬）+ 主標題 + 副標題 + `ChevronRight`（箭頭項）。
- 危險項目（初始化資料庫）：主標題用 `danger` 色。
- Footer：App 名稱 + 版本。

#### 快速記帳（QuickAddBottomSheet）
- 目前為帳戶選擇列表，選後導向 `TransactionFormScreen`。
- 設計稿規劃完整表單（類型選擇器、金額輸入、分類快選、帳戶/日期選擇），待後續實作。

### 底部導覽列

自訂 `FinanceBottomBar`（非 Material3 `NavigationBar`），定義於 `AppNavGraph.kt`：
- 5 分頁：首頁（`Home`）、帳戶（`AccountBalanceWallet`）、記帳（`Add`，特殊）、報表（`BarChart`）、設定（`Settings`）。
- 「記帳」按鈕：漸層 pill 背景（52×32dp，`radius 16dp`），白色 icon，點擊彈出 `QuickAddBottomSheet`。
- 一般分頁：選中時 `primary@12%` 背景圓角框。
- 高度 64dp，頂部 `shadow 8dp`。

### 清算狀態圓點
- `Cleared (C)` → `incomeGreen`
- `Uncleared (U)` → `outlineVariant`
- `Reconciled (X)` → `primary`

## 測試

- **單元測試**（`app/src/test/`）：使用 MockK 建立模擬物件、`kotlinx-coroutines-test`、`Turbine` 驗證 Flow 輸出、`InstantTaskExecutorRule` / `StandardTestDispatcher`。
- **儀器化測試**（`app/src/androidTest/`）：使用 `Room.inMemoryDatabaseBuilder` 建立真實記憶體內資料庫，每個 DAO 對應一個測試類別。
- 新增 ViewModel / UseCase 時，**先寫測試**，確保 `AppResult` 成功與失敗兩條分支都涵蓋。

## 語言與字串慣例

- UI 字串與錯誤訊息均以 **繁體中文（台灣）** 撰寫，新增的使用者可見字串請維持此慣例。
- 避免硬編碼英文 debug 字串洩漏到 UI。

## 給 AI 代理的重要提醒

1. **檔案編輯含中文**：優先使用 `replace_string_in_file` / `insert_edit_into_file`；若需整檔重寫，**先 `Remove-Item` 再 `create_file`**。切勿用 PowerShell 的 `Set-Content` 寫入含中文的 Kotlin 檔（Windows PowerShell 5.1 預設編碼會造成 UTF-8 亂碼）。
2. **Entity / DAO 變更**：必須同時升 `AppDatabase.version`，否則會觸發 Room migration 例外。
3. **新增 Repository 實作時**：到 `RepositoryModule` 加 `@Binds`；若只是擴充既有介面方法則不需改 DI。
4. **UI 樣式一致性**：所有新畫面必須使用共用元件庫（`CategoryBadge`、`AmountText`、`AccountTypeIconCircle`、`FinanceChip`、`FinanceDivider`）和設計 Token（`extendedColors`）。卡片容器一律使用白色背景 + `outlineVariant@40%` 邊框 + `RoundedCornerShape(16.dp)`。
5. **帳戶登記簿已改為卡片式**：不再使用橫向捲動表格。交易列表按日期分組，每組為一個白色卡片容器。
6. **底部導覽列**：5 分頁（首頁/帳戶/記帳/報表/設定），「記帳」為特殊漸層按鈕。修改底部導覽邏輯時注意 `topLevelRoutes` 包含 `Accounts`。
7. **交易表單改動**：`TransactionForm` 舊路由仍保留作為回退；任何修改需確認與 `AccountRegister` 共用的 UseCase 行為一致。
8. **測試端同步**：修改 ViewModel 建構子參數時，務必同步更新 `app/src/test/.../*ViewModelTest.kt`，否則 `compileDebugUnitTestKotlin` 會失敗。
9. **完成每次程式修改後，執行 `./gradlew assembleDebug` 確認建置成功**；涉及 UseCase / ViewModel 的修改另加 `./gradlew test`。
10. **`material-icons-extended` 依賴**：已加入 `build.gradle.kts`，可使用 `AccountBalance`、`Payments`、`CreditCard`、`TrendingUp`、`FilterList`、`CalendarMonth`、`BarChart`、`AccountBalanceWallet` 等擴充 icon。
