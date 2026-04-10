# 🔍 Logcat Guide

Logcat is Android's real-time log viewer built into Android Studio.  
Every layer of this app writes structured logs so you can watch exactly what happens,
in what order, and why.

---

## Opening Logcat in Android Studio

1. Run the app on an emulator or device
2. At the bottom of Android Studio, click **Logcat** (or press **Alt+6**)
3. Make sure your device/emulator is selected in the dropdown at the top

---

## Filtering by Tag

This app uses three distinct tags. Filter one at a time to focus on a single layer.

| Tag to filter | Layer | What you will see |
|---|---|---|
| `Lifecycle` | Activity | Every Android lifecycle method as it fires |
| `MainViewModel` | ViewModel | All user actions, validation results, state changes |
| `ItemRepository` | Repository | Raw data operations — success, failure, counts |

**How to filter in Android Studio Logcat:**  
In the search box at the top of the Logcat panel, type:
```
tag:Lifecycle
```
or
```
tag:MainViewModel
```
or combine them:
```
tag:Lifecycle|tag:MainViewModel|tag:ItemRepository
```

---

## Log Levels

Android has five log levels. We use all three meaningful ones in this project.

| Level | Method | Color in Logcat | Used when |
|-------|--------|----------------|-----------|
| DEBUG | `Log.d(tag, msg)` | White/Grey | Normal operation — "this happened successfully" |
| WARN  | `Log.w(tag, msg)` | Yellow/Orange | Something was rejected — not a crash, just unexpected input |
| ERROR | `Log.e(tag, msg, exception)` | Red | An exception was caught — should not normally appear |

---

## What You Should See — Scenario Walkthroughs

### App first launch

```
D  Lifecycle        onCreate called
D  Lifecycle        onStart called
D  Lifecycle        onResume called
D  MainViewModel    ViewModel created — init block running
D  ItemRepository   getItems() → returning 0 item(s)
D  MainViewModel    refreshDisplayedList: total=0, shown=0, sorted=false, query=""
```

**What this tells you:**
- Activity lifecycle fires in order: onCreate → onStart → onResume
- ViewModel `init` block runs once, right after creation
- `refreshDisplayedList` immediately fetches from the repository (0 items at start)

---

### Typing "Apple" in the text field (one character at a time)

```
D  MainViewModel    onInputChanged: length=1/50
D  MainViewModel    onInputChanged: length=2/50
D  MainViewModel    onInputChanged: length=3/50
D  MainViewModel    onInputChanged: length=4/50
D  MainViewModel    onInputChanged: length=5/50
```

**What this tells you:**
- `onInputChanged` fires on every keystroke
- The ViewModel updates `inputText` and clears any error message each time

---

### Tapping "Add to list" with "Apple" typed

```
D  ItemRepository   addItem() OK → "Apple" | total=1
D  MainViewModel    addItem: "Apple" accepted by repository
D  ItemRepository   getItems() → returning 1 item(s)
D  MainViewModel    refreshDisplayedList: total=1, shown=1, sorted=false, query=""
```

**What this tells you:**
- The Repository confirms the add and reports the new total
- The ViewModel receives `true` (success) from the repository
- `refreshDisplayedList` is called → fetches → updates the displayed list

---

### Trying to add "Apple" again (duplicate)

```
W  ItemRepository   addItem() DUPLICATE → "Apple" already exists
W  MainViewModel    addItem: duplicate "Apple" rejected by repository
```

**What this tells you:**
- `Log.w` (WARN) appears — yellow in Logcat — because this is unexpected input, not a crash
- Repository catches the duplicate first; ViewModel receives `false` and sets an error message
- No `refreshDisplayedList` is called (nothing changed in the data)

---

### Typing when the input is already 50 characters

```
W  MainViewModel    onInputChanged: exceeds limit 51/50
```

**What this tells you:**
- The ViewModel rejected the character before it was added to `inputText`
- The text field stayed at 50 characters (the state was never updated with the 51st char)

---

### Searching for "app"

```
D  MainViewModel    onSearchChanged: "a"
D  ItemRepository   getItems() → returning 3 item(s)
D  MainViewModel    refreshDisplayedList: total=3, shown=2, sorted=false, query="a"
D  MainViewModel    onSearchChanged: "ap"
D  ItemRepository   getItems() → returning 3 item(s)
D  MainViewModel    refreshDisplayedList: total=3, shown=1, sorted=false, query="ap"
D  MainViewModel    onSearchChanged: "app"
D  ItemRepository   getItems() → returning 3 item(s)
D  MainViewModel    refreshDisplayedList: total=3, shown=1, sorted=false, query="app"
```

**What this tells you:**
- Every character change triggers a new filter pass
- `total=3` stays constant (all items are always in the repository)
- `shown=` drops as the filter gets more specific

---

### Tapping "Sort A–Z"

```
D  MainViewModel    toggleSort: isSorted=true
D  ItemRepository   getItems() → returning 3 item(s)
D  MainViewModel    refreshDisplayedList: total=3, shown=3, sorted=true, query=""
```

Tapping again:
```
D  MainViewModel    toggleSort: isSorted=false
D  ItemRepository   getItems() → returning 3 item(s)
D  MainViewModel    refreshDisplayedList: total=3, shown=3, sorted=false, query=""
```

---

### Deleting an item

```
D  ItemRepository   removeItem() OK → "Apple" | remaining=2
D  MainViewModel    removeItem: "Apple" removed
D  ItemRepository   getItems() → returning 2 item(s)
D  MainViewModel    refreshDisplayedList: total=2, shown=2, sorted=false, query=""
```

---

### Tapping "Clear All" (3 items in list)

```
D  ItemRepository   clearItems() → cleared 3 item(s)
D  MainViewModel    clearItems: 3 item(s) cleared
D  ItemRepository   getItems() → returning 0 item(s)
D  MainViewModel    refreshDisplayedList: total=0, shown=0, sorted=false, query=""
```

---

### Rotating the screen (with 2 items in the list)

```
D  Lifecycle        onPause called
D  Lifecycle        onStop called
D  Lifecycle        onDestroy called
D  Lifecycle        onCreate called
D  Lifecycle        onStart called
D  Lifecycle        onResume called
D  ItemRepository   getItems() → returning 2 item(s)
D  MainViewModel    refreshDisplayedList: total=2, shown=2, sorted=false, query=""
```

**What this tells you (this is the key lesson):**
- `onDestroy` fires — the Activity is completely destroyed
- `onCreate` fires — a brand new Activity is created
- **`MainViewModel` does NOT log "ViewModel created"** — the ViewModel instance survived, because Android keeps it alive through configuration changes
- `refreshDisplayedList` runs again (because `MainScreen` recomposes), but the data is already there — the repository still has all 2 items

---

### Pressing Back to exit the app

```
D  Lifecycle        onPause called
D  Lifecycle        onStop called
D  Lifecycle        onDestroy called
D  MainViewModel    onCleared — ViewModel is being destroyed
```

**What this tells you:**
- This time `onCleared` fires — the ViewModel is finally destroyed because the user left permanently
- Compare with rotation: rotation triggers `onDestroy` but NOT `onCleared`

---

## Quick Reference — Log Pattern

```
[Level]  [Tag]            [Message]
  D      ItemRepository   addItem() OK → "Banana" | total=2
  W      MainViewModel    addItem: duplicate "Banana" rejected by repository
  E      MainViewModel    addItem ERROR  ← only if an exception was thrown (rare)
```

