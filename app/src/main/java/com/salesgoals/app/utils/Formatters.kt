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

    private fun safe(value: Double): Double =
        if (value.isFinite()) value else 0.0

    fun money(value: Double): String = currencyFormat.format(safe(value))
    fun units(value: Double): String = numberFormat.format(safe(value))
    fun percent(value: Double): String = "${percentFormat.format(safe(value))}%"

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

    fun dayOfMonth(date: String): Int {
        return try {
            date.substring(8, 10).toInt()
        } catch (e: Exception) {
            1
        }
    }

    /** Devuelve true si el período tiene formato válido YYYY-MM con MM 01-12. */
    fun isValidPeriod(period: String): Boolean {
        val parts = period.split("-")
        if (parts.size != 2) return false
        val y = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        return y in 2000..2100 && m in 1..12
    }

    /** Si el período no es válido, devuelve el actual. */
    fun safePeriod(period: String?): String =
        if (period != null && isValidPeriod(period)) period else currentPeriod()

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

    /**
     * Parser tolerante de Double. Maneja:
     *  - "287947479"           → 287947479.0
     *  - "287.947.479"         → 287947479.0   (puntos como miles AR)
     *  - "287,947,479"         → 287947479.0   (comas como miles US)
     *  - "1.234,56"            → 1234.56       (formato AR)
     *  - "1,234.56"            → 1234.56       (formato US)
     *  - "$ 1.000"             → 1000.0
     *  - "1234,5"              → 1234.5
     *  - "1234.5"              → 1234.5
     */
    fun toDouble(text: String): Double {
        if (text.isBlank()) return 0.0
        var cleaned = text.replace("$", "").replace(" ", "").replace(" ", "").trim()
        if (cleaned.isEmpty()) return 0.0

        val dotCount = cleaned.count { it == '.' }
        val commaCount = cleaned.count { it == ',' }

        cleaned = when {
            dotCount == 0 && commaCount == 0 -> cleaned
            dotCount > 1 && commaCount == 0 -> cleaned.replace(".", "")           // miles con puntos
            commaCount > 1 && dotCount == 0 -> cleaned.replace(",", "")           // miles con comas
            dotCount == 1 && commaCount == 0 -> cleaned                            // decimal punto
            commaCount == 1 && dotCount == 0 -> cleaned.replace(",", ".")         // decimal coma
            dotCount > 0 && commaCount > 0 -> {
                // mezcla: el último símbolo es el decimal, el resto separadores de miles
                val lastDot = cleaned.lastIndexOf('.')
                val lastComma = cleaned.lastIndexOf(',')
                if (lastDot > lastComma) cleaned.replace(",", "")                 // formato US (1,234.56)
                else cleaned.replace(".", "").replace(",", ".")                   // formato AR (1.234,56)
            }
            else -> cleaned
        }
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /**
     * Días transcurridos del periodo. Si el periodo es PASADO devuelve workingDays
     * (el mes completo). Si es FUTURO devuelve 0. Si es el periodo actual,
     * devuelve el día del mes capeado por workingDays.
     */
    fun elapsedWorkingDays(workingDays: Int, period: String? = null, today: Date = Date()): Int {
        val cal = Calendar.getInstance().apply { time = today }
        val currYear = cal.get(Calendar.YEAR)
        val currMonth = cal.get(Calendar.MONTH) + 1
        val currDay = cal.get(Calendar.DAY_OF_MONTH)
        if (period != null) {
            val parts = period.split("-")
            if (parts.size >= 2) {
                val py = parts[0].toIntOrNull()
                val pm = parts[1].toIntOrNull()
                if (py != null && pm != null) {
                    val periodKey = py * 100 + pm
                    val currKey = currYear * 100 + currMonth
                    if (periodKey < currKey) return workingDays  // periodo pasado
                    if (periodKey > currKey) return 0            // periodo futuro
                }
            }
        }
        return currDay.coerceIn(0, workingDays)
    }
}
