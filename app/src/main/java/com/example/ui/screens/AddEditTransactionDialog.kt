package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountSourceType
import com.example.data.model.BankAccount
import com.example.data.model.CategoryConstants
import com.example.data.model.PaymentMethod
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionBottomSheet(
    initialTransaction: Transaction? = null,
    bankAccounts: List<BankAccount> = emptyList(),
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddBankClick: () -> Unit,
    onSave: (
        amount: Double,
        type: TransactionType,
        category: String,
        note: String,
        timestamp: Long,
        paymentMethod: String,
        accountSourceType: AccountSourceType,
        bankAccountId: String?,
        bankAccountName: String?
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf(initialTransaction?.amount?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category ?: if (type == TransactionType.EXPENSE) "Food & Dining" else "Salary"
        )
    }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var timestamp by remember { mutableLongStateOf(initialTransaction?.timestamp ?: System.currentTimeMillis()) }
    var paymentMethod by remember { mutableStateOf(initialTransaction?.paymentMethod ?: PaymentMethod.CASH.displayName) }
    var accountSourceType by remember { mutableStateOf(initialTransaction?.accountSourceType ?: AccountSourceType.CASH) }
    var selectedBankId by remember { mutableStateOf(initialTransaction?.bankAccountId ?: bankAccounts.firstOrNull()?.id) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var bankDropdownExpanded by remember { mutableStateOf(false) }

    // Dynamic Live Clock Ticker when adding new transaction
    var isLiveClock by remember { mutableStateOf(initialTransaction == null) }
    var liveFormattedTime by remember { mutableStateOf("") }

    LaunchedEffect(isLiveClock) {
        while (isLiveClock) {
            val now = System.currentTimeMillis()
            timestamp = now
            liveFormattedTime = SimpleDateFormat("EEE, MMM d, yyyy • h:mm:ss a", Locale.getDefault()).format(Date(now))
            delay(1000)
        }
    }

    val displayDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    val displayTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

    fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                timestamp = pickedCal.timeInMillis
                isLiveClock = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val pickedCal = Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                timestamp = pickedCal.timeInMillis
                isLiveClock = false
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    val categories = if (type == TransactionType.EXPENSE) {
        CategoryConstants.expenseCategories
    } else {
        CategoryConstants.incomeCategories
    }

    val paymentMethods = listOf(
        PaymentMethod.CASH.displayName,
        PaymentMethod.DEBIT_CARD.displayName,
        PaymentMethod.CREDIT_CARD.displayName,
        PaymentMethod.BANK_TRANSFER.displayName,
        PaymentMethod.DIGITAL_WALLET.displayName,
        PaymentMethod.BNPL_TABBY_TAMARA.displayName,
        PaymentMethod.OTHER.displayName
    )

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible

    // Intercept back button / gesture when software keyboard is visible so that pressing back closes the keyboard first
    BackHandler(enabled = isImeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier.testTag("add_edit_bottom_sheet")
    ) {
        // Also inside the bottom sheet content to handle sheet window focus
        BackHandler(enabled = isImeVisible) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (initialTransaction == null) "New Entry" else "Edit Entry",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (initialTransaction?.deviceName?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Created on ${initialTransaction.deviceName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row {
                    if (initialTransaction != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(initialTransaction.id)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("delete_transaction_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Income / Expense Toggle Segment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Expense Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (type == TransactionType.EXPENSE) ExpenseRed else Color.Transparent)
                        .clickable {
                            type = TransactionType.EXPENSE
                            selectedCategory = CategoryConstants.expenseCategories.first().name
                        }
                        .testTag("toggle_expense_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (type == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (type == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Income Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (type == TransactionType.INCOME) IncomeGreen else Color.Transparent)
                        .clickable {
                            type = TransactionType.INCOME
                            selectedCategory = CategoryConstants.incomeCategories.first().name
                        }
                        .testTag("toggle_income_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (type == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (type == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                errorText = null
                            },
                            placeholder = { Text("0.00", style = MaterialTheme.typography.headlineMedium) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transaction_amount_input")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account Source: Cash vs Bank Account
            Text(
                text = "Account Source (Cash or Bank):",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = accountSourceType == AccountSourceType.CASH,
                    onClick = {
                        accountSourceType = AccountSourceType.CASH
                        paymentMethod = PaymentMethod.CASH.displayName
                    },
                    label = { Text("💵 Cash On Hand") },
                    modifier = Modifier.weight(1f).testTag("select_cash_source")
                )
                FilterChip(
                    selected = accountSourceType == AccountSourceType.BANK,
                    onClick = {
                        accountSourceType = AccountSourceType.BANK
                        if (paymentMethod == PaymentMethod.CASH.displayName) {
                            paymentMethod = PaymentMethod.DEBIT_CARD.displayName
                        }
                    },
                    label = { Text("🏦 Bank Account") },
                    modifier = Modifier.weight(1f).testTag("select_bank_source")
                )
            }

            // Bank selection if Bank source is chosen
            if (accountSourceType == AccountSourceType.BANK) {
                Spacer(modifier = Modifier.height(10.dp))
                if (bankAccounts.isEmpty()) {
                    OutlinedButton(
                        onClick = onAddBankClick,
                        modifier = Modifier.fillMaxWidth().testTag("add_bank_shortcut_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("No Bank Accounts Found — Add Bank Now")
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = bankDropdownExpanded,
                        onExpandedChange = { bankDropdownExpanded = !bankDropdownExpanded }
                    ) {
                        val selectedBank = bankAccounts.firstOrNull { it.id == selectedBankId } ?: bankAccounts.first()
                        OutlinedTextField(
                            value = selectedBank.bankName + if (selectedBank.accountNumberMasked.isNotBlank()) " (${selectedBank.accountNumberMasked})" else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Bank Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("bank_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = bankDropdownExpanded,
                            onDismissRequest = { bankDropdownExpanded = false }
                        ) {
                            bankAccounts.forEach { bank ->
                                DropdownMenuItem(
                                    text = { Text(bank.bankName + if (bank.accountNumberMasked.isNotBlank()) " (${bank.accountNumberMasked})" else "") },
                                    onClick = {
                                        selectedBankId = bank.id
                                        bankDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Add Another Bank...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    bankDropdownExpanded = false
                                    onAddBankClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Date & Time Bar
            Text(
                text = "Date & Time (Dynamic):",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Picker Chip
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker() }
                        .testTag("date_picker_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Time Picker Chip
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker() }
                        .testTag("time_picker_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                    val catColor = Color(cat.colorHex)

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = cat.name }
                            .testTag("cat_chip_${cat.id}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) catColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(cat.iconName),
                                contentDescription = cat.name,
                                tint = if (isSelected) Color.White else catColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(paymentMethods) { method ->
                    val isSelected = paymentMethod == method
                    FilterChip(
                        selected = isSelected,
                        onClick = { paymentMethod = method },
                        label = { Text(method, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Mention what it's for)") },
                placeholder = { Text("e.g. Tabby installment, groceries from supermarket, bonus...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input"),
                singleLine = false,
                maxLines = 3
            )

            if (errorText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorText = "Please enter a valid amount > 0"
                        return@Button
                    }

                    val chosenBank = if (accountSourceType == AccountSourceType.BANK) {
                        bankAccounts.firstOrNull { it.id == selectedBankId } ?: bankAccounts.firstOrNull()
                    } else null

                    onSave(
                        amount,
                        type,
                        selectedCategory,
                        note.trim(),
                        timestamp,
                        paymentMethod,
                        accountSourceType,
                        chosenBank?.id,
                        chosenBank?.bankName
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTransaction == null) "Save Entry" else "Update Entry",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
