package com.example.kotlinapppractise

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinapppractise.ui.theme.KotlinAppPractiseTheme
import com.example.kotlinapppractise.viewmodel.MainViewModel

/**
 * ─────────────────────────────────────────────
 *  LAYER 3 — UI  (Screen / "Fragment" Layer)
 * ─────────────────────────────────────────────
 *
 * Architecture recap:
 *
 *   ┌─────────────────────────────────────────┐
 *   │  MainActivity / MainScreen  (UI Layer)   │  ← You are here
 *   │  • Renders state from ViewModel          │
 *   │  • Sends user events → ViewModel         │
 *   └──────────────────┬──────────────────────┘
 *                      │ calls fun / reads uiState
 *   ┌──────────────────▼──────────────────────┐
 *   │  MainViewModel  (Business Logic Layer)   │
 *   │  • Validates input                       │
 *   │  • Manages UI state (survives rotation)  │
 *   │  • Delegates data ops → Repository       │
 *   └──────────────────┬──────────────────────┘
 *                      │ add / remove / clear / get
 *   ┌──────────────────▼──────────────────────┐
 *   │  ItemRepository  (Data Layer)            │
 *   │  • Single source of truth                │
 *   │  • In-memory list (could be Room/API)    │
 *   └─────────────────────────────────────────┘
 *
 * Why split these layers?
 *   ✔ Each layer has ONE responsibility → easier to test
 *   ✔ ViewModel survives rotation → no data loss
 *   ✔ Repository can be swapped (e.g., Room DB) without touching UI
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("Lifecycle", "onCreate called")
        setContent {
            KotlinAppPractiseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // viewModel() is a Compose helper that:
                    //   • Creates MainViewModel the first time
                    //   • Returns the SAME instance on every recomposition & rotation
                    val vm: MainViewModel = viewModel()
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = vm
                    )
                }
            }
        }
    }

    override fun onStart()   { super.onStart();   Log.d("Lifecycle", "onStart called") }
    override fun onResume()  { super.onResume();  Log.d("Lifecycle", "onResume called") }
    override fun onPause()   { super.onPause();   Log.d("Lifecycle", "onPause called") }
    override fun onStop()    { super.onStop();    Log.d("Lifecycle", "onStop called") }
    override fun onDestroy() { super.onDestroy(); Log.d("Lifecycle", "onDestroy called") }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MainScreen — UI Layer
//  Reads from viewModel.uiState and sends events back via viewModel.xxx()
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel          // injected from MainActivity
) {
    // Reading uiState here makes this Composable recompose whenever it changes.
    // The ViewModel owns the state; the UI just observes and renders it.
    val state = viewModel.uiState

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Architecture label ────────────────────────────────────────────
        Text(
            text = "UI Screen → ViewModel → Repository",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        HorizontalDivider()

        // ── Input field with validation + character counter ───────────────
        OutlinedTextField(
            value = state.inputText,
            onValueChange = { viewModel.onInputChanged(it) },   // event → VM
            label = { Text("Enter something") },
            isError = state.errorMessage != null,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Error message (left side)
                    Text(
                        text = state.errorMessage ?: "",
                        color = if (state.errorMessage != null)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Character counter (right side)
                    Text(
                        text = "${state.inputText.length}/50",
                        color = if (state.errorMessage != null)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // ── Add button ────────────────────────────────────────────────────
        Button(
            onClick = { viewModel.addItem() },          // event → VM → Repository
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add to list")
        }

        // ── Clear button ──────────────────────────────────────────────────
        OutlinedButton(
            onClick = { viewModel.clearItems() },       // event → VM → Repository
            modifier = Modifier.fillMaxWidth(),
            enabled = state.items.isNotEmpty()
        ) {
            Text("Clear All")
        }

        // ── Item count + Sort toggle ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.searchQuery.isBlank())
                           "Items: ${state.items.size}"
                       else
                           "Items: ${state.items.size}  (${state.displayedList.size} shown)",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = { viewModel.toggleSort() },   // event → VM
                enabled = state.items.isNotEmpty()
            ) {
                Text(if (state.isSorted) "Sorted ✓" else "Sort A–Z")
            }
        }

        // ── Search / Filter ───────────────────────────────────────────────
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchChanged(it) },  // event → VM
            label = { Text("Search list") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.items.isNotEmpty()
        )

        // ── List (filtered + sorted by ViewModel) ─────────────────────────
        // The UI just renders whatever displayedList the ViewModel computed.
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.displayedList) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item)
                        TextButton(
                            onClick = { viewModel.removeItem(item) }    // event → VM → Repository
                        ) {
                            Text("X")
                        }
                    }
                }
            }
        }
    }
}