package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.model.BankAccountBalance
import com.example.data.model.CashBalance
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransferMoneyDialog(
    bankBalances: List<BankAccountBalance>,
    cashBalance: CashBalance,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onTransfer: (
        fromType: AccountSourceType,
        fromBankId: String?,
        fromBankName: String?,
        toType: AccountSourceType,
        toBankId: String?,
        toBankName: String?,
        amount: Double,
        note: String,
        timestamp: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isImeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // From Account state
    var fromType by remember {
        mutableStateOf(if (bankBalances.isNotEmpty()) AccountSourceType.BANK else AccountSourceType.CASH)
    }
    var fromBankId by remember {
        mutableStateOf(bankBalances.firstOrNull()?.account?.id)
    }

    // To Account state (default to Cash if from is Bank, or first Bank if from is Cash)
    var toType by remember {
        mutableStateOf(if (fromType == AccountSourceType.BANK) AccountSourceType.CASH else AccountSourceType.BANK)
    }
    var toBankId by remember {
        mutableStateOf(
            if (fromType == AccountSourceType.BANK && bankBalances.size > 1) {
                bankBalances.getOrNull(1)?.account?.id
            } else {
                bankBalances.firstOrNull()?.account?.id
            }
        )
    }

    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var fromBankDropdownExpanded by remember { mutableStateOf(false) }
    var toBankDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    fun showDateTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance()
                pickedCal.set(Calendar.YEAR, year)
                pickedCal.set(Calendar.MONTH, month)
                pickedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        pickedCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        pickedCal.set(Calendar.MINUTE, minute)
                        timestamp = pickedCal.timeInMillis
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Compute Source & Target Available Balances
    val fromBalance = if (fromType == AccountSourceType.CASH) {
        cashBalance.currentCashOnHand
    } else {
        bankBalances.firstOrNull { it.account.id == fromBankId }?.currentBalance ?: 0.0
    }

    val toBalance = if (toType == AccountSourceType.CASH) {
        cashBalance.currentCashOnHand
    } else {
        bankBalances.firstOrNull { it.account.id == toBankId }?.currentBalance ?: 0.0
    }

    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0
    val fromAfter = fromBalance - enteredAmount
    val toAfter = toBalance + enteredAmount

    // Check if transferring to same account
    val isSameAccount = (fromType == toType) && (fromType == AccountSourceType.CASH || fromBankId == toBankId)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("transfer_money_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Transfer Money",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Move funds between Cash & Banks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quick preset note templates
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = fromType == AccountSourceType.BANK && toType == AccountSourceType.CASH,
                        onClick = {
                            fromType = AccountSourceType.BANK
                            toType = AccountSourceType.CASH
                            if (note.isBlank() || note.contains("Deposit")) note = "ATM Cash Withdrawal"
                        },
                        label = { Text("ATM Withdrawal (Bank ➜ Cash)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = fromType == AccountSourceType.CASH && toType == AccountSourceType.BANK,
                        onClick = {
                            fromType = AccountSourceType.CASH
                            toType = AccountSourceType.BANK
                            if (note.isBlank() || note.contains("Withdrawal")) note = "Cash Bank Deposit"
                        },
                        label = { Text("Cash Deposit (Cash ➜ Bank)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // FROM SECTION
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FROM (Source)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Avail: $currencySymbol${String.format(Locale.getDefault(), "%.2f", fromBalance)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (fromBalance >= 0) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = fromType == AccountSourceType.CASH,
                                onClick = {
                                    fromType = AccountSourceType.CASH
                                    errorText = null
                                },
                                label = { Text("Cash on Hand") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = fromType == AccountSourceType.BANK,
                                onClick = {
                                    fromType = AccountSourceType.BANK
                                    errorText = null
                                },
                                label = { Text("Bank") },
                                leadingIcon = { Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (fromType == AccountSourceType.BANK && bankBalances.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = fromBankDropdownExpanded,
                                onExpandedChange = { fromBankDropdownExpanded = !fromBankDropdownExpanded }
                            ) {
                                val selectedBank = bankBalances.firstOrNull { it.account.id == fromBankId } ?: bankBalances.first()
                                OutlinedTextField(
                                    value = "${selectedBank.account.bankName} ($currencySymbol${String.format(Locale.getDefault(), "%.2f", selectedBank.currentBalance)})",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Source Bank") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromBankDropdownExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = fromBankDropdownExpanded,
                                    onDismissRequest = { fromBankDropdownExpanded = false }
                                ) {
                                    bankBalances.forEach { b ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(b.account.bankName, fontWeight = FontWeight.Bold)
                                                    Text("Balance: $currencySymbol${String.format(Locale.getDefault(), "%.2f", b.currentBalance)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            onClick = {
                                                fromBankId = b.account.id
                                                fromBankDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SWAP BUTTON
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            val tempType = fromType
                            val tempBankId = fromBankId
                            fromType = toType
                            fromBankId = toBankId
                            toType = tempType
                            toBankId = tempBankId
                            errorText = null
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .size(36.dp)
                            .testTag("swap_transfer_direction_btn")
                    ) {
                        Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Swap accounts", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                    }
                }

                // TO SECTION
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TO (Destination)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = IncomeGreen
                            )
                            Text(
                                text = "Avail: $currencySymbol${String.format(Locale.getDefault(), "%.2f", toBalance)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (toBalance >= 0) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = toType == AccountSourceType.CASH,
                                onClick = {
                                    toType = AccountSourceType.CASH
                                    errorText = null
                                },
                                label = { Text("Cash on Hand") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = toType == AccountSourceType.BANK,
                                onClick = {
                                    toType = AccountSourceType.BANK
                                    errorText = null
                                },
                                label = { Text("Bank") },
                                leadingIcon = { Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (toType == AccountSourceType.BANK && bankBalances.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = toBankDropdownExpanded,
                                onExpandedChange = { toBankDropdownExpanded = !toBankDropdownExpanded }
                            ) {
                                val selectedBank = bankBalances.firstOrNull { it.account.id == toBankId } ?: bankBalances.first()
                                OutlinedTextField(
                                    value = "${selectedBank.account.bankName} ($currencySymbol${String.format(Locale.getDefault(), "%.2f", selectedBank.currentBalance)})",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Destination Bank") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toBankDropdownExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = toBankDropdownExpanded,
                                    onDismissRequest = { toBankDropdownExpanded = false }
                                ) {
                                    bankBalances.forEach { b ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(b.account.bankName, fontWeight = FontWeight.Bold)
                                                    Text("Balance: $currencySymbol${String.format(Locale.getDefault(), "%.2f", b.currentBalance)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            onClick = {
                                                toBankId = b.account.id
                                                toBankDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // AMOUNT INPUT
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorText = null
                    },
                    label = { Text("Transfer Amount ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                )

                // Quick Amount chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(50.0, 100.0, 200.0, 500.0, 1000.0).forEach { amt ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                amountText = String.format(Locale.getDefault(), "%.2f", amt)
                                errorText = null
                            },
                            label = { Text("$currencySymbol$amt") }
                        )
                    }
                    if (fromBalance > 0) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                amountText = String.format(Locale.getDefault(), "%.2f", fromBalance)
                                errorText = null
                            },
                            label = { Text("Max ($currencySymbol${String.format(Locale.getDefault(), "%.2f", fromBalance)})") }
                        )
                    }
                }

                // LIVE BALANCE PREVIEW
                if (enteredAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Balance After Transfer:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Source (From):", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", fromAfter)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (fromAfter >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Destination (To):", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", toAfter)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = IncomeGreen
                                )
                            }
                        }
                    }
                }

                // NOTE INPUT
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Memo (Optional)") },
                    placeholder = { Text("e.g. ATM withdrawal, rent transfer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transfer_note_input")
                )

                // DATE & TIME PICKER
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDateTimePicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dateFormat.format(Date(timestamp)), style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Change", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (isSameAccount) {
                    Text(
                        text = "Source and destination accounts must be different.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSameAccount) {
                        errorText = "Source and destination cannot be the same account"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorText = "Please enter a valid transfer amount > 0"
                        return@Button
                    }

                    val fromBank = if (fromType == AccountSourceType.BANK) {
                        bankBalances.firstOrNull { it.account.id == fromBankId }?.account
                    } else null

                    val toBank = if (toType == AccountSourceType.BANK) {
                        bankBalances.firstOrNull { it.account.id == toBankId }?.account
                    } else null

                    onTransfer(
                        fromType,
                        fromBank?.id,
                        fromBank?.bankName,
                        toType,
                        toBank?.id,
                        toBank?.bankName,
                        amount,
                        note.trim(),
                        timestamp
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_transfer_button"),
                enabled = !isSameAccount && enteredAmount > 0
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
