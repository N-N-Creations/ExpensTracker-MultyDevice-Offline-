package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.CurrencyItem
import com.example.data.model.DefaultCurrencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class RateFetchResult {
    data class Success(val ratesCount: Int, val source: String, val timestamp: Long) : RateFetchResult()
    data class Error(val message: String) : RateFetchResult()
}

class CurrencyService(context: Context) {
    private val prefs = context.getSharedPreferences("app_currency_prefs_v2", Context.MODE_PRIVATE)

    fun getActiveCurrencyCode(): String {
        return prefs.getString("active_currency_code", "SAR") ?: "SAR"
    }

    fun setActiveCurrencyCode(code: String) {
        prefs.edit().putString("active_currency_code", code).apply()
    }

    fun getActiveCurrencySymbol(): String {
        val code = getActiveCurrencyCode()
        val customSymbol = prefs.getString("custom_symbol_${code}", null)
        if (!customSymbol.isNullOrBlank()) return customSymbol
        return DefaultCurrencies.getByCode(code).symbol
    }

    fun setCustomCurrencySymbol(code: String, symbol: String) {
        prefs.edit().putString("custom_symbol_${code}", symbol).apply()
    }

    fun getLastUpdatedTimestamp(): Long {
        return prefs.getLong("rates_last_updated", 0L)
    }

    fun getCurrencies(): List<CurrencyItem> {
        val baseList = DefaultCurrencies.list
        return baseList.map { defaultItem ->
            val savedRate = prefs.getString("rate_from_sar_${defaultItem.code}", null)?.toDoubleOrNull()
            val customSymbol = prefs.getString("custom_symbol_${defaultItem.code}", null)
            val finalRate = savedRate ?: defaultItem.rateFromSar
            val finalSymbol = customSymbol ?: defaultItem.symbol
            val isCustom = savedRate != null && savedRate != defaultItem.rateFromSar
            defaultItem.copy(
                rateFromSar = finalRate,
                symbol = finalSymbol,
                isCustom = isCustom
            )
        }
    }

    fun setManualRate(code: String, rateFromSar: Double) {
        if (code == "SAR") return // SAR is always 1.0 base
        if (rateFromSar <= 0) return
        prefs.edit()
            .putString("rate_from_sar_${code}", rateFromSar.toString())
            .putLong("rates_last_updated", System.currentTimeMillis())
            .apply()
    }

    fun resetRateToDefault(code: String) {
        prefs.edit()
            .remove("rate_from_sar_${code}")
            .remove("custom_symbol_${code}")
            .apply()
    }

    fun resetAllRatesToDefault() {
        val editor = prefs.edit()
        DefaultCurrencies.list.forEach { item ->
            editor.remove("rate_from_sar_${item.code}")
            editor.remove("custom_symbol_${item.code}")
        }
        editor.remove("rates_last_updated")
        editor.apply()
    }

    /**
     * Fetches live exchange rates relative to SAR from free online open exchange rates API
     */
    suspend fun fetchLiveRatesOnline(): RateFetchResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://open.er-api.com/v6/latest/SAR")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "ExpenseTracker-Android")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                if (json.optString("result") == "success" || json.has("rates")) {
                    val ratesObj = json.getJSONObject("rates")
                    val editor = prefs.edit()
                    var updatedCount = 0

                    DefaultCurrencies.list.forEach { currency ->
                        if (currency.code != "SAR" && ratesObj.has(currency.code)) {
                            val rate = ratesObj.getDouble(currency.code)
                            editor.putString("rate_from_sar_${currency.code}", rate.toString())
                            updatedCount++
                        }
                    }

                    val now = System.currentTimeMillis()
                    editor.putLong("rates_last_updated", now)
                    editor.apply()

                    return@withContext RateFetchResult.Success(
                        ratesCount = updatedCount,
                        source = "Open Exchange Rates (Live)",
                        timestamp = now
                    )
                } else {
                    return@withContext RateFetchResult.Error("API returned unexpected data structure.")
                }
            } else {
                return@withContext RateFetchResult.Error("Server returned HTTP error $responseCode")
            }
        } catch (e: Exception) {
            Log.e("CurrencyService", "Failed to fetch live rates", e)
            return@withContext RateFetchResult.Error(e.localizedMessage ?: "Network connection error")
        } finally {
            connection?.disconnect()
        }
    }
}
