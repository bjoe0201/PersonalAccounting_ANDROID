# PersonalAccounting_ANDROID

![版本](https://img.shields.io/badge/版本-0.1.0-blue)
![Build](https://img.shields.io/badge/build-1-lightgrey)
![平台](https://img.shields.io/badge/平台-Android%2026%2B-green)

基於 `PLANS/ANDROID` 規劃文件開發的 Android 離線個人記帳 App。

## 目前實作狀態

### 已完成

- 專案骨架：Kotlin + Compose + Room + Hilt
- 資料庫初始化與種子資料植入
- 帳戶 CRUD
- 儀表板首頁
- 帳戶明細（依帳戶顯示交易清單）
- 一般交易建立
- 快照增量更新
- 月報表摘要
- 幣別管理（設定本位幣 / 更新匯率）
- 設定功能：重建月結快照、資料庫備份、資料庫初始化

### 主要模組

- `app/src/main/java/com/finance/manager/android/presentation/dashboard/`
- `app/src/main/java/com/finance/manager/android/presentation/account/`
- `app/src/main/java/com/finance/manager/android/presentation/accountregister/`
- `app/src/main/java/com/finance/manager/android/presentation/transaction/`
- `app/src/main/java/com/finance/manager/android/presentation/report/`
- `app/src/main/java/com/finance/manager/android/presentation/settings/`
- `app/src/main/java/com/finance/manager/android/presentation/currency/`

## 備註

- 種子資料於首次啟動時自動植入。
- `PLANS/ANDROID/` 為需求規格與遷移參考文件。
