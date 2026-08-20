# 🚀 Release Notes

## Version 2.2.0 (Latest) — 🔒 Enhanced Security, Rotating P2P PIN & Safe Data Management

### 🌟 What's New & Highlights

#### 1. 🔑 30-Second Rotating Security Sync PIN (Two-Way P2P Sync)
- **Peer-to-Peer 2-Way Security Protection**: Syncing between Device A and Device B now requires entering a randomly generated **6-Digit Security PIN** (`X-Sync-Pin`) displayed on the host machine before any data can be read or merged.
- **30-Second Auto-Expiring Rotation**: The security PIN automatically rotates every 30 seconds to safeguard unattended sync sessions and prevent unauthorized connections on shared local Wi-Fi networks.
- **Live Visual Countdown & Progress Indicator**: Displays an animated M3 progress indicator and real-time seconds badge (`30s` down to `1s` with a red warning in the final 5 seconds), alongside a manual PIN regeneration tool.
- **Unauthorized Request Blocking**: Sync requests without a valid or matching active PIN are immediately rejected with an HTTP 401 Unauthorized response.

#### 2. 🛡️ GitHub-Style Data Wipe Verification (Danger Zone)
- **Zero Dummy Data Initialization**: New app installations start completely clean with zero pre-seeded mock transactions, accounts, or commitments.
- **Type-To-Confirm Challenge Verification**: Data wipe and reset operations now generate dynamic security tokens (e.g. `DELETE-9B2K7`, `PURGE-3F8M1`, `RESET-8X2Q9`) that require manual, case-sensitive character entry to confirm.
- **Granular Data Removal**: Checkboxes allow you to selectively wipe individual components (Transactions, Bank Accounts, BNPL / Reserved Obligations, Monthly Budgets, Cash Denominations) or execute a complete Factory Reset.

---

## Version 2.1.0 — 🔄 Local Peer-to-Peer Sync Engine & Reconciliation
- **Local Network Sync**: Direct socket communication over Wi-Fi / Hotspot without any cloud servers or third-party databases.
- **Fast Delta Syncing**: High-speed incremental merging using per-device timestamp watermarks.
- **Smart Denomination Reconciliation**: 3-tier intelligent reconciliation algorithm for physical cash counts across devices.
- **Detailed Sync Logs**: Real-time diagnostic logging panel tracking payload transfers and handshake events.

---

## Version 2.0.0 — 💵 Cash Denomination Counter & BNPL Management
- **Banknote & Coin Counter**: Denomination counter supporting 500, 200, 100, 50, 20, 10, 5, 2, and 1 bills with custom denomination addition.
- **Buy-Now-Pay-Later (Tabby & Tamara)**: Installment schedule tracking, remaining balances, and auto-debit reminders.
- **Safe Daily Spend Gauge**: Dynamic calculation of daily spending limits based on budget ceilings and days remaining.

---

## Version 1.0.0 — Initial Release
- Core income and expense logging with category breakdowns.
- Multiple bank account profiles and internal fund transfers.
- Offline-first Room SQLite architecture.
- JSON backup & CSV spreadsheet export.
