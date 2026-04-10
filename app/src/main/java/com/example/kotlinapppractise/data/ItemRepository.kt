package com.example.kotlinapppractise.data

import android.util.Log

/**
 * ─────────────────────────────────────────────
 *  LAYER 1 — REPOSITORY  (Data Layer)
 * ─────────────────────────────────────────────
 *
 * The Repository is the single source of truth for data.
 * It abstracts where data comes from (memory, database, network, etc.)
 * so that the ViewModel never needs to know the details.
 *
 * In a real app this would talk to:
 *   • Room database  → local persistence
 *   • Retrofit API   → remote server
 *   • DataStore      → preferences
 *
 * For this learning demo everything is kept in-memory.
 */
class ItemRepository {

    companion object {
        private const val TAG = "ItemRepository"
    }

    // Internal mutable list — only this class can modify it
    private val _items = mutableListOf<String>()

    // ── Read ──────────────────────────────────────────────────────────────

    /**
     * Returns an immutable snapshot of the current item list.
     * Callers cannot accidentally mutate the repository's data.
     */
    fun getItems(): List<String> {
        val snapshot = _items.toList()
        Log.d(TAG, "getItems() → returning ${snapshot.size} item(s)")
        return snapshot
    }

    // ── Write ─────────────────────────────────────────────────────────────

    /**
     * Attempts to add [item] (trimmed) to the list.
     * @return true  → item was added successfully
     *         false → duplicate detected, nothing changed
     */
    fun addItem(item: String): Boolean {
        val trimmed = item.trim()
        return try {
            if (_items.any { it.equals(trimmed, ignoreCase = true) }) {
                Log.w(TAG, "addItem() DUPLICATE → \"$trimmed\" already exists")
                false
            } else {
                _items.add(trimmed)
                Log.d(TAG, "addItem() OK → \"$trimmed\" | total=${_items.size}")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "addItem() ERROR for \"$trimmed\"", e)
            false
        }
    }

    /**
     * Removes the exact [item] string from the list.
     * @return true  → item was found and removed
     *         false → item not found
     */
    fun removeItem(item: String): Boolean {
        return try {
            val removed = _items.remove(item)
            if (removed) {
                Log.d(TAG, "removeItem() OK → \"$item\" | remaining=${_items.size}")
            } else {
                Log.w(TAG, "removeItem() NOT FOUND → \"$item\"")
            }
            removed
        } catch (e: Exception) {
            Log.e(TAG, "removeItem() ERROR for \"$item\"", e)
            false
        }
    }

    /**
     * Removes all items from the list.
     * @return the number of items that were cleared
     */
    fun clearItems(): Int {
        return try {
            val count = _items.size
            _items.clear()
            Log.d(TAG, "clearItems() → cleared $count item(s)")
            count
        } catch (e: Exception) {
            Log.e(TAG, "clearItems() ERROR", e)
            0
        }
    }
}

