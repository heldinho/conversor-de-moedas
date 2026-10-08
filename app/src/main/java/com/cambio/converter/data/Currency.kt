package com.cambio.converter.data

enum class Currency(
    val code: String,
    val label: String,
    val symbol: String,
    val flag: String,
    val localeTag: String
) {
    USD("USD", "Dólar Americano", "$", "🇺🇸", "en_US"),
    EUR("EUR", "Euro", "€", "🇪🇺", "pt_BR"),
    BRL("BRL", "Real Brasileiro", "R$", "🇧🇷", "pt_BR");

    companion object {
        fun fromCode(code: String?): Currency? = entries.firstOrNull { it.code.equals(code, true) }
    }
}
