<div align="center">

# 🏡 Namma HomeStay

**Discover & host authentic Karnataka rural homestays**

![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-FFCA28?style=flat&logo=firebase&logoColor=black)
![Gemini AI](https://img.shields.io/badge/Gemini-1.5%20Flash-8E75B2?style=flat&logo=google&logoColor=white)

</div>

---

## ✨ Features

| For Hosts | For Travellers |
|-----------|---------------|
| 📋 List & manage homestays | 🔍 Browse & search listings |
| 🛏️ Room management with images | 💬 Send inquiries & chat |
| 🍱 Daily menu updates | ❤️ Wishlist & save stays |
| 📅 Block dates per room | 🏠 View rooms, amenities & photos |
| 📨 Inquiry inbox with real-time chat | 📍 Nearby attractions |
| ✅ Safety verification checklist | — |

**AI-Powered (Gemini 1.5 Flash)**
- Auto-generate listing descriptions
- Analyse food photos to identify dishes
- Voice-to-dish menu entry
- AI dish description generation

---

## 🛠 Tech Stack

- **UI** — Jetpack Compose + Material 3
- **Auth** — Firebase Authentication (Email/Password)
- **Database** — Cloud Firestore (images stored as base64)
- **AI** — Google Gemini 1.5 Flash via `generativeai` SDK
- **Image Loading** — Coil 2 with custom `DataUriFetcher` for base64
- **Navigation** — Jetpack Navigation Compose

---

## 🚀 Setup

### 1. Clone
```bash
git clone https://github.com/Rohan9731/Namma-HomeStay.git
cd Namma-HomeStay
```

### 2. Firebase
- Create a project at [console.firebase.google.com](https://console.firebase.google.com)
- Enable **Authentication** (Email/Password) and **Firestore**
- Download `google-services.json` → place it in the `app/` directory

> ⚠️ `google-services.json` is gitignored — add your own file. Never commit it publicly.

### 3. Gemini API Key
Open `app/src/main/java/com/namma/homestay/ai/GeminiService.kt` and replace:
```kotlin
private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"
```
Get a free key at [aistudio.google.com](https://aistudio.google.com/app/apikey)

### 4. Build & Run
Open in **Android Studio**, sync Gradle, and run on a device/emulator (API 24+).

---

## 📁 Project Structure

```
app/src/main/java/com/namma/homestay/
├── ai/               # GeminiService — all AI features
├── models/           # Data classes (Listing, Room, Dish, Inquiry…)
├── repository/       # FirebaseRepository — all Firestore operations
└── ui/
    ├── screens/      # All Compose screens (17 screens)
    └── theme/        # Colors, typography, theme
```

---

## 📸 Screens

`Welcome` → `Role Selection` → **Host:** `Dashboard › Listings › Rooms › Menu › Inquiries › Chat`  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Traveller:** `Home › Listing Detail › Inquire › Chat › Wishlist`

---

<div align="center">
Made with ☕ for Karnataka's homestay hosts
</div>
