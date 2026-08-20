package com.example.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

sealed interface ServerState {
    object Stopped : ServerState
    data class Running(val hostAddress: String, val port: Int) : ServerState
    data class Error(val message: String) : ServerState
}

class LocalNetworkSyncServer(
    private val repository: ExpenseRepository,
    private val deviceIdentityService: DeviceIdentityService,
    private val context: Context
) {
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState = _serverState.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<String>>(emptyList())
    val serverLogs = _serverLogs.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun log(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _serverLogs.value = listOf("[$time] $msg") + _serverLogs.value.take(40)
    }

    fun startServer(port: Int = 8890) {
        if (serverSocket != null && !serverSocket!!.isClosed) {
            log("Server already active on port $port")
            return
        }

        serverJob?.cancel()
        serverJob = scope.launch {
            try {
                val ip = getDeviceLocalIpAddress(context)
                val socket = ServerSocket(port)
                serverSocket = socket
                _serverState.value = ServerState.Running(ip, port)
                val myDevice = deviceIdentityService.getDeviceName()
                log("Sync Server started at http://$ip:$port ($myDevice)")
                log("Ready to accept 2-way sync requests from other devices.")

                while (isActive && !socket.isClosed) {
                    try {
                        val clientSocket = socket.accept()
                        launch(Dispatchers.IO) {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (isActive && !socket.isClosed) {
                            log("Connection error: ${e.localizedMessage}")
                        }
                    }
                }
            } catch (e: Exception) {
                _serverState.value = ServerState.Error(e.localizedMessage ?: "Failed to start server")
                log("Failed to start server: ${e.localizedMessage}")
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            _serverState.value = ServerState.Stopped
            log("Sync Server stopped.")
        } catch (e: Exception) {
            log("Error stopping server: ${e.localizedMessage}")
        }
    }

    private suspend fun handleClient(clientSocket: Socket) = withContext(Dispatchers.IO) {
        try {
            clientSocket.soTimeout = 20000
            val inStream = clientSocket.getInputStream()
            val outStream = clientSocket.getOutputStream()

            // Read HTTP request line and headers
            val headerSb = StringBuilder()
            var b: Int
            while (inStream.read().also { b = it } != -1) {
                headerSb.append(b.toChar())
                if (headerSb.endsWith("\r\n\r\n") || headerSb.endsWith("\n\n")) {
                    break
                }
            }

            val fullHeader = headerSb.toString()
            if (fullHeader.isBlank()) return@withContext

            val lines = fullHeader.lines()
            val requestLine = lines.firstOrNull() ?: return@withContext
            log("Incoming request: $requestLine from ${clientSocket.inetAddress.hostAddress}")

            var contentLength = 0
            for (headerLine in lines) {
                if (headerLine.lowercase().startsWith("content-length:")) {
                    contentLength = headerLine.substring(15).trim().toIntOrNull() ?: 0
                }
            }

            val myDeviceName = deviceIdentityService.getDeviceName()
            val myDeviceId = deviceIdentityService.getDeviceId()

            if (requestLine.startsWith("GET /info")) {
                val responseJson = JSONObject().apply {
                    put("status", "ok")
                    put("deviceName", myDeviceName)
                    put("deviceId", myDeviceId)
                    put("timestamp", System.currentTimeMillis())
                }
                val body = responseJson.toString()
                sendHttpResponse(outStream, 200, "OK", "application/json", body)
                log("Replied to /info")
            } else if (requestLine.startsWith("GET /export")) {
                val jsonSnapshot = repository.exportToJsonString(deviceId = myDeviceId, deviceName = myDeviceName)
                sendHttpResponse(outStream, 200, "OK", "application/json", jsonSnapshot)
                log("Sent full snapshot export (${jsonSnapshot.length} bytes)")
            } else if (requestLine.startsWith("POST /sync")) {
                val bodyBytes: ByteArray
                if (contentLength > 0) {
                    bodyBytes = ByteArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val count = inStream.read(bodyBytes, readTotal, contentLength - readTotal)
                        if (count == -1) break
                        readTotal += count
                    }
                } else {
                    bodyBytes = inStream.readBytes()
                }

                val requestBody = String(bodyBytes, Charsets.UTF_8)
                val remoteSnapshot = repository.parseSnapshotFromJson(requestBody)
                val mergedCount = repository.mergeSnapshot(remoteSnapshot)

                val peerDeviceId = remoteSnapshot.deviceId
                val peerDeviceName = remoteSnapshot.deviceName.ifBlank { "Remote Device" }

                // Record peer device last sync timestamp and record count
                if (peerDeviceId.isNotBlank()) {
                    deviceIdentityService.recordSyncEvent(
                        peerDeviceId = peerDeviceId,
                        peerDeviceName = peerDeviceName,
                        timestamp = System.currentTimeMillis(),
                        recordCount = mergedCount
                    )
                }

                log("2-Way Sync: Received & merged $mergedCount records from '$peerDeviceName' ($peerDeviceId)")

                // Return our updated snapshot (delta since peer's requested timestamp, or full if 0)
                val currentSnapshotJson = repository.exportToJsonString(
                    deviceId = myDeviceId,
                    deviceName = myDeviceName,
                    sinceTimestamp = remoteSnapshot.sinceTimestamp
                )
                sendHttpResponse(outStream, 200, "OK", "application/json", currentSnapshotJson)
                log("Sync handshake completed successfully with '$peerDeviceName'!")
            } else {
                sendHttpResponse(outStream, 404, "Not Found", "text/plain", "Not Found")
            }
        } catch (e: Exception) {
            log("Error handling client: ${e.localizedMessage}")
        } finally {
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private fun sendHttpResponse(
        outStream: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: String
    ) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        outStream.write(header.toByteArray(Charsets.UTF_8))
        outStream.write(bytes)
        outStream.flush()
    }

    companion object {
        fun getDeviceLocalIpAddress(context: Context): String {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val wifiInfo = wifiManager?.connectionInfo
                val ipInt = wifiInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    @Suppress("DEPRECATION")
                    val formatted = Formatter.formatIpAddress(ipInt)
                    if (formatted != "0.0.0.0" && formatted != "127.0.0.1") {
                        return formatted
                    }
                }
            } catch (_: Exception) {}

            // Fallback to NetworkInterface (works for Wi-Fi, Hotspot/AP, and USB tethering)
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                val sortedInterfaces = interfaces.sortedByDescending {
                    when {
                        it.name.startsWith("wlan", ignoreCase = true) -> 3
                        it.name.startsWith("ap", ignoreCase = true) -> 2
                        it.name.startsWith("rndis", ignoreCase = true) -> 1
                        else -> 0
                    }
                }
                for (intf in sortedInterfaces) {
                    if (!intf.isUp || intf.isLoopback) continue
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && host != "127.0.0.1") {
                                return host
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            return "127.0.0.1"
        }
    }
}
