<div align="center">

# 🏡 Namma HomeStay

### *Empowering rural Karnataka families to share their home, food & culture*

![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-FFCA28?style=flat&logo=firebase&logoColor=black)
![Gemini AI](https://img.shields.io/badge/Gemini-1.5%20Flash-8E75B2?style=flat&logo=google&logoColor=white)

</div>

---

## 🌿 The Problem

Families in Karnataka's coastal and rural belts have spare rooms and serve incredible local food — but they're not tech-savvy enough to list on major booking platforms. They miss out on the growing **Eco-Tourism** and **Agri-Tourism** market.

**Namma HomeStay** is a simplified host portal built *for farmers and homemakers* — not hotel managers.

---

## ✨ What It Does

| For Hosts | For Travellers |
|-----------|---------------|
| 📷 Home profile with room & farm photos | 🔍 Browse & search local homestays |
| 🍱 Update today's menu in under 1 minute | ❤️ Wishlist & save favourite stays |
| 🛏️ Manage rooms with availability & pricing | 💬 Send inquiries & chat with hosts |
| 📅 Block dates per room | 📍 Discover nearby secret spots |
| 📨 Inquiry inbox with call & chat | 🏠 View rooms, food menu & amenities |
| ✅ Safety & cleanliness verification checklist | — |

**Powered by Gemini 1.5 Flash AI**
- 📸 Snap a food photo → AI identifies the dish and fills in details
- 🎙️ Speak dish names → voice-to-menu entry
- ✍️ AI-generated listing descriptions from your amenities & location

---

## 🎯 Impact Goals

- **Rural Income** — A third source of income for farming families
- **Sustainable Tourism** — Promoting low-impact, local-first travel
- **Digital Literacy** — Teaching micro-entrepreneurs to manage a digital business

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Auth | Firebase Authentication (Email/Password) |
| Database | Cloud Firestore — real-time menu & availability |
| Images | Base64 in Firestore + Coil 2 with custom `DataUriFetcher` |
| AI | Google Gemini 1.5 Flash (`generativeai` SDK 0.9.0) |
| Navigation | Jetpack Navigation Compose |

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

> ⚠️ `google-services.json` is gitignored — add your own. Never commit it publicly.

### 3. Gemini API Key
In `app/src/main/java/com/namma/homestay/ai/GeminiService.kt`, replace:
```kotlin
private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"
```
Get a free key at [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)

### 4. Build & Run
Open in **Android Studio**, sync Gradle, run on device/emulator (API 24+).

---

## 📁 Project Structure

```
app/src/main/java/com/namma/homestay/
├── ai/               # GeminiService — food photo analysis, voice-to-dish, AI descriptions
├── models/           # Data classes (Listing, Room, Dish, Menu, Inquiry…)
├── repository/       # FirebaseRepository — all Firestore & Auth operations
└── ui/
    ├── screens/      # 17 Compose screens (host + traveller flows)
    └── theme/        # Warm Karnataka-inspired color palette & typography
```

---

## 🗺 User Flow

```
Welcome → Role Selection
    ├── Host:      Dashboard → Listings → Rooms → Daily Menu → Inquiries → Chat
    └── Traveller: Browse → Listing Detail → Inquire → Chat → Wishlist
```

---

<div align="center">
Built for the <strong>Namma Karnataka</strong> homestay community ☕🌿
</div>
