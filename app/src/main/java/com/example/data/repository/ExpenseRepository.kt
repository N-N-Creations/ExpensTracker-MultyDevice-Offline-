package com.example.data.repository

import android.content.Context
import com.example.data.local.BankAccountDao
import com.example.data.local.BudgetDao
import com.example.data.local.ReservedPaymentDao
import com.example.data.local.TransactionDao
import com.example.data.model.AccountSourceType
import com.example.data.model.BankAccount
import com.example.data.model.BankAccountBalance
import com.example.data.model.CashBalance
import com.example.data.model.DenominationItem
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.PeriodSummary
import com.example.data.model.RecurringFrequency
import com.example.data.model.ReservedPayment
import com.example.data.model.ReservedPaymentStatus
import com.example.data.model.SyncSnapshot
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val bankAccountDao: BankAccountDao,
    private val reservedPaymentDao: ReservedPaymentDao,
    private val context: Context? = null
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllActiveTransactions()
    val allBudgets: Flow<List<MonthlyBudget>> = budgetDao.getAllBudgets()
    val allBankAccounts: Flow<List<BankAccount>> = bankAccountDao.getAllActiveBankAccounts()
    val allReservedPayments: Flow<List<ReservedPayment>> = reservedPaymentDao.getAllActiveReservedPayments()
    val pendingReservedPayments: Flow<List<ReservedPayment>> = reservedPaymentDao.getPendingReservedPayments()

    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(startTime, endTime)
    }

    suspend fun getTransactionsBetweenList(startTime: Long, endTime: Long): List<Transaction> {
        return transactionDao.getTransactionsBetweenList(startTime, endTime)
    }

    fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudget?> {
        return budgetDao.getBudgetForMonth(monthYear)
    }

    // --- Transactions CRUD ---
    suspend fun saveTransaction(transaction: Transaction) {
        val updated = transaction.copy(updatedAt = System.currentTimeMillis())
        transactionDao.insertTransaction(updated)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.softDeleteTransaction(id, System.currentTimeMillis())
    }

    // --- Bank Accounts CRUD ---
    suspend fun saveBankAccount(bankAccount: BankAccount) {
        val updated = bankAccount.copy(updatedAt = System.currentTimeMillis())
        bankAccountDao.insertOrUpdate(updated)
    }

    suspend fun archiveBankAccount(id: String) {
        bankAccountDao.archiveBankAccount(id)
    }

    // --- Reserved Payments CRUD ---
    suspend fun saveReservedPayment(payment: ReservedPayment) {
        val updated = payment.copy(updatedAt = System.currentTimeMillis())
        reservedPaymentDao.insertOrUpdate(updated)
    }

    suspend fun markReservedPaymentAsPaid(reservedPaymentId: String): Transaction? = withContext(Dispatchers.IO) {
        val reserved = reservedPaymentDao.getReservedPaymentById(reservedPaymentId) ?: return@withContext null
        val newTx = Transaction(
            id = UUID.randomUUID().toString(),
            amount = reserved.amount,
            type = TransactionType.EXPENSE,
            category = reserved.category,
            note = "${reserved.title}${if (reserved.note.isNotBlank()) " - " + reserved.note else ""}",
            timestamp = System.currentTimeMillis(),
            paymentMethod = if (reserved.accountSourceType == AccountSourceType.BANK) PaymentMethod.BNPL_TABBY_TAMARA.displayName else PaymentMethod.CASH.displayName,
            accountSourceType = reserved.accountSourceType,
            bankAccountId = reserved.bankAccountId,
            bankAccountName = reserved.bankAccountName,
            linkedReservedPaymentId = reserved.id,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(newTx)
        reservedPaymentDao.markAsPaid(reserved.id, newTx.id)

        // If this is a recurring / installment payment and more installments remain, schedule the next one!
        val hasMoreInstallments = when {
            reserved.totalInstallments > 1 && reserved.currentInstallment < reserved.totalInstallments -> true
            reserved.totalInstallments == 0 && reserved.frequency != RecurringFrequency.ONCE -> true // ongoing
            reserved.frequency != RecurringFrequency.ONCE && reserved.totalInstallments > 0 && reserved.currentInstallment < reserved.totalInstallments -> true
            else -> false
        }

        if (hasMoreInstallments) {
            val cal = Calendar.getInstance().apply { timeInMillis = reserved.dueDate }
            when (reserved.frequency) {
                RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                RecurringFrequency.BI_WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 14)
                RecurringFrequency.MONTHLY, RecurringFrequency.ONCE -> {
                    cal.add(Calendar.MONTH, 1)
                    if (reserved.specificDayOfMonth in 1..31) {
                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        cal.set(Calendar.DAY_OF_MONTH, Math.min(reserved.specificDayOfMonth, maxDay))
                    }
                }
            }

            val nextInstallment = reserved.currentInstallment + 1
            val nextTitle = if (reserved.totalInstallments > 1) {
                // e.g. "Tabby - Shoes (1/4)" -> "Tabby - Shoes (2/4)"
                val baseTitle = reserved.title.replace(Regex("\\(\\d+/\\d+\\)$"), "").trim()
                "$baseTitle ($nextInstallment/${reserved.totalInstallments})"
            } else {
                reserved.title
            }

            val nextReserved = ReservedPayment(
                id = UUID.randomUUID().toString(),
                title = nextTitle,
                amount = reserved.amount,
                dueDate = cal.timeInMillis,
                category = reserved.category,
                accountSourceType = reserved.accountSourceType,
                bankAccountId = reserved.bankAccountId,
                bankAccountName = reserved.bankAccountName,
                frequency = reserved.frequency,
                totalInstallments = reserved.totalInstallments,
                currentInstallment = nextInstallment,
                specificDayOfMonth = reserved.specificDayOfMonth,
                parentRecurringId = reserved.parentRecurringId ?: reserved.id,
                reminderDaysBefore = reserved.reminderDaysBefore,
                status = ReservedPaymentStatus.PENDING,
                note = reserved.note,
                updatedAt = System.currentTimeMillis()
            )
            reservedPaymentDao.insertOrUpdate(nextReserved)
        }

        newTx
    }

    suspend fun deleteReservedPayment(id: String) {
        reservedPaymentDao.softDelete(id)
    }

    // --- Account Transfers (Cash <=> Bank, Bank <=> Bank) ---
    suspend fun transferMoney(
        fromType: AccountSourceType,
        fromBankId: String?,
        fromBankName: String?,
        toType: AccountSourceType,
        toBankId: String?,
        toBankName: String?,
        amount: Double,
        note: String,
        timestamp: Long,
        deviceId: String = "",
        deviceName: String = ""
    ): Pair<Transaction, Transaction> = withContext(Dispatchers.IO) {
        val transferId1 = UUID.randomUUID().toString()
        val transferId2 = UUID.randomUUID().toString()

        val sourceName = if (fromType == AccountSourceType.BANK) (fromBankName ?: "Bank Account") else "Cash on Hand"
        val destName = if (toType == AccountSourceType.BANK) (toBankName ?: "Bank Account") else "Cash on Hand"

        val outTx = Transaction(
            id = transferId1,
            amount = amount,
            type = TransactionType.EXPENSE,
            category = "Transfer",
            note = "Transfer to $destName${if (note.isNotBlank()) " - $note" else ""}",
            timestamp = timestamp,
            paymentMethod = PaymentMethod.BANK_TRANSFER.displayName,
            accountSourceType = fromType,
            bankAccountId = fromBankId,
            bankAccountName = fromBankName,
            isTransfer = true,
            transferCounterpartId = transferId2,
            deviceId = deviceId,
            deviceName = deviceName,
            updatedAt = System.currentTimeMillis()
        )

        val inTx = Transaction(
            id = transferId2,
            amount = amount,
            type = TransactionType.INCOME,
            category = "Transfer",
            note = "Transfer from $sourceName${if (note.isNotBlank()) " - $note" else ""}",
            timestamp = timestamp,
            paymentMethod = PaymentMethod.BANK_TRANSFER.displayName,
            accountSourceType = toType,
            bankAccountId = toBankId,
            bankAccountName = toBankName,
            isTransfer = true,
            transferCounterpartId = transferId1,
            deviceId = deviceId,
            deviceName = deviceName,
            updatedAt = System.currentTimeMillis()
        )

        transactionDao.insertTransactions(listOf(outTx, inTx))
        Pair(outTx, inTx)
    }

    suspend fun setMonthlyBudget(monthYear: String, limit: Double, thresholdPercent: Int = 80) {
        val budget = MonthlyBudget(
            monthYear = monthYear,
            totalLimit = limit,
            alertThresholdPercent = thresholdPercent,
            updatedAt = System.currentTimeMillis()
        )
        budgetDao.upsertBudget(budget)
    }

    // --- Period Summary including Reserved Committed Payments ---
    suspend fun calculatePeriodSummary(startTime: Long, endTime: Long): PeriodSummary = withContext(Dispatchers.Default) {
        val txs = transactionDao.getTransactionsBetweenList(startTime, endTime)
        var totalIncome = 0.0
        var totalExpense = 0.0
        val dailyExpenseMap = mutableMapOf<String, Double>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (tx in txs) {
            // Exclude internal transfers between Cash and Banks from general revenue/spending figures
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
            totalReservedCommitted = 0.0,
            netBalance = netBalance,
            savingsRate = savingsRate,
            averageDailyExpense = avgDailyExpense,
            transactionCount = txs.size,
            highestExpenseDay = highestDay,
            highestExpenseAmount = highestAmount
        )
    }

    // --- Data Export & Snapshot (Includes Transactions, Budgets, Bank Accounts, BNPL Reserved Payments & Denominations) ---
    // Cash Denominations Persistence & Reactive State Flow with Vector Delta (CRDT) Support
    private val denomPrefs = context?.getSharedPreferences("denomination_prefs_v2", Context.MODE_PRIVATE)
    private val defaultDenominationValues = listOf(500.0, 200.0, 100.0, 50.0, 20.0, 10.0, 5.0, 2.0, 1.0)

    private val _denominationsFlow = MutableStateFlow<List<DenominationItem>>(loadSavedDenominations())
    val denominationsFlow: StateFlow<List<DenominationItem>> = _denominationsFlow.asStateFlow()

    private fun getEffectiveDeviceId(): String {
        return try {
            val devPrefs = context?.getSharedPreferences("expense_device_identity_prefs", Context.MODE_PRIVATE)
            devPrefs?.getString("device_id", null) ?: "dev_local"
        } catch (_: Exception) {
            "dev_local"
        }
    }

    private fun serializeDeltas(deltas: Map<String, com.example.data.model.DeviceDeltaRecord>): String {
        val json = JSONObject()
        for ((devId, record) in deltas) {
            val recObj = JSONObject()
            recObj.put("delta", record.delta)
            recObj.put("updatedAt", record.updatedAt)
            json.put(devId, recObj)
        }
        return json.toString()
    }

    private fun deserializeDeltas(jsonStr: String?): Map<String, com.example.data.model.DeviceDeltaRecord> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, com.example.data.model.DeviceDeltaRecord>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val recObj = json.optJSONObject(key)
                if (recObj != null) {
                    map[key] = com.example.data.model.DeviceDeltaRecord(
                        delta = recObj.optInt("delta", 0),
                        updatedAt = recObj.optLong("updatedAt", System.currentTimeMillis())
                    )
                } else {
                    val deltaInt = json.optInt(key, 0)
                    map[key] = com.example.data.model.DeviceDeltaRecord(delta = deltaInt, updatedAt = System.currentTimeMillis())
                }
            }
        } catch (_: Exception) {}
        return map
    }

    private fun loadSavedDenominations(): List<DenominationItem> {
        val prefs = denomPrefs ?: return defaultDenominationValues.map { DenominationItem(it, 0, false) }
        val activeDenomsStr = prefs.getString("active_denoms", "") ?: ""
        val activeValues = if (activeDenomsStr.isNotBlank()) {
            activeDenomsStr.split(",").mapNotNull { it.toDoubleOrNull() }
        } else {
            defaultDenominationValues
        }
        val lastUpdatedAll = prefs.getLong("denoms_last_updated", System.currentTimeMillis())

        return activeValues.distinct().sortedDescending().map { value ->
            val count = prefs.getInt("count_${value}", 0)
            val baseCount = prefs.getInt("base_${value}", count)
            val isCustom = !defaultDenominationValues.contains(value)
            val itemUpdated = prefs.getLong("updated_${value}", lastUpdatedAll)
            val deltasStr = prefs.getString("deltas_${value}", null)
            val deltas = deserializeDeltas(deltasStr)

            val effectiveCount = if (deltas.isNotEmpty()) {
                (baseCount + deltas.values.sumOf { it.delta }).coerceAtLeast(0)
            } else {
                count
            }

            DenominationItem(
                value = value,
                count = effectiveCount,
                isCustom = isCustom,
                updatedAt = itemUpdated,
                baseCount = baseCount,
                deviceDeltas = deltas
            )
        }
    }

    fun getDenominations(): List<DenominationItem> = loadSavedDenominations()

    fun saveDenominations(items: List<DenominationItem>) {
        val prefs = denomPrefs ?: return
        val editor = prefs.edit()
        val activeValues = items.map { it.value }
        val now = System.currentTimeMillis()
        editor.putString("active_denoms", activeValues.joinToString(","))
        editor.putLong("denoms_last_updated", now)
        items.forEach { item ->
            editor.putInt("count_${item.value}", item.count)
            editor.putInt("base_${item.value}", item.baseCount)
            editor.putLong("updated_${item.value}", if (item.updatedAt > 0) item.updatedAt else now)
            editor.putString("deltas_${item.value}", serializeDeltas(item.deviceDeltas))
        }
        editor.apply()
        _denominationsFlow.value = items
    }

    fun updateDenominationCount(value: Double, newCount: Int, deviceId: String = "") {
        val devId = if (deviceId.isNotBlank()) deviceId else getEffectiveDeviceId()
        val clamped = newCount.coerceAtLeast(0)
        val current = _denominationsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.value == value }
        if (index != -1) {
            val item = current[index]
            val now = System.currentTimeMillis()
            val otherDeltasSum = item.deviceDeltas.filterKeys { it != devId }.values.sumOf { it.delta }
            val myNewDelta = clamped - item.baseCount - otherDeltasSum

            val updatedDeltas = item.deviceDeltas.toMutableMap()
            updatedDeltas[devId] = com.example.data.model.DeviceDeltaRecord(delta = myNewDelta, updatedAt = now)

            val computedCount = (item.baseCount + updatedDeltas.values.sumOf { it.delta }).coerceAtLeast(0)
            current[index] = item.copy(
                count = computedCount,
                updatedAt = now,
                deviceDeltas = updatedDeltas
            )
            saveDenominations(current)
        }
    }

    fun incrementDenomination(value: Double, deviceId: String = "") {
        val devId = if (deviceId.isNotBlank()) deviceId else getEffectiveDeviceId()
        val current = _denominationsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.value == value }
        if (index != -1) {
            val item = current[index]
            val now = System.currentTimeMillis()
            val currentMyDelta = item.deviceDeltas[devId]?.delta ?: 0
            val updatedDeltas = item.deviceDeltas.toMutableMap()
            updatedDeltas[devId] = com.example.data.model.DeviceDeltaRecord(delta = currentMyDelta + 1, updatedAt = now)

            val computedCount = (item.baseCount + updatedDeltas.values.sumOf { it.delta }).coerceAtLeast(0)
            current[index] = item.copy(
                count = computedCount,
                updatedAt = now,
                deviceDeltas = updatedDeltas
            )
            saveDenominations(current)
        }
    }

    fun decrementDenomination(value: Double, deviceId: String = "") {
        val devId = if (deviceId.isNotBlank()) deviceId else getEffectiveDeviceId()
        val current = _denominationsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.value == value }
        if (index != -1 && current[index].count > 0) {
            val item = current[index]
            val now = System.currentTimeMillis()
            val currentMyDelta = item.deviceDeltas[devId]?.delta ?: 0
            val updatedDeltas = item.deviceDeltas.toMutableMap()
            updatedDeltas[devId] = com.example.data.model.DeviceDeltaRecord(delta = currentMyDelta - 1, updatedAt = now)

            val computedCount = (item.baseCount + updatedDeltas.values.sumOf { it.delta }).coerceAtLeast(0)
            current[index] = item.copy(
                count = computedCount,
                updatedAt = now,
                deviceDeltas = updatedDeltas
            )
            saveDenominations(current)
        }
    }

    fun resetAllDenominations(deviceId: String = "") {
        val devId = if (deviceId.isNotBlank()) deviceId else getEffectiveDeviceId()
        val now = System.currentTimeMillis()
        val current = _denominationsFlow.value.map { item ->
            val updatedDeltas = mapOf(devId to com.example.data.model.DeviceDeltaRecord(delta = 0, updatedAt = now))
            item.copy(
                count = 0,
                baseCount = 0,
                deviceDeltas = updatedDeltas,
                updatedAt = now
            )
        }
        saveDenominations(current)
    }

    fun addCustomDenomination(value: Double) {
        if (value <= 0.0) return
        val current = _denominationsFlow.value.toMutableList()
        if (current.none { it.value == value }) {
            val isCustom = !defaultDenominationValues.contains(value)
            current.add(
                DenominationItem(
                    value = value,
                    count = 0,
                    isCustom = isCustom,
                    updatedAt = System.currentTimeMillis(),
                    baseCount = 0,
                    deviceDeltas = emptyMap()
                )
            )
            val sorted = current.sortedByDescending { it.value }
            saveDenominations(sorted)
        }
    }

    fun removeDenomination(value: Double) {
        val current = _denominationsFlow.value.filterNot { it.value == value }
        denomPrefs?.edit()
            ?.remove("count_${value}")
            ?.remove("base_${value}")
            ?.remove("updated_${value}")
            ?.remove("deltas_${value}")
            ?.apply()
        saveDenominations(current)
    }

    fun restoreDefaultDenominations(deviceId: String = "") {
        val currentMap = _denominationsFlow.value.associateBy { it.value }
        val now = System.currentTimeMillis()
        val restored = defaultDenominationValues.sortedDescending().map { value ->
            val existing = currentMap[value]
            existing?.copy(isCustom = false, updatedAt = now) ?: DenominationItem(
                value = value,
                count = 0,
                isCustom = false,
                updatedAt = now,
                baseCount = 0,
                deviceDeltas = emptyMap()
            )
        }
        saveDenominations(restored)
    }

    /**
     * Vector Delta CRDT Merge for Cash Denominations:
     * Synchronizes physical cash counts across 2, 3, or N peer devices in any order.
     * Merges per-device delta vectors so local additions and deductions propagate without data loss or double-counting.
     */
    fun mergeDenominationsVector(snapshot: SyncSnapshot, localDeviceId: String = ""): Int {
        val remoteDenominations = snapshot.denominations
        if (remoteDenominations.isEmpty()) return 0
        val myDevId = if (localDeviceId.isNotBlank()) localDeviceId else getEffectiveDeviceId()
        val remoteDevId = snapshot.deviceId

        val localList = getDenominations().toMutableList()
        val localMap = localList.associateBy { it.value }.toMutableMap()
        val now = System.currentTimeMillis()
        var changedCount = 0

        val allValues = (localMap.keys + remoteDenominations.map { it.value }).distinct()
        val mergedResult = mutableListOf<DenominationItem>()

        for (value in allValues) {
            val localItem = localMap[value]
            val remoteItem = remoteDenominations.firstOrNull { it.value == value }

            val isCustom = (localItem?.isCustom == true) || (remoteItem?.isCustom == true)

            if (localItem == null && remoteItem != null) {
                // New denomination discovered from peer
                mergedResult.add(remoteItem.copy(updatedAt = now))
                changedCount++
                continue
            }

            if (remoteItem == null && localItem != null) {
                // Present locally
                mergedResult.add(localItem)
                continue
            }

            if (localItem != null && remoteItem != null) {
                // Base Count:
                val baseCount = if (localItem.baseCount == remoteItem.baseCount) {
                    localItem.baseCount
                } else {
                    if (localItem.updatedAt >= remoteItem.updatedAt) localItem.baseCount else remoteItem.baseCount
                }

                // Merge Device Deltas Map (Per-Device Vector Clock)
                val mergedDeltas = mutableMapOf<String, com.example.data.model.DeviceDeltaRecord>()
                val allDevIds = (localItem.deviceDeltas.keys + remoteItem.deviceDeltas.keys).distinct()

                for (devId in allDevIds) {
                    val locRec = localItem.deviceDeltas[devId]
                    val remRec = remoteItem.deviceDeltas[devId]

                    when {
                        locRec != null && remRec == null -> {
                            mergedDeltas[devId] = locRec
                        }
                        remRec != null && locRec == null -> {
                            mergedDeltas[devId] = remRec
                        }
                        locRec != null && remRec != null -> {
                            if (devId == myDevId && myDevId.isNotBlank()) {
                                mergedDeltas[devId] = if (locRec.updatedAt >= remRec.updatedAt) locRec else remRec
                            } else if (devId == remoteDevId && remoteDevId.isNotBlank()) {
                                mergedDeltas[devId] = if (remRec.updatedAt >= locRec.updatedAt) remRec else locRec
                            } else {
                                val winningRec = if (remRec.updatedAt > locRec.updatedAt) remRec else locRec
                                mergedDeltas[devId] = winningRec
                            }
                        }
                    }
                }

                val finalCount = if (mergedDeltas.isEmpty()) {
                    if (remoteItem.updatedAt > localItem.updatedAt) remoteItem.count else localItem.count
                } else {
                    (baseCount + mergedDeltas.values.sumOf { it.delta }).coerceAtLeast(0)
                }

                if (finalCount != localItem.count || mergedDeltas != localItem.deviceDeltas) {
                    changedCount++
                }

                mergedResult.add(
                    DenominationItem(
                        value = value,
                        count = finalCount,
                        isCustom = isCustom,
                        updatedAt = maxOf(localItem.updatedAt, remoteItem.updatedAt, now),
                        baseCount = baseCount,
                        deviceDeltas = mergedDeltas
                    )
                )
            }
        }

        val sortedResult = mergedResult.sortedByDescending { it.value }
        saveDenominations(sortedResult)
        return changedCount
    }

    suspend fun createExportSnapshot(
        deviceId: String = "",
        deviceName: String = "Android Device",
        sinceTimestamp: Long = 0L
    ): SyncSnapshot = withContext(Dispatchers.IO) {
        val allTx = transactionDao.getAllTransactionsSnapshot()
        val allBudgets = budgetDao.getAllBudgetsSnapshot()
        val allBanks = bankAccountDao.getAllActiveBankAccounts().firstOrNull() ?: emptyList()
        val allReserved = reservedPaymentDao.getAllForSync()
        val allDenoms = getDenominations()

        // Incremental sync filter: if sinceTimestamp > 0, include records updated since (with 5-second buffer)
        val effectiveSince = if (sinceTimestamp > 0L) (sinceTimestamp - 5000L).coerceAtLeast(0L) else 0L

        val transactions = if (effectiveSince > 0L) {
            allTx.filter { it.updatedAt >= effectiveSince }
        } else {
            allTx
        }

        val budgets = if (effectiveSince > 0L) {
            allBudgets.filter { it.updatedAt >= effectiveSince }
        } else {
            allBudgets
        }

        val bankAccounts = if (effectiveSince > 0L) {
            allBanks.filter { it.updatedAt >= effectiveSince }
        } else {
            allBanks
        }

        val reservedPayments = if (effectiveSince > 0L) {
            allReserved.filter { it.updatedAt >= effectiveSince }
        } else {
            allReserved
        }

        val denominations = if (effectiveSince > 0L) {
            allDenoms.filter { it.updatedAt >= effectiveSince || it.count > 0 }
        } else {
            allDenoms
        }

        SyncSnapshot(
            version = 2,
            exportTimestamp = System.currentTimeMillis(),
            deviceId = deviceId,
            deviceName = deviceName,
            sinceTimestamp = sinceTimestamp,
            transactions = transactions,
            budgets = budgets,
            bankAccounts = bankAccounts,
            reservedPayments = reservedPayments,
            denominations = denominations
        )
    }

    suspend fun exportToJsonString(
        deviceId: String = "",
        deviceName: String = "Android Device",
        sinceTimestamp: Long = 0L
    ): String = withContext(Dispatchers.Default) {
        val snapshot = createExportSnapshot(deviceId, deviceName, sinceTimestamp)
        val root = JSONObject()
        root.put("version", snapshot.version)
        root.put("exportTimestamp", snapshot.exportTimestamp)
        root.put("deviceId", snapshot.deviceId)
        root.put("deviceName", snapshot.deviceName)
        root.put("sinceTimestamp", snapshot.sinceTimestamp)

        val txArray = JSONArray()
        for (tx in snapshot.transactions) {
            val txObj = JSONObject()
            txObj.put("id", tx.id)
            txObj.put("amount", tx.amount)
            txObj.put("type", tx.type.name)
            txObj.put("category", tx.category)
            txObj.put("note", tx.note)
            txObj.put("timestamp", tx.timestamp)
            txObj.put("paymentMethod", tx.paymentMethod)
            txObj.put("accountSourceType", tx.accountSourceType.name)
            txObj.put("bankAccountId", tx.bankAccountId ?: "")
            txObj.put("bankAccountName", tx.bankAccountName ?: "")
            txObj.put("linkedReservedPaymentId", tx.linkedReservedPaymentId ?: "")
            txObj.put("isTransfer", tx.isTransfer)
            txObj.put("transferCounterpartId", tx.transferCounterpartId ?: "")
            txObj.put("deviceId", tx.deviceId)
            txObj.put("deviceName", tx.deviceName)
            txObj.put("updatedAt", tx.updatedAt)
            txObj.put("isDeleted", tx.isDeleted)
            txArray.put(txObj)
        }
        root.put("transactions", txArray)

        val bgArray = JSONArray()
        for (b in snapshot.budgets) {
            val bObj = JSONObject()
            bObj.put("monthYear", b.monthYear)
            bObj.put("totalLimit", b.totalLimit)
            bObj.put("alertThresholdPercent", b.alertThresholdPercent)
            bObj.put("updatedAt", b.updatedAt)
            bgArray.put(bObj)
        }
        root.put("budgets", bgArray)

        val bankArray = JSONArray()
        for (ba in snapshot.bankAccounts) {
            val baObj = JSONObject()
            baObj.put("id", ba.id)
            baObj.put("bankName", ba.bankName)
            baObj.put("accountNumberMasked", ba.accountNumberMasked)
            baObj.put("initialBalance", ba.initialBalance)
            baObj.put("colorHex", ba.colorHex)
            baObj.put("iconName", ba.iconName)
            baObj.put("isArchived", ba.isArchived)
            baObj.put("updatedAt", ba.updatedAt)
            bankArray.put(baObj)
        }
        root.put("bankAccounts", bankArray)

        val resArray = JSONArray()
        for (rp in snapshot.reservedPayments) {
            val rpObj = JSONObject()
            rpObj.put("id", rp.id)
            rpObj.put("title", rp.title)
            rpObj.put("amount", rp.amount)
            rpObj.put("dueDate", rp.dueDate)
            rpObj.put("category", rp.category)
            rpObj.put("accountSourceType", rp.accountSourceType.name)
            rpObj.put("bankAccountId", rp.bankAccountId ?: "")
            rpObj.put("bankAccountName", rp.bankAccountName ?: "")
            rpObj.put("frequency", rp.frequency.name)
            rpObj.put("totalInstallments", rp.totalInstallments)
            rpObj.put("currentInstallment", rp.currentInstallment)
            rpObj.put("specificDayOfMonth", rp.specificDayOfMonth)
            rpObj.put("parentRecurringId", rp.parentRecurringId ?: "")
            rpObj.put("reminderDaysBefore", rp.reminderDaysBefore)
            rpObj.put("status", rp.status.name)
            rpObj.put("paidTransactionId", rp.paidTransactionId ?: "")
            rpObj.put("note", rp.note)
            rpObj.put("updatedAt", rp.updatedAt)
            rpObj.put("isDeleted", rp.isDeleted)
            resArray.put(rpObj)
        }
        root.put("reservedPayments", resArray)

        val denomArray = JSONArray()
        for (d in snapshot.denominations) {
            val dObj = JSONObject()
            dObj.put("value", d.value)
            dObj.put("count", d.count)
            dObj.put("baseCount", d.baseCount)
            dObj.put("isCustom", d.isCustom)
            dObj.put("updatedAt", d.updatedAt)

            val deltasObj = JSONObject()
            for ((devId, record) in d.deviceDeltas) {
                val recObj = JSONObject()
                recObj.put("delta", record.delta)
                recObj.put("updatedAt", record.updatedAt)
                deltasObj.put(devId, recObj)
            }
            dObj.put("deviceDeltas", deltasObj)
            denomArray.put(dObj)
        }
        root.put("denominations", denomArray)

        root.toString(2)
    }

    suspend fun exportToCsvString(startTime: Long? = null, endTime: Long? = null): String = withContext(Dispatchers.Default) {
        val txs = if (startTime != null && endTime != null) {
            transactionDao.getTransactionsBetweenList(startTime, endTime)
        } else {
            transactionDao.getAllTransactionsSnapshot().filter { !it.isDeleted }
        }

        val sb = StringBuilder()
        sb.append("ID,Date,Time,Type,Category,Amount,Account Source,Bank Name,Payment Method,Device,Note\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        for (tx in txs) {
            val d = Date(tx.timestamp)
            val dateStr = dateFormat.format(d)
            val timeStr = timeFormat.format(d)
            val safeNote = "\"" + tx.note.replace("\"", "\"\"") + "\""
            val safeBankName = "\"" + (tx.bankAccountName ?: "") + "\""
            val safeDeviceName = "\"" + (tx.deviceName.ifBlank { "Local Device" }).replace("\"", "\"\"") + "\""
            sb.append("${tx.id},$dateStr,$timeStr,${tx.type.name},\"${tx.category}\",${tx.amount},\"${tx.accountSourceType.displayName}\",$safeBankName,\"${tx.paymentMethod}\",$safeDeviceName,$safeNote\n")
        }
        sb.toString()
    }

    // --- Import & Smart Merge ---
    fun parseSnapshotFromJson(jsonStr: String): SyncSnapshot {
        val root = JSONObject(jsonStr)
        val version = root.optInt("version", 2)
        val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())
        val deviceId = root.optString("deviceId", "")
        val deviceName = root.optString("deviceName", "Unknown")
        val sinceTimestamp = root.optLong("sinceTimestamp", 0L)

        val txList = mutableListOf<Transaction>()
        val txArray = root.optJSONArray("transactions")
        if (txArray != null) {
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val typeStr = obj.optString("type", "EXPENSE")
                val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                val accTypeStr = obj.optString("accountSourceType", "CASH")
                val accType = try { AccountSourceType.valueOf(accTypeStr) } catch (e: Exception) { AccountSourceType.CASH }

                val tx = Transaction(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    amount = obj.optDouble("amount", 0.0),
                    type = type,
                    category = obj.optString("category", "General"),
                    note = obj.optString("note", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    paymentMethod = obj.optString("paymentMethod", PaymentMethod.CASH.displayName),
                    accountSourceType = accType,
                    bankAccountId = obj.optString("bankAccountId").takeIf { it.isNotBlank() },
                    bankAccountName = obj.optString("bankAccountName").takeIf { it.isNotBlank() },
                    linkedReservedPaymentId = obj.optString("linkedReservedPaymentId").takeIf { it.isNotBlank() },
                    isTransfer = obj.optBoolean("isTransfer", false),
                    transferCounterpartId = obj.optString("transferCounterpartId").takeIf { it.isNotBlank() },
                    deviceId = obj.optString("deviceId", ""),
                    deviceName = obj.optString("deviceName", deviceName),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    isDeleted = obj.optBoolean("isDeleted", false)
                )
                txList.add(tx)
            }
        }

        val bgList = mutableListOf<MonthlyBudget>()
        val bgArray = root.optJSONArray("budgets")
        if (bgArray != null) {
            for (i in 0 until bgArray.length()) {
                val obj = bgArray.getJSONObject(i)
                val bg = MonthlyBudget(
                    monthYear = obj.optString("monthYear", ""),
                    totalLimit = obj.optDouble("totalLimit", 0.0),
                    alertThresholdPercent = obj.optInt("alertThresholdPercent", 80),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                if (bg.monthYear.isNotEmpty()) {
                    bgList.add(bg)
                }
            }
        }

        val bankList = mutableListOf<BankAccount>()
        val bankArray = root.optJSONArray("bankAccounts")
        if (bankArray != null) {
            for (i in 0 until bankArray.length()) {
                val obj = bankArray.getJSONObject(i)
                val bank = BankAccount(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    bankName = obj.optString("bankName", "My Bank"),
                    accountNumberMasked = obj.optString("accountNumberMasked", ""),
                    initialBalance = obj.optDouble("initialBalance", 0.0),
                    colorHex = obj.optLong("colorHex", 0xFF1976D2),
                    iconName = obj.optString("iconName", "account_balance"),
                    isArchived = obj.optBoolean("isArchived", false),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                bankList.add(bank)
            }
        }

        val resList = mutableListOf<ReservedPayment>()
        val resArray = root.optJSONArray("reservedPayments")
        if (resArray != null) {
            for (i in 0 until resArray.length()) {
                val obj = resArray.getJSONObject(i)
                val accTypeStr = obj.optString("accountSourceType", "BANK")
                val accType = try { AccountSourceType.valueOf(accTypeStr) } catch (e: Exception) { AccountSourceType.BANK }
                val freqStr = obj.optString("frequency", "ONCE")
                val freq = try { RecurringFrequency.valueOf(freqStr) } catch (e: Exception) { RecurringFrequency.ONCE }
                val statStr = obj.optString("status", "PENDING")
                val status = try { ReservedPaymentStatus.valueOf(statStr) } catch (e: Exception) { ReservedPaymentStatus.PENDING }

                val rp = ReservedPayment(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Reserved Payment"),
                    amount = obj.optDouble("amount", 0.0),
                    dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                    category = obj.optString("category", "Shopping"),
                    accountSourceType = accType,
                    bankAccountId = obj.optString("bankAccountId").takeIf { it.isNotBlank() },
                    bankAccountName = obj.optString("bankAccountName").takeIf { it.isNotBlank() },
                    frequency = freq,
                    totalInstallments = obj.optInt("totalInstallments", 1),
                    currentInstallment = obj.optInt("currentInstallment", 1),
                    specificDayOfMonth = obj.optInt("specificDayOfMonth", 0),
                    parentRecurringId = obj.optString("parentRecurringId").takeIf { it.isNotBlank() },
                    reminderDaysBefore = obj.optInt("reminderDaysBefore", 3),
                    status = status,
                    paidTransactionId = obj.optString("paidTransactionId").takeIf { it.isNotBlank() },
                    note = obj.optString("note", ""),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    isDeleted = obj.optBoolean("isDeleted", false)
                )
                resList.add(rp)
            }
        }

        val denomList = mutableListOf<DenominationItem>()
        val denomArray = root.optJSONArray("denominations")
        if (denomArray != null) {
            for (i in 0 until denomArray.length()) {
                val obj = denomArray.getJSONObject(i)
                val value = obj.optDouble("value", 0.0)
                val count = obj.optInt("count", 0)
                val baseCount = obj.optInt("baseCount", count)
                val isCustom = obj.optBoolean("isCustom", false)
                val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

                val deltasMap = mutableMapOf<String, com.example.data.model.DeviceDeltaRecord>()
                val deltasObj = obj.optJSONObject("deviceDeltas")
                if (deltasObj != null) {
                    val keys = deltasObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val rObj = deltasObj.optJSONObject(k)
                        if (rObj != null) {
                            deltasMap[k] = com.example.data.model.DeviceDeltaRecord(
                                delta = rObj.optInt("delta", 0),
                                updatedAt = rObj.optLong("updatedAt", updatedAt)
                            )
                        } else {
                            val dInt = deltasObj.optInt(k, 0)
                            deltasMap[k] = com.example.data.model.DeviceDeltaRecord(delta = dInt, updatedAt = updatedAt)
                        }
                    }
                }

                if (value > 0.0) {
                    denomList.add(
                        DenominationItem(
                            value = value,
                            count = count,
                            isCustom = isCustom,
                            updatedAt = updatedAt,
                            baseCount = baseCount,
                            deviceDeltas = deltasMap
                        )
                    )
                }
            }
        }

        return SyncSnapshot(
            version = version,
            exportTimestamp = exportTimestamp,
            deviceId = deviceId,
            deviceName = deviceName,
            sinceTimestamp = sinceTimestamp,
            transactions = txList,
            budgets = bgList,
            bankAccounts = bankList,
            reservedPayments = resList,
            denominations = denomList
        )
    }

    suspend fun mergeSnapshot(snapshot: SyncSnapshot): Int = withContext(Dispatchers.IO) {
        val localTxs = transactionDao.getAllTransactionsSnapshot().associateBy { it.id }.toMutableMap()
        var mergedCount = 0

        val toInsertTxs = mutableListOf<Transaction>()
        for (remoteTx in snapshot.transactions) {
            val localTx = localTxs[remoteTx.id]
            if (localTx == null || remoteTx.updatedAt > localTx.updatedAt) {
                val normalizedTx = if (remoteTx.deviceName.isBlank() && snapshot.deviceName.isNotBlank()) {
                    remoteTx.copy(deviceName = snapshot.deviceName)
                } else {
                    remoteTx
                }
                toInsertTxs.add(normalizedTx)
                mergedCount++
            }
        }
        if (toInsertTxs.isNotEmpty()) {
            transactionDao.insertTransactions(toInsertTxs)
        }

        // Merge budgets
        val localBudgets = budgetDao.getAllBudgetsSnapshot().associateBy { it.monthYear }
        val toInsertBudgets = mutableListOf<MonthlyBudget>()
        for (remoteBudget in snapshot.budgets) {
            val localBudget = localBudgets[remoteBudget.monthYear]
            if (localBudget == null || remoteBudget.updatedAt > localBudget.updatedAt) {
                toInsertBudgets.add(remoteBudget)
            }
        }
        if (toInsertBudgets.isNotEmpty()) {
            budgetDao.insertBudgets(toInsertBudgets)
        }

        // Merge bank accounts
        if (snapshot.bankAccounts.isNotEmpty()) {
            bankAccountDao.insertOrUpdateAll(snapshot.bankAccounts)
            mergedCount += snapshot.bankAccounts.size
        }

        // Merge reserved payments
        if (snapshot.reservedPayments.isNotEmpty()) {
            reservedPaymentDao.insertOrUpdateAll(snapshot.reservedPayments)
            mergedCount += snapshot.reservedPayments.size
        }

        // Merge cash denominations with Vector Delta CRDT synchronization
        if (snapshot.denominations.isNotEmpty()) {
            val denomMerged = mergeDenominationsVector(snapshot)
            mergedCount += denomMerged
        }

        mergedCount
    }

    suspend fun replaceAllWithSnapshot(snapshot: SyncSnapshot) = withContext(Dispatchers.IO) {
        transactionDao.clearAll()
        budgetDao.clearAll()
        bankAccountDao.deleteAll()
        reservedPaymentDao.deleteAll()
        transactionDao.insertTransactions(snapshot.transactions)
        budgetDao.insertBudgets(snapshot.budgets)
        bankAccountDao.insertOrUpdateAll(snapshot.bankAccounts)
        reservedPaymentDao.insertOrUpdateAll(snapshot.reservedPayments)
        if (snapshot.denominations.isNotEmpty()) {
            saveDenominations(snapshot.denominations)
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val prefs = context?.getSharedPreferences("expense_tracker_init_v2", Context.MODE_PRIVATE)
        val hasInitialized = prefs?.getBoolean("has_completed_initial_seed", false) ?: false
        if (hasInitialized) return@withContext

        // Mark initialization complete with clean database (no dummy data)
        prefs?.edit()?.putBoolean("has_completed_initial_seed", true)?.apply()
    }

    suspend fun wipeSelectedData(
        wipeTransactions: Boolean,
        wipeBanks: Boolean,
        wipeBudgets: Boolean,
        wipeReserved: Boolean,
        resetDenominations: Boolean
    ) = withContext(Dispatchers.IO) {
        if (wipeTransactions) {
            transactionDao.clearAll()
        }
        if (wipeBanks) {
            bankAccountDao.deleteAll()
        }
        if (wipeBudgets) {
            budgetDao.clearAll()
        }
        if (wipeReserved) {
            reservedPaymentDao.deleteAll()
        }
        if (resetDenominations) {
            resetAllDenominations()
        }
    }

    suspend fun clearAllTransactions(includeReservedPayments: Boolean = true) = withContext(Dispatchers.IO) {
        transactionDao.clearAll()
        if (includeReservedPayments) {
            reservedPaymentDao.deleteAll()
        }
    }

    suspend fun clearAllReservedPayments() = withContext(Dispatchers.IO) {
        reservedPaymentDao.deleteAll()
    }
}
