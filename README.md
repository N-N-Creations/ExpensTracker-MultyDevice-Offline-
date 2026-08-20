# Expense Tracker 💰

An **offline-first, privacy-focused personal finance manager** built natively for Android using Kotlin and Jetpack Compose. Effortlessly track daily expenses and income, manage bank accounts and cash on hand, monitor Tabby/Tamara BNPL installments, calculate cash denominations, and synchronize data between devices over local Wi-Fi or hotspot without cloud servers.

---

## ✨ Key Features

### 1. 📊 Expense & Income Tracking
- **Quick Logging**: Add expenses and income with custom categories, payment methods (Cash, Card, Bank Transfer, BNPL), notes, and timestamps.
- **Dynamic Period Filtering**: Filter your finances by Today, Yesterday, This Week, This Month, This Year, or a Custom Date Range.
- **Search & Quick Categories**: Instantly search transactions by note, category, or amount.

### 2. 🏦 Bank Accounts & Cash Management
- **Multiple Bank Accounts**: Add, edit, or archive multiple bank accounts with initial balances, masked account numbers, and custom color tags.
- **Account Transfers**: Record seamless internal fund transfers between Cash on Hand and Bank Accounts, or between separate Bank Accounts.
- **Live Account Reconciliation**: Balances update dynamically based on recorded income, expenses, and pending commitments.

### 3. 🛍️ BNPL & Reserved Commitments (Tabby / Tamara / Bills)
- **Installment Tracking**: Manage Buy-Now-Pay-Later (BNPL) plans like Tabby and Tamara with automated installment schedules (e.g., installment 2 of 4).
- **Auto-Debit Reminders**: Alerts when an upcoming installment is due or when a linked bank account has insufficient balance to cover the payment.
- **Committed Budget Integration**: Pending reserved payments are factored into your monthly budget calculations to prevent overspending.

### 4. 💵 Cash Denomination Counter & Calculator
- **Physical Cash Tracker**: Count banknotes and coins (500, 200, 100, 50, 20, 10, 5, 2, 1) with instant total and piece calculations.
- **Custom Denominations**: Add custom bill or coin values tailored to your currency.
- **One-Tap Quick Actions**: Copy breakdown summary to clipboard or reset counts.

### 5. 🔄 Local Peer-to-Peer Device Sync (No Cloud Required)
- **Direct LAN & Hotspot Sync**: Sync financial records between two or more Android devices on the same Wi-Fi network or mobile hotspot.
- **Duplicate-Proof Two-Way Merge**: Unique persistent UUIDs ensure conflict-free syncing with zero duplicate records.
- **Incremental Fast Sync**: Tracks per-device last synced transaction timestamps for fast delta synchronization.
- **Device Origin Tags**: Every transaction displays which device originally created it (e.g., `Pixel 8`, `Galaxy S24`).
- **Smart Denomination Reconciliation**:
  - Automatically resolves cash breakdowns by matching the unified merged cash-in-hand total.
  - Prioritizes exact matches, closest accuracy, and most recent active device timestamps.

### 6. 📈 Financial Analytics & Visual Reports
- **Category Breakdown**: Interactive visual progress bars and percentage distribution for spending and income.
- **Daily Spend Trends**: Visual spend points tracking daily velocity and peak spending days.
- **Key Metrics**: Savings rate percentage, net balance, average daily spend, and highest expense day.

### 7. 🎯 Budget Planning & Safe Daily Spend
- **Monthly Limit & Thresholds**: Set a monthly spending ceiling with configurable alert thresholds (e.g., 80% warning).
- **Safe Daily Spend Indicator**: Calculates how much you can safely spend per remaining day of the month.
- **Visual Alert Banners**: Color-coded badges indicate whether your budget is Safe, Approaching Limit, or Exceeded.

### 8. 📁 Backup, Restore & CSV Export
- **Full JSON Backup**: Export and import complete portable backups including transactions, budgets, bank accounts, reserved payments, and denomination counts.
- **CSV Export**: Export formatted transaction history for analysis in Microsoft Excel, Google Sheets, or Numbers.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3).
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Repository Pattern.
- **Database**: [Room](https://developer.android.com/training/data-storage/room) for robust on-device SQLite persistence via Kotlin Symbol Processing (KSP).
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow` for reactive, lifecycle-aware state updates.
- **Networking**: Embedded lightweight local HTTP/TCP socket server and client for offline LAN sync.
- **Language**: 100% Kotlin.

---

## 📱 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 34 (Android 14) or higher
- JDK 17

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/expense-tracker.git
   cd expense-tracker
   ```
2. Open the project in **Android Studio**.
3. Allow Gradle to sync dependencies.
4. Select your target device or emulator and click **Run** (`Shift + F10`).

---

## 🔒 Privacy & Security

- **100% Offline-First**: Your financial data is stored exclusively on your device.
- **No Third-Party Tracking**: No telemetry, analytics trackers, or external cloud databases.
- **Direct P2P Sync**: Data transfer happens strictly between devices on your local private network.

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
