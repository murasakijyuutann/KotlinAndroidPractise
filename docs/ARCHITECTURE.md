# 🏗️ Architecture Guide

This document explains the three-layer architecture used in this project,
why each layer exists, and exactly what each piece of code is responsible for.

---

## Why Split Into Layers?

Before the refactor, all logic lived inside one `@Composable` function:

```kotlin
// ❌ Old approach — everything in one place
@Composable
fun MainScreen() {
    var inputText by remember { mutableStateOf("") }   // state
    var itemList  by remember { mutableStateOf(listOf<String>()) }

    Button(onClick = {
        itemList = itemList + inputText                 // logic
    }) { Text("Add") }
}
```

**Problems with this:**

| Problem | Why it hurts |
|---------|-------------|
| `remember {}` state is reset on screen rotation | User loses all their data just by tilting the phone |
| Logic is inseparable from the UI | Cannot write a unit test without spinning up the whole composable |
| File grows to hundreds of lines | Hard to navigate, hard to change one thing without breaking another |

The solution is to give **each concern its own layer**.

---

## The Three Layers

```
┌─────────────────────────────────────────────┐
│  LAYER 3 · UI  —  MainActivity.kt           │
│                    MainScreen @Composable   │
│                                             │
│  Job: draw the screen, forward user input   │
│  Knows about: ViewModel (read state + call  │
│               functions)                    │
│  Does NOT know: how data is stored, or any  │
│                 validation rules            │
└──────────────────────┬──────────────────────┘
                       │  reads  uiState
                       │  calls  addItem() / removeItem() / …
┌──────────────────────▼──────────────────────┐
│  LAYER 2 · ViewModel  —  MainViewModel.kt   │
│                                             │
│  Job: validate input, manage UI state,      │
│       translate repository results into     │
│       error messages the UI can display     │
│  Knows about: Repository (calls its         │
│               functions)                    │
│  Does NOT know: what the UI looks like,     │
│                 or how data is persisted    │
└──────────────────────┬──────────────────────┘
                       │  add / remove / clear / get
┌──────────────────────▼──────────────────────┐
│  LAYER 1 · Repository  —  ItemRepository.kt │
│                                             │
│  Job: store and retrieve items              │
│  Knows about: the actual data structure     │
│               (in-memory list today,        │
│                Room DB tomorrow)            │
│  Does NOT know: ViewModels, UI, or          │
│                 validation rules            │
└─────────────────────────────────────────────┘
```

---

## Layer 1 — `ItemRepository.kt`

### Responsibility
Be the **single source of truth** for item data.  
Only this class reads or writes `_items`. Nobody else.

### Key code decisions

#### Private backing list
```kotlin
private val _items = mutableListOf<String>()
```
- `private` — other classes cannot access `_items` at all
- The underscore prefix `_` is a Kotlin/Android convention for "internal backing property"

#### Return a copy, not the original
```kotlin
fun getItems(): List<String> = _items.toList()
```
- `.toList()` creates a **new read-only snapshot**
- If we returned `_items` directly, a caller could call `.add()` on it and bypass all the duplicate-checking logic

#### Boolean return values communicate success/failure
```kotlin
fun addItem(item: String): Boolean   // true = added, false = duplicate
fun removeItem(item: String): Boolean // true = removed, false = not found
fun clearItems(): Int                 // returns how many were deleted
```
- The ViewModel uses these return values to decide what error message to show the user
- Functions that say what happened are much easier to test than functions that silently do nothing

#### Every function is wrapped in `try/catch`
```kotlin
return try {
    _items.add(trimmed)
    true
} catch (e: Exception) {
    Log.e(TAG, "addItem() ERROR", e)
    false
}
```
- If something unexpected goes wrong, we log it and return a safe value
- The app never crashes from a data operation — it degrades gracefully

---

## Layer 2 — `MainViewModel.kt`

### Responsibility
Be the **brain of the screen**: validate input, call the repository, and expose
the result as simple state the UI can render without any logic of its own.

### Key code decisions

#### `MainUiState` — one object for everything
```kotlin
data class MainUiState(
    val items: List<String>         = emptyList(),
    val displayedList: List<String> = emptyList(),
    val inputText: String           = "",
    val searchQuery: String         = "",
    val isSorted: Boolean           = false,
    val errorMessage: String?       = null       // null means "no error"
)
```
- A `data class` automatically generates `.copy()`, `equals()`, `hashCode()`, and `toString()`
- `.copy()` lets us change one field at a time cleanly (see below)
- `errorMessage: String?` — the `?` means nullable. `null` = "everything is fine"

#### Compose-observable state with write protection
```kotlin
var uiState by mutableStateOf(MainUiState())
    private set
```
- `mutableStateOf` — Compose watches this. When it changes, any composable that read it redraws automatically
- `private set` — the UI composable can **read** `uiState`, but cannot **write** it. Only the ViewModel can write. This enforces the one-way data flow

#### `uiState.copy()` — immutable updates
```kotlin
// Change only errorMessage; everything else stays the same
uiState = uiState.copy(errorMessage = "Input cannot be empty")

// Change two fields at once
uiState = uiState.copy(inputText = "", errorMessage = null)
```
- Never mutate individual fields; always produce a **new state object**
- Compose detects the new object and redraws — this is the standard pattern for state management in Compose

#### `init {}` — runs once at creation
```kotlin
init {
    Log.d(TAG, "ViewModel created — init block running")
    refreshDisplayedList()
}
```
- The `init` block runs exactly once: when the ViewModel is first created
- **Rotate the screen** and you will see in Logcat that "ViewModel created" does NOT appear again — the ViewModel instance survived the rotation

#### Validation chain in `addItem()`
```kotlin
fun addItem() {
    val text = uiState.inputText.trim()
    when {
        text.isBlank()           -> { /* error: empty */ }
        text.length > MAX_LENGTH -> { /* error: too long */ }
        else -> {
            val added = repository.addItem(text)    // ask the repository
            if (added) { /* success */ } else { /* error: duplicate */ }
        }
    }
}
```
- `when { }` is Kotlin's multi-branch expression, cleaner than nested `if/else`
- Local checks happen **before** calling the repository, saving unnecessary work
- The duplicate check is delegated to the repository because only the repository knows what is stored

#### `refreshDisplayedList()` — the central pipeline
```kotlin
private fun refreshDisplayedList() {
    val all      = repository.getItems()                              // 1. fetch
    val filtered = if (query.isBlank()) all
                   else all.filter { it.contains(query, ignoreCase = true) }  // 2. filter
    val displayed = if (sorted) filtered.sorted() else filtered       // 3. sort
    uiState = uiState.copy(items = all, displayedList = displayed)   // 4. push to UI
}
```
- Called after every change (add, remove, clear, search, sort)
- One function = one place to maintain the pipeline logic

#### `onCleared()` — ViewModel teardown hook
```kotlin
override fun onCleared() {
    super.onCleared()
    Log.d(TAG, "onCleared — ViewModel is being destroyed")
}
```
- Called by Android when the user **permanently leaves** the screen (presses Back and exits)
- In a real app: cancel Coroutines, close database connections, release camera handles
- Here it just logs so you can observe it in Logcat

---

## Layer 3 — `MainActivity.kt` / `MainScreen`

### Responsibility
**Draw** the current `uiState` and **forward** user gestures to the ViewModel. Nothing else.

### Key code decisions

#### Getting the ViewModel
```kotlin
val vm: MainViewModel = viewModel()
```
- `viewModel()` is a Compose extension from `lifecycle-viewmodel-compose`
- First call → creates a new `MainViewModel`
- Every recomposition / rotation → returns the **exact same instance**

#### Reading state
```kotlin
val state = viewModel.uiState   // one line, everything the screen needs
```
- Because `uiState` is backed by `mutableStateOf`, Compose registers a dependency here
- When `uiState` changes → this composable recomposes → UI updates

#### Forwarding events
```kotlin
onClick      = { viewModel.addItem() }
onClick      = { viewModel.removeItem(item) }
onValueChange = { viewModel.onInputChanged(it) }
```
- The UI never calculates anything, never touches a list, never validates
- It says "something happened" and lets the ViewModel handle it

#### Unidirectional Data Flow (UDF)
```
State flows DOWN:   ViewModel.uiState ──▶ MainScreen draws it
Events flow UP:     User taps ──▶ MainScreen calls ──▶ ViewModel handles it
```
This one-way flow makes bugs much easier to trace.

---

## Lifecycle of a Single "Add" Action

```
1. User types "Apple" → onValueChange fires
2. MainScreen calls viewModel.onInputChanged("Apple")
3. ViewModel checks length ≤ 50 → updates uiState.inputText = "Apple"
4. Compose detects uiState changed → redraws OutlinedTextField with "Apple"

5. User taps "Add to list" → onClick fires
6. MainScreen calls viewModel.addItem()
7. ViewModel trims → checks blank → checks length → calls repository.addItem("Apple")
8. Repository checks duplicates → adds to _items → returns true
9. ViewModel receives true → clears inputText → calls refreshDisplayedList()
10. refreshDisplayedList() fetches from repository → applies filter/sort → updates uiState
11. Compose detects uiState changed → redraws the entire screen with "Apple" in the list
```

---

## What Happens on Screen Rotation?

```
Phone is rotated
       ↓
Android destroys MainActivity  (onPause → onStop → onDestroy)
       ↓
Android recreates MainActivity (onCreate → onStart → onResume)
       ↓
setContent { } runs again
       ↓
viewModel() is called — but ViewModel already exists, so returns SAME instance
       ↓
val state = viewModel.uiState — reads the SAME uiState that was there before rotation
       ↓
Screen redraws with all items intact ✔
```

> **Try it:** add a few items, rotate the emulator (Ctrl+F11), items are still there.
> Open Logcat and notice "ViewModel created" does NOT appear a second time.

