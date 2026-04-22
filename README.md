# PersonalAccounting_ANDROID

Android offline-first finance app scaffold based on the migration documents in `PLANS/ANDROID`.

## Current implementation status

### Implemented
- Project scaffold: Kotlin + Compose + Room + Hilt
- Database bootstrap and seed data initialization
- Account CRUD
- Dashboard home
- Account register (transaction list by account)
- General transaction creation
- Snapshot incremental update
- Monthly report summary
- Currency management (set home currency / update rate)
- Settings actions: rebuild snapshots, database backup

### Key modules
- `app/src/main/java/com/finance/manager/android/presentation/dashboard/`
- `app/src/main/java/com/finance/manager/android/presentation/account/`
- `app/src/main/java/com/finance/manager/android/presentation/accountregister/`
- `app/src/main/java/com/finance/manager/android/presentation/transaction/`
- `app/src/main/java/com/finance/manager/android/presentation/report/`
- `app/src/main/java/com/finance/manager/android/presentation/settings/`
- `app/src/main/java/com/finance/manager/android/presentation/currency/`

## Notes
- Seed data is inserted on first launch.
- The workspace currently does not contain Gradle Wrapper files (`gradlew`, `gradlew.bat`), so command-line build execution was not verifiable in this environment.
- `PLANS/ANDROID/` remains the requirements and migration reference set.
