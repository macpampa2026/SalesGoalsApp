package com.salesgoals.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.salesgoals.app.data.models.AdvisorBudgetPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ExportImportHelper {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * Genera el archivo JSON del presupuesto del asesor en cache externo
     * y devuelve la Uri lista para compartir.
     */
    fun exportAdvisorPayload(
        context: Context,
        payload: AdvisorBudgetPayload,
        format: ExportFormat = ExportFormat.JSON
    ): Uri {
        val baseName = "presupuesto_${sanitize(payload.advisorName)}_${payload.period}"
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file: File = when (format) {
            ExportFormat.JSON -> File(dir, "$baseName.json").apply {
                writeText(json.encodeToString(payload))
            }
            ExportFormat.CSV -> File(dir, "$baseName.csv").apply {
                writeText(payloadToCsv(payload))
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun importPayload(context: Context, uri: Uri): AdvisorBudgetPayload {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("No se pudo leer el archivo")
        return when {
            text.trimStart().startsWith("{") -> json.decodeFromString(text)
            else -> csvToPayload(text)
        }
    }

    fun shareIntent(uri: Uri, format: ExportFormat = ExportFormat.JSON): Intent {
        val mime = if (format == ExportFormat.JSON) "application/json" else "text/csv"
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Presupuesto del mes")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun whatsappIntent(uri: Uri): Intent = shareIntent(uri).apply {
        setPackage("com.whatsapp")
    }

    fun emailIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Presupuesto Mensual")
        putExtra(Intent.EXTRA_TEXT, "Adjunto el presupuesto del mes.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        selector = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:") }
    }

    private fun sanitize(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifBlank { "asesor" }

    private fun payloadToCsv(p: AdvisorBudgetPayload): String = buildString {
        appendLine("campo,valor")
        appendLine("asesor,${p.advisorName}")
        appendLine("sucursal,${p.branchName}")
        appendLine("periodo,${p.period}")
        appendLine("dias_laborales,${p.workingDays}")
        appendLine("volumen,${p.goals.volume}")
        appendLine("credito,${p.goals.credit}")
        appendLine("garantia,${p.goals.warranty}")
        appendLine("credito_efectivo,${p.goals.cashCredit}")
        appendLine("celulares,${p.goals.phones}")
        appendLine("notas,\"${p.notes.replace("\"", "'")}\"")
    }

    private fun csvToPayload(text: String): AdvisorBudgetPayload {
        val map = mutableMapOf<String, String>()
        text.lineSequence().drop(1).forEach { line ->
            val parts = line.split(",", limit = 2)
            if (parts.size == 2) map[parts[0].trim()] = parts[1].trim().trim('"')
        }
        return AdvisorBudgetPayload(
            advisorName = map["asesor"].orEmpty(),
            branchName = map["sucursal"].orEmpty(),
            period = map["periodo"].orEmpty(),
            workingDays = map["dias_laborales"]?.toIntOrNull() ?: 22,
            goals = com.salesgoals.app.data.models.VariableSet(
                volume = map["volumen"]?.toDoubleOrNull() ?: 0.0,
                credit = map["credito"]?.toDoubleOrNull() ?: 0.0,
                warranty = map["garantia"]?.toDoubleOrNull() ?: 0.0,
                cashCredit = map["credito_efectivo"]?.toDoubleOrNull() ?: 0.0,
                phones = map["celulares"]?.toDoubleOrNull() ?: 0.0
            ),
            notes = map["notas"].orEmpty()
        )
    }
}

enum class ExportFormat { JSON, CSV }
