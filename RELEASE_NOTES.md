# 🚀 Release Notes — Version 1.0.0

### 🔒 Complete Financial Ledger with Local P2P Sync & 30s Rotating PIN Security

Welcome to **Version 1.0.0** of Expense Tracker! This release delivers an all-in-one, 100% offline-first financial ledger featuring direct local peer-to-peer sync, dynamic PIN security, cash denomination counting, and BNPL commitment tracking.

---

### 🌟 Key Features & Capabilities

#### 1. 🔄 Local Peer-to-Peer Sync with 30s Rotating Security PIN
- **Zero-Cloud P2P Sync**: Synchronize records directly between your devices over local Wi-Fi or mobile personal hotspot without any cloud servers or third-party databases.
- **30-Second Auto-Expiring Security PIN**: The host device generates a random 6-digit PIN that automatically rotates every 30 seconds to safeguard unattended sync sessions and prevent unauthorized access on shared networks.
- **Live Visual Countdown & Progress Bar**: Real-time visual progress bar and seconds counter (`30s` down to `1s` with a red warning state), with manual PIN refresh and copy tools.
- **Unauthorized Connection Blocking**: Connection requests without a valid or matching active PIN are immediately rejected with HTTP 401 Unauthorized.
- **Conflict-Free Two-Way Merge**: High-speed incremental merging using per-device timestamp watermarks and smart physical cash reconciliation.

#### 2. 🛡️ Danger Zone & GitHub-Style Data Wipe Verification
- **Zero Dummy Data Initialization**: Fresh app installations start 100% clean with zero pre-seeded dummy records.
- **Type-To-Confirm Challenge**: Reset operations generate dynamic alphanumeric security tokens (e.g. `DELETE-9B2K7`, `PURGE-3F8M1`, `RESET-8X2Q9`) requiring exact case-sensitive keyboard confirmation.
- **Granular Reset**: Selectively wipe specific tables (Transactions, Bank Accounts, BNPL Commitments, Budgets, Cash Denominations) or perform a complete Factory Reset.

#### 3. 🏦 Bank Accounts & Physical Cash Denomination Counter
- **Multi-Account Management**: Track checking, savings, and physical cash wallets with balance adjustments and internal account transfers.
- **Cash Denomination Counter**: Count physical banknotes and coins (500, 200, 100, 50, 20, 10, 5, 2, 1) with custom denomination support and one-tap clipboard copying.

#### 4. 🛍️ BNPL & Reserved Commitments
- **Installment Tracking**: Manage Tabby, Tamara, credit commitments, and recurring bills with remaining balance calculations and due date alerts.

#### 5. 🎯 Budget Planning & Financial Analytics
- **Safe Daily Spend Gauge**: Dynamic calculation of allowable daily expenditure based on monthly budget ceilings.
- **Visual Breakdown**: Interactive category charts, monthly trend comparisons, and financial health summaries.

#### 6. 💱 Multi-Currency & Data Portability
- **Global Currencies**: Full support for SAR, AED, USD, EUR, GBP, KWD, BHD, OMR, QAR, EGP, and custom symbols.
- **JSON & CSV Export**: One-tap full database backup/restore and clean CSV spreadsheet exports.

---

### 📦 APK Build Information
- **Version**: `1.0.0`
- **Version Code**: `1`
- **Target OS**: Android 8.0+ (API level 26+)
- **Architecture**: 100% Offline-First (Room SQLite & Local Socket Sync)
