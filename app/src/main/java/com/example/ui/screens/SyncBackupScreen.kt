package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeviceSyncRecord
import com.example.sync.ClientSyncState
import com.example.sync.ServerState
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

private fun generateWipeChallengeCode(): String {
    val verbs = listOf("DELETE", "WIPE", "RESET", "PURGE", "ERASE")
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val suffix = (1..5).map { chars.random() }.joinToString("")
    return "${verbs.random()}-$suffix"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SyncBackupScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val serverLogs by viewModel.serverLogs.collectAsStateWithLifecycle()
    val clientSyncState by viewModel.clientSyncState.collectAsStateWithLifecycle()
    val clientLogs by viewModel.clientLogs.collectAsStateWithLifecycle()
    val syncedDevices by viewModel.syncedDevices.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
    val deviceId by viewModel.deviceId.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Local Network Sync, 1: File Backup/Restore, 2: Settings

    var targetHostIp by remember { mutableStateOf("") }
    var forceFullSync by remember { mutableStateOf(false) }
    var editDeviceNameDialogVisible by remember { mutableStateOf(false) }
    var editDeviceNameInput by remember { mutableStateOf("") }
    var importJsonDialogVisible by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var replaceAllOnImport by remember { mutableStateOf(false) }
    var importResultMessage by remember { mutableStateOf<String?>(null) }
    var currencyManagerVisible by remember { mutableStateOf(false) }
    var wipeDataDialogVisible by remember { mutableStateOf(false) }
    var wipeChallengeCode by remember { mutableStateOf("") }
    var typedChallengeInput by remember { mutableStateOf("") }
    var wipeTransactionsChecked by remember { mutableStateOf(true) }
    var wipeReservedPaymentsChecked by remember { mutableStateOf(true) }
    var wipeBankAccountsChecked by remember { mutableStateOf(false) }
    var wipeBudgetsChecked by remember { mutableStateOf(false) }
    var resetDenominationsChecked by remember { mutableStateOf(false) }

    val activeCurrencyCode by viewModel.activeCurrencyCode.collectAsStateWithLifecycle()

    // JSON file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonString.isNotEmpty()) {
                    importJsonText = jsonString
                    importJsonDialogVisible = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val copyToClipboard = { text: String, label: String ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device Sync & Backup",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Direct Sync", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Sync, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Backup & Export", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Preferences", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.CloudOff, contentDescription = null) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    // TAB 0: DIRECT LOCAL NETWORK / BLUETOOTH SYNC

                    // Section 0: Device Identity & Name
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("device_identity_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Smartphone,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "This Device Identity",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Ledger entries will be tagged with this name",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            editDeviceNameInput = deviceName
                                            editDeviceNameDialogVisible = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Rename", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Device Name:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = deviceName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.clickable { copyToClipboard(deviceId, "Device ID") }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "ID: ${deviceId.take(10)}...",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Copy ID",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // How Sync Works Banner
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("sync_info_banner"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "How 2-Way Device Sync Works",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("🔄", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Two-Way Merge: Syncs everything including transactions, budgets, bank accounts, reserved payments, and cash denominations between devices.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("🛡️", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Duplicate-Proof: Every transaction has a persistent unique ID. Syncing multiple times will never create duplicate entries.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("🏷️", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Device Origin Tag: Every transaction in your ledger displays which device originally created it.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("💵", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Smart Cash Denomination Reconciliation: Auto-resolves cash breakdown by picking the device matching or closest to the merged cash-in-hand total, prioritizing the most recent active device.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: Host Sync Server on this Device
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("host_sync_server_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "1. Host Sync Server",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Turn this device into a sync receiver",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (serverState) {
                                            is ServerState.Running -> IncomeGreen.copy(alpha = 0.15f)
                                            is ServerState.Error -> ExpenseRed.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = when (serverState) {
                                                is ServerState.Running -> "ACTIVE"
                                                is ServerState.Error -> "ERROR"
                                                else -> "STOPPED"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = when (serverState) {
                                                is ServerState.Running -> IncomeGreen
                                                is ServerState.Error -> ExpenseRed
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (serverState is ServerState.Running) {
                                    val running = serverState as ServerState.Running
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Device Sync Address:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${running.hostAddress}:${running.port}",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { copyToClipboard("${running.hostAddress}:${running.port}", "Sync IP Address") }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy IP")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { viewModel.stopSyncServer() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Stop Server", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.startSyncServer() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("start_sync_server_button")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Start Sync Server (This Device)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Connect & Sync with Another Device
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("client_connect_sync_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "2. Connect & Sync with Other Device",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Enter the Sync Address shown on your other device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = targetHostIp,
                                    onValueChange = { targetHostIp = it },
                                    label = { Text("Host Device IP & Port") },
                                    placeholder = { Text("e.g. 192.168.1.15:8890") },
                                    leadingIcon = { Icon(Icons.Default.NetworkCheck, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                                if (clipText.isNotBlank()) {
                                                    targetHostIp = clipText.trim()
                                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste IP")
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("target_sync_ip_input"),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Fast Incremental Sync Toggle
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { forceFullSync = !forceFullSync },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (!forceFullSync) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = !forceFullSync,
                                            onCheckedChange = { forceFullSync = !it }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = if (!forceFullSync) "⚡ Fast Incremental Sync Active" else "🔄 Full Sync (All Records)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (!forceFullSync) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (!forceFullSync) "Syncs only changes made since last sync with this device" else "Transfers all historic records without timestamp filtering",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (targetHostIp.isNotBlank()) {
                                            viewModel.syncWithPeer(targetHostIp, forceFullSync = forceFullSync)
                                        }
                                    },
                                    enabled = targetHostIp.isNotBlank() && clientSyncState !is ClientSyncState.Connecting,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("sync_now_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (clientSyncState is ClientSyncState.Connecting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Syncing with peer...")
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sync Now & 2-Way Merge", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Sync status result box
                                AnimatedVisibility(visible = clientSyncState !is ClientSyncState.Idle) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    when (val state = clientSyncState) {
                                        is ClientSyncState.Success -> {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = IncomeGreen.copy(alpha = 0.15f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = state.message,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = IncomeGreen
                                                    )
                                                }
                                            }
                                        }
                                        is ClientSyncState.Error -> {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = ExpenseRed.copy(alpha = 0.15f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Error, contentDescription = null, tint = ExpenseRed)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = state.error,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ExpenseRed
                                                    )
                                                }
                                            }
                                        }
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Synced Devices & Last Sync Markers
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("synced_devices_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Devices,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Paired Devices",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Per-device sync timestamps & fast markers",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (syncedDevices.isNotEmpty()) {
                                        TextButton(onClick = { viewModel.clearAllSyncHistory() }) {
                                            Text("Clear All", color = ExpenseRed, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (syncedDevices.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Sync,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "No devices synced yet",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "When you sync with other phones or tablets, each device's last sync marker is recorded here to enable instant delta syncing.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        val timeFormat = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault())
                                        for (device in syncedDevices) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .background(IncomeGreen.copy(alpha = 0.15f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                tint = IncomeGreen,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = device.deviceName,
                                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                                ) {
                                                                    Text(
                                                                        text = "ID: ${device.deviceId.take(8)}",
                                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                                            fontFamily = FontFamily.Monospace,
                                                                            fontSize = 10.sp
                                                                        ),
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(3.dp))
                                                            Text(
                                                                text = "Last Synced: ${if (device.lastSyncTimestamp > 0) timeFormat.format(java.util.Date(device.lastSyncTimestamp)) else "Never"}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            if (device.lastRecordCount > 0) {
                                                                Text(
                                                                    text = "⚡ ${device.lastRecordCount} updates merged",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                                    color = IncomeGreen
                                                                )
                                                            }
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.removeSyncedDevice(device.deviceId) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteSweep,
                                                            contentDescription = "Forget Device Sync Marker",
                                                            tint = MaterialTheme.colorScheme.outline,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // How to sync with Bluetooth / Wi-Fi Hotspot Step-by-Step
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WifiTethering, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sync Without Wi-Fi Router (Hotspot)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "1. Turn on Mobile Hotspot or Bluetooth Tethering on Device A.\n" +
                                            "2. Connect Device B to Device A's hotspot.\n" +
                                            "3. Tap 'Start Sync Server' on Device A, then enter Device A's IP on Device B and tap 'Sync Now'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // TAB 1: FILE BACKUP & RESTORE (OFFLINE DATA PORTABILITY)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("export_backup_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Export Full Backup",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Save all expenses, income, bank accounts, reserved payments, budgets, and cash denominations into a portable JSON backup file.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val jsonStr = viewModel.getExportJson()
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, jsonStr)
                                                type = "application/json"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Export Expense Tracker Backup"))
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("export_json_backup_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export & Share JSON Backup", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val csvStr = viewModel.getExportCsv()
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, csvStr)
                                                type = "text/csv"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Export CSV Spreadsheet"))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export CSV for Excel / Sheets")
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("import_restore_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Restore & Import Data",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Restore data from a JSON backup file or pasted text when changing your phone.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.FileDownload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select File", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            importJsonText = ""
                                            importJsonDialogVisible = true
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Paste JSON")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 2: PREFERENCES & CURRENCY MANAGEMENT
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("currency_settings_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Active Currency",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = IncomeGreen.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Default: SAR",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = IncomeGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Current: $activeCurrencyCode ($currencySymbol)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = { currencyManagerVisible = true },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("manage_currencies_button")
                                    ) {
                                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Currencies & Rates")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Quick Switch Active Currency:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val popularCurrencies = listOf(
                                    Triple("SAR", "🇸🇦", "SAR"),
                                    Triple("USD", "🇺🇸", "$"),
                                    Triple("EUR", "🇪🇺", "€"),
                                    Triple("AED", "🇦🇪", "AED"),
                                    Triple("KWD", "🇰🇼", "KWD"),
                                    Triple("GBP", "🇬🇧", "£"),
                                    Triple("EGP", "🇪🇬", "EGP"),
                                    Triple("INR", "🇮🇳", "₹")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    popularCurrencies.take(4).forEach { (code, flag, _) ->
                                        val isSelected = activeCurrencyCode == code
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.setActiveCurrency(code) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(flag, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = code,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    popularCurrencies.drop(4).forEach { (code, flag, _) ->
                                        val isSelected = activeCurrencyCode == code
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.setActiveCurrency(code) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(flag, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = code,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = { currencyManagerVisible = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Open Live Rate Converter & All Currencies")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // TAB 2: DATA RESET & CLEAR TRANSACTIONS (DANGER ZONE)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("clear_transactions_danger_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed)
                                    Text(
                                        text = "Danger Zone: Data Wipe & Reset",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ExpenseRed
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Permanently erase transactions, banks, budgets, or reset all data to start clean. Requires GitHub-style typed confirmation code verification.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        wipeChallengeCode = generateWipeChallengeCode()
                                        typedChallengeInput = ""
                                        wipeTransactionsChecked = true
                                        wipeReservedPaymentsChecked = true
                                        wipeBankAccountsChecked = false
                                        wipeBudgetsChecked = false
                                        resetDenominationsChecked = false
                                        wipeDataDialogVisible = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("clear_all_transactions_button")
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Wipe / Reset Data (Confirmation Required)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Currency Manager Modal Sheet
    if (currencyManagerVisible) {
        CurrencyManagerDialog(
            viewModel = viewModel,
            onDismiss = { currencyManagerVisible = false }
        )
    }

    // GitHub-Style Data Wipe Confirmation Dialog
    if (wipeDataDialogVisible) {
        val isImeVisible = WindowInsets.isImeVisible
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        BackHandler(enabled = isImeVisible) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }

        val hasAnySelection = wipeTransactionsChecked || wipeReservedPaymentsChecked ||
                wipeBankAccountsChecked || wipeBudgetsChecked || resetDenominationsChecked
        val isCodeMatched = typedChallengeInput.trim().equals(wipeChallengeCode, ignoreCase = false)

        AlertDialog(
            onDismissRequest = { wipeDataDialogVisible = false },
            modifier = Modifier.padding(vertical = 16.dp),
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = ExpenseRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Permanently Delete Data",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Warning Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ExpenseRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "This action cannot be undone. Selected items will be permanently erased from this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Checkbox Selection for what to delete
                    Text(
                        text = "Select data to delete:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // All Transactions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wipeTransactionsChecked = !wipeTransactionsChecked }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = wipeTransactionsChecked,
                                onCheckedChange = { wipeTransactionsChecked = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "All Transactions (Expenses & Income)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Reserved Payments
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wipeReservedPaymentsChecked = !wipeReservedPaymentsChecked }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = wipeReservedPaymentsChecked,
                                onCheckedChange = { wipeReservedPaymentsChecked = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scheduled & BNPL Reserved Payments",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Bank Accounts
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wipeBankAccountsChecked = !wipeBankAccountsChecked }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = wipeBankAccountsChecked,
                                onCheckedChange = { wipeBankAccountsChecked = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Configured Bank Accounts",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Budgets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wipeBudgetsChecked = !wipeBudgetsChecked }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = wipeBudgetsChecked,
                                onCheckedChange = { wipeBudgetsChecked = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Monthly Budget Limits",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Denominations
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { resetDenominationsChecked = !resetDenominationsChecked }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = resetDenominationsChecked,
                                onCheckedChange = { resetDenominationsChecked = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset Physical Cash Denominations (Zero count)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Select All / Deselect All Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val allSelected = wipeTransactionsChecked && wipeReservedPaymentsChecked &&
                                wipeBankAccountsChecked && wipeBudgetsChecked && resetDenominationsChecked
                        TextButton(
                            onClick = {
                                val next = !allSelected
                                wipeTransactionsChecked = next
                                wipeReservedPaymentsChecked = next
                                wipeBankAccountsChecked = next
                                wipeBudgetsChecked = next
                                resetDenominationsChecked = next
                            }
                        ) {
                            Text(if (allSelected) "Deselect All" else "Select All (Factory Reset)")
                        }
                    }

                    HorizontalDivider()

                    // GitHub-style Verification Challenge Section
                    Text(
                        text = "To confirm deletion, type the code below exactly:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Code Display Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = wipeChallengeCode,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    wipeChallengeCode = generateWipeChallengeCode()
                                    typedChallengeInput = ""
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Generate new code", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Input Text Field
                    OutlinedTextField(
                        value = typedChallengeInput,
                        onValueChange = { typedChallengeInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wipe_confirm_input"),
                        placeholder = { Text("Type $wipeChallengeCode") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            if (typedChallengeInput.isNotEmpty()) {
                                if (isCodeMatched) {
                                    Icon(Icons.Default.Check, contentDescription = "Match", tint = IncomeGreen)
                                } else {
                                    Icon(Icons.Default.Close, contentDescription = "Mismatch", tint = ExpenseRed)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )
                    )

                    // Helper feedback message
                    if (typedChallengeInput.isNotEmpty()) {
                        if (isCodeMatched) {
                            Text(
                                text = "✓ Confirmation code matches",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = IncomeGreen
                            )
                        } else {
                            Text(
                                text = "✕ Code does not match yet (case-sensitive)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isCodeMatched && hasAnySelection) {
                            viewModel.wipeSelectedData(
                                wipeTransactions = wipeTransactionsChecked,
                                wipeBanks = wipeBankAccountsChecked,
                                wipeBudgets = wipeBudgetsChecked,
                                wipeReserved = wipeReservedPaymentsChecked,
                                resetDenominations = resetDenominationsChecked
                            )
                            wipeDataDialogVisible = false
                            Toast.makeText(context, "Selected data permanently wiped!", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = isCodeMatched && hasAnySelection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        disabledContainerColor = ExpenseRed.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_wipe_data_button")
                ) {
                    Text("Permanently Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { wipeDataDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // JSON Import Dialog
    if (importJsonDialogVisible) {
        AlertDialog(
            onDismissRequest = { importJsonDialogVisible = false },
            title = { Text("Restore / Import Data", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Paste or review the JSON backup text:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("{\"version\": 1, \"transactions\": [...]}") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.clickable { replaceAllOnImport = !replaceAllOnImport },
                            shape = RoundedCornerShape(8.dp),
                            color = if (replaceAllOnImport) ExpenseRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (replaceAllOnImport) "⚠️ Replace All Records" else "Merge with Existing (Recommended)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (replaceAllOnImport) ExpenseRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val count = viewModel.importJsonData(importJsonText, replaceAllOnImport)
                                Toast.makeText(context, "Successfully restored $count items!", Toast.LENGTH_LONG).show()
                                importJsonDialogVisible = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = importJsonText.isNotBlank()
                ) {
                    Text("Import Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { importJsonDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Device Name Dialog
    if (editDeviceNameDialogVisible) {
        AlertDialog(
            onDismissRequest = { editDeviceNameDialogVisible = false },
            icon = {
                Icon(
                    Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Set Device Name", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Give this device a recognizable name (e.g., 'John's Galaxy S24', 'Work Phone', 'iPad / Tablet'). This name will appear on transactions in the ledger and during sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editDeviceNameInput,
                        onValueChange = { editDeviceNameInput = it },
                        label = { Text("Device Name") },
                        placeholder = { Text("e.g. My Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editDeviceNameInput.isNotBlank()) {
                            viewModel.setDeviceName(editDeviceNameInput.trim())
                            Toast.makeText(context, "Device renamed to '${editDeviceNameInput.trim()}'", Toast.LENGTH_SHORT).show()
                            editDeviceNameDialogVisible = false
                        }
                    },
                    enabled = editDeviceNameInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Name", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editDeviceNameDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
