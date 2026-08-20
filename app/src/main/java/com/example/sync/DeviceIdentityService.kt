package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.data.model.DeviceSyncRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class DeviceIdentityService(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("expense_device_identity_prefs", Context.MODE_PRIVATE)

    private val _deviceName = MutableStateFlow(getSavedDeviceName())
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _deviceId = MutableStateFlow(getSavedDeviceId())
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _syncedDevices = MutableStateFlow(loadSyncedDevices())
    val syncedDevices: StateFlow<List<DeviceSyncRecord>> = _syncedDevices.asStateFlow()

    init {
        // Ensure default IDs exist on initial launch
        if (!prefs.contains(KEY_DEVICE_ID)) {
            val generated = "dev_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            _deviceId.value = generated
        }
        if (!prefs.contains(KEY_DEVICE_NAME)) {
            val defaultName = getSystemDefaultDeviceName()
            prefs.edit().putString(KEY_DEVICE_NAME, defaultName).apply()
            _deviceName.value = defaultName
        }
    }

    fun getSavedDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = "dev_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun getDeviceId(): String = getSavedDeviceId()

    fun getSavedDeviceName(): String {
        val name = prefs.getString(KEY_DEVICE_NAME, null)
        return if (!name.isNullOrBlank()) name else getSystemDefaultDeviceName()
    }

    fun getDeviceName(): String = getSavedDeviceName()

    fun setDeviceName(newName: String) {
        val trimmed = newName.trim().ifEmpty { getSystemDefaultDeviceName() }
        prefs.edit().putString(KEY_DEVICE_NAME, trimmed).apply()
        _deviceName.value = trimmed
    }

    // --- Per-Device Sync History & Watermark Timestamps ---
    fun getLastSyncTimestamp(peerDeviceId: String): Long {
        if (peerDeviceId.isBlank()) return 0L
        return prefs.getLong(KEY_PREFIX_LAST_SYNC + peerDeviceId, 0L)
    }

    fun recordSyncEvent(peerDeviceId: String, peerDeviceName: String, timestamp: Long = System.currentTimeMillis(), recordCount: Int = 0) {
        if (peerDeviceId.isBlank()) return
        val cleanName = peerDeviceName.trim().ifBlank { "Unknown Device" }

        val editor = prefs.edit()
        editor.putLong(KEY_PREFIX_LAST_SYNC + peerDeviceId, timestamp)
        editor.putString(KEY_PREFIX_NAME + peerDeviceId, cleanName)

        val currentList = loadSyncedDevices().toMutableList()
        val index = currentList.indexOfFirst { it.deviceId == peerDeviceId }
        val newRecord = DeviceSyncRecord(
            deviceId = peerDeviceId,
            deviceName = cleanName,
            lastSyncTimestamp = timestamp,
            lastRecordCount = recordCount
        )

        if (index >= 0) {
            currentList[index] = newRecord
        } else {
            currentList.add(0, newRecord)
        }

        // Save JSON index
        saveSyncedDevicesJson(currentList, editor)
        editor.apply()

        _syncedDevices.value = currentList.sortedByDescending { it.lastSyncTimestamp }
    }

    fun clearDeviceSyncHistory(peerDeviceId: String) {
        val editor = prefs.edit()
        editor.remove(KEY_PREFIX_LAST_SYNC + peerDeviceId)
        editor.remove(KEY_PREFIX_NAME + peerDeviceId)

        val updated = loadSyncedDevices().filter { it.deviceId != peerDeviceId }
        saveSyncedDevicesJson(updated, editor)
        editor.apply()

        _syncedDevices.value = updated
    }

    fun clearAllSyncHistory() {
        val allKeys = prefs.all.keys.toList()
        val editor = prefs.edit()
        for (key in allKeys) {
            if (key.startsWith(KEY_PREFIX_LAST_SYNC) || key.startsWith(KEY_PREFIX_NAME) || key == KEY_SYNCED_DEVICES_JSON) {
                editor.remove(key)
            }
        }
        editor.apply()
        _syncedDevices.value = emptyList()
    }

    private fun loadSyncedDevices(): List<DeviceSyncRecord> {
        val jsonStr = prefs.getString(KEY_SYNCED_DEVICES_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<DeviceSyncRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("deviceId")
                val name = obj.optString("deviceName")
                val time = obj.optLong("lastSyncTimestamp", 0L)
                val count = obj.optInt("lastRecordCount", 0)
                if (id.isNotBlank()) {
                    list.add(DeviceSyncRecord(id, name, time, count))
                }
            }
            list.sortedByDescending { it.lastSyncTimestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveSyncedDevicesJson(list: List<DeviceSyncRecord>, editor: SharedPreferences.Editor) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("deviceId", item.deviceId)
                put("deviceName", item.deviceName)
                put("lastSyncTimestamp", item.lastSyncTimestamp)
                put("lastRecordCount", item.lastRecordCount)
            }
            array.put(obj)
        }
        editor.putString(KEY_SYNCED_DEVICES_JSON, array.toString())
    }

    private fun getSystemDefaultDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val combined = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            if (manufacturer.isNotBlank()) "$manufacturer $model" else model
        }
        return combined.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }.ifEmpty { "My Android Phone" }
    }

    companion object {
        private const val KEY_DEVICE_ID = "pref_device_id"
        private const val KEY_DEVICE_NAME = "pref_device_name"
        private const val KEY_PREFIX_LAST_SYNC = "last_sync_ts_"
        private const val KEY_PREFIX_NAME = "peer_name_"
        private const val KEY_SYNCED_DEVICES_JSON = "pref_synced_devices_json"
    }
}
