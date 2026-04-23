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

- **底部導覽分頁**（頂層）：`Dashboard`（首頁）、`Reports`（報表）、`Settings`（設定）。
- **子路由**：`Accounts`、`AccountRegister/{accountId}`、`AccountForm/{accountId?}`、`TransactionForm/{accountId}/{transactionId?}`。
- **主資料管理**：`CategoryManagement`、`PayeeManagement`、`TagManagement`、`Currencies`（一律由「設定」頁進入）。
- 畫面以 lambda 參數接收導覽回呼，**不持有** `NavController`。

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

## UI 設計語言（對齊 Web 桌面版）

- **主色系**：藍色漸層（`GradientStart` → `GradientEnd`）用於頭部卡片；`extendedColors`（`incomeGreen` / `expenseRed` / `transferBlue`）用於金額著色。
- **Dashboard**：上下堆疊——淨值卡 → 帳戶清冊（依 `AccountType` 分群、可展開收合）→ 本月收支 → 最近交易。底部導覽列精簡為「首頁 / 報表 / 設定」。
- **交易清冊（`AccountRegisterScreen`）**：橫式表格；固定欄寬（`日期 | 收/付款人 | 類別 | 標籤 | 備註 | 支出 | 收入 | 餘額 | 操作`）+ 共用 `ScrollState`；表頭與每列水平滑動同步；支援累計餘額逐列顯示。
- **主資料管理**：`TopAppBar`（返回 + 標題 + 「+ 新增」）+ 搜尋列 +（Category）FilterChip +「顯示已隱藏」Switch + LazyColumn + 編輯 AlertDialog / ModalBottomSheet。

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
4. **畫面重構**：對齊 Web 桌面版清冊樣式（橫式表格 + 固定欄寬 + 共用 `ScrollState`）；手機直立下需確保可水平捲動。
5. **交易表單改動**：`TransactionForm` 舊路由仍保留作為回退；任何修改需確認與 `AccountRegister` 共用的 UseCase 行為一致。
6. **測試端同步**：修改 ViewModel 建構子參數時，務必同步更新 `app/src/test/.../*ViewModelTest.kt`，否則 `compileDebugUnitTestKotlin` 會失敗。
7. **完成每次程式修改後，執行 `./gradlew assembleDebug` 確認建置成功**；涉及 UseCase / ViewModel 的修改另加 `./gradlew test`。

