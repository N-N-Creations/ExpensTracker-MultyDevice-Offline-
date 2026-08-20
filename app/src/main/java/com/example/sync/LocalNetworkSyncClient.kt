package com.example.sync

import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed interface ClientSyncState {
    object Idle : ClientSyncState
    object Connecting : ClientSyncState
    data class Success(val message: String, val mergedCount: Int) : ClientSyncState
    data class Error(val error: String) : ClientSyncState
}

class LocalNetworkSyncClient(
    private val repository: ExpenseRepository,
    private val deviceIdentityService: DeviceIdentityService
) {
    private val _syncState = MutableStateFlow<ClientSyncState>(ClientSyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _clientLogs = MutableStateFlow<List<String>>(emptyList())
    val clientLogs = _clientLogs.asStateFlow()

    private fun log(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _clientLogs.value = listOf("[$time] $msg") + _clientLogs.value.take(40)
    }

    suspend fun syncWithHost(
        hostIp: String,
        port: Int = 8890,
        forceFullSync: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = ClientSyncState.Connecting
        val cleanIp = hostIp.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val baseUrl = if (cleanIp.contains(":")) cleanIp else "$cleanIp:$port"
        val infoUrlString = "http://$baseUrl/info"
        val syncUrlString = "http://$baseUrl/sync"

        log("Connecting to peer at: $syncUrlString")

        try {
            // Optional quick info probe to check host device ID and identify last sync watermark
            var probedPeerDeviceId = ""
            var probedPeerDeviceName = ""
            try {
                val infoConn = URL(infoUrlString).openConnection() as HttpURLConnection
                infoConn.connectTimeout = 3000
                infoConn.readTimeout = 3000
                if (infoConn.responseCode == HttpURLConnection.HTTP_OK) {
                    val infoStr = infoConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val infoObj = JSONObject(infoStr)
                    probedPeerDeviceId = infoObj.optString("deviceId", "")
                    probedPeerDeviceName = infoObj.optString("deviceName", "")
                }
                infoConn.disconnect()
            } catch (_: Exception) {}

            val myDeviceId = deviceIdentityService.getDeviceId()
            val myDeviceName = deviceIdentityService.getDeviceName()

            // Determine last sync watermark for this specific peer device
            val lastSyncTimestamp = if (forceFullSync || probedPeerDeviceId.isBlank()) {
                0L
            } else {
                deviceIdentityService.getLastSyncTimestamp(probedPeerDeviceId)
            }

            val syncMode = if (lastSyncTimestamp > 0L) "Fast Delta Sync" else "Full Sync"
            log("Sync Mode: $syncMode ${if (lastSyncTimestamp > 0L) "(checking changes since last sync)" else "(all records)"}")

            val url = URL(syncUrlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 25000
            connection.doOutput = true
            connection.doInput = true

            // Send this device's current data snapshot (delta if lastSyncTimestamp > 0)
            val clientSnapshotJson = repository.exportToJsonString(
                deviceId = myDeviceId,
                deviceName = myDeviceName,
                sinceTimestamp = lastSyncTimestamp
            )
            val bytes = clientSnapshotJson.toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)

            val os = connection.outputStream
            os.write(bytes)
            os.flush()
            os.close()

            val responseCode = connection.responseCode
            log("Peer responded with HTTP $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val serverResponseJson = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val serverSnapshot = repository.parseSnapshotFromJson(serverResponseJson)
                val mergedCount = repository.mergeSnapshot(serverSnapshot)

                val effectivePeerId = if (serverSnapshot.deviceId.isNotBlank()) serverSnapshot.deviceId else probedPeerDeviceId
                val effectivePeerName = if (serverSnapshot.deviceName.isNotBlank()) serverSnapshot.deviceName else (if (probedPeerDeviceName.isNotBlank()) probedPeerDeviceName else "Remote Device")

                val syncNow = System.currentTimeMillis()
                if (effectivePeerId.isNotBlank()) {
                    deviceIdentityService.recordSyncEvent(
                        peerDeviceId = effectivePeerId,
                        peerDeviceName = effectivePeerName,
                        timestamp = syncNow,
                        recordCount = mergedCount
                    )
                }

                val successMsg = "Synced successfully with '$effectivePeerName'!\n($syncMode: $mergedCount updates merged, 0 duplicates)"
                log(successMsg)
                _syncState.value = ClientSyncState.Success(successMsg, mergedCount)
                connection.disconnect()
                return@withContext true
            } else {
                val errorBody = try {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                } catch (_: Exception) { null }
                val errMsg = "Sync failed with HTTP $responseCode from host${if (!errorBody.isNullOrBlank()) ": $errorBody" else ""}"
                log(errMsg)
                _syncState.value = ClientSyncState.Error(errMsg)
                connection.disconnect()
                return@withContext false
            }
        } catch (e: Exception) {
            val errMsg = if (e.localizedMessage?.contains("Cleartext", ignoreCase = true) == true) {
                "Cleartext traffic error: please ensure usesCleartextTraffic is enabled."
            } else {
                "Sync error: ${e.localizedMessage ?: "Could not reach host device. Ensure both devices are on the same Wi-Fi / Hotspot and host server is running."}"
            }
            log(errMsg)
            _syncState.value = ClientSyncState.Error(errMsg)
            return@withContext false
        }
    }

    fun resetState() {
        _syncState.value = ClientSyncState.Idle
    }
}

