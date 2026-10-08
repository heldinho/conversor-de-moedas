package com.cambio.converter.ui

import com.cambio.converter.data.Currency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MoneyFormat {

    private val ptBr: Locale = Locale("pt", "BR")

    private fun symbols(): DecimalFormatSymbols =
        DecimalFormatSymbols(ptBr).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }

    fun money(value: Double, currency: Currency): String {
        val magnitude = abs(value)
        val pattern = when {
            magnitude >= 1 -> "#,##0.00"
            magnitude >= 0.01 -> "#,##0.0000"
            else -> "#,##0.000000"
        }
        return "${currency.symbol} ${DecimalFormat(pattern, symbols()).format(value)}"
    }

    fun rate(value: Double): String {
        val pattern = when {
            value >= 1_000 -> "#,##0.00"
            value >= 1 -> "#,##0.0000"
            else -> "#,##0.000000"
        }
        return DecimalFormat(pattern, symbols()).format(value)
    }

    fun parse(input: String): Double? {
        val text = input.trim().replace(" ", "")
        if (text.isEmpty()) return null
        val normalized = when {
            text.contains(',') && text.contains('.') ->
                if (text.lastIndexOf(',') > text.lastIndexOf('.')) {
                    text.replace(".", "").replace(',', '.')
                } else {
                    text.replace(",", "")
                }
            text.contains(',') -> text.replace(',', '.')
            else -> text
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    fun isoDate(raw: String): String {
        val iso = Regex("""\d{4}-\d{2}-\d{2}""").find(raw)?.value ?: return raw.ifBlank { "—" }
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
        }.getOrNull() ?: return iso
        return SimpleDateFormat("dd/MM/yyyy", ptBr).format(parsed)
    }

    fun clock(millis: Long): String =
        SimpleDateFormat("HH:mm", ptBr).format(Date(millis))

    private fun abs(value: Double): Double = if (value < 0) -value else value
}
