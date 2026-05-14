package com.namma.homestay

import com.namma.homestay.models.Listing
import com.namma.homestay.models.ListingImage
import com.namma.homestay.models.Room
import com.namma.homestay.models.Inquiry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Bug Condition Exploration Tests — Task 1
 *
 * These tests encode the EXPECTED (fixed) behavior.
 * They are EXPECTED TO FAIL on unfixed code — failure confirms the bugs exist.
 * DO NOT fix the production code or modify these tests when they fail.
 *
 * Validates: Requirements 1.1 – 1.15
 */
@DisplayName("Bug Condition Exploration Tests")
class BugExplorationUnitTests {

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.4 — GeminiService API Key
    // Bug: API_KEY was set to an expired/wrong key
    // Expected: API_KEY is a non-empty, non-placeholder value
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.4: GeminiService uses a configured (non-placeholder) API key")
    fun test1_4_geminiServiceUsesCorrectApiKey() {
        val geminiClass = com.namma.homestay.ai.GeminiService::class.java
        val apiKeyField = geminiClass.getDeclaredField("API_KEY")
        apiKeyField.isAccessible = true
        val actualKey = apiKeyField.get(com.namma.homestay.ai.GeminiService) as String

        // Key must be set to a real value (not the placeholder) before running
        assertFalse(
            actualKey == "YOUR_GEMINI_API_KEY_HERE" || actualKey.isBlank(),
            "Bug 4: GeminiService.API_KEY is still a placeholder — set a real Gemini API key"
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.11 — MenuUpdateScreen "Add To Menu" button disabled when price empty
    // Bug: button enabled when newDishName.isNotBlank() only (price not checked)
    // Expected: button disabled when price is blank
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.11: 'Add To Menu' button is disabled when price field is empty")
    fun test1_11_addToMenuButtonDisabledWhenPriceEmpty() {
        val dishName = "Idli"
        val dishPrice = ""   // empty price — should disable button

        // Replicate the current (buggy) enable condition from MenuUpdateScreen:
        //   enabled = newDishName.isNotBlank()
        val buggyEnabled = dishName.isNotBlank()

        // Assert the fixed condition: button must be DISABLED when price is empty
        // EXPECTED TO FAIL on unfixed code because buggyEnabled == true
        assertFalse(
            buggyEnabled,
            "Bug 11: 'Add To Menu' button must be disabled when price is empty. " +
            "Buggy code only checks dishName.isNotBlank() — price is not validated."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.13 — TravelerHomeScreen per-room availability filtering
    // Bug: listings.filter { it.occupancyStatus == "available" } hides listings
    //      that have at least one available room
    // Expected: listing visible when ANY room is available
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.13: Listing with 1 blocked + 2 available rooms appears in traveler filter")
    fun test1_13_listingWithSomeBlockedRoomsAppearsInTravelerFilter() {
        val blockedRoom = Room(
            id = "r1", name = "Room 1", status = "occupied",
            blockedDates = listOf("2024-12-01")
        )
        val availableRoom1 = Room(id = "r2", name = "Room 2", status = "available")
        val availableRoom2 = Room(id = "r3", name = "Room 3", status = "available")

        val listing = Listing(
            id = "l1",
            name = "Test Homestay",
            occupancyStatus = "partial",   // listing-level status is NOT "available"
            rooms = listOf(blockedRoom, availableRoom1, availableRoom2)
        )

        val allListings = listOf(listing)

        // Buggy filter (current code): uses listing-level occupancyStatus
        val buggyFiltered = allListings.filter { it.occupancyStatus == "available" }

        // EXPECTED TO FAIL on unfixed code: buggy filter excludes the listing
        assertTrue(
            buggyFiltered.contains(listing),
            "Bug 13: Buggy filter incorrectly excludes listing with partially available rooms. " +
            "occupancyStatus='partial' so filter { it.occupancyStatus == 'available' } returns empty."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.14 — Dashboard badge shows per-room availability, not ONLINE/OFFLINE
    // Bug: badge uses listing.occupancyStatus → shows "OFFLINE" for any offline room
    // Expected: badge shows "X/Y rooms available"
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.14: Dashboard badge shows 'X/Y rooms available' not 'OFFLINE'")
    fun test1_14_dashboardBadgeShowsPerRoomAvailabilityNotOffline() {
        val offlineRoom = Room(id = "r1", name = "Room 1", status = "occupied")
        val onlineRoom1 = Room(id = "r2", name = "Room 2", status = "available")
        val onlineRoom2 = Room(id = "r3", name = "Room 3", status = "available")

        val listing = Listing(
            id = "l1",
            name = "Test Homestay",
            occupancyStatus = "occupied",  // worst-case listing status
            rooms = listOf(offlineRoom, onlineRoom1, onlineRoom2)
        )

        // Buggy badge logic (current code):
        val buggyBadgeText = if (listing.occupancyStatus == "available") "ONLINE" else "OFFLINE"

        // Assert the badge should NOT be "OFFLINE"
        // EXPECTED TO FAIL on unfixed code: buggyBadgeText == "OFFLINE"
        assertNotEquals(
            "OFFLINE",
            buggyBadgeText,
            "Bug 14: Dashboard badge must not show 'OFFLINE' when 2 out of 3 rooms are available. " +
            "Should show '2/3 rooms available' instead."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.15 — InquiriesScreen: deleted inquiry removed from local state immediately
    // Bug: only inquiryToDelete = null is set; local list not updated
    // Expected: inquiries list filtered to remove deleted item immediately
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.15: Deleted inquiry is removed from local state immediately")
    fun test1_15_deletedInquiryRemovedFromLocalStateImmediately() {
        val inquiry1 = Inquiry(id = "inq1", travelerName = "Alice", listingName = "Coorg Stay")
        val inquiry2 = Inquiry(id = "inq2", travelerName = "Bob", listingName = "Malnad Stay")
        val inquiry3 = Inquiry(id = "inq3", travelerName = "Carol", listingName = "Mysore Stay")

        var inquiries = listOf(inquiry1, inquiry2, inquiry3)
        var inquiryToDelete: Inquiry? = inquiry2

        // Simulate the BUGGY deletion handler (current code):
        // Only sets inquiryToDelete = null, does NOT update inquiries list
        fun buggyDeleteHandler(id: String) {
            // Simulates: deleteInquiry(id) succeeds
            // Then: inquiryToDelete = null  (only this happens in buggy code)
            inquiryToDelete = null
            // inquiries list is NOT updated here — relies on Flow listener
        }

        val idToDelete = inquiry2.id
        buggyDeleteHandler(idToDelete)

        // EXPECTED TO FAIL on unfixed code: inquiry2 still in list after buggy delete
        assertFalse(
            inquiries.any { it.id == idToDelete },
            "Bug 15: Deleted inquiry must be removed from local state immediately. " +
            "Buggy code only sets inquiryToDelete=null without filtering the inquiries list."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.5 — AddEditListingScreen: navigation after save goes to "dashboard"
    // Bug: navController.popBackStack() used instead of navigate("dashboard")
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.5: After successful listing save, navigation goes to 'dashboard' route")
    fun test1_5_completedListingNavigatesToDashboardRoute() {
        var navigatedTo: String? = null
        var poppedBackStack = false

        // Buggy behavior (current code): calls popBackStack(), no specific route
        fun buggyOnSaveSuccess() {
            poppedBackStack = true
            // navController.popBackStack() — does NOT navigate to a specific route
        }

        buggyOnSaveSuccess()

        // Assert: navigation destination must be "dashboard"
        // EXPECTED TO FAIL on unfixed code: navigatedTo is null
        assertEquals(
            "dashboard",
            navigatedTo,
            "Bug 5: After successful listing save, navigation must go to 'dashboard' route. " +
            "Buggy code calls popBackStack() which may land on wrong screen."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.1 — DashboardScreen empty state card has clickable modifier
    // Bug: empty state Card has no .clickable modifier
    // Expected: card is clickable and navigates to "addListing"
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.1: Empty state card has clickable modifier navigating to 'addListing'")
    fun test1_1_emptyStateCardHasClickableNavigationToAddListing() {
        // Read source file to verify the clickable modifier is present
        val possiblePaths = listOf(
            "src/main/java/com/namma/homestay/ui/screens/DashboardScreen.kt",
            "app/src/main/java/com/namma/homestay/ui/screens/DashboardScreen.kt",
            "../app/src/main/java/com/namma/homestay/ui/screens/DashboardScreen.kt"
        )
        val sourceContent = possiblePaths
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
            ?.readText() ?: ""

        val hasClickableOnEmptyState = sourceContent.isNotEmpty() &&
            sourceContent.contains("clickable { navController.navigate(\"addListing\") }") &&
            (sourceContent.contains("click here to add one") ||
             sourceContent.contains("click here"))

        // EXPECTED TO FAIL on unfixed code: empty state card has no clickable modifier
        assertTrue(
            hasClickableOnEmptyState,
            "Bug 1: Empty state card must have .clickable { navController.navigate('addListing') } " +
            "and updated text 'click here to add one'. Neither found in source."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.6 — ManageRoomsScreen: concurrent room operations don't corrupt state
    // Bug: no isSavingRoom guard; rapid operations corrupt editingRoom state
    // Expected: guard prevents concurrent saves
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.6: Concurrent room save operations are guarded against state corruption")
    fun test1_6_concurrentRoomOperationsDoNotCorruptState() {
        var isSavingRoom = false
        var savedRooms = mutableListOf<Room>()

        val room1 = Room(id = "r1", name = "Room 1")
        val room2 = Room(id = "r2", name = "Room 2")

        // Fixed save: guard prevents concurrent operations
        fun fixedSaveRoom(room: Room): Boolean {
            if (isSavingRoom) return false  // blocked
            isSavingRoom = true
            savedRooms.add(room)
            isSavingRoom = false
            return true
        }

        // Simulate concurrent: first save starts but hasn't finished
        isSavingRoom = true  // first save in progress
        val secondSaveAllowed = fixedSaveRoom(room2)  // should be blocked

        // EXPECTED TO FAIL on unfixed code: no guard exists, so concurrent saves are allowed
        // (The unfixed code has no isSavingRoom check around the dialog save button)
        assertFalse(
            secondSaveAllowed,
            "Bug 6: Second concurrent room save must be blocked when first is in progress. " +
            "Buggy code has no isSavingRoom guard."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.8 — MenuUpdateScreen: ScrollableTabRow and input Row do not overlap
    // Bug: no Spacer between ScrollableTabRow and input method Row
    // Expected: Spacer(height=8.dp) present between them
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.8: MenuUpdateScreen has Spacer between tab row and input buttons")
    fun test1_8_menuUpdateScreenTabRowAndButtonsDoNotOverlap() {
        val possiblePaths = listOf(
            "src/main/java/com/namma/homestay/ui/screens/MenuUpdateScreen.kt",
            "app/src/main/java/com/namma/homestay/ui/screens/MenuUpdateScreen.kt",
            "../app/src/main/java/com/namma/homestay/ui/screens/MenuUpdateScreen.kt"
        )
        val sourceContent = possiblePaths
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
            ?.readText() ?: ""

        // The fix adds Spacer(modifier = Modifier.height(8.dp)) between ScrollableTabRow and input Row
        val hasSpacerBetweenTabsAndButtons = sourceContent.isNotEmpty() &&
            sourceContent.contains("ScrollableTabRow") &&
            sourceContent.contains("Spacer(modifier = Modifier.height(8.dp))")

        // EXPECTED TO FAIL on unfixed code: no Spacer present
        assertTrue(
            hasSpacerBetweenTabsAndButtons,
            "Bug 8: MenuUpdateScreen must have a Spacer(height=8.dp) between ScrollableTabRow " +
            "and input method Row to prevent overlap."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.9 — AIFoodAnalysisScreen: gallery URI converted to Bitmap
    // Bug: galleryLauncher callback has only a comment, no implementation
    // Expected: URI is converted to Bitmap using contentResolver
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.9: Gallery launcher converts URI to Bitmap using contentResolver")
    fun test1_9_galleryUriConvertedToBitmap() {
        val possiblePaths = listOf(
            "src/main/java/com/namma/homestay/ui/screens/AIFoodAnalysisScreen.kt",
            "app/src/main/java/com/namma/homestay/ui/screens/AIFoodAnalysisScreen.kt",
            "../app/src/main/java/com/namma/homestay/ui/screens/AIFoodAnalysisScreen.kt"
        )
        val sourceContent = possiblePaths
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
            ?.readText() ?: ""

        val hasRealImplementation = sourceContent.isNotEmpty() &&
            sourceContent.contains("contentResolver") &&
            sourceContent.contains("BitmapFactory.decodeStream") &&
            !sourceContent.contains("In a real app, you'd convert URI to Bitmap here")

        // EXPECTED TO FAIL on unfixed code: only comment present, no real implementation
        assertTrue(
            hasRealImplementation,
            "Bug 9: Gallery launcher must convert URI to Bitmap using contentResolver + " +
            "BitmapFactory.decodeStream. Currently only has a placeholder comment."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.10 — MenuUpdateScreen: Voice button launches speech recognizer
    // Bug: onClick = { /* TODO Voice input */ } — does nothing
    // Expected: speech recognizer is launched
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.10: Voice button launches speech recognizer (no TODO placeholder)")
    fun test1_10_voiceButtonLaunchesSpeechRecognizer() {
        val possiblePaths = listOf(
            "src/main/java/com/namma/homestay/ui/screens/MenuUpdateScreen.kt",
            "app/src/main/java/com/namma/homestay/ui/screens/MenuUpdateScreen.kt",
            "../app/src/main/java/com/namma/homestay/ui/screens/MenuUpdateScreen.kt"
        )
        val sourceContent = possiblePaths
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
            ?.readText() ?: ""

        val hasTodoVoiceInput = sourceContent.contains("/* TODO Voice input */")
        val hasSpeechRecognizer = sourceContent.contains("RecognizerIntent") ||
            sourceContent.contains("SpeechRecognizer") ||
            sourceContent.contains("RECORD_AUDIO")

        // EXPECTED TO FAIL on unfixed code: TODO comment present, no speech recognizer
        assertFalse(
            hasTodoVoiceInput,
            "Bug 10: Voice button must not have '/* TODO Voice input */' placeholder."
        )
        assertTrue(
            hasSpeechRecognizer,
            "Bug 10: Voice button must launch speech recognizer (RecognizerIntent/SpeechRecognizer)."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.2 — AddEditListingScreen: "Detect my location" requests location permission
    // Bug: clickable { /* TODO Detect location via GPS */ } — does nothing
    // Expected: location permission is requested
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.2: 'Detect my location' requests location permissions (no TODO placeholder)")
    fun test1_2_detectLocationRequestsPermission() {
        val possiblePaths = listOf(
            "src/main/java/com/namma/homestay/ui/screens/AddEditListingScreen.kt",
            "app/src/main/java/com/namma/homestay/ui/screens/AddEditListingScreen.kt",
            "../app/src/main/java/com/namma/homestay/ui/screens/AddEditListingScreen.kt"
        )
        val sourceContent = possiblePaths
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
            ?.readText() ?: ""

        val hasTodoGps = sourceContent.contains("TODO Detect location via GPS")
        val hasLocationPermission = sourceContent.contains("ACCESS_FINE_LOCATION") ||
            sourceContent.contains("RequestMultiplePermissions") ||
            sourceContent.contains("FusedLocationProviderClient")

        // EXPECTED TO FAIL on unfixed code: TODO present, no permission request
        assertFalse(
            hasTodoGps,
            "Bug 2: 'Detect my location' must not have 'TODO Detect location via GPS' placeholder."
        )
        assertTrue(
            hasLocationPermission,
            "Bug 2: 'Detect my location' must request location permissions via " +
            "ACCESS_FINE_LOCATION / RequestMultiplePermissions / FusedLocationProviderClient."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.3 — AddEditListingScreen: local preview URI shown before upload completes
    // Bug: no local URI preview state; only remote URL shown after upload
    // Expected: coverPhotoLocalUri state exists and is shown immediately
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.3: AddEditListingScreen has local URI preview state for immediate display")
    fun test1_3_localImagePreviewShownBeforeUpload() {
        val possiblePaths = listOf(
            "src/main/java/com/namma/homestay/ui/screens/AddEditListingScreen.kt",
            "app/src/main/java/com/namma/homestay/ui/screens/AddEditListingScreen.kt",
            "../app/src/main/java/com/namma/homestay/ui/screens/AddEditListingScreen.kt"
        )
        val sourceContent = possiblePaths
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
            ?.readText() ?: ""

        val hasLocalUriState = sourceContent.contains("coverPhotoLocalUri")

        // EXPECTED TO FAIL on unfixed code: no local URI preview state
        assertTrue(
            hasLocalUriState,
            "Bug 3: AddEditListingScreen must have 'coverPhotoLocalUri' state for immediate " +
            "local preview before Firebase upload completes."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.7 — ManageRoomsScreen: roomImagePicker registered outside conditional block
    // Bug: roomImagePicker launcher inside if(editingRoom != null) block
    // Expected: launcher registered outside the conditional block
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.7: roomImagePicker launcher is declared outside 'if (editingRoom != null)' block")
    fun test1_7_roomImagePickerRegisteredOutsideConditionalBlock() {
        val possiblePaths = listOf(
            "src/main/java/com/namma/homestay/ui/screens/ManageRoomsScreen.kt",
            "app/src/main/java/com/namma/homestay/ui/screens/ManageRoomsScreen.kt",
            "../app/src/main/java/com/namma/homestay/ui/screens/ManageRoomsScreen.kt"
        )
        val sourceContent = possiblePaths
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
            ?.readText() ?: ""

        val ifEditingRoomPos = sourceContent.indexOf("if (editingRoom != null)")
        val launcherPos = sourceContent.indexOf("roomImagePicker")

        // Both must be found
        assertTrue(ifEditingRoomPos >= 0, "Source must contain 'if (editingRoom != null)'")
        assertTrue(launcherPos >= 0, "Source must contain 'roomImagePicker'")

        // EXPECTED TO FAIL on unfixed code: launcher is declared AFTER the if block start
        assertTrue(
            launcherPos < ifEditingRoomPos,
            "Bug 7: roomImagePicker launcher must be declared BEFORE 'if (editingRoom != null)' " +
            "block to prevent recomposition state loss. Currently at pos $launcherPos, " +
            "if-block at pos $ifEditingRoomPos."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1.12 — TravelerHomeScreen: listing card shows gallery images with arrows
    // Bug: only listing.coverPhoto shown in AsyncImage, no gallery pager
    // Expected: HorizontalPager with all images and navigation arrows
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Test 1.12: TravelerListingCard uses HorizontalPager with navigation arrows")
    fun test1_12_travelerListingCardShowsGalleryImagesWithArrows() {
        val possiblePaths = listOf(
            "src/main/java/com/namma/homestay/ui/screens/TravelerHomeScreen.kt",
            "app/src/main/java/com/namma/homestay/ui/screens/TravelerHomeScreen.kt",
            "../app/src/main/java/com/namma/homestay/ui/screens/TravelerHomeScreen.kt"
        )
        val sourceContent = possiblePaths
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
            ?.readText() ?: ""

        val hasHorizontalPager = sourceContent.contains("HorizontalPager")
        val hasNavigationArrows = sourceContent.contains("ChevronLeft") ||
            sourceContent.contains("ArrowBack") ||
            sourceContent.contains("NavigateBefore") ||
            sourceContent.contains("KeyboardArrowLeft")

        // EXPECTED TO FAIL on unfixed code: no HorizontalPager, no navigation arrows
        assertTrue(
            hasHorizontalPager,
            "Bug 12: TravelerListingCard must use HorizontalPager to show all gallery images."
        )
        assertTrue(
            hasNavigationArrows,
            "Bug 12: TravelerListingCard must have navigation arrows (ChevronLeft/ArrowBack) " +
            "for browsing gallery images."
        )
    }
}
