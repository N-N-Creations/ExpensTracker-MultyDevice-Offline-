package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DenominationItem
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.Locale

@Composable
fun CashDenominationSection(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val cashBalance by viewModel.cashBalance.collectAsStateWithLifecycle()
    val trackerState by viewModel.denominationTrackerState.collectAsStateWithLifecycle()

    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val systemCash = cashBalance.currentCashOnHand
    val physicalCash = trackerState.totalCountedCash
    val variance = physicalCash - systemCash
    val isExactMatch = Math.abs(variance) < 0.01

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Comparison & Reconciliation Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("denomination_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isExactMatch -> IncomeGreen.copy(alpha = 0.12f)
                        variance > 0 -> WarningYellow.copy(alpha = 0.15f)
                        else -> ExpenseRed.copy(alpha = 0.12f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cash Reconciliation",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isExactMatch -> IncomeGreen.copy(alpha = 0.2f)
                                variance > 0 -> WarningYellow.copy(alpha = 0.25f)
                                else -> ExpenseRed.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = when {
                                    isExactMatch -> "✓ Balanced"
                                    variance > 0 -> "+ Surplus"
                                    else -> "- Shortage"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = when {
                                    isExactMatch -> IncomeGreen
                                    variance > 0 -> Color(0xFFE65100)
                                    else -> ExpenseRed
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2-Column Comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Physical Count Column
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Physical Counted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, physicalCash),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${trackerState.totalPieces} bills / coins",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // System Cash on Hand Column
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "System Cash on Hand",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, systemCash),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "From ledger transactions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Variance message pill
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isExactMatch) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isExactMatch) IncomeGreen else if (variance > 0) Color(0xFFE65100) else ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isExactMatch -> "Physical cash perfectly matches your system cash balance ($currencySymbol${String.format(Locale.getDefault(), "%.2f", physicalCash)})."
                                    variance > 0 -> "Physical cash is higher by $currencySymbol${String.format(Locale.getDefault(), "%.2f", variance)} than system records."
                                    else -> "Physical cash is short by $currencySymbol${String.format(Locale.getDefault(), "%.2f", Math.abs(variance))} compared to system records."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Independent Disclaimer Note
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Independent Counter: Changing denomination counts will NOT alter your system cash balance or transactions.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Action Toolbar: Quick Reset, Copy Breakdown, Add Denomination
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val sb = StringBuilder()
                        sb.append("💵 Cash Denomination Breakdown\n")
                        sb.append("----------------------------\n")
                        trackerState.items.filter { it.count > 0 }.forEach { item ->
                            val denomLabel = if (item.value >= 1.0) String.format(Locale.getDefault(), "%.0f", item.value) else String.format(Locale.getDefault(), "%.2f", item.value)
                            sb.append("$currencySymbol$denomLabel x ${item.count} = $currencySymbol${String.format(Locale.getDefault(), "%.2f", item.subtotal)}\n")
                        }
                        sb.append("----------------------------\n")
                        sb.append("Total Counted: $currencySymbol${String.format(Locale.getDefault(), "%.2f", physicalCash)} (${trackerState.totalPieces} pieces)\n")
                        sb.append("System Cash: $currencySymbol${String.format(Locale.getDefault(), "%.2f", systemCash)}\n")
                        sb.append("Variance: ${if (variance >= 0) "+" else "-"}$currencySymbol${String.format(Locale.getDefault(), "%.2f", Math.abs(variance))}\n")

                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Cash Breakdown", sb.toString()))
                        Toast.makeText(context, "Breakdown copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("copy_breakdown_btn")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { showResetConfirmDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("reset_denominations_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset (0)", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = { showAddCustomDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.1f).testTag("add_custom_denom_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Value", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Header for Denominations List with Restore Defaults button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Denominations (${trackerState.items.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = {
                            viewModel.restoreDefaultDenominations()
                            Toast.makeText(context, "Restored standard denominations (500, 200, 100, 50, 20, 10, 5, 2, 1)", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Restore Defaults", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(
                    text = "Total: $currencySymbol${String.format(Locale.getDefault(), "%.2f", physicalCash)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Denomination Rows
        items(
            items = trackerState.items,
            key = { it.value }
        ) { item ->
            DenominationRowCard(
                item = item,
                currencySymbol = currencySymbol,
                onCountChange = { newCount ->
                    viewModel.updateDenominationCount(item.value, newCount)
                },
                onIncrement = {
                    viewModel.incrementDenomination(item.value)
                },
                onDecrement = {
                    viewModel.decrementDenomination(item.value)
                },
                onDelete = {
                    viewModel.removeDenomination(item.value)
                }
            )
        }
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Denomination Counts?") },
            text = { Text("This will reset all bill and coin quantities back to 0. (Your system cash balance is untouched).") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllDenominations()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "All denomination counts reset to 0", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Custom Denomination Dialog
    if (showAddCustomDialog) {
        var customValueText by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = { Text("Add Custom Note / Coin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter denomination face value (e.g. 5000, 250, 0.10):", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = customValueText,
                        onValueChange = {
                            customValueText = it
                            errorMsg = null
                        },
                        label = { Text("Face Value ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_denom_input")
                    )
                    if (errorMsg != null) {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val v = customValueText.toDoubleOrNull()
                        if (v == null || v <= 0.0) {
                            errorMsg = "Please enter a valid amount > 0"
                            return@Button
                        }
                        viewModel.addCustomDenomination(v)
                        showAddCustomDialog = false
                    },
                    modifier = Modifier.testTag("save_custom_denom_btn")
                ) {
                    Text("Add Denomination")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DenominationRowCard(
    item: DenominationItem,
    currencySymbol: String,
    onCountChange: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isNote = item.value >= 5.0
    val denomLabel = if (item.value >= 1.0 && item.value % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "%.0f", item.value)
    } else {
        String.format(Locale.getDefault(), "%.2f", item.value)
    }

    var localText by remember(item.count) { mutableStateOf(if (item.count > 0) item.count.toString() else "0") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("denom_row_${denomLabel}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.count > 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Face Value Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(96.dp)
            ) {
                Surface(
                    shape = if (isNote) RoundedCornerShape(8.dp) else CircleShape,
                    color = if (isNote) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(if (isNote) 42.dp else 38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = denomLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isNote) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = if (isNote) "Note" else "Coin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.isCustom) {
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Middle: Controls (- / input / +)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Decrement Button
                FilledTonalIconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(36.dp),
                    enabled = item.count > 0
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }

                // Text Input
                OutlinedTextField(
                    value = localText,
                    onValueChange = { input ->
                        val cleaned = input.filter { it.isDigit() }
                        localText = cleaned
                        val countVal = cleaned.toIntOrNull() ?: 0
                        onCountChange(countVal)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .width(60.dp)
                        .height(50.dp)
                        .testTag("denom_count_input_${denomLabel}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                // Increment Button
                FilledTonalIconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }
            }

            // Right: Subtotal & Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.width(110.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, item.subtotal),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (item.count > 0) FontWeight.ExtraBold else FontWeight.Normal
                        ),
                        color = if (item.count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.count} pcs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Denomination", tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
