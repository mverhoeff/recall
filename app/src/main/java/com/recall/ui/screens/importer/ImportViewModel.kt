package com.recall.ui.screens.importer

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.db.Deck
import com.recall.data.repo.BackupRepository
import com.recall.data.repo.DeckRepository
import com.recall.io.DeckFileReader
import com.recall.io.ParsedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

const val UNREADABLE = "This file couldn't be opened."

// The same wait as opening a deck from the Library, so the deck page slides in already filled.
private const val OPEN_WAIT_MS = 300L

data class PickedFile(val name: String, val sizeBytes: Long, val parsed: ParsedFile)

data class ImportUiState(
    val file: PickedFile? = null,
    val decks: List<Deck> = emptyList(),
    val intoExisting: Boolean = false,
    val newDeckName: String = "",
    val existingDeckId: Long? = null,
    val busy: Boolean = false,
) {
    val canImport: Boolean
        get() {
            val parsed = file?.parsed as? ParsedFile.Deck ?: return false
            if (parsed.cards.isEmpty() || busy) return false
            return if (intoExisting) existingDeckId != null else newDeckName.isNotBlank()
        }
}

class ImportViewModel(
    targetDeckId: Long?,
    private val repository: DeckRepository,
    private val backups: BackupRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState(intoExisting = targetDeckId != null, existingDeckId = targetDeckId))
    val state: StateFlow<ImportUiState> = _state

    init {
        viewModelScope.launch {
            val decks = repository.summaries.first().map { it.deck }.sortedBy { it.name.lowercase() }
            _state.update { current ->
                current.copy(
                    decks = decks,
                    intoExisting = current.intoExisting && decks.isNotEmpty(),
                    existingDeckId = current.existingDeckId?.takeIf { id -> decks.any { it.id == id } },
                )
            }
        }
    }

    fun load(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { read(resolver, uri) }
            val name = (file.parsed as? ParsedFile.Deck)?.name.orEmpty()
            _state.update { it.copy(file = file, newDeckName = name) }
        }
    }

    fun setIntoExisting(existing: Boolean) = _state.update { it.copy(intoExisting = existing && it.decks.isNotEmpty()) }

    fun setNewDeckName(name: String) = _state.update { it.copy(newDeckName = name) }

    fun selectDeck(deckId: Long) = _state.update { it.copy(existingDeckId = deckId) }

    /** Imports the previewed cards; [onImported] gets the deck they went into. */
    fun import(onImported: (Long) -> Unit) {
        val current = _state.value
        if (!current.canImport) return
        val parsed = current.file?.parsed as? ParsedFile.Deck ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val deckId = if (current.intoExisting) {
                repository.importCards(current.existingDeckId, null, parsed.cards)
            } else {
                repository.importCards(null, current.newDeckName, parsed.cards)
            }
            withTimeoutOrNull(OPEN_WAIT_MS) { repository.prefetchDetails(deckId) }
            onImported(deckId)
        }
    }

    fun restore(onRestored: () -> Unit) {
        val backup = (_state.value.file?.parsed as? ParsedFile.Backup)?.backup ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            backups.restore(backup)
            onRestored()
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
