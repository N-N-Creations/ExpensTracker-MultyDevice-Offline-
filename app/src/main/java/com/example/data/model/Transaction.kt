package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class AccountSourceType(val displayName: String) {
    CASH("Cash"),
    BANK("Bank Account")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    BANK_TRANSFER("Bank Transfer"),
    DEBIT_CARD("Debit Card"),
    CREDIT_CARD("Credit Card"),
    DIGITAL_WALLET("UPI / Digital Wallet"),
    BNPL_TABBY_TAMARA("Tabby / Tamara / BNPL"),
    OTHER("Other")
}

enum class ReservedPaymentStatus {
    PENDING,
    PAID,
    CANCELLED
}

enum class RecurringFrequency(val displayName: String) {
    ONCE("One-time (Due on date)"),
    WEEKLY("Weekly"),
    BI_WEEKLY("Every 2 Weeks"),
    MONTHLY("Monthly (e.g. Tabby/Tamara Installment)")
}

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val bankName: String,
    val accountNumberMasked: String = "",
    val initialBalance: Double = 0.0,
    val colorHex: Long = 0xFF1976D2,
    val iconName: String = "account_balance",
    val isArchived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reserved_payments")
data class ReservedPayment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val category: String = "Shopping",
    val accountSourceType: AccountSourceType = AccountSourceType.BANK,
    val bankAccountId: String? = null,
    val bankAccountName: String? = null,
    val frequency: RecurringFrequency = RecurringFrequency.ONCE,
    val totalInstallments: Int = 1, // Total times to repeat: e.g. 4 for Tabby, 3 for Tamara, 12 for EMI, 0 for ongoing
    val currentInstallment: Int = 1, // Current installment number e.g. 1 of 4
    val specificDayOfMonth: Int = 0, // e.g. 5th of month (0 means use dueDate's day)
    val parentRecurringId: String? = null,
    val reminderDaysBefore: Int = 3, // Alert reminder N days before
    val status: ReservedPaymentStatus = ReservedPaymentStatus.PENDING,
    val paidTransactionId: String? = null,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = PaymentMethod.CASH.displayName,
    val accountSourceType: AccountSourceType = AccountSourceType.CASH,
    val bankAccountId: String? = null,
    val bankAccountName: String? = null,
    val linkedReservedPaymentId: String? = null,
    val isTransfer: Boolean = false,
    val transferCounterpartId: String? = null,
    val deviceId: String = "",
    val deviceName: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "budgets")
data class MonthlyBudget(
    @PrimaryKey val monthYear: String, // Format: "YYYY-MM", e.g. "2026-08"
    val totalLimit: Double,
    val alertThresholdPercent: Int = 80, // Default alert at 80%
    val updatedAt: Long = System.currentTimeMillis()
)

data class CategoryItem(
    val id: String,
    val name: String,
    val type: TransactionType,
    val colorHex: Long,
    val iconName: String
)

object CategoryConstants {
    val expenseCategories = listOf(
        CategoryItem("food", "Food & Dining", TransactionType.EXPENSE, 0xFFFF7043, "restaurant"),
        CategoryItem("groceries", "Groceries", TransactionType.EXPENSE, 0xFF4CAF50, "shopping_cart"),
        CategoryItem("bnpl", "Tabby / Tamara (BNPL)", TransactionType.EXPENSE, 0xFF7C4DFF, "credit_score"),
        CategoryItem("transport", "Transportation", TransactionType.EXPENSE, 0xFF29B6F6, "directions_car"),
        CategoryItem("shopping", "Shopping", TransactionType.EXPENSE, 0xFFAB47BC, "shopping_bag"),
        CategoryItem("bills", "Bills & Utilities", TransactionType.EXPENSE, 0xFFEF5350, "receipt_long"),
        CategoryItem("housing", "Housing & Rent", TransactionType.EXPENSE, 0xFF7E57C2, "home"),
        CategoryItem("entertainment", "Entertainment", TransactionType.EXPENSE, 0xFFFFA726, "movie"),
        CategoryItem("health", "Health & Medical", TransactionType.EXPENSE, 0xFF26A69A, "medical_services"),
        CategoryItem("education", "Education", TransactionType.EXPENSE, 0xFF5C6BC0, "school"),
        CategoryItem("personal", "Personal Care", TransactionType.EXPENSE, 0xFFEC407A, "spa"),
        CategoryItem("travel", "Travel", TransactionType.EXPENSE, 0xFF26C6DA, "flight"),
        CategoryItem("other_expense", "Other Expense", TransactionType.EXPENSE, 0xFF78909C, "more_horiz")
    )

    val incomeCategories = listOf(
        CategoryItem("salary", "Salary", TransactionType.INCOME, 0xFF43A047, "payments"),
        CategoryItem("freelance", "Freelance / Gig", TransactionType.INCOME, 0xFF00ACC1, "laptop_mac"),
        CategoryItem("business", "Business", TransactionType.INCOME, 0xFF3949AB, "storefront"),
        CategoryItem("investment", "Investments & Dividends", TransactionType.INCOME, 0xFF00897B, "trending_up"),
        CategoryItem("bonus", "Bonus & Awards", TransactionType.INCOME, 0xFFFDD835, "card_giftcard"),
        CategoryItem("allowance", "Allowance & Gifts", TransactionType.INCOME, 0xFFD81B60, "redeem"),
        CategoryItem("rental", "Rental Income", TransactionType.INCOME, 0xFF8E24AA, "real_estate_agent"),
        CategoryItem("refund", "Refund / Cashback", TransactionType.INCOME, 0xFF1E88E5, "currency_exchange"),
        CategoryItem("other_income", "Other Income", TransactionType.INCOME, 0xFF546E7A, "account_balance_wallet")
    )

    fun getCategoryItem(name: String, type: TransactionType): CategoryItem {
        val list = if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories
        return list.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: (if (type == TransactionType.EXPENSE) expenseCategories.last() else incomeCategories.last())
    }
}

data class PeriodSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val totalReservedCommitted: Double = 0.0,
    val netBalance: Double,
    val savingsRate: Double,
    val averageDailyExpense: Double,
    val transactionCount: Int,
    val highestExpenseDay: String = "",
    val highestExpenseAmount: Double = 0.0
)

data class BankAccountBalance(
    val account: BankAccount,
    val currentBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val pendingReservedAmount: Double
)

data class CashBalance(
    val initialCash: Double = 0.0,
    val totalCashIncome: Double = 0.0,
    val totalCashExpense: Double = 0.0,
    val currentCashOnHand: Double = 0.0
)

data class DeviceSyncRecord(
    val deviceId: String,
    val deviceName: String,
    val lastSyncTimestamp: Long,
    val lastRecordCount: Int = 0
)

data class DeviceDeltaRecord(
    val delta: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DenominationItem(
    val value: Double,
    val count: Int = 0,
    val isCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val baseCount: Int = 0,
    val deviceDeltas: Map<String, DeviceDeltaRecord> = emptyMap()
) {
    val subtotal: Double get() = value * count
}

data class SyncSnapshot(
    val version: Int = 2,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val deviceName: String = "",
    val sinceTimestamp: Long = 0L,
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<MonthlyBudget> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val reservedPayments: List<ReservedPayment> = emptyList(),
    val denominations: List<DenominationItem> = emptyList(),
    val activeCurrencyCode: String? = null
)
