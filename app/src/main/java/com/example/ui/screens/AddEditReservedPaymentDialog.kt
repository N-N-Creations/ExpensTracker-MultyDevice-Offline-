package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountSourceType
import com.example.data.model.BankAccount
import com.example.data.model.CategoryConstants
import com.example.data.model.RecurringFrequency
import com.example.data.model.ReservedPayment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditReservedPaymentDialog(
    reservedPayment: ReservedPayment? = null,
    bankAccounts: List<BankAccount> = emptyList(),
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        dueDate: Long,
        category: String,
        accountSourceType: AccountSourceType,
        bankAccountId: String?,
        bankAccountName: String?,
        frequency: RecurringFrequency,
        reminderDaysBefore: Int,
        totalInstallments: Int,
        currentInstallment: Int,
        specificDayOfMonth: Int,
        note: String
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isImeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    var title by remember { mutableStateOf(reservedPayment?.title ?: "") }
    var amountText by remember { mutableStateOf(reservedPayment?.amount?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "") }
    var dueDate by remember {
        mutableLongStateOf(
            reservedPayment?.dueDate ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 5) }.timeInMillis
        )
    }
    var selectedCategory by remember { mutableStateOf(reservedPayment?.category ?: "Tabby / Tamara (BNPL)") }
    var accountSourceType by remember { mutableStateOf(reservedPayment?.accountSourceType ?: AccountSourceType.BANK) }
    var selectedBankId by remember { mutableStateOf(reservedPayment?.bankAccountId ?: bankAccounts.firstOrNull()?.id) }
    var frequency by remember { mutableStateOf(reservedPayment?.frequency ?: RecurringFrequency.MONTHLY) }
    var totalInstallments by remember { mutableIntStateOf(reservedPayment?.totalInstallments ?: 1) }
    var currentInstallment by remember { mutableIntStateOf(reservedPayment?.currentInstallment ?: 1) }
    var specificDayOfMonth by remember { mutableIntStateOf(reservedPayment?.specificDayOfMonth ?: 0) }
    var reminderDaysBefore by remember { mutableIntStateOf(reservedPayment?.reminderDaysBefore ?: 3) }
    var note by remember { mutableStateOf(reservedPayment?.note ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    var isCustomInstallment by remember { mutableStateOf(totalInstallments !in listOf(1, 3, 4, 6, 12)) }
    var customInstallmentText by remember { mutableStateOf(if (isCustomInstallment) totalInstallments.toString() else "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var bankDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    fun updateDueDateForDayOfMonth(day: Int) {
        if (day in 1..31) {
            val nowCal = Calendar.getInstance()
            val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
            val cal = Calendar.getInstance()
            val maxDayThisMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val effectiveDay = day.coerceAtMost(maxDayThisMonth)

            cal.set(Calendar.DAY_OF_MONTH, effectiveDay)
            cal.set(Calendar.HOUR_OF_DAY, 10)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)

            // If day has already passed this month, schedule for next month
            if (cal.timeInMillis < System.currentTimeMillis()) {
                cal.add(Calendar.MONTH, 1)
                val maxDayNextMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(maxDayNextMonth))
            }
            dueDate = cal.timeInMillis
        }
    }

    fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = dueDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance()
                pickedCal.set(year, month, dayOfMonth, 10, 0, 0)
                dueDate = pickedCal.timeInMillis
                specificDayOfMonth = dayOfMonth
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_edit_reserved_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreditScore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (reservedPayment == null) "Reserve Future Payment" else "Edit Reserved Payment",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (reservedPayment != null && onDelete != null) {
                    IconButton(onClick = {
                        onDelete(reservedPayment.id)
                        onDismiss()
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
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
                // Quick preset templates for Tabby / Tamara / Bills
                Text("Quick Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = totalInstallments == 4 && frequency == RecurringFrequency.MONTHLY,
                        onClick = {
                            if (title.isBlank() || title.contains("Installment") || title.contains("Tamara") || title.contains("Rent")) {
                                title = "Tabby Installment"
                            }
                            selectedCategory = "Tabby / Tamara (BNPL)"
                            frequency = RecurringFrequency.MONTHLY
                            totalInstallments = 4
                            currentInstallment = 1
                            isCustomInstallment = false
                        },
                        label = { Text("Tabby (4x Monthly)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = totalInstallments == 3 && frequency == RecurringFrequency.MONTHLY,
                        onClick = {
                            if (title.isBlank() || title.contains("Installment") || title.contains("Tabby") || title.contains("Rent")) {
                                title = "Tamara Installment"
                            }
                            selectedCategory = "Tabby / Tamara (BNPL)"
                            frequency = RecurringFrequency.MONTHLY
                            totalInstallments = 3
                            currentInstallment = 1
                            isCustomInstallment = false
                        },
                        label = { Text("Tamara (3x Monthly)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = totalInstallments == 12 && frequency == RecurringFrequency.MONTHLY,
                        onClick = {
                            if (title.isBlank() || title.contains("Installment") || title.contains("Tabby") || title.contains("Tamara")) {
                                title = "Apartment Rent / EMI"
                            }
                            selectedCategory = "Housing & Rent"
                            frequency = RecurringFrequency.MONTHLY
                            totalInstallments = 12
                            currentInstallment = 1
                            isCustomInstallment = false
                        },
                        label = { Text("Rent (12x Monthly)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = totalInstallments == 1 && frequency == RecurringFrequency.ONCE,
                        onClick = {
                            if (title.isBlank() || title.contains("Installment")) {
                                title = "One-Time Bill / Purchase"
                            }
                            frequency = RecurringFrequency.ONCE
                            totalInstallments = 1
                            currentInstallment = 1
                            isCustomInstallment = false
                        },
                        label = { Text("One-Time Only") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorText = null
                    },
                    label = { Text("Title / Purpose (e.g. Tabby iPhone, Rent)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reserved_title_input")
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorText = null
                    },
                    label = { Text("Amount Per Installment ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reserved_amount_input")
                )

                // NUMBER OF TIMES TO REPEAT / INSTALLMENTS
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("How many times will it repeat? (Installments)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1 to "1x (Once)", 3 to "3x (Tamara)", 4 to "4x (Tabby)", 6 to "6x", 12 to "12x (1 Yr)").forEach { (count, label) ->
                                FilterChip(
                                    selected = !isCustomInstallment && totalInstallments == count,
                                    onClick = {
                                        totalInstallments = count
                                        isCustomInstallment = false
                                        if (count == 1) {
                                            frequency = RecurringFrequency.ONCE
                                        } else if (frequency == RecurringFrequency.ONCE) {
                                            frequency = RecurringFrequency.MONTHLY
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                            FilterChip(
                                selected = isCustomInstallment,
                                onClick = {
                                    isCustomInstallment = true
                                    if (customInstallmentText.isBlank()) customInstallmentText = totalInstallments.toString()
                                },
                                label = { Text("Custom Count") }
                            )
                        }

                        if (isCustomInstallment) {
                            OutlinedTextField(
                                value = customInstallmentText,
                                onValueChange = {
                                    customInstallmentText = it.filter { ch -> ch.isDigit() }
                                    totalInstallments = customInstallmentText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                },
                                label = { Text("Total Number of Repetitions (e.g. 24)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("custom_installments_input")
                            )
                        }
                    }
                }

                // PAYMENT DATE & DAY OF MONTH SPECIFICATION
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("On what date should it pay?", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        // Quick Day of Month Buttons
                        Text("Day of the Month:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                                FilterChip(
                                    selected = specificDayOfMonth == day,
                                    onClick = {
                                        specificDayOfMonth = day
                                        updateDueDateForDayOfMonth(day)
                                    },
                                    label = { Text("Day $day") }
                                )
                            }
                        }

                        // Calendar Due Date Picker button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker() }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Next Payment Due Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(dateFormat.format(Date(dueDate)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                                Text("Select Date", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Account Source: Bank vs Cash
                Text("Deduct from Account:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = accountSourceType == AccountSourceType.BANK,
                        onClick = { accountSourceType = AccountSourceType.BANK },
                        label = { Text("Bank Account") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = accountSourceType == AccountSourceType.CASH,
                        onClick = { accountSourceType = AccountSourceType.CASH },
                        label = { Text("Cash on Hand") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // If Bank selected and banks exist, choose bank account
                if (accountSourceType == AccountSourceType.BANK && bankAccounts.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = bankDropdownExpanded,
                        onExpandedChange = { bankDropdownExpanded = !bankDropdownExpanded }
                    ) {
                        val selectedBank = bankAccounts.firstOrNull { it.id == selectedBankId } ?: bankAccounts.first()
                        OutlinedTextField(
                            value = selectedBank.bankName + if (selectedBank.accountNumberMasked.isNotBlank()) " (${selectedBank.accountNumberMasked})" else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Bank for Auto-Debit/Check") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = bankDropdownExpanded,
                            onDismissRequest = { bankDropdownExpanded = false }
                        ) {
                            bankAccounts.forEach { bank ->
                                DropMenuItemCustom(
                                    bank = bank,
                                    onClick = {
                                        selectedBankId = bank.id
                                        bankDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Advance Reminder Days
                Text("Advance Reminder Alert:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 5, 7).forEach { days ->
                        FilterChip(
                            selected = reminderDaysBefore == days,
                            onClick = { reminderDaysBefore = days },
                            label = { Text("$days Days Before") },
                            leadingIcon = {
                                if (reminderDaysBefore == days) {
                                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. Ensure funds ready before 15th)") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorText = "Please enter payment title"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorText = "Please enter a valid amount > 0"
                        return@Button
                    }

                    val chosenBank = if (accountSourceType == AccountSourceType.BANK) {
                        bankAccounts.firstOrNull { it.id == selectedBankId } ?: bankAccounts.firstOrNull()
                    } else null

                    val finalInstallments = if (isCustomInstallment) {
                        customInstallmentText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    } else {
                        totalInstallments
                    }

                    val finalFreq = if (finalInstallments > 1 && frequency == RecurringFrequency.ONCE) {
                        RecurringFrequency.MONTHLY
                    } else {
                        frequency
                    }

                    onSave(
                        title.trim(),
                        amount,
                        dueDate,
                        selectedCategory,
                        accountSourceType,
                        chosenBank?.id,
                        chosenBank?.bankName,
                        finalFreq,
                        reminderDaysBefore,
                        finalInstallments,
                        currentInstallment,
                        specificDayOfMonth,
                        note.trim()
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_reserved_button")
            ) {
                Text(if (reservedPayment == null) "Reserve & Schedule" else "Update Reservation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DropMenuItemCustom(bank: BankAccount, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(bank.bankName + if (bank.accountNumberMasked.isNotBlank()) " (${bank.accountNumberMasked})" else "") },
        onClick = onClick
    )
}
