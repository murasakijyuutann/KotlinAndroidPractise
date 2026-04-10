package com.example.kotlinapppractise.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.kotlinapppractise.data.ItemRepository

/**
 * ─────────────────────────────────────────────
 *  LAYER 2 — VIEWMODEL  (Business Logic Layer)
 * ─────────────────────────────────────────────
 *
 * The ViewModel sits between the UI and the Repository.
 * Key responsibilities:
 *   • Hold and expose UI state that survives screen rotation
 *   • Validate user input BEFORE sending it to the repository
 *   • Call repository methods and translate results into UI state
 *   • Keep the UI layer completely ignorant of data details
 *
 * ViewModel lifetime:
 *   Created  → when the screen first opens
 *   Survives → screen rotation / configuration changes
 *   Destroyed→ when the screen is permanently removed from back-stack
 *
 * This is why heavy objects (repositories, network clients) live here,
 * not in a Composable function which recomposes frequently.
 */

// ── UI State Data Class ───────────────────────────────────────────────────
/**
 * A single data class that represents everything the UI needs to render.
 * Using a data class makes it easy to reason about the full state at once.
 */
data class MainUiState(
    val items: List<String>       = emptyList(),   // all items in repository
    val displayedList: List<String> = emptyList(), // filtered + sorted subset
    val inputText: String         = "",            // current text-field value
    val searchQuery: String       = "",            // current search text
    val isSorted: Boolean         = false,         // sort toggle
    val errorMessage: String?     = null           // null = no error
)

// ── ViewModel ────────────────────────────────────────────────────────────
class MainViewModel : ViewModel() {

    companion object {
        private const val TAG          = "MainViewModel"
        private const val MAX_LENGTH   = 50          // same limit as before
    }

    // Repository instance — the ViewModel owns it.
    // In a production app, this would be injected via Hilt/Koin.
    private val repository = ItemRepository()

    // Compose-observable state — any Composable that reads `uiState`
    // will automatically recompose when the value changes.
    var uiState by mutableStateOf(MainUiState())
        private set    // UI can READ, only ViewModel can WRITE

    init {
        Log.d(TAG, "ViewModel created — init block running")
        refreshDisplayedList()   // initialise the derived list
    }

    // ── Input field ──────────────────────────────────────────────────────

    /** Called every time the user types in the main text field. */
    fun onInputChanged(text: String) {
        try {
            if (text.length <= MAX_LENGTH) {
                uiState = uiState.copy(inputText = text, errorMessage = null)
                Log.d(TAG, "onInputChanged: length=${text.length}/$MAX_LENGTH")
            } else {
                uiState = uiState.copy(errorMessage = "Maximum $MAX_LENGTH characters allowed")
                Log.w(TAG, "onInputChanged: exceeds limit ${text.length}/$MAX_LENGTH")
            }
        } catch (e: Exception) {
            Log.e(TAG, "onInputChanged ERROR", e)
        }
    }

    // ── Add item ─────────────────────────────────────────────────────────

    /** Validates input then delegates to the repository. */
    fun addItem() {
        try {
            val text = uiState.inputText.trim()
            when {
                text.isBlank() -> {
                    uiState = uiState.copy(errorMessage = "Input cannot be empty")
                    Log.w(TAG, "addItem: blank input rejected")
                }
                text.length > MAX_LENGTH -> {
                    uiState = uiState.copy(errorMessage = "Maximum $MAX_LENGTH characters allowed")
                    Log.w(TAG, "addItem: input too long (${text.length})")
                }
                else -> {
                    val added = repository.addItem(text)
                    if (added) {
                        Log.d(TAG, "addItem: \"$text\" accepted by repository")
                        uiState = uiState.copy(inputText = "", errorMessage = null)
                        refreshDisplayedList()
                    } else {
                        uiState = uiState.copy(errorMessage = "\"$text\" already exists in the list")
                        Log.w(TAG, "addItem: duplicate \"$text\" rejected by repository")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "addItem ERROR", e)
        }
    }

    // ── Remove item ───────────────────────────────────────────────────────

    fun removeItem(item: String) {
        try {
            val removed = repository.removeItem(item)
            if (removed) {
                Log.d(TAG, "removeItem: \"$item\" removed")
                refreshDisplayedList()
            } else {
                Log.w(TAG, "removeItem: \"$item\" not found in repository")
            }
        } catch (e: Exception) {
            Log.e(TAG, "removeItem ERROR for \"$item\"", e)
        }
    }

    // ── Clear all ─────────────────────────────────────────────────────────

    fun clearItems() {
        try {
            val count = repository.clearItems()
            Log.d(TAG, "clearItems: $count item(s) cleared")
            uiState = uiState.copy(searchQuery = "")
            refreshDisplayedList()
        } catch (e: Exception) {
            Log.e(TAG, "clearItems ERROR", e)
        }
    }

    // ── Search ────────────────────────────────────────────────────────────

    fun onSearchChanged(query: String) {
        try {
            uiState = uiState.copy(searchQuery = query)
            Log.d(TAG, "onSearchChanged: \"$query\"")
            refreshDisplayedList()
        } catch (e: Exception) {
            Log.e(TAG, "onSearchChanged ERROR", e)
        }
    }

    // ── Sort ──────────────────────────────────────────────────────────────

    fun toggleSort() {
        try {
            val newSorted = !uiState.isSorted
            uiState = uiState.copy(isSorted = newSorted)
            Log.d(TAG, "toggleSort: isSorted=$newSorted")
            refreshDisplayedList()
        } catch (e: Exception) {
            Log.e(TAG, "toggleSort ERROR", e)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Re-fetches the item list from the repository, applies the active
     * search filter and sort preference, then updates [uiState].
     *
     * Call this after every operation that changes repository data
     * or filter/sort settings.
     */
    private fun refreshDisplayedList() {
        try {
            val all = repository.getItems()
            val query = uiState.searchQuery
            val sorted = uiState.isSorted

            val filtered = if (query.isBlank()) all
                           else all.filter { it.contains(query, ignoreCase = true) }

            val displayed = if (sorted) filtered.sorted() else filtered

            uiState = uiState.copy(items = all, displayedList = displayed)

            Log.d(
                TAG,
                "refreshDisplayedList: total=${all.size}, shown=${displayed.size}, " +
                "sorted=$sorted, query=\"$query\""
            )
        } catch (e: Exception) {
            Log.e(TAG, "refreshDisplayedList ERROR", e)
        }
    }

    // ── ViewModel lifecycle ────────────────────────────────────────────────

    /**
     * onCleared() is called by Android right before the ViewModel is destroyed.
     * Use it to cancel coroutines, close streams, release resources, etc.
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared — ViewModel is being destroyed")
    }
}

