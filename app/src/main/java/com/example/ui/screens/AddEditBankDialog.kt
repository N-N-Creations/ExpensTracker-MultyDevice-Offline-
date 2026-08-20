package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccount

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditBankDialog(
    bankAccount: BankAccount? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, initialBalance: Double, maskedNumber: String, colorHex: Long) -> Unit,
    onArchive: ((String) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isImeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    var bankName by remember { mutableStateOf(bankAccount?.bankName ?: "") }
    var initialBalanceText by remember { mutableStateOf(bankAccount?.initialBalance?.toString() ?: "") }
    var maskedNumber by remember { mutableStateOf(bankAccount?.accountNumberMasked ?: "") }
    var selectedColor by remember { mutableLongStateOf(bankAccount?.colorHex ?: 0xFF1976D2) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val colorOptions = listOf(
        0xFF1976D2, // Blue
        0xFF1B5E20, // Green
        0xFF0D47A1, // Deep Blue
        0xFF6A1B9A, // Purple
        0xFFC2185B, // Pink
        0xFFE65100, // Orange
        0xFF00695C, // Teal
        0xFF37474F  // Charcoal
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_edit_bank_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = Color(selectedColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (bankAccount == null) "Add Bank Account" else "Edit Bank Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = {
                        bankName = it
                        errorText = null
                    },
                    label = { Text("Bank Name (e.g. Al Rajhi, SNB, Chase)") },
                    placeholder = { Text("e.g. Al Rajhi Bank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_name_input")
                )

                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = {
                        initialBalanceText = it
                        errorText = null
                    },
                    label = { Text("Starting / Current Balance") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_balance_input")
                )

                OutlinedTextField(
                    value = maskedNumber,
                    onValueChange = { maskedNumber = it },
                    label = { Text("Account / IBAN Ending (Optional)") },
                    placeholder = { Text("e.g. •••• 4128") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_account_number_input")
                )

                Text(
                    text = "Bank Color Card",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colorOptions) { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(colorHex), CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bankName.isBlank()) {
                        errorText = "Please enter bank name"
                        return@Button
                    }
                    val balance = initialBalanceText.toDoubleOrNull() ?: 0.0
                    onSave(bankName.trim(), balance, maskedNumber.trim(), selectedColor)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_bank_button")
            ) {
                Text("Save Account")
            }
        },
        dismissButton = {
            Row {
                if (bankAccount != null && onArchive != null) {
                    TextButton(
                        onClick = {
                            onArchive(bankAccount.id)
                            onDismiss()
                        }
                    ) {
                        Text("Archive", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
