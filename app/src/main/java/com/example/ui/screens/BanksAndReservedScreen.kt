package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccount
import com.example.data.model.ReservedPayment
import com.example.data.model.ReservedPaymentStatus
import com.example.ui.components.BankAccountCard
import com.example.ui.components.ReservedPaymentItemCard
import com.example.ui.components.ReservedPaymentReminderBanner
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.Locale

@Composable
fun BanksAndReservedScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val bankBalances by viewModel.bankAccountBalances.collectAsStateWithLifecycle()
    val cashBalance by viewModel.cashBalance.collectAsStateWithLifecycle()
    val allBankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val reservedPayments by viewModel.allReservedPayments.collectAsStateWithLifecycle()
    val reminderAlerts by viewModel.reservedReminders.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Reserved / BNPL, 1: Banks & Cash, 2: Denomination Tracker

    var showAddBankDialog by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<BankAccount?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }

    var showAddReservedDialog by remember { mutableStateOf(false) }
    var editingReserved by remember { mutableStateOf<ReservedPayment?>(null) }
    var showFinishedSection by remember { mutableStateOf(false) }

    val pendingPayments = remember(reservedPayments) {
        reservedPayments.filter { it.status == ReservedPaymentStatus.PENDING }
    }
    val finishedPayments = remember(reservedPayments) {
        reservedPayments.filter { it.status == ReservedPaymentStatus.PAID }
    }

    val totalBankAssets = bankBalances.sumOf { it.currentBalance }
    val totalPendingReserved = pendingPayments.sumOf { it.amount }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Switcher with 3 options: Reserved, Banks & Cash, Denomination Counter
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CreditScore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reserved / BNPL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.testTag("tab_reserved_bnpl")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Banks & Cash", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.testTag("tab_banks_cash")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Denominations", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.testTag("tab_denominations")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (selectedTab == 2) {
            // Independent Cash Denomination Tracker Section
            CashDenominationSection(viewModel = viewModel)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    // --- Reserved Payments & Tabby/Tamara Section ---
                    item {
                        // Reserved Overview Card
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("reserved_overview_card"),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Committed in Reserved / BNPL",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, totalPendingReserved),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Button(
                                        onClick = { showAddReservedDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reserve")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "💡 Payments automatically reserve room in your monthly budget and alert you before auto-debit dates (Tabby, Tamara, Rent EMI, etc.)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // High Priority Reminder Alerts (Upcoming / Overdue / Low Bank Balance)
                    item {
                        ReservedPaymentReminderBanner(
                            alerts = reminderAlerts,
                            currencySymbol = currencySymbol,
                            onPayClick = { payment ->
                                viewModel.markReservedPaymentAsPaid(payment.id)
                            }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Obligations (${pendingPayments.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (pendingPayments.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${pendingPayments.size} pending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (pendingPayments.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (finishedPayments.isNotEmpty())
                                        IncomeGreen.copy(alpha = 0.08f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (finishedPayments.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.CreditScore,
                                        contentDescription = null,
                                        tint = if (finishedPayments.isNotEmpty()) IncomeGreen else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (finishedPayments.isNotEmpty()) "All Obligations Paid!" else "No Reserved Payments Yet",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (finishedPayments.isNotEmpty()) IncomeGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (finishedPayments.isNotEmpty())
                                            "You have no pending scheduled BNPL or reserved payments. Great job staying on top of your bills!"
                                        else
                                            "Add Tabby/Tamara installments, rent, or recurring bills to get auto-budget deductions and bank balance reminders.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    if (finishedPayments.isEmpty()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = { showAddReservedDialog = true },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("+ Reserve First Payment")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(pendingPayments) { payment ->
                            ReservedPaymentItemCard(
                                payment = payment,
                                currencySymbol = currencySymbol,
                                onPayClick = {
                                    viewModel.markReservedPaymentAsPaid(payment.id)
                                },
                                onClick = {
                                    editingReserved = payment
                                },
                                onDelete = {
                                    viewModel.deleteReservedPayment(payment.id)
                                }
                            )
                        }
                    }

                    // Finished / Completed Payments Section
                    if (finishedPayments.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFinishedSection = !showFinishedSection }
                                    .testTag("finished_payments_accordion_toggle"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = IncomeGreen.copy(alpha = 0.15f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.DoneAll,
                                                    contentDescription = null,
                                                    tint = IncomeGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "Finished / Completed Payments",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${finishedPayments.size} paid obligation${if (finishedPayments.size > 1) "s" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = if (showFinishedSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (showFinishedSection) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (showFinishedSection) {
                            items(finishedPayments) { payment ->
                                ReservedPaymentItemCard(
                                    payment = payment,
                                    currencySymbol = currencySymbol,
                                    onPayClick = {},
                                    onClick = {
                                        editingReserved = payment
                                    },
                                    onDelete = {
                                        viewModel.deleteReservedPayment(payment.id)
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // --- Bank Accounts & Cash Management Section ---
                    item {
                        // Net Liquidity / Asset Overview Card
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("liquidity_overview_card"),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Liquid Assets (Bank + Cash)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val totalAssets = totalBankAssets + cashBalance.currentCashOnHand
                                        Text(
                                            text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, totalAssets),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = if (totalAssets >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { showTransferDialog = true },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("open_transfer_dialog_btn")
                                        ) {
                                            Text("Transfer")
                                        }
                                        Button(
                                            onClick = { showAddBankDialog = true },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.testTag("open_add_bank_dialog_btn")
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Bank")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Bank Total Pill
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("In Banks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, totalBankAssets),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Cash On Hand Pill (clickable to jump to denomination counter)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedTab = 2 }
                                            .testTag("cash_pill_jump_denom"),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Cash on Hand", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("Count ➜", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            }
                                            Text(
                                                text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, cashBalance.currentCashOnHand),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = IncomeGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Configured Bank Accounts (${bankBalances.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (bankBalances.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("No Bank Accounts Added", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "Add your bank accounts to separate cash flow and check balances before recurring payments.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(onClick = { showAddBankDialog = true }) {
                                        Text("+ Add Bank Account")
                                    }
                                }
                            }
                        }
                    } else {
                        items(bankBalances) { balance ->
                            BankAccountCard(
                                balance = balance,
                                currencySymbol = currencySymbol,
                                onClick = {
                                    editingBank = balance.account
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(70.dp)) // padding for bottom nav & FAB
                }
            }
        }
    }

    // Dialogs
    if (showAddBankDialog || editingBank != null) {
        AddEditBankDialog(
            bankAccount = editingBank,
            onDismiss = {
                showAddBankDialog = false
                editingBank = null
            },
            onSave = { name, initialBalance, maskedNumber, colorHex ->
                if (editingBank == null) {
                    viewModel.addBankAccount(name, initialBalance, maskedNumber, colorHex)
                } else {
                    viewModel.updateBankAccount(
                        editingBank!!.copy(
                            bankName = name,
                            initialBalance = initialBalance,
                            accountNumberMasked = maskedNumber,
                            colorHex = colorHex
                        )
                    )
                }
                showAddBankDialog = false
                editingBank = null
            },
            onArchive = { bankId ->
                viewModel.archiveBankAccount(bankId)
                editingBank = null
            }
        )
    }

    if (showTransferDialog) {
        TransferMoneyDialog(
            bankBalances = bankBalances,
            cashBalance = cashBalance,
            currencySymbol = currencySymbol,
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromType, fromBankId, fromBankName, toType, toBankId, toBankName, amount, note, timestamp ->
                viewModel.transferMoney(
                    fromType = fromType,
                    fromBankId = fromBankId,
                    fromBankName = fromBankName,
                    toType = toType,
                    toBankId = toBankId,
                    toBankName = toBankName,
                    amount = amount,
                    note = note,
                    timestamp = timestamp
                )
            }
        )
    }

    if (showAddReservedDialog || editingReserved != null) {
        AddEditReservedPaymentDialog(
            reservedPayment = editingReserved,
            bankAccounts = allBankAccounts,
            currencySymbol = currencySymbol,
            onDismiss = {
                showAddReservedDialog = false
                editingReserved = null
            },
            onSave = { title, amount, dueDate, category, accountSource, bankId, bankName, freq, reminderDays, totalInstallments, currentInstallment, specificDayOfMonth, note ->
                if (editingReserved == null) {
                    viewModel.addReservedPayment(
                        title = title,
                        amount = amount,
                        dueDate = dueDate,
                        category = category,
                        accountSourceType = accountSource,
                        bankAccountId = bankId,
                        bankAccountName = bankName,
                        frequency = freq,
                        reminderDaysBefore = reminderDays,
                        note = note,
                        totalInstallments = totalInstallments,
                        currentInstallment = currentInstallment,
                        specificDayOfMonth = specificDayOfMonth
                    )
                } else {
                    viewModel.updateReservedPayment(
                        editingReserved!!.copy(
                            title = title,
                            amount = amount,
                            dueDate = dueDate,
                            category = category,
                            accountSourceType = accountSource,
                            bankAccountId = bankId,
                            bankAccountName = bankName,
                            frequency = freq,
                            reminderDaysBefore = reminderDays,
                            totalInstallments = totalInstallments,
                            currentInstallment = currentInstallment,
                            specificDayOfMonth = specificDayOfMonth,
                            note = note
                        )
                    )
                }
                showAddReservedDialog = false
                editingReserved = null
            },
            onDelete = { id ->
                viewModel.deleteReservedPayment(id)
                editingReserved = null
            }
        )
    }
}
