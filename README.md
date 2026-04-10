# 🟣 KotlinAppPractise

A hands-on Android learning project built with **Kotlin + Jetpack Compose**.  
It demonstrates a clean **UI → ViewModel → Repository** architecture with input validation,
character counting, search, sort, and full Logcat logging on every operation.

---

## 📂 Project Structure

```
KotlinAppPractise/
├── README.md                     ← You are here
├── docs/
│   ├── ARCHITECTURE.md           ← How the 3 layers work together
│   ├── LOGCAT_GUIDE.md           ← How to read debug logs in Android Studio
│   └── NEXT_STEPS.md             ← Practice ideas to extend this app
└── app/src/main/java/com/example/kotlinapppractise/
    ├── MainActivity.kt            ← LAYER 3 · UI Screen
    ├── data/
    │   └── ItemRepository.kt     ← LAYER 1 · Data Layer
    └── viewmodel/
        └── MainViewModel.kt      ← LAYER 2 · Business Logic
```

---

## 🚀 Getting Started

### Requirements
- Android Studio Hedgehog or newer
- Android SDK 24+ (minSdk = 24)
- Kotlin 2.x

### Run the app
1. Clone or open this project in Android Studio
2. Wait for Gradle sync to finish
3. Press **▶ Run** (Shift+F10) or deploy to an emulator / physical device

---

## ✨ Features

| Feature | Where it lives |
|---------|----------------|
| Text input with character counter | `MainScreen` → `OutlinedTextField` |
| Input validation (blank, length, duplicate) | `MainViewModel.addItem()` |
| Add / Remove / Clear items | `MainViewModel` → `ItemRepository` |
| Live search / filter | `MainViewModel.onSearchChanged()` |
| Alphabetical sort toggle | `MainViewModel.toggleSort()` |
| Lifecycle logging (onCreate … onDestroy) | `MainActivity` |
| Full Logcat logging on every operation | All 3 layers |

---

## 📖 Documentation

| File | Contents |
|------|----------|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Detailed explanation of every layer and design decision |
| [`docs/LOGCAT_GUIDE.md`](docs/LOGCAT_GUIDE.md) | How to filter and read Logcat output |
| [`docs/NEXT_STEPS.md`](docs/NEXT_STEPS.md) | Graded practice ideas to take the project further |

---

## 🏗️ Architecture at a Glance

```
┌─────────────────────────────────┐
│   MainActivity / MainScreen     │  UI Layer       — draws, forwards events
└────────────┬────────────────────┘
             │ calls fun  /  reads uiState
┌────────────▼────────────────────┐
│        MainViewModel            │  Logic Layer    — validates, transforms
└────────────┬────────────────────┘
             │ add / remove / clear / get
┌────────────▼────────────────────┐
│        ItemRepository           │  Data Layer     — single source of truth
└─────────────────────────────────┘
```

> **Key insight:** the UI never touches data directly.  
> The ViewModel never knows where data is stored.  
> Only the Repository knows how data is persisted.

---

## 🔖 Logcat Tags

| Tag | Layer | What you see |
|-----|-------|-------------|
| `Lifecycle` | Activity | onCreate / onStart / onResume / onPause / onStop / onDestroy |
| `MainViewModel` | ViewModel | validation results, sort/search changes, state updates |
| `ItemRepository` | Repository | add OK/DUPLICATE, remove OK/NOT FOUND, clear count |

