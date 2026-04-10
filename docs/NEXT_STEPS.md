# 🚀 Next Steps — Practice Ideas

This app is a solid foundation. Every idea below builds directly on the code you already have.  
They are ordered from easiest to hardest. Each one is a real Android skill used in production apps.

---

## ⭐ Beginner

### 1. Edit an existing item
**What to add:** a text field inside each Card that appears when you tap the item text.  
**What you will learn:** conditional UI, updating a specific item in a list.

```kotlin
// In ItemRepository, add:
fun updateItem(old: String, new: String): Boolean

// In MainViewModel, add:
fun updateItem(old: String, new: String)
```

---

### 2. Item count badge on the "Clear All" button
**What to add:** show the number of items inside the button text: `"Clear All (3)"`.  
**What you will learn:** reading state in UI, string interpolation.

```kotlin
// In MainScreen — just change the button text:
Text("Clear All (${state.items.size})")
```

---

### 3. Empty state message
**What to add:** when the list is empty, show a centred message like "No items yet. Add one above!"  
**What you will learn:** conditional composables, `if/else` inside `@Composable`.

```kotlin
if (state.displayedList.isEmpty()) {
    Text("No items yet.")
} else {
    LazyColumn { ... }
}
```

---

### 4. Dismiss error on focus change
**What to add:** clear the `errorMessage` when the user taps away from the text field.  
**What you will learn:** `FocusManager`, `onFocusChanged` modifier.

---

## ⭐⭐ Intermediate

### 5. Undo delete with a Snackbar
**What to add:** when an item is deleted, show a Snackbar with an "Undo" action.  
**What you will learn:** `SnackbarHostState`, coroutines, temporary state.

```kotlin
// In ViewModel, keep the last deleted item:
private var lastDeletedItem: String? = null

fun removeItem(item: String) {
    lastDeletedItem = item
    // ... delete ...
}

fun undoDelete() {
    lastDeletedItem?.let { addItem(it) }
    lastDeletedItem = null
}
```

---

### 6. Item timestamps
**What to add:** store when each item was added and show it as "Added 2 min ago".  
**What you will learn:** `data class`, `System.currentTimeMillis()`, formatting time.

```kotlin
// Replace List<String> with:
data class Item(val text: String, val addedAt: Long = System.currentTimeMillis())
```
This change flows through all 3 layers — a great way to see how connected they are.

---

### 7. Persist data with DataStore
**What to add:** save and reload the list so it survives the app being fully closed.  
**What you will learn:** `DataStore<Preferences>`, coroutines, `viewModelScope`, `suspend` functions.

```kotlin
// Add to libs.versions.toml:
// androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version = "1.0.0" }

// In ItemRepository, inject a DataStore and use:
suspend fun saveItems(items: List<String>)
suspend fun loadItems(): List<String>
```

---

### 8. Multiple screens with Navigation
**What to add:** a second screen that shows the details of a tapped item.  
**What you will learn:** `NavController`, `NavHost`, passing arguments between screens, back stack.

```kotlin
// Add navigation-compose dependency
// Define routes:
sealed class Screen(val route: String) {
    object List   : Screen("list")
    object Detail : Screen("detail/{itemText}")
}
```

---

## ⭐⭐⭐ Advanced

### 9. Room Database
**What to add:** replace the in-memory `_items` list in `ItemRepository` with a Room database.  
**What you will learn:** `@Entity`, `@Dao`, `@Database`, `Flow<List<Item>>`, migrations.

```kotlin
@Entity
data class ItemEntity(@PrimaryKey val text: String, val addedAt: Long)

@Dao
interface ItemDao {
    @Query("SELECT * FROM itementity") fun getAll(): Flow<List<ItemEntity>>
    @Insert suspend fun insert(item: ItemEntity)
    @Delete suspend fun delete(item: ItemEntity)
}
```
Because the ViewModel and UI don't know where data comes from, you only need to change `ItemRepository`.

---

### 10. Dependency Injection with Hilt
**What to add:** inject `ItemRepository` into `MainViewModel` instead of creating it manually.  
**What you will learn:** `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Inject`.

```kotlin
// Instead of: private val repository = ItemRepository()
// ViewModel becomes:
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ItemRepository
) : ViewModel()
```
This makes the app fully testable — you can inject a fake repository in tests.

---

### 11. Unit Tests for the ViewModel
**What to add:** JUnit tests that verify `addItem`, duplicate detection, and sort logic.  
**What you will learn:** `TestCoroutineDispatcher`, mocking with `Mockito`, asserting state changes.

```kotlin
class MainViewModelTest {
    @Test
    fun `addItem with blank input sets error message`() {
        val vm = MainViewModel()
        vm.onInputChanged("")
        vm.addItem()
        assertEquals("Input cannot be empty", vm.uiState.errorMessage)
    }
}
```

---

### 12. Retrofit — Fetch items from a real API
**What to add:** load items from a public REST API (e.g., JSONPlaceholder) instead of typing them manually.  
**What you will learn:** `Retrofit`, `suspend` functions, `viewModelScope.launch`, loading states.

```kotlin
// New state fields:
data class MainUiState(
    // ...existing fields...
    val isLoading: Boolean = false,
    val networkError: String? = null
)

// In ViewModel:
fun loadFromNetwork() {
    viewModelScope.launch {
        uiState = uiState.copy(isLoading = true)
        try {
            val items = repository.fetchFromApi()
            // ...
        } catch (e: IOException) {
            uiState = uiState.copy(networkError = "No internet connection")
        } finally {
            uiState = uiState.copy(isLoading = false)
        }
    }
}
```

---

## Skill Progression Summary

```
Current app
    │
    ├── ⭐   Edit items, empty state, dismiss error
    ├── ⭐⭐  Undo delete, timestamps, DataStore, Navigation
    └── ⭐⭐⭐ Room DB, Hilt DI, Unit Tests, Retrofit API
```

Each step introduces one new concept while reusing everything you already understand.  
The architecture you have now (Repository → ViewModel → UI) scales to all of them.

