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
import com.example.kotlinapppractise.ui.theme.KotlinAppPractiseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("Lifecycle", "onCreate called")
        setContent {
            KotlinAppPractiseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        Log.d("Lifecycle", "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d("Lifecycle", "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d("Lifecycle", "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("Lifecycle", "onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Lifecycle", "onDestroy called")
    }
}

private const val MAX_INPUT_LENGTH = 50

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }
    var itemList by remember { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = {
                try {
                    if (it.length <= MAX_INPUT_LENGTH) {
                        inputText = it
                        errorMessage = null
                        Log.d("MainScreen", "Input changed: length=${it.length}/$MAX_INPUT_LENGTH")
                    } else {
                        errorMessage = "Maximum $MAX_INPUT_LENGTH characters allowed"
                        Log.w("MainScreen", "Input exceeds max length: ${it.length}/$MAX_INPUT_LENGTH")
                    }
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error handling input change", e)
                }
            },
            label = { Text("Enter something") },
            isError = errorMessage != null,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = if (errorMessage != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${inputText.length}/$MAX_INPUT_LENGTH",
                        color = if (errorMessage != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                try {
                    when {
                        inputText.isBlank() -> {
                            errorMessage = "Input cannot be empty"
                            Log.w("MainScreen", "Add attempted but input was blank")
                        }
                        inputText.length > MAX_INPUT_LENGTH -> {
                            errorMessage = "Maximum $MAX_INPUT_LENGTH characters allowed"
                            Log.w("MainScreen", "Add attempted but input exceeds max length: ${inputText.length}/$MAX_INPUT_LENGTH")
                        }
                        else -> {
                            itemList = itemList + inputText.trim()
                            Log.d("MainScreen", "Item added: $inputText | Total items: ${itemList.size}")
                            inputText = ""
                            errorMessage = null
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error adding item", e)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add to list")
        }

        OutlinedButton(
            onClick = {
                try {
                    val clearedCount = itemList.size
                    itemList = emptyList()
                    Log.d("MainScreen", "List cleared | $clearedCount items removed")
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error clearing list", e)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = itemList.isNotEmpty()
        ) {
            Text("Clear All")
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(itemList) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item)
                        TextButton(onClick = {
                            try {
                                itemList = itemList.filter { it != item }
                                Log.d("MainScreen", "Item deleted: $item | Remaining items: ${itemList.size}")
                            } catch (e: Exception) {
                                Log.e("MainScreen", "Error deleting item: $item", e)
                            }
                        }) {
                            Text("X")
                        }
                    }
                }
            }
        }
    }
}