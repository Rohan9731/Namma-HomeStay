package com.namma.homestay.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.namma.homestay.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.*

object FirebaseRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // ==================== AUTH ====================

    fun getCurrentUser() = auth.currentUser
    fun isLoggedIn() = auth.currentUser != null
    fun signOut() = auth.signOut()

    suspend fun signInWithEmailPassword(email: String, password: String): Result<Unit> {
        return try {
            withTimeout(30000) {
                auth.signInWithEmailAndPassword(email, password).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun registerWithEmailPassword(email: String, password: String, name: String): Result<Unit> {
        return try {
            val result = withTimeout(30000) {
                auth.createUserWithEmailAndPassword(email, password).await()
            }
            val uid = result.user?.uid ?: throw Exception("Registration failed")
            val userProfile = UserProfile(
                uid = uid, // Must match rules
                name = name,
                email = email,
                role = "traveler",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            withTimeout(20000) {
                db.collection("users").document(uid).set(userProfile).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Registration failed", e)
            Result.failure(e)
        }
    }

    // ==================== USER ====================

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val doc = withTimeout(10000) {
                db.collection("users").document(userId).get().await()
            }
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting user profile", e)
            null
        }
    }

    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            val updatedProfile = userProfile.copy(updatedAt = System.currentTimeMillis())
            withTimeout(10000) {
                db.collection("users").document(userProfile.uid).set(updatedProfile).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating user profile", e)
            Result.failure(e)
        }
    }

    suspend fun switchRole(newRole: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            withTimeout(10000) {
                db.collection("users").document(uid).update(
                    "role", newRole,
                    "updatedAt", System.currentTimeMillis()
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error switching role", e)
            Result.failure(e)
        }
    }

    // ==================== WISHLIST (Matches /wishlists collection) ====================

    suspend fun toggleWishlist(listingId: String, isAdding: Boolean): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val wishId = "${uid}_$listingId"
            withTimeout(10000) {
                if (isAdding) {
                    val item = WishlistItem(
                        id = wishId,
                        travelerId = uid,
                        listingId = listingId,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    db.collection("wishlists").document(wishId).set(item).await()
                } else {
                    db.collection("wishlists").document(wishId).delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error toggling wishlist", e)
            Result.failure(e)
        }
    }

    suspend fun getWishlist(userId: String): List<Listing> {
        return try {
            val wishlistItems = withTimeout(10000) {
                db.collection("wishlists")
                    .whereEqualTo("travelerId", userId)
                    .get()
                    .await()
            }
            val listingIds = wishlistItems.documents.mapNotNull { it.getString("listingId") }
            if (listingIds.isEmpty()) return emptyList()

            // Firestore whereIn limit is 10, but let's assume small wishlist for now
            val listingsSnapshot = withTimeout(10000) {
                db.collection("listings")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), listingIds)
                    .get()
                    .await()
            }
            listingsSnapshot.documents.mapNotNull { it.toObject(Listing::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting wishlist", e)
            emptyList()
        }
    }

    // ==================== LISTINGS ====================

    suspend fun createListing(listing: Listing): Result<String> {
        return try {
            val docRef = db.collection("listings").document()
            val listingToCreate = listing.copy(
                id = docRef.id,
                hostId = auth.currentUser?.uid ?: "",
                occupancyStatus = "available", // Matches Rules
                createdAt = FieldValue.serverTimestamp(),
                updatedAt = FieldValue.serverTimestamp()
            )
            withTimeout(15000) {
                docRef.set(listingToCreate).await()
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error creating listing", e)
            Result.failure(e)
        }
    }

    suspend fun updateListing(listing: Listing): Result<Unit> {
        return try {
            val updatedListing = listing.copy(updatedAt = FieldValue.serverTimestamp())
            withTimeout(15000) {
                db.collection("listings").document(listing.id).set(updatedListing).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating listing", e)
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        return try {
            withTimeout(10000) {
                db.collection("listings").document(listingId).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error deleting listing", e)
            Result.failure(e)
        }
    }

    suspend fun getListing(listingId: String): Listing? {
        return try {
            val doc = withTimeout(10000) {
                db.collection("listings").document(listingId).get().await()
            }
            doc.toObject(Listing::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting listing", e)
            null
        }
    }

    fun listenToHostListings(hostId: String): Flow<List<Listing>> = callbackFlow {
        val subscription = db.collection("listings")
            .whereEqualTo("hostId", hostId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val listings = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Listing::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("FirebaseRepository", "Error deserializing listing ${doc.id}", e)
                            null
                        }
                    }
                    trySend(listings)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun listenToAllListings(): Flow<List<Listing>> = callbackFlow {
        val subscription = db.collection("listings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val listings = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Listing::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("FirebaseRepository", "Error deserializing listing ${doc.id}", e)
                            null
                        }
                    }
                    trySend(listings)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getHostListings(hostId: String): List<Listing> {
        return try {
            val snapshot = withTimeout(10000) {
                db.collection("listings")
                    .whereEqualTo("hostId", hostId)
                    .get()
                    .await()
            }
            snapshot.documents.mapNotNull { it.toObject(Listing::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting host listings", e)
            emptyList()
        }
    }

    suspend fun getAllListings(): List<Listing> {
        return try {
            val snapshot = withTimeout(15000) {
                db.collection("listings")
                    .get()
                    .await()
            }
            snapshot.documents.mapNotNull { it.toObject(Listing::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting all listings", e)
            emptyList()
        }
    }

    suspend fun getFilteredListings(query: String?, amenities: List<String>): List<Listing> {
        return try {
            val snapshot = withTimeout(15000) {
                db.collection("listings")
                    .get()
                    .await()
            }
            snapshot.documents
                .mapNotNull { it.toObject(Listing::class.java)?.copy(id = it.id) }
                .filter { listing ->
                    val matchesQuery = query.isNullOrBlank() ||
                            listing.name.contains(query, ignoreCase = true) ||
                            listing.location.address.contains(query, ignoreCase = true)
                    val matchesAmenities = amenities.isEmpty() || amenities.all { listing.amenities.contains(it) }
                    matchesQuery && matchesAmenities
                }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting filtered listings", e)
            emptyList()
        }
    }

    // ==================== ROOMS ====================

    suspend fun addRoom(listingId: String, room: Room): Result<Unit> {
        return try {
            val listing = getListing(listingId) ?: return Result.failure(Exception("Listing not found"))
            val currentRooms = listing.rooms ?: emptyList()
            // Check if room already exists to avoid duplicates
            if (currentRooms.any { it.id == room.id }) {
                return updateRoom(listingId, room)
            }
            val updatedRooms = currentRooms + room
            withTimeout(15000) {
                db.collection("listings").document(listingId).update(
                    "rooms", updatedRooms,
                    "updatedAt", System.currentTimeMillis()
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error adding room", e)
            Result.failure(e)
        }
    }

    suspend fun updateRoom(listingId: String, room: Room): Result<Unit> {
        return try {
            val listing = getListing(listingId) ?: return Result.failure(Exception("Listing not found"))
            val currentRooms = listing.rooms ?: emptyList()
            val updatedRooms = currentRooms.map { if (it.id == room.id) room else it }
            withTimeout(15000) {
                db.collection("listings").document(listingId).update(
                    "rooms", updatedRooms,
                    "updatedAt", System.currentTimeMillis()
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating room", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(listingId: String, roomId: String): Result<Unit> {
        return try {
            val listing = getListing(listingId) ?: return Result.failure(Exception("Listing not found"))
            val updatedRooms = listing.rooms.filter { it.id != roomId }
            withTimeout(10000) {
                db.collection("listings").document(listingId).update(
                    "rooms", updatedRooms,
                    "updatedAt", System.currentTimeMillis()
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error deleting room", e)
            Result.failure(e)
        }
    }

    // ==================== INQUIRIES ====================

    suspend fun createInquiry(inquiry: Inquiry): Result<String> {
        return try {
            val docRef = db.collection("inquiries").document()
            val inquiryToCreate = inquiry.copy(
                id = docRef.id,
                travelerId = auth.currentUser?.uid ?: "",
                createdAt = FieldValue.serverTimestamp(),
                updatedAt = FieldValue.serverTimestamp()
            )
            withTimeout(10000) {
                docRef.set(inquiryToCreate).await()
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error creating inquiry", e)
            Result.failure(e)
        }
    }

    suspend fun getHostInquiries(hostId: String): List<Inquiry> {
        return try {
            val snapshot = withTimeout(10000) {
                db.collection("inquiries")
                    .whereEqualTo("hostId", hostId)
                    .get()
                    .await()
            }
            snapshot.documents.mapNotNull { it.toObject(Inquiry::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting host inquiries", e)
            emptyList()
        }
    }

    suspend fun getTravelerInquiries(travelerId: String): List<Inquiry> {
        return try {
            val snapshot = withTimeout(10000) {
                db.collection("inquiries")
                    .whereEqualTo("travelerId", travelerId)
                    .get()
                    .await()
            }
            snapshot.documents.mapNotNull { it.toObject(Inquiry::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting traveler inquiries", e)
            emptyList()
        }
    }

    fun listenToInquiries(userId: String, isHost: Boolean): Flow<List<Inquiry>> = callbackFlow {
        val field = if (isHost) "hostId" else "travelerId"
        val subscription = db.collection("inquiries")
            .whereEqualTo(field, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Don't close the flow — Firestore will retry automatically after
                    // transient errors or reconnects. Closing permanently breaks the UI.
                    Log.e("FirebaseRepository", "Inquiries listener error (will retry)", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val inquiries = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Inquiry::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("FirebaseRepository", "Error deserializing inquiry ${doc.id}", e)
                            null
                        }
                    }
                    trySend(inquiries)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateInquiryStatus(inquiryId: String, status: String): Result<Unit> {
        return try {
            val inquiry = getInquiry(inquiryId) ?: return Result.failure(Exception("Inquiry not found"))
            val oldStatus = inquiry.status
            
            withTimeout(10000) {
                db.collection("inquiries").document(inquiryId).update(
                    "status", status,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()

                // If accepted, block the dates in the listing
                if (status == "accepted" && oldStatus != "accepted") {
                    blockDatesForInquiry(inquiry)
                } else if (status != "accepted" && oldStatus == "accepted") {
                    unblockDatesForInquiry(inquiry)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating inquiry status", e)
            Result.failure(e)
        }
    }

    suspend fun markInquiryAsRead(inquiryId: String): Result<Unit> {
        return try {
            withTimeout(5000) {
                db.collection("inquiries").document(inquiryId).update("read", true).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error marking inquiry as read", e)
            Result.failure(e)
        }
    }

    private suspend fun unblockDatesForInquiry(inquiry: Inquiry) {
        try {
            val listing = getListing(inquiry.listingId) ?: return
            val datesToRemove = generateDateRange(inquiry.checkIn, inquiry.checkOut)
            val selectedRoomIds = inquiry.selectedRoomIds

            val updatedRooms = listing.rooms.map { room ->
                if (selectedRoomIds.contains(room.id) || (selectedRoomIds.isEmpty() && listing.rooms.size == 1)) {
                    val newBlockedDates = room.blockedDates.filter { !datesToRemove.contains(it) }
                    val newStatus = if (newBlockedDates.isEmpty()) "available" else room.status
                    room.copy(blockedDates = newBlockedDates, status = newStatus)
                } else {
                    room
                }
            }

            val anyOccupied = updatedRooms.any { it.status == "occupied" }
            val allAvailable = updatedRooms.all { it.status == "available" }

            db.collection("listings").document(listing.id).update(
                "rooms", updatedRooms,
                "occupancyStatus", when {
                    allAvailable -> "available"
                    anyOccupied -> "occupied"
                    else -> "partial"
                },
                "updatedAt", System.currentTimeMillis()
            ).await()
            Log.d("FirebaseRepository", "Unblocked dates for inquiry ${inquiry.id}")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error unblocking dates", e)
        }
    }

    private suspend fun blockDatesForInquiry(inquiry: Inquiry) {
        try {
            val listing = getListing(inquiry.listingId) ?: return
            val datesToBlock = generateDateRange(inquiry.checkIn, inquiry.checkOut)
            val selectedRoomIds = inquiry.selectedRoomIds

            val updatedRooms = listing.rooms.map { room ->
                if (selectedRoomIds.contains(room.id) || (selectedRoomIds.isEmpty() && listing.rooms.size == 1)) {
                    val newBlockedDates = (room.blockedDates + datesToBlock).distinct()
                    room.copy(blockedDates = newBlockedDates, status = "occupied")
                } else {
                    room
                }
            }

            val allOccupied = updatedRooms.all { it.status == "occupied" }
            val allAvailable = updatedRooms.all { it.status == "available" }

            db.collection("listings").document(listing.id).update(
                "rooms", updatedRooms,
                "occupancyStatus", when {
                    allOccupied -> "occupied"
                    allAvailable -> "available"
                    else -> "partial"
                },
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Log.d("FirebaseRepository", "Blocked dates for inquiry ${inquiry.id}")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error blocking dates", e)
        }
    }

    private fun generateDateRange(startDate: String, endDate: String): List<String> {
        if (startDate.isEmpty() || endDate.isEmpty()) return emptyList()
        val dates = mutableListOf<String>()
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val start = sdf.parse(startDate)
            val end = sdf.parse(endDate)
            if (start != null && end != null) {
                val calendar = java.util.Calendar.getInstance()
                calendar.time = start
                // Include check-in but usually check-out day is free for next guest
                // For blocking purposes, we block until the day before check-out if we want to allow same-day turnover
                // But let's keep it simple: block the whole range
                while (!calendar.time.after(end)) {
                    dates.add(sdf.format(calendar.time))
                    calendar.add(java.util.Calendar.DATE, 1)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Date parsing error", e)
        }
        return dates
    }

    fun isRoomAvailable(room: Room, checkIn: String, checkOut: String): Boolean {
        if (checkIn.isEmpty() || checkOut.isEmpty()) return true
        val requestedDates = generateDateRange(checkIn, checkOut)
        return requestedDates.none { room.blockedDates.contains(it) }
    }

    suspend fun getInquiry(inquiryId: String): Inquiry? {
        return try {
            val snapshot = db.collection("inquiries").document(inquiryId).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(Inquiry::class.java)?.copy(id = snapshot.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting inquiry $inquiryId", e)
            null
        }
    }

    fun listenToInquiry(inquiryId: String): Flow<Inquiry?> = callbackFlow {
        val subscription = db.collection("inquiries").document(inquiryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        trySend(snapshot.toObject(Inquiry::class.java)?.copy(id = snapshot.id))
                    } catch (e: Exception) {
                        trySend(null)
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun deleteInquiry(inquiryId: String): Result<Unit> {
        return try {
            Log.d("FirebaseRepository", "Starting deletion of inquiry $inquiryId")
            val inquiry = getInquiry(inquiryId)
            
            // 1. Unblock dates if it was an accepted inquiry
            if (inquiry != null && inquiry.status == "accepted") {
                try {
                    unblockDatesForInquiry(inquiry)
                } catch (e: Exception) {
                    Log.e("FirebaseRepository", "Failed to unblock dates for inquiry $inquiryId, continuing deletion", e)
                }
            }
            
            // 2. Delete messages sub-collection in chunks of 500 (Firestore batch limit)
            try {
                val messages = db.collection("inquiries").document(inquiryId).collection("messages").get().await()
                if (!messages.isEmpty) {
                    val batch = db.batch()
                    var count = 0
                    for (doc in messages.documents) {
                        batch.delete(doc.reference)
                        count++
                        if (count >= 500) break // Safety for single batch
                    }
                    batch.commit().await()
                    Log.d("FirebaseRepository", "Deleted $count messages for inquiry $inquiryId")
                }
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error deleting messages sub-collection for $inquiryId", e)
                // We continue even if messages fail to delete, to ensure the main document is removed
            }

            // 3. Delete the inquiry document
            withTimeout(20000) {
                db.collection("inquiries").document(inquiryId).delete().await()
            }
            Log.d("FirebaseRepository", "Successfully deleted inquiry document $inquiryId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error deleting inquiry $inquiryId", e)
            Result.failure(e)
        }
    }

    // ==================== CHAT ====================

    suspend fun sendMessage(inquiryId: String, message: ChatMessage): Result<Unit> {
        return try {
            val msgRef = db.collection("inquiries").document(inquiryId)
                .collection("messages").document()
            val msgToCreate = message.copy(
                id = msgRef.id,
                createdAt = FieldValue.serverTimestamp()
            )
            // Write message independently — do NOT batch with the inquiry update so that
            // a rule mismatch on the parent doc does not block the message from being stored.
            withTimeout(15000) {
                msgRef.set(msgToCreate).await()
            }
            // Best-effort: update lastMessage preview on the inquiry document.
            // If this fails (e.g. due to security rules) the message is still saved.
            try {
                db.collection("inquiries").document(inquiryId).update(
                    "lastMessage", message.text,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (e: Exception) {
                Log.w("FirebaseRepository", "lastMessage update failed (message was still sent)", e)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error sending message", e)
            Result.failure(e)
        }
    }

    fun listenToMessages(inquiryId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (inquiryId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val subscription = db.collection("inquiries").document(inquiryId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Don't close the flow — just log. Firestore retries automatically.
                    Log.e("FirebaseRepository", "Messages listener error for $inquiryId (will retry)", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("FirebaseRepository", "Error deserializing message ${doc.id}", e)
                            null
                        }
                    }
                    trySend(messages)
                }
            }
        awaitClose { subscription.remove() }
    }

    // ==================== MENU ====================

    suspend fun getMenu(listingId: String, date: String): Menu? {
        return try {
            val menuDocId = "${listingId}_$date"
            val doc = withTimeout(10000) {
                db.collection("menus").document(menuDocId).get().await()
            }
            doc.toObject(Menu::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting menu", e)
            null
        }
    }

    suspend fun saveMenu(menu: Menu): Result<Unit> {
        return try {
            val menuDocId = "${menu.listingId}_${menu.date}"
            val menuToSave = menu.copy(
                id = menuDocId,
                hostId = auth.currentUser?.uid ?: "",
                createdAt = if (menu.createdAt.toTimestamp() > 0) menu.createdAt else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            withTimeout(10000) {
                db.collection("menus").document(menuDocId).set(menuToSave).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error saving menu", e)
            Result.failure(e)
        }
    }

    // ==================== IMAGE UPLOAD ====================
    // Images are encoded as base64 data URIs and stored directly in the parent
    // Firestore document (listing.coverPhoto, listing.images[].url, room.images[].url,
    // dish.imageUrl). No separate collection is used — that avoids needing extra
    // Firestore security rules and keeps the logic simple.
    // Images are shrunk to max 400 px / 40 % JPEG quality (~10–30 KB binary,
    // ~14–40 KB as base64 text) to stay well within the 1 MB Firestore document limit.

    suspend fun uploadImage(context: Context, uri: Uri, path: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(Exception("Cannot open image — URI not readable"))
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (originalBitmap == null) {
                    return@withContext Result.failure(Exception("Cannot decode image — unsupported format?"))
                }
                // Resize to max 400 px on the longest side, preserving aspect ratio
                val maxDim = 400
                val w = originalBitmap.width
                val h = originalBitmap.height
                val scale = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h, 1.0f)
                val scaledBitmap = if (scale < 1.0f) {
                    android.graphics.Bitmap.createScaledBitmap(
                        originalBitmap, (w * scale).toInt(), (h * scale).toInt(), true
                    )
                } else {
                    originalBitmap
                }
                // Compress to JPEG at 40 % quality
                val bos = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 40, bos)
                val base64Str = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
                val dataUri = "data:image/jpeg;base64,$base64Str"
                Log.d("FirebaseRepository", "Image encoded: ${bos.size()} bytes binary, path=$path")
                Result.success(dataUri)
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error encoding image for path=$path", e)
                Result.failure(e)
            }
        }
    }

    // ==================== STATS ====================

    suspend fun getHostStats(hostId: String): HostStats {
        return try {
            val listings = getHostListings(hostId)
            val inquiries = getHostInquiries(hostId)
            val totalRooms = listings.sumOf { it.rooms.size }
            val occupiedRooms = listings.sumOf { listing ->
                listing.rooms.count { it.status == "occupied" }
            }
            val totalBookings = inquiries.count { it.status == "accepted" }
            val totalRevenue = inquiries
                .filter { it.status == "accepted" }
                .sumOf { it.totalPrice }
            
            HostStats(
                totalProperties = listings.size,
                totalRooms = totalRooms,
                occupiedRooms = occupiedRooms,
                availableRooms = totalRooms - occupiedRooms,
                totalBookings = totalBookings,
                totalRevenue = totalRevenue,
                pendingInquiries = inquiries.count { it.status == "sent" }
            )
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting host stats", e)
            HostStats()
        }
    }
}

data class HostStats(
    val totalProperties: Int = 0,
    val totalRooms: Int = 0,
    val occupiedRooms: Int = 0,
    val availableRooms: Int = 0,
    val totalBookings: Int = 0,
    val totalRevenue: Double = 0.0,
    val pendingInquiries: Int = 0
)
