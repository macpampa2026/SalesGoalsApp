package com.salesgoals.app.utils

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton que sostiene la URI de un archivo entrante (intent VIEW o SEND)
 * mientras la UI lo procesa.
 */
object PendingImport {
    private val _uri = MutableStateFlow<Uri?>(null)
    val uri: StateFlow<Uri?> = _uri.asStateFlow()

    fun set(uri: Uri?) { _uri.value = uri }
    fun consume(): Uri? {
        val u = _uri.value
        _uri.value = null
        return u
    }
}
