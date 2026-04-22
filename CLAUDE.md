# CLAUDE.md

此檔案提供 Claude Code (claude.ai/code) 在本儲存庫中運作時所需的指引。

## 建置與測試指令

本專案需要 Android Studio 或 JDK + Android SDK 環境。Gradle Wrapper 已包含於專案中（`gradlew` / `gradlew.bat`）。

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

- **底部導覽分頁**（頂層）：Dashboard（首頁）、Accounts（帳戶）、Reports（報表）、Settings（設定）
- **子路由**：AccountRegister、AccountForm、TransactionForm、Currencies
- 畫面以 lambda 參數接收導覽回呼，**不持有** `NavController`。

## 資料庫

- Room 資料庫名稱：`finance_manager.db`，版本 1，`exportSchema = true`（Schema 匯出至 `app/schemas/`）。
- 共 11 個 Entity，核心為：`AccountEntity`、`TransactionEntity`、`TransactionSplitEntity`、`AccountMonthlyBalanceEntity`、`CategoryEntity`、`CurrencyEntity`。
- **種子資料**：首次啟動時由 `DatabaseInitializer` 植入（以 `currencyDao().getCount() > 0` 判斷）。`forceInitialize()` 清空全部資料表後重新植入，用於「設定 → 初始化資料庫」。
- **快照機制**：`AccountMonthlyBalanceEntity` 快取每個帳戶的月結餘額。`UpdateSnapshotUseCase` 更新單一月份；`RebuildAllSnapshotsUseCase` 重建全部快照。
- 轉帳交易會建立**兩筆**相互連結的 `TransactionEntity`（各屬一個帳戶），以 `linked_transaction_id` 串聯。

## 狀態管理模式

```kotlin
// ViewModel
private val _uiState = MutableStateFlow(XxxUiState())
val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()

// Screen
val uiState by viewModel.uiState.collectAsState()
```

ViewModel 在 `init {}` 中訂閱 Repository 的 Flow。變更操作呼叫 Use Case 後，將結果（成功訊息 / 錯誤訊息）透過同一個 `uiState` 傳遞給畫面。

## Use Case 慣例

- 每個 Use Case 只有一個公開的 `operator fun invoke(...)`。
- 變更型操作以 `suspend` 回傳 `AppResult<T>`；讀取型操作直接回傳 `Flow<T>`。
- 驗證邏輯（名稱不可空白、重複名稱檢查、金額不為零、分割合計需符合）在 Use Case 內、呼叫 Repository 之前完成。
- 跨多資料表的變更（建立交易、轉帳、刪除）以 `db.withTransaction { }` 包裹。

## 測試

- **單元測試**（`app/src/test/`）：使用 MockK 建立模擬物件、`kotlinx-coroutines-test`、`Turbine` 驗證 Flow 輸出、`InstantTaskExecutorRule` / `StandardTestDispatcher`。
- **儀器化測試**（`app/src/androidTest/`）：使用 `Room.inMemoryDatabaseBuilder` 建立真實記憶體內資料庫，每個 DAO 對應一個測試類別。

## 語言

UI 字串與錯誤訊息均以**繁體中文（台灣）**撰寫，新增的使用者可見字串請維持此慣例。
