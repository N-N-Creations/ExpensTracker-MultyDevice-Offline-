package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.BalanceSummaryCard
import com.example.ui.components.MonthlyBudgetAlertBanner
import com.example.ui.components.PeriodFilterBar
import com.example.ui.components.ReservedPaymentReminderBanner
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.TimePeriodFilter
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    onNavigateToBudget: () -> Unit,
    onNavigateToSync: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val filteredTxs by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val periodSummary by viewModel.currentPeriodSummary.collectAsStateWithLifecycle()
    val budgetStatus by viewModel.monthlyBudgetStatus.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val reminderAlerts by viewModel.reservedReminders.collectAsStateWithLifecycle()

    val activeCurrencyCode by viewModel.activeCurrencyCode.collectAsStateWithLifecycle()
    var currencyManagerVisible by remember { mutableStateOf(false) }

    var liveDateStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val format = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            liveDateStr = format.format(Date())
            delay(10000)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Expenses & Income",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (liveDateStr.isNotEmpty()) {
                            Text(
                                text = liveDateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Currency Quick Selector Chip
                    Surface(
                        modifier = Modifier
                            .clickable { currencyManagerVisible = true }
                            .padding(end = 8.dp)
                            .testTag("top_bar_currency_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (activeCurrencyCode == "SAR") "🇸🇦 SAR" else "$activeCurrencyCode ($currencySymbol)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Dedicated Sync Device button in Top Bar
                    Surface(
                        modifier = Modifier
                            .clickable { onNavigateToSync() }
                            .padding(end = 12.dp)
                            .testTag("top_bar_sync_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Devices",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sync",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // High-priority Tabby / Tamara & Low Bank Balance Reminder Alerts
            item {
                ReservedPaymentReminderBanner(
                    alerts = reminderAlerts,
                    currencySymbol = currencySymbol,
                    onPayClick = { payment ->
                        viewModel.markReservedPaymentAsPaid(payment.id)
                    }
                )
            }

            // Net Balance & Income/Expense Overview Card (factoring in committed bills)
            item {
                BalanceSummaryCard(
                    income = periodSummary.totalIncome,
                    expense = periodSummary.totalExpense,
                    netBalance = periodSummary.netBalance,
                    currencySymbol = currencySymbol,
                    reservedCommitted = periodSummary.totalReservedCommitted
                )
            }

            // Monthly Budget Alert Banner
            item {
                MonthlyBudgetAlertBanner(
                    budgetCalc = budgetStatus,
                    currencySymbol = currencySymbol,
                    onConfigureBudgetClick = onNavigateToBudget
                )
            }

            // Period Filter Selector Bar
            item {
                PeriodFilterBar(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { viewModel.setPeriod(it) }
                )
            }

            // Search and Type Filter Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search note, category, bank...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_bar_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Type Filter Chips: All, Expense, Income
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val next = when (filterType) {
                                    null -> TransactionType.EXPENSE
                                    TransactionType.EXPENSE -> TransactionType.INCOME
                                    TransactionType.INCOME -> null
                                }
                                viewModel.setFilterType(next)
                            }
                            .testTag("type_filter_toggle"),
                        shape = RoundedCornerShape(12.dp),
                        color = when (filterType) {
                            TransactionType.EXPENSE -> ExpenseRed.copy(alpha = 0.15f)
                            TransactionType.INCOME -> IncomeGreen.copy(alpha = 0.15f)
                            null -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (filterType) {
                                    TransactionType.EXPENSE -> Icons.Default.ArrowUpward
                                    TransactionType.INCOME -> Icons.Default.ArrowDownward
                                    null -> Icons.Default.Tune
                                },
                                contentDescription = "Filter",
                                tint = when (filterType) {
                                    TransactionType.EXPENSE -> ExpenseRed
                                    TransactionType.INCOME -> IncomeGreen
                                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (filterType) {
                                    TransactionType.EXPENSE -> "Expense"
                                    TransactionType.INCOME -> "Income"
                                    null -> "All"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = when (filterType) {
                                    TransactionType.EXPENSE -> ExpenseRed
                                    TransactionType.INCOME -> IncomeGreen
                                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            // Transactions Header & Count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions (${filteredTxs.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (filterType != null || searchQuery.isNotEmpty()) {
                        Text(
                            text = "Reset Filters",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.setFilterType(null)
                                viewModel.setSearchQuery("")
                                viewModel.setFilterCategory(null)
                            }
                        )
                    }
                }
            }

            // Empty State
            if (filteredTxs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Transactions Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap the + button below to log cash or bank expenses and income.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredTxs,
                    key = { it.id }
                ) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        currencySymbol = currencySymbol,
                        onClick = { onEditTransactionClick(tx) },
                        onDelete = { viewModel.deleteTransaction(tx.id) }
                    )
                }
            }
        }
    }

    if (currencyManagerVisible) {
        CurrencyManagerDialog(
            viewModel = viewModel,
            onDismiss = { currencyManagerVisible = false }
        )
    }
}
