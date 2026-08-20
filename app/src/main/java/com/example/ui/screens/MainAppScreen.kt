package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccountSourceType
import com.example.data.model.BankAccount
import com.example.data.model.Transaction
import com.example.ui.viewmodel.ExpenseViewModel

sealed class ScreenNav(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Home : ScreenNav("home", "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home")
    object Accounts : ScreenNav("accounts", "Accounts", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance, "nav_accounts")
    object Charts : ScreenNav("charts", "Charts", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_charts")
    object Budget : ScreenNav("budget", "Budget", Icons.Filled.Savings, Icons.Outlined.Savings, "nav_budget")
    object Reports : ScreenNav("reports", "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports")
    object Sync : ScreenNav("sync", "Sync", Icons.Filled.Sync, Icons.Outlined.Sync, "nav_sync")
}

@Composable
fun MainAppScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableIntStateOf(0) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var showAddBankDirectly by remember { mutableStateOf(false) }

    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val allBankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()

    val navItems = listOf(
        ScreenNav.Home,
        ScreenNav.Accounts,
        ScreenNav.Charts,
        ScreenNav.Budget,
        ScreenNav.Reports,
        ScreenNav.Sync
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = currentTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            0 -> HomeScreen(
                viewModel = viewModel,
                onNavigateToBudget = { currentTab = 3 },
                onNavigateToSync = { currentTab = 5 },
                onAddTransactionClick = {
                    editingTransaction = null
                    isAddSheetOpen = true
                },
                onEditTransactionClick = { tx ->
                    editingTransaction = tx
                    isAddSheetOpen = true
                },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> BanksAndReservedScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            2 -> ChartsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            3 -> BudgetScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            4 -> ReportsScreen(
                viewModel = viewModel,
                onEditTransactionClick = { tx ->
                    editingTransaction = tx
                    isAddSheetOpen = true
                },
                modifier = Modifier.padding(innerPadding)
            )
            5 -> SyncBackupScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }

        // Add / Edit Transaction Sheet Modal
        if (isAddSheetOpen) {
            AddEditTransactionBottomSheet(
                initialTransaction = editingTransaction,
                bankAccounts = allBankAccounts,
                currencySymbol = currencySymbol,
                onDismiss = {
                    isAddSheetOpen = false
                    editingTransaction = null
                },
                onAddBankClick = {
                    showAddBankDirectly = true
                },
                onSave = { amount, type, category, note, timestamp, paymentMethod, accountSource, bankId, bankName ->
                    if (editingTransaction != null) {
                        viewModel.updateTransaction(
                            editingTransaction!!.copy(
                                amount = amount,
                                type = type,
                                category = category,
                                note = note,
                                timestamp = timestamp,
                                paymentMethod = paymentMethod,
                                accountSourceType = accountSource,
                                bankAccountId = bankId,
                                bankAccountName = bankName
                            )
                        )
                    } else {
                        viewModel.addTransaction(
                            amount = amount,
                            type = type,
                            category = category,
                            note = note,
                            timestamp = timestamp,
                            paymentMethod = paymentMethod,
                            accountSourceType = accountSource,
                            bankAccountId = bankId,
                            bankAccountName = bankName
                        )
                    }
                    isAddSheetOpen = false
                    editingTransaction = null
                },
                onDelete = { id ->
                    viewModel.deleteTransaction(id)
                }
            )
        }

        if (showAddBankDirectly) {
            AddEditBankDialog(
                onDismiss = { showAddBankDirectly = false },
                onSave = { name, initialBalance, maskedNumber, colorHex ->
                    viewModel.addBankAccount(name, initialBalance, maskedNumber, colorHex)
                    showAddBankDirectly = false
                }
            )
        }
    }
}
