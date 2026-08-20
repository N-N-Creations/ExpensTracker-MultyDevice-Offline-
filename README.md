# 💰 Expense Tracker

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform: Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Language: Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="UI: Jetpack Compose" />
  <img src="https://img.shields.io/badge/Database-Room_SQLite-FFA000?style=for-the-badge&logo=sqlite&logoColor=white" alt="Database: Room SQLite" />
  <img src="https://img.shields.io/badge/Architecture-MVVM_Clean-00C853?style=for-the-badge" alt="Architecture: MVVM" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" alt="License: MIT" />
</p>

<p align="center">
  <b>A private, offline-first personal finance and cash management app for Android.</b><br>
  Track daily income & expenses, balance bank accounts and physical cash, manage Tabby/Tamara BNPL installments, count banknotes with physical cash denomination sheets, and synchronize multiple devices over local Wi-Fi/Hotspot without any cloud servers.
</p>

---

## 📑 Table of Contents

- [✨ Key Features](#-key-features)
  - [1. 📊 Expense & Income Tracking](#1--expense--income-tracking)
  - [2. 🏦 Bank Accounts & Cash Management](#2--bank-accounts--cash-management)
  - [3. 🛍️ BNPL & Reserved Commitments](#3-️-bnpl--reserved-commitments-tabby--tamara--bills)
  - [4. 💵 Physical Cash Denomination Counter](#4--physical-cash-denomination-counter)
  - [5. 🔄 Local Peer-to-Peer Device Sync & Rotating Security PIN](#5--local-peer-to-peer-device-sync--rotating-security-pin)
  - [6. 📈 Financial Analytics & Visual Reports](#6--financial-analytics--visual-reports)
  - [7. 🎯 Budget Planning & Safe Daily Spend](#7--budget-planning--safe-daily-spend)
  - [8. 💱 Multi-Currency & Data Portability](#8--multi-currency--data-portability)
  - [9. 🛡️ Danger Zone & GitHub-Style Data Wipe Verification](#9-️-danger-zone--github-style-data-wipe-verification)
- [🛠️ Architecture & Tech Stack](#️-architecture--tech-stack)
- [⚙️ Technical Specifications](#️-technical-specifications)
- [📱 Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Build and Run](#build-and-run)
- [🔒 Privacy & Security Manifesto](#-privacy--security-manifesto)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

---

## ✨ Key Features

### 1. 📊 Expense & Income Tracking
- **Rapid Transaction Entry**: Log transactions with amounts, categories, customizable timestamps, payment methods (Cash, Card, Bank Transfer, BNPL), and notes.
- **Dynamic Time Filters**: Switch between **Today**, **Yesterday**, **This Week**, **This Month**, **This Year**, or define a **Custom Date Range**.
- **Instant Search & Category Pills**: Find records by keyword, notes, category, or amount with live result counters.

### 2. 🏦 Bank Accounts & Cash Management
- **Multiple Bank Profiles**: Track separate bank accounts with custom color badges, masked account numbers, and starting balances.
- **Internal Fund Transfers**: Move funds between *Cash on Hand* and *Bank Accounts*, or perform inter-bank transfers with automatic dual-entry ledger logging.
- **Live Reconciliation**: Balances update in real time based on transactions, transfers, and committed reserve payments.

### 3. 🛍️ BNPL & Reserved Commitments (Tabby / Tamara / Bills)
- **Installment Tracking**: Organize Buy-Now-Pay-Later payment plans with automated installment schedules (e.g., installment 2 of 4).
- **Auto-Debit Reminders & Low-Balance Warnings**: Visual indicators alert you before a payment is due and warn if the linked account has insufficient funds.
- **Committed Budget Protection**: Pending commitments are automatically deducted from your available disposable budget to avoid overspending.

### 4. 💵 Physical Cash Denomination Counter
- **Currency Notes & Coins Breakdown**: Dedicated counter for physical cash (500, 200, 100, 50, 20, 10, 5, 2, 1) with real-time piece and value totals.
- **Custom Denominations**: Add or customize banknotes and coins to match any regional currency.
- **One-Tap Clipboard Copy**: Share or save formatted denomination summaries instantly.

### 5. 🔄 Local Peer-to-Peer Device Sync & Rotating Security PIN
Sync your ledger across your phone, tablet, and family devices seamlessly over your private network:

```
┌─────────────────┐       Local Wi-Fi / Hotspot       ┌─────────────────┐
│ Device A (Host) │ ◄───────────────────────────────► │ Device B (Peer) │
│ Embedded Server │     🔒 30s Rotating PIN Auth      │ Socket Client   │
│ & 6-Digit PIN   │     Encrypted Local Payload       │ (Enter PIN)     │
└─────────────────┘                                   └─────────────────┘
```

<details>
<summary><b>Click to expand P2P Sync & PIN Security Mechanics</b></summary>

- **Zero Cloud Dependency**: Operates entirely over local Wi-Fi or mobile personal hotspot via direct socket communication.
- **🔒 30-Second Rotating Security PIN**: Connecting devices must supply the active 6-digit PIN displayed on the host screen. The PIN automatically rotates every 30 seconds to protect unattended sessions and prevent unauthorized access on public/shared Wi-Fi.
- **Live Countdown & Progress Bar**: Real-time visual progress indicator and seconds countdown (`30s` down to `1s`) with instant manual PIN refresh and copy controls.
- **Conflict-Free Two-Way Merge**: Every transaction is assigned an immutable UUID, ensuring duplicate-free reconciliation.
- **Incremental Fast Delta Sync**: High-speed syncing based on per-device transaction timestamps.
- **Device Origin Attribution**: Every synced record identifies its originating device (e.g., `Pixel 8`, `Galaxy S24`).
- **3-Tier Smart Denomination Reconciliation**:
  1. *Exact Ledger Match*: Adopts the physical cash breakdown matching the new merged cash-in-hand total.
  2. *Closest Accuracy*: Selects the configuration with the smallest variance against the combined ledger.
  3. *Timestamp Priority*: Retains the most recently updated count while preserving custom peer denominations.
</details>

### 6. 📈 Financial Analytics & Visual Reports
- **Category Spending Breakdown**: Visual distribution bars showing where your money goes.
- **Daily Spend Trend Charts**: Track spending velocity and identify peak expense days across the month.
- **Key Financial Metrics**: Real-time savings rate percentage, net balance, average daily expenditure, and monthly projections.

### 7. 🎯 Budget Planning & Safe Daily Spend
- **Monthly Spending Limits**: Set a monthly budget ceiling with configurable alert thresholds (e.g., 80% warning).
- **Safe Daily Spend Gauge**: Dynamically calculates your safe daily allowance based on remaining funds and days left in the month.
- **Status Badges**: Clear color-coded alerts (**Safe**, **Approaching Limit**, **Exceeded**).

### 8. 💱 Multi-Currency & Data Portability
- **Currency Manager**: Set primary currency symbols and manage custom exchange rates.
- **Full JSON Backup & Restore**: Backup and restore your complete database offline with a single tap.
- **CSV Data Export**: Export clean, standardized CSV files formatted for Microsoft Excel, Google Sheets, or Apple Numbers.

### 9. 🛡️ Danger Zone & GitHub-Style Data Wipe Verification
- **Zero Dummy Data Guarantee**: Fresh installations launch entirely clean without mock transactions or pre-filled records.
- **Type-To-Confirm Challenge Verification**: High-risk wipe operations generate dynamic alphanumeric confirmation codes (e.g. `DELETE-9B2K7`, `PURGE-3F8M1`, `RESET-8X2Q9`) requiring exact, case-sensitive keyboard input before execution.
- **Granular Reset Capabilities**: Selectively wipe specific tables (Transactions, Bank Accounts, BNPL / Reserved Commitments, Budgets, Cash Denominations) or trigger a comprehensive Factory Reset.

---

## 🛠️ Architecture & Tech Stack

```
app/
 ├── data/
 │    ├── local/         # Room Database, DAOs, Entity definitions
 │    ├── model/         # Domain models (Transaction, BankAccount, Currency, etc.)
 │    └── repository/    # ExpenseRepository (Single Source of Truth)
 ├── ui/
 │    ├── components/    # Reusable Compose UI components & design system
 │    ├── screens/       # Main screens (Home, Analytics, Budget, Sync, Dialogs)
 │    ├── theme/         # Material 3 Color Schemes, Typography, Shapes
 │    └── viewmodel/     # State management with Kotlin StateFlow & Coroutines
 └── utils/              # Export helpers, formatters, and network utilities
```

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0+ (100% Coroutines & Flow) |
| **UI Framework** | Jetpack Compose with Material Design 3 (M3) |
| **State Management** | Android ViewModel + `StateFlow` + `collectAsStateWithLifecycle` |
| **Local Persistence** | Room Database with SQLite & KSP (Kotlin Symbol Processing) |
| **P2P Networking** | Embedded Local HTTP/Socket Server & OkHttp Client |
| **System Integration** | Android Predictive Back Handler (`WindowInsets.isImeVisible`) |

---

## ⚙️ Technical Specifications

| Specification | Requirement / Detail |
| :--- | :--- |
| **Minimum SDK** | Android 8.0 (API Level 26) |
| **Target SDK** | Android 14 / 15 (API Level 34 / 35) |
| **JDK Version** | OpenJDK 17 |
| **Build System** | Gradle (Kotlin DSL - `.gradle.kts`) |
| **Design System** | Material Design 3 Dynamic Theming (Light & Dark Mode) |

---

## 📱 Getting Started

### Prerequisites
- [Android Studio Ladybug (2024.2.1+)](https://developer.android.com/studio) or newer
- Android SDK 34+
- Java Development Kit (JDK) 17

### Build and Run
1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/expense-tracker.git
   cd expense-tracker
   ```

2. **Open in Android Studio:**
   - Select **Open an Existing Project** and navigate to the cloned folder.

3. **Build the APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on Device or Emulator:**
   - Select your connected device and press **`Shift + F10`** (or click the green **Run** button).

---

## 🔒 Privacy & Security Manifesto

> [!IMPORTANT]
> **Your financial data is private and belongs only to you.**
>
> - 🛡️ **Zero Cloud Storage**: No remote database servers, accounts, or cloud logins.
> - 🚫 **Zero Telemetry & Ads**: No analytics SDKs, trackers, or advertising frameworks.
> - 🔐 **Local Network Sync**: Direct peer-to-peer communication that never leaves your local Wi-Fi or hotspot.
> - 🔑 **30-Second Expiring PIN**: Zero unauthorized access on local networks through auto-rotating security keys.
> - 🛡️ **Accidental Wipe Protection**: GitHub-style type-to-confirm challenge codes preventing unintended data deletion.

---

## 🤝 Contributing

Contributions are welcome! If you'd like to improve the app:
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'feat: add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
