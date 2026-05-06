package com.salesgoals.app.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    private val esAR: Locale = Locale("es", "AR")

    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(esAR).apply {
        maximumFractionDigits = 0
    }

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(esAR).apply {
        maximumFractionDigits = 0
    }

    private val percentFormat: NumberFormat = NumberFormat.getNumberInstance(esAR).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }

    fun money(value: Double): String = currencyFormat.format(value)
    fun units(value: Double): String = numberFormat.format(value)
    fun percent(value: Double): String = "${percentFormat.format(value)}%"

    fun formatValue(value: Double, isCurrency: Boolean): String =
        if (isCurrency) money(value) else units(value)

    /** YYYY-MM del mes actual */
    fun currentPeriod(date: Date = Date()): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(date)
    }

    /** YYYY-MM-DD para hoy */
    fun today(date: Date = Date()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(date)
    }

    /** Day of month a partir de YYYY-MM-DD */
    fun dayOfMonth(date: String): Int {
        return try {
            date.substring(8, 10).toInt()
        } catch (e: Exception) {
            1
        }
    }

    fun friendlyDate(date: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(date) ?: return date
            val out = SimpleDateFormat("EEE dd MMM", esAR)
            out.format(d).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            date
        }
    }

    fun toDouble(text: String): Double {
        if (text.isBlank()) return 0.0
        val cleaned = text.replace(".", "").replace(",", ".")
            .replace("$", "").replace(" ", "").trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /** Días transcurridos en el mes hasta hoy (capeado a workingDays) */
    fun elapsedWorkingDays(workingDays: Int, today: Date = Date()): Int {
        val cal = Calendar.getInstance().apply { time = today }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return day.coerceIn(0, workingDays)
    }
}
