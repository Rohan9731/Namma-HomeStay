<div align="center">

# ?? Namma HomeStay

### *Empowering rural Karnataka families to share their home, food & culture*

![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-FFCA28?style=flat&logo=firebase&logoColor=black)

</div>

---

## ?? The Problem

Families in Karnataka's coastal and rural belts have spare rooms and serve incredible local food — but they're not tech-savvy enough to list on major booking platforms. They miss the growing **Eco-Tourism** and **Agri-Tourism** market.

**Namma HomeStay** is a simplified host portal built *for farmers and homemakers*, not hotel managers.

---

## ?? Two Roles, One App

### ?? Host Flow
Hosts manage everything from a single dashboard — no technical knowledge required.

| Feature | Details |
|---------|---------|
| **Listing Management** | Create a home profile with name, location, amenities, cover photo and room images |
| **Room Management** | Add multiple rooms — type (attached/shared bath), capacity, price and photos per room |
| **Daily Menu** | Update today's food offerings per meal category (Breakfast, Lunch, Snacks, Dinner) in under 1 minute |
| **Availability Calendar** | Block specific dates per room using an interactive month-view calendar |
| **Inquiry Inbox** | View all guest requests with check-in/out dates, guest count and total price |
| **Real-time Chat** | Message travellers directly from the inquiry thread |
| **Verification Checklist** | Pre-guest readiness checklist across 4 categories: Room Prep, Kitchen & Dining, Safety & Comfort, Final Touches |
| **Dashboard** | Live stats — total rooms, occupied vs available, active listings count |

### ?? Traveller Flow

| Feature | Details |
|---------|---------|
| **Browse & Search** | Filter homestays by category — Home Food, Heritage, Farm Stay, Near Forest, WiFi Ready |
| **Listing Detail** | Photo gallery, room options, amenities, today's menu and nearby secret spots |
| **Booking Inquiry** | Select rooms, pick check-in/out via date picker, specify guests, send a message |
| **Real-time Chat** | Message the host directly after inquiry is sent |
| **Wishlist** | Save and revisit favourite homestays |

---

## ?? Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Auth | Firebase Authentication — Email/Password |
| Database | Cloud Firestore — real-time menu, availability & inquiries |
| Images | Base64-encoded and stored directly in Firestore (no Storage subscription needed) |
| Image Loading | Coil 2 with a custom `DataUriFetcher` for base64 `data:` URIs |
| Navigation | Jetpack Navigation Compose |

---

## ?? Setup

### 1. Clone
```bash
git clone https://github.com/Rohan9731/Namma-HomeStay.git
cd Namma-HomeStay
```

### 2. Firebase
1. Create a project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable **Authentication** (Email/Password) and **Cloud Firestore**
3. Download `google-services.json` ? place it in `app/`

> ?? `google-services.json` is gitignored — never commit it. Add your own.

### 3. Gemini AI *(optional)*
In `app/src/main/java/com/namma/homestay/ai/GeminiService.kt`:
```kotlin
private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"
```
Get a free key at [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey). All core features work without it.

### 4. Build
Open in **Android Studio**, sync Gradle, run on any device or emulator (API 24+).

---

## ?? Project Structure

```
app/src/main/java/com/namma/homestay/
+-- ai/               # Gemini AI service (food photo, voice, descriptions)
+-- models/           # Data classes — Listing, Room, Dish, Menu, Inquiry, Message
+-- repository/       # FirebaseRepository — all Auth & Firestore operations
+-- ui/
    +-- screens/      # 17 Compose screens (host + traveller flows)
    +-- theme/        # Warm Karnataka-inspired palette & typography
```

---

## ?? Screen Flow

```
Welcome ? Role Selection
    +-- Host      ? Dashboard ? My Listings ? Rooms ? Daily Menu
    ¦                        ? Block Dates  ? Inquiries ? Chat ? Checklist
    +-- Traveller ? Browse ? Listing Detail ? Inquiry ? Chat ? Wishlist
```

---

## ?? Impact

- **Rural Income** — A third income source for farming families alongside crops and livestock
- **Sustainable Tourism** — Low-impact, local-first travel over resort tourism
- **Digital Literacy** — Simple enough for a homemaker to manage independently

---

<div align="center">
Built for the <strong>Namma Karnataka</strong> homestay community ? ??
</div>
