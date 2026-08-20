package com.example.data.model

data class CurrencyItem(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val rateFromSar: Double, // 1 SAR = X of this currency (e.g. 0.2667 for USD)
    val isCustom: Boolean = false
) {
    // 1 unit of this currency in SAR (e.g. 3.75 SAR for 1 USD)
    val sarPerUnit: Double
        get() = if (rateFromSar > 0) 1.0 / rateFromSar else 0.0

    fun convertFromSar(sarAmount: Double): Double {
        return sarAmount * rateFromSar
    }

    fun convertToSar(foreignAmount: Double): Double {
        return if (rateFromSar > 0) foreignAmount / rateFromSar else foreignAmount
    }
}

object DefaultCurrencies {
    val SAR = CurrencyItem(
        code = "SAR",
        name = "Saudi Riyal",
        symbol = "SAR",
        flag = "🇸🇦",
        rateFromSar = 1.0
    )

    val list = listOf(
        SAR,
        CurrencyItem("USD", "US Dollar", "$", "🇺🇸", 0.26667),
        CurrencyItem("EUR", "Euro", "€", "🇪🇺", 0.24500),
        CurrencyItem("AED", "UAE Dirham", "AED", "🇦🇪", 0.97930),
        CurrencyItem("KWD", "Kuwaiti Dinar", "KWD", "🇰🇼", 0.08200),
        CurrencyItem("BHD", "Bahraini Dinar", "BHD", "🇧🇭", 0.10050),
        CurrencyItem("OMR", "Omani Rial", "OMR", "🇴🇲", 0.10260),
        CurrencyItem("QAR", "Qatari Riyal", "QAR", "🇶🇦", 0.97100),
        CurrencyItem("GBP", "British Pound", "£", "🇬🇧", 0.21000),
        CurrencyItem("EGP", "Egyptian Pound", "EGP", "🇪🇬", 13.0500),
        CurrencyItem("INR", "Indian Rupee", "₹", "🇮🇳", 22.3500),
        CurrencyItem("PHP", "Philippine Peso", "₱", "🇵🇭", 15.2000),
        CurrencyItem("PKR", "Pakistani Rupee", "PKR", "🇵🇰", 74.5000),
        CurrencyItem("CAD", "Canadian Dollar", "C$", "🇨🇦", 0.36500),
        CurrencyItem("AUD", "Australian Dollar", "A$", "🇦🇺", 0.40500),
        CurrencyItem("TRY", "Turkish Lira", "₺", "🇹🇷", 9.05000),
        CurrencyItem("JPY", "Japanese Yen", "¥", "🇯🇵", 40.5000),
        CurrencyItem("CNY", "Chinese Yuan", "¥", "🇨🇳", 1.92000)
    )

    fun getByCode(code: String): CurrencyItem {
        return list.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SAR
    }
}
