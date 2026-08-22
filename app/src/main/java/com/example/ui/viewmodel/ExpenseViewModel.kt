package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AccountSourceType
import com.example.data.model.BankAccount
import com.example.data.model.BankAccountBalance
import com.example.data.model.CashBalance
import com.example.data.model.CategoryConstants
import com.example.data.model.CurrencyItem
import com.example.data.model.DefaultCurrencies
import com.example.data.model.DenominationItem
import com.example.data.model.DeviceSyncRecord
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.PeriodSummary
import com.example.data.model.RecurringFrequency
import com.example.data.model.ReservedPayment
import com.example.data.model.ReservedPaymentStatus
import com.example.data.model.SyncSnapshot
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.CurrencyService
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.RateFetchResult
import com.example.sync.ClientSyncState
import com.example.sync.DeviceIdentityService
import com.example.sync.LocalNetworkSyncClient
import com.example.sync.LocalNetworkSyncServer
import com.example.sync.ServerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimePeriodFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Period")
}

enum class BudgetAlertStatus {
    NO_BUDGET,
    SAFE,
    WARNING_APPROACHING, // >= 80% (including reserved upcoming payments)
    EXCEEDED // > 100%
}

data class CategorySpend(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val colorHex: Long,
    val count: Int,
    val type: TransactionType
)

data class DailySpendPoint(
    val dateLabel: String,
    val dayOfMonth: Int,
    val expense: Double,
    val income: Double
)

data class FilterParams(
    val period: TimePeriodFilter,
    val customStart: Long,
    val customEnd: Long,
    val query: String,
    val typeFilter: TransactionType?,
    val accountSource: AccountSourceType?,
    val bankAccountId: String?
)

data class ReservedPaymentReminderAlert(
    val payment: ReservedPayment,
    val daysUntilDue: Long,
    val isDueSoon: Boolean, // within reminderDaysBefore
    val isOverdue: Boolean,
    val targetBankCurrentBalance: Double?,
    val isBankBalanceLow: Boolean
)

data class MonthlyBudgetCalculation(
    val monthYear: String,
    val budgetLimit: Double,
    val spentAmount: Double,
    val reservedPendingAmount: Double,
    val totalCommittedAmount: Double,
    val remainingAmount: Double,
    val percentage: Float,
    val alertThresholdPercent: Int,
    val alertStatus: BudgetAlertStatus,
    val daysRemainingInMonth: Int,
    val safeDailySpend: Double
)

data class DenominationTrackerState(
    val items: List<DenominationItem>,
    val totalCountedCash: Double,
    val totalPieces: Int
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ExpenseRepository(
        db.transactionDao(),
        db.budgetDao(),
        db.bankAccountDao(),
        db.reservedPaymentDao(),
        application
    )
    val deviceIdentityService = DeviceIdentityService(application)
    val deviceName: StateFlow<String> = deviceIdentityService.deviceName
    val deviceId: StateFlow<String> = deviceIdentityService.deviceId

    fun setDeviceName(newName: String) {
        deviceIdentityService.setDeviceName(newName)
    }

    val syncServer = LocalNetworkSyncServer(repository, deviceIdentityService, application)
    val syncClient = LocalNetworkSyncClient(repository, deviceIdentityService)

    // Currency & Multi-Currency Management (Default: SAR)
    private val currencyService = CurrencyService(application)

    private val _activeCurrencyCode = MutableStateFlow(currencyService.getActiveCurrencyCode())
    val activeCurrencyCode = _activeCurrencyCode.asStateFlow()

    private val _currencySymbol = MutableStateFlow(currencyService.getActiveCurrencySymbol())
    val currencySymbol = _currencySymbol.asStateFlow()

    private val _currencies = MutableStateFlow<List<CurrencyItem>>(currencyService.getCurrencies())
    val currencies = _currencies.asStateFlow()

    private val _ratesLastUpdated = MutableStateFlow(currencyService.getLastUpdatedTimestamp())
    val ratesLastUpdated = _ratesLastUpdated.asStateFlow()

    private val _isFetchingRates = MutableStateFlow(false)
    val isFetchingRates = _isFetchingRates.asStateFlow()

    private val _rateFetchMessage = MutableStateFlow<String?>(null)
    val rateFetchMessage = _rateFetchMessage.asStateFlow()

    // Time Period Filter State
    private val _selectedPeriod = MutableStateFlow(TimePeriodFilter.THIS_MONTH)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _customStartTime = MutableStateFlow(0L)
    val customStartTime = _customStartTime.asStateFlow()

    private val _customEndTime = MutableStateFlow(System.currentTimeMillis())
    val customEndTime = _customEndTime.asStateFlow()

    // Search & Type Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType = _filterType.asStateFlow()

    private val _filterCategory = MutableStateFlow<String?>(null)
    val filterCategory = _filterCategory.asStateFlow()

    private val _filterAccountSource = MutableStateFlow<AccountSourceType?>(null)
    val filterAccountSource = _filterAccountSource.asStateFlow()

    private val _filterBankAccountId = MutableStateFlow<String?>(null)
    val filterBankAccountId = _filterBankAccountId.asStateFlow()

    // All active transactions
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All budgets
    val allBudgets: StateFlow<List<MonthlyBudget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Bank Accounts
    val allBankAccounts: StateFlow<List<BankAccount>> = repository.allBankAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Reserved Payments
    val allReservedPayments: StateFlow<List<ReservedPayment>> = repository.allReservedPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReservedPayments: StateFlow<List<ReservedPayment>> = repository.pendingReservedPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Month Budget
    val currentMonthYear: String
        get() = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    val currentMonthBudget: StateFlow<MonthlyBudget?> = repository.getBudgetForMonth(currentMonthYear)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Combined Filter State
    private val filterCriteria = combine(
        combine(selectedPeriod, customStartTime, customEndTime) { p, s, e -> Triple(p, s, e) },
        combine(searchQuery, filterType) { q, t -> Pair(q, t) },
        combine(filterAccountSource, filterBankAccountId) { a, b -> Pair(a, b) }
    ) { (period, customStart, customEnd), (query, typeFilter), (accSource, bankId) ->
        FilterParams(period, customStart, customEnd, query, typeFilter, accSource, bankId)
    }

    // Filtered Transactions Flow
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        filterCriteria,
        filterCategory
    ) { txs, params, catFilter ->
        val (startTime, endTime) = getTimeBounds(params.period, params.customStart, params.customEnd)

        txs.filter { tx ->
            val matchesTime = if (params.period == TimePeriodFilter.ALL_TIME) true else tx.timestamp in startTime..endTime
            val matchesType = params.typeFilter == null || tx.type == params.typeFilter
            val matchesCat = catFilter == null || tx.category.equals(catFilter, ignoreCase = true)
            val matchesAccount = params.accountSource == null || tx.accountSourceType == params.accountSource
            val matchesBank = params.bankAccountId == null || tx.bankAccountId == params.bankAccountId
            val matchesQuery = params.query.isBlank() ||
                    tx.category.contains(params.query, ignoreCase = true) ||
                    tx.note.contains(params.query, ignoreCase = true) ||
                    tx.amount.toString().contains(params.query) ||
                    tx.paymentMethod.contains(params.query, ignoreCase = true) ||
                    (tx.bankAccountName ?: "").contains(params.query, ignoreCase = true)
            matchesTime && matchesType && matchesCat && matchesAccount && matchesBank && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Period Summary Statistics (factoring in pending reserved payments due in this period)
    val currentPeriodSummary: StateFlow<PeriodSummary> = combine(
        filteredTransactions,
        pendingReservedPayments,
        selectedPeriod,
        customStartTime,
        customEndTime
    ) { txs, reservedList, period, customStart, customEnd ->
        val (startTime, endTime) = getTimeBounds(period, customStart, customEnd)

        var totalIncome = 0.0
        var totalExpense = 0.0
        val dailyExpenseMap = mutableMapOf<String, Double>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (tx in txs) {
            if (!tx.isTransfer && !tx.category.equals("Transfer", ignoreCase = true)) {
                if (tx.type == TransactionType.INCOME) {
                    totalIncome += tx.amount
                } else {
                    totalExpense += tx.amount
                    val dayKey = dateFormat.format(Date(tx.timestamp))
                    dailyExpenseMap[dayKey] = (dailyExpenseMap[dayKey] ?: 0.0) + tx.amount
                }
            }
        }

        // Pending reserved payments due in this period
        val reservedInPeriod = reservedList.filter { it.dueDate in startTime..endTime }
        val totalReservedCommitted = reservedInPeriod.sumOf { it.amount }

        val netBalance = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome) * 100.0 else 0.0
        val daysCount = Math.max(1, ((endTime - startTime) / (1000 * 60 * 60 * 24)).toInt() + 1)
        val avgDailyExpense = totalExpense / daysCount

        var highestDay = ""
        var highestAmount = 0.0
        for ((day, amount) in dailyExpenseMap) {
            if (amount > highestAmount) {
                highestAmount = amount
                highestDay = day
            }
        }

        PeriodSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalReservedCommitted = totalReservedCommitted,
            netBalance = netBalance,
            savingsRate = savingsRate,
            averageDailyExpense = avgDailyExpense,
            transactionCount = txs.size,
            highestExpenseDay = highestDay,
            highestExpenseAmount = highestAmount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PeriodSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)
    )

    // Bank Account Balances Flow (Dynamic calculation from initial balance + income - expense - pending reserved)
    val bankAccountBalances: StateFlow<List<BankAccountBalance>> = combine(
        allBankAccounts,
        allTransactions,
        pendingReservedPayments
    ) { banks, txs, reservedList ->
        banks.map { bank ->
            val bankTxs = txs.filter { it.bankAccountId == bank.id }
            val totalIncome = bankTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = bankTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val pendingReserved = reservedList.filter { it.bankAccountId == bank.id }.sumOf { it.amount }
            val currentBalance = bank.initialBalance + totalIncome - totalExpense

            BankAccountBalance(
                account = bank,
                currentBalance = currentBalance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                pendingReservedAmount = pendingReserved
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cash Balance Flow
    val cashBalance: StateFlow<CashBalance> = combine(
        allTransactions,
        pendingReservedPayments
    ) { txs, reservedList ->
        val cashTxs = txs.filter { it.accountSourceType == AccountSourceType.CASH }
        val cashIncome = cashTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val cashExpense = cashTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val currentCash = cashIncome - cashExpense

        CashBalance(
            initialCash = 0.0,
            totalCashIncome = cashIncome,
            totalCashExpense = cashExpense,
            currentCashOnHand = currentCash
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashBalance())

    // Reserved Payment Reminders & Low Bank Balance Warnings Flow
    val reservedReminders: StateFlow<List<ReservedPaymentReminderAlert>> = combine(
        pendingReservedPayments,
        bankAccountBalances
    ) { pendingList, balances ->
        val now = System.currentTimeMillis()
        val bankMap = balances.associateBy { it.account.id }

        pendingList.map { payment ->
            val diffMillis = payment.dueDate - now
            val daysUntilDue = (diffMillis / (1000 * 60 * 60 * 24))
            val isOverdue = diffMillis < 0
            val isDueSoon = !isOverdue && daysUntilDue <= payment.reminderDaysBefore

            val targetBankBalance = payment.bankAccountId?.let { bankMap[it]?.currentBalance }
            val isBankBalanceLow = targetBankBalance != null && targetBankBalance < payment.amount

            ReservedPaymentReminderAlert(
                payment = payment,
                daysUntilDue = daysUntilDue,
                isDueSoon = isDueSoon,
                isOverdue = isOverdue,
                targetBankCurrentBalance = targetBankBalance,
                isBankBalanceLow = isBankBalanceLow
            )
        }.sortedBy { it.payment.dueDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Spends Breakdown for Visual Charts
    val categoryBreakdown: StateFlow<List<CategorySpend>> = combine(
        filteredTransactions,
        filterType
    ) { txs, typeFilter ->
        val effectiveType = typeFilter ?: TransactionType.EXPENSE
        val filtered = txs.filter { !it.isTransfer && !it.category.equals("Transfer", ignoreCase = true) && it.type == effectiveType }
        val total = filtered.sumOf { it.amount }

        if (total <= 0.0) return@combine emptyList()

        val grouped = filtered.groupBy { it.category }
        grouped.map { (catName, items) ->
            val sum = items.sumOf { it.amount }
            val pct = ((sum / total) * 100).toFloat()
            val catItem = CategoryConstants.getCategoryItem(catName, effectiveType)
            CategorySpend(
                categoryName = catName,
                totalAmount = sum,
                percentage = pct,
                colorHex = catItem.colorHex,
                count = items.size,
                type = effectiveType
            )
        }.sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Spend Points for Trend Chart
    val dailyTrendPoints: StateFlow<List<DailySpendPoint>> = combine(
        filteredTransactions,
        selectedPeriod,
        customStartTime,
        customEndTime
    ) { txs, period, customStart, customEnd ->
        val (startTime, endTime) = getTimeBounds(period, customStart, customEnd)
        val cal = Calendar.getInstance().apply { timeInMillis = startTime }
        val endCal = Calendar.getInstance().apply { timeInMillis = endTime }

        val points = mutableListOf<DailySpendPoint>()
        val dayFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val dayExpenseMap = mutableMapOf<Int, Double>()
        val dayIncomeMap = mutableMapOf<Int, Double>()

        for (tx in txs) {
            if (!tx.isTransfer && !tx.category.equals("Transfer", ignoreCase = true)) {
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                val dayOfYear = txCal.get(Calendar.DAY_OF_YEAR)
                if (tx.type == TransactionType.EXPENSE) {
                    dayExpenseMap[dayOfYear] = (dayExpenseMap[dayOfYear] ?: 0.0) + tx.amount
                } else {
                    dayIncomeMap[dayOfYear] = (dayIncomeMap[dayOfYear] ?: 0.0) + tx.amount
                }
            }
        }

        while (!cal.after(endCal) && points.size < 31) {
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val label = dayFormat.format(cal.time)
            val expense = dayExpenseMap[dayOfYear] ?: 0.0
            val income = dayIncomeMap[dayOfYear] ?: 0.0

            points.add(DailySpendPoint(label, dayOfMonth, expense, income))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Budget Status & Smart Alerts (Deducts both Spent & Reserved BNPL / Tamara / Tabby Payments)
    val monthlyBudgetStatus: StateFlow<MonthlyBudgetCalculation> = combine(
        allTransactions,
        pendingReservedPayments,
        currentMonthBudget
    ) { txs, reservedList, budget ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val daysRemaining = Math.max(1, maxDay - currentDay + 1)

        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val monthEnd = cal.timeInMillis

        val monthExpenses = txs.filter {
            !it.isTransfer && !it.category.equals("Transfer", ignoreCase = true) && it.type == TransactionType.EXPENSE && it.timestamp in monthStart..monthEnd
        }.sumOf { it.amount }

        // Pending reserved payments due in this month (Tabby, Tamara, recurring bills)
        val monthPendingReserved = reservedList.filter {
            it.dueDate in monthStart..monthEnd
        }.sumOf { it.amount }

        val totalCommitted = monthExpenses + monthPendingReserved

        if (budget == null || budget.totalLimit <= 0) {
            MonthlyBudgetCalculation(
                monthYear = currentMonthYear,
                budgetLimit = 0.0,
                spentAmount = monthExpenses,
                reservedPendingAmount = monthPendingReserved,
                totalCommittedAmount = totalCommitted,
                remainingAmount = -totalCommitted,
                percentage = 0f,
                alertThresholdPercent = 80,
                alertStatus = BudgetAlertStatus.NO_BUDGET,
                daysRemainingInMonth = daysRemaining,
                safeDailySpend = 0.0
            )
        } else {
            val limit = budget.totalLimit
            val remaining = limit - totalCommitted
            val pct = ((totalCommitted / limit) * 100).toFloat()
            val threshold = budget.alertThresholdPercent

            val status = when {
                totalCommitted > limit -> BudgetAlertStatus.EXCEEDED
                pct >= threshold -> BudgetAlertStatus.WARNING_APPROACHING
                else -> BudgetAlertStatus.SAFE
            }

            val safeDaily = if (remaining > 0) remaining / daysRemaining else 0.0

            MonthlyBudgetCalculation(
                monthYear = budget.monthYear,
                budgetLimit = limit,
                spentAmount = monthExpenses,
                reservedPendingAmount = monthPendingReserved,
                totalCommittedAmount = totalCommitted,
                remainingAmount = remaining,
                percentage = pct,
                alertThresholdPercent = threshold,
                alertStatus = status,
                daysRemainingInMonth = daysRemaining,
                safeDailySpend = safeDaily
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyBudgetCalculation(currentMonthYear, 0.0, 0.0, 0.0, 0.0, 0.0, 0f, 80, BudgetAlertStatus.NO_BUDGET, 1, 0.0)
    )

    // Sync Server & Client states
    val serverState: StateFlow<ServerState> = syncServer.serverState
    val serverPin: StateFlow<String> = syncServer.serverPin
    val serverPinRemainingSeconds: StateFlow<Int> = syncServer.pinSecondsRemaining
    val serverLogs: StateFlow<List<String>> = syncServer.serverLogs
    val clientSyncState: StateFlow<ClientSyncState> = syncClient.syncState
    val clientLogs: StateFlow<List<String>> = syncClient.clientLogs
    val syncedDevices: StateFlow<List<DeviceSyncRecord>> = deviceIdentityService.syncedDevices

    // Independent Cash Denomination Tracker (Synced across devices)
    val denominationTrackerState: StateFlow<DenominationTrackerState> = repository.denominationsFlow
        .map { items ->
            val total = items.sumOf { it.subtotal }
            val pieces = items.sumOf { it.count }
            DenominationTrackerState(items, total, pieces)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DenominationTrackerState(emptyList(), 0.0, 0)
        )



    fun updateDenominationCount(value: Double, newCount: Int) {
        val currentDevId = deviceId.value
        repository.updateDenominationCount(value, newCount, currentDevId)
    }

    fun incrementDenomination(value: Double) {
        val currentDevId = deviceId.value
        repository.incrementDenomination(value, currentDevId)
    }

    fun decrementDenomination(value: Double) {
        val currentDevId = deviceId.value
        repository.decrementDenomination(value, currentDevId)
    }

    fun resetAllDenominations() {
        val currentDevId = deviceId.value
        repository.resetAllDenominations(currentDevId)
    }

    fun addCustomDenomination(value: Double) {
        repository.addCustomDenomination(value)
    }

    fun removeDenomination(value: Double) {
        repository.removeDenomination(value)
    }

    fun restoreDefaultDenominations() {
        val currentDevId = deviceId.value
        repository.restoreDefaultDenominations(currentDevId)
    }

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // --- Actions: Bank Accounts ---
    fun addBankAccount(name: String, initialBalance: Double, maskedNumber: String = "", colorHex: Long = 0xFF1976D2) {
        viewModelScope.launch {
            repository.saveBankAccount(
                BankAccount(
                    bankName = name,
                    initialBalance = initialBalance,
                    accountNumberMasked = maskedNumber,
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateBankAccount(account: BankAccount) {
        viewModelScope.launch {
            repository.saveBankAccount(account)
        }
    }

    fun archiveBankAccount(id: String) {
        viewModelScope.launch {
            repository.archiveBankAccount(id)
        }
    }

    // --- Actions: Account Transfers (Cash <=> Bank, Bank <=> Bank) ---
    fun transferMoney(
        fromType: AccountSourceType,
        fromBankId: String?,
        fromBankName: String?,
        toType: AccountSourceType,
        toBankId: String?,
        toBankName: String?,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.transferMoney(
                fromType = fromType,
                fromBankId = fromBankId,
                fromBankName = fromBankName,
                toType = toType,
                toBankId = toBankId,
                toBankName = toBankName,
                amount = amount,
                note = note,
                timestamp = timestamp,
                deviceId = deviceIdentityService.getSavedDeviceId(),
                deviceName = deviceIdentityService.getSavedDeviceName()
            )
        }
    }

    // --- Actions: Reserved Payments (Tabby / Tamara / BNPL / EMIs) ---
    fun addReservedPayment(
        title: String,
        amount: Double,
        dueDate: Long,
        category: String,
        accountSourceType: AccountSourceType,
        bankAccountId: String?,
        bankAccountName: String?,
        frequency: RecurringFrequency,
        reminderDaysBefore: Int,
        totalInstallments: Int = 1,
        currentInstallment: Int = 1,
        specificDayOfMonth: Int = 0,
        note: String
    ) {
        viewModelScope.launch {
            repository.saveReservedPayment(
                ReservedPayment(
                    title = title,
                    amount = amount,
                    dueDate = dueDate,
                    category = category,
                    accountSourceType = accountSourceType,
                    bankAccountId = bankAccountId,
                    bankAccountName = bankAccountName,
                    frequency = frequency,
                    totalInstallments = totalInstallments,
                    currentInstallment = currentInstallment,
                    specificDayOfMonth = specificDayOfMonth,
                    reminderDaysBefore = reminderDaysBefore,
                    note = note
                )
            )
        }
    }

    fun updateReservedPayment(payment: ReservedPayment) {
        viewModelScope.launch {
            repository.saveReservedPayment(payment)
        }
    }

    fun markReservedPaymentAsPaid(reservedId: String) {
        viewModelScope.launch {
            repository.markReservedPaymentAsPaid(reservedId)
        }
    }

    fun deleteReservedPayment(id: String) {
        viewModelScope.launch {
            repository.deleteReservedPayment(id)
        }
    }

    // --- Actions: Transactions ---
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        note: String,
        timestamp: Long,
        paymentMethod: String,
        accountSourceType: AccountSourceType = AccountSourceType.CASH,
        bankAccountId: String? = null,
        bankAccountName: String? = null
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                amount = amount,
                type = type,
                category = category,
                note = note,
                timestamp = timestamp,
                paymentMethod = paymentMethod,
                accountSourceType = accountSourceType,
                bankAccountId = bankAccountId,
                bankAccountName = bankAccountName,
                deviceId = deviceIdentityService.getSavedDeviceId(),
                deviceName = deviceIdentityService.getSavedDeviceName(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveTransaction(tx)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val updated = transaction.copy(
                deviceId = if (transaction.deviceId.isNotBlank()) transaction.deviceId else deviceIdentityService.getSavedDeviceId(),
                deviceName = if (transaction.deviceName.isNotBlank()) transaction.deviceName else deviceIdentityService.getSavedDeviceName(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveTransaction(updated)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun setBudget(monthYear: String, limit: Double, alertThresholdPercent: Int = 80) {
        viewModelScope.launch {
            repository.setMonthlyBudget(monthYear, limit, alertThresholdPercent)
        }
    }

    fun setPeriod(period: TimePeriodFilter) {
        _selectedPeriod.value = period
    }

    fun setCustomDateRange(startTime: Long, endTime: Long) {
        _customStartTime.value = startTime
        _customEndTime.value = endTime
        _selectedPeriod.value = TimePeriodFilter.CUSTOM
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: TransactionType?) {
        _filterType.value = type
    }

    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
    }

    fun setFilterAccountSource(source: AccountSourceType?) {
        _filterAccountSource.value = source
    }

    fun setFilterBankAccountId(bankId: String?) {
        _filterBankAccountId.value = bankId
    }

    // --- Currency & Conversion Actions ---
    fun setActiveCurrency(code: String) {
        currencyService.setActiveCurrencyCode(code)
        _activeCurrencyCode.value = code
        val symbol = currencyService.getActiveCurrencySymbol()
        _currencySymbol.value = symbol
    }

    fun setCurrencySymbol(symbol: String) {
        val code = _activeCurrencyCode.value
        currencyService.setCustomCurrencySymbol(code, symbol)
        _currencySymbol.value = symbol
    }

    fun setManualExchangeRate(code: String, rateFromSar: Double) {
        currencyService.setManualRate(code, rateFromSar)
        _currencies.value = currencyService.getCurrencies()
        _ratesLastUpdated.value = currencyService.getLastUpdatedTimestamp()
    }

    fun resetCurrencyRate(code: String) {
        currencyService.resetRateToDefault(code)
        _currencies.value = currencyService.getCurrencies()
        _ratesLastUpdated.value = currencyService.getLastUpdatedTimestamp()
    }

    fun resetAllCurrencyRates() {
        currencyService.resetAllRatesToDefault()
        _currencies.value = currencyService.getCurrencies()
        _ratesLastUpdated.value = 0L
    }

    fun fetchLiveExchangeRates() {
        viewModelScope.launch {
            _isFetchingRates.value = true
            _rateFetchMessage.value = null
            when (val result = currencyService.fetchLiveRatesOnline()) {
                is RateFetchResult.Success -> {
                    _currencies.value = currencyService.getCurrencies()
                    _ratesLastUpdated.value = result.timestamp
                    _rateFetchMessage.value = "Successfully fetched live rates for ${result.ratesCount} currencies"
                }
                is RateFetchResult.Error -> {
                    _rateFetchMessage.value = "Failed to fetch online rates: ${result.message}"
                }
            }
            _isFetchingRates.value = false
        }
    }

    fun clearRateFetchMessage() {
        _rateFetchMessage.value = null
    }

    fun convertAmount(amount: Double, fromCode: String, toCode: String): Double {
        if (fromCode == toCode) return amount
        val currList = _currencies.value
        val fromItem = currList.firstOrNull { it.code == fromCode } ?: DefaultCurrencies.getByCode(fromCode)
        val toItem = currList.firstOrNull { it.code == toCode } ?: DefaultCurrencies.getByCode(toCode)
        val amountInSar = fromItem.convertToSar(amount)
        return toItem.convertFromSar(amountInSar)
    }

    // --- Action: Wipe Data & Clear Transactions ---
    fun wipeSelectedData(
        wipeTransactions: Boolean,
        wipeBanks: Boolean,
        wipeBudgets: Boolean,
        wipeReserved: Boolean,
        resetDenominations: Boolean
    ) {
        viewModelScope.launch {
            repository.wipeSelectedData(
                wipeTransactions = wipeTransactions,
                wipeBanks = wipeBanks,
                wipeBudgets = wipeBudgets,
                wipeReserved = wipeReserved,
                resetDenominations = resetDenominations
            )
        }
    }

    fun clearAllTransactions(includeReservedPayments: Boolean = true) {
        viewModelScope.launch {
            repository.clearAllTransactions(includeReservedPayments)
        }
    }

    fun clearAllReservedPayments() {
        viewModelScope.launch {
            repository.clearAllReservedPayments()
        }
    }

    // --- Direct Local Network Sync Actions ---
    fun startSyncServer(port: Int = 8890) {
        syncServer.startServer(port)
    }

    fun stopSyncServer() {
        syncServer.stopServer()
    }

    fun regenerateServerPin(): String {
        return syncServer.regeneratePin()
    }

    fun syncWithPeer(hostAddressWithPort: String, pin: String = "", forceFullSync: Boolean = false) {
        val parts = hostAddressWithPort.trim().split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 8890 else 8890
        viewModelScope.launch {
            syncClient.syncWithHost(host, port, pin, forceFullSync)
        }
    }

    fun removeSyncedDevice(peerDeviceId: String) {
        deviceIdentityService.clearDeviceSyncHistory(peerDeviceId)
    }

    fun clearAllSyncHistory() {
        deviceIdentityService.clearAllSyncHistory()
    }

    // --- Offline Backup & Portability ---
    suspend fun getExportJson(): String = repository.exportToJsonString(
        deviceId = deviceIdentityService.getSavedDeviceId(),
        deviceName = deviceIdentityService.getSavedDeviceName()
    )
    suspend fun getExportCsv(): String = repository.exportToCsvString()

    suspend fun importJsonData(json: String, replaceAll: Boolean = false): Int {
        val snapshot = repository.parseSnapshotFromJson(json)
        return if (replaceAll) {
            repository.replaceAllWithSnapshot(snapshot)
            snapshot.transactions.size + snapshot.budgets.size + snapshot.bankAccounts.size + snapshot.reservedPayments.size
        } else {
            repository.mergeSnapshot(snapshot)
        }
    }

    private fun getTimeBounds(period: TimePeriodFilter, customStart: Long, customEnd: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        return when (period) {
            TimePeriodFilter.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            TimePeriodFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            TimePeriodFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            TimePeriodFilter.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            TimePeriodFilter.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
                cal.set(Calendar.DAY_OF_YEAR, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            TimePeriodFilter.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
            TimePeriodFilter.CUSTOM -> {
                val start = if (customStart > 0) customStart else 0L
                val end = if (customEnd > 0) customEnd else now
                Pair(start, end)
            }
        }
    }
}
