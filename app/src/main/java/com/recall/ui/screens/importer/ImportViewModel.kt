package com.recall.ui.screens.importer

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.io.DeckFileReader
import com.recall.io.ParsedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

const val UNREADABLE = "This file couldn't be opened."

data class PickedFile(val name: String, val sizeBytes: Long, val parsed: ParsedFile)

data class ImportUiState(val file: PickedFile? = null)

class ImportViewModel : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state

    fun load(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { read(resolver, uri) }
            _state.update { it.copy(file = file) }
        }
    }

    private fun read(resolver: ContentResolver, uri: Uri): PickedFile {
        var name = uri.lastPathSegment.orEmpty()
        var size = -1L
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    if (!cursor.isNull(0)) name = cursor.getString(0)
                    if (!cursor.isNull(1)) size = cursor.getLong(1)
                }
            }
            val bytes = resolver.openInputStream(uri)?.use { it.readAtMost(DeckFileReader.MAX_BYTES + 1) }
                ?: return PickedFile(name, size.coerceAtLeast(0), ParsedFile.Failure(UNREADABLE))
            return PickedFile(name, if (size >= 0) size else bytes.size.toLong(), DeckFileReader.read(bytes, name))
        } catch (_: IOException) {
        } catch (_: SecurityException) {
        }
        return PickedFile(name, size.coerceAtLeast(0), ParsedFile.Failure(UNREADABLE))
    }
}

// Reading one byte past the limit is enough for the reader to call the file too large, without holding all of it.
private fun InputStream.readAtMost(limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (out.size() < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read < 0) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}
