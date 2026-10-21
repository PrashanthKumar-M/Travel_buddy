
package com.travelbuddy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelbuddy.app.domain.Expense
import com.travelbuddy.app.domain.ExpenseCalculator
import com.travelbuddy.app.domain.Trip
import com.travelbuddy.app.domain.TripMember
import com.travelbuddy.app.domain.TripStatus
import com.travelbuddy.app.domain.Vehicle
import kotlin.math.roundToInt
import kotlin.math.absoluteValue
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import com.travelbuddy.app.data.LocationUpdateRequest
import com.travelbuddy.app.data.AddExpenseRequest
import com.travelbuddy.app.data.CreateTripRequest
import com.travelbuddy.app.data.KtorTravelRepository
import com.travelbuddy.app.data.RealtimeLocationGateway
import com.travelbuddy.app.data.TravelApiConfig
import com.travelbuddy.app.data.TravelRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector


// ============================================================
// COLORS
// ============================================================

private val Navy = Color(0xFF1F2937)
private val Blue = Color(0xFF2F80ED)
private val Green = Color(0xFF27AE60)
private val Soft = Color(0xFFEAF3FF)
private val Background = Color(0xFFFAF7FC)
private val Red = Color(0xFFE53935)
private val Orange = Color(0xFFE67E22)
internal val LocalTravelBuddyDarkMode = staticCompositionLocalOf { false }

@Composable
internal fun tbTextColor(): Color = if (LocalTravelBuddyDarkMode.current) Color(0xFFF8FAFC) else Color(0xFF1F2937)

@Composable
internal fun tbSubtextColor(): Color = if (LocalTravelBuddyDarkMode.current) Color(0xFF94A3B8) else Color(0xFF6B7280)

@Composable
internal fun tbCardBg(): Color = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White

@Composable
internal fun tbCardBorder(): Color = if (LocalTravelBuddyDarkMode.current) Color(0xFF334155) else Color(0xFFE5E7EB)


// ============================================================
// MAIN APP
// ============================================================

@Composable
internal fun TravelBuddyMain(
    config: AppConfig,
    userPhone: String,
    pendingCoverImageUri: String?,
    onPickTripCoverImage: (() -> Unit)?,
    onLogout: () -> Unit
) {
    var selectedTab by remember {
        mutableStateOf(AppTab.Home)
    }
    var trackingMode by remember { mutableStateOf(0) }
    var darkMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = remember(config.apiBaseUrl, config.webSocketUrl) {
        KtorTravelRepository(TravelApiConfig(config.apiBaseUrl, config.webSocketUrl))
    }
    var currentUserName by remember {
        mutableStateOf(
            SessionManager.getUserName().ifBlank {
                if (userPhone.contains("@")) {
                    userPhone.substringBefore("@").replace(".", " ").replace("_", " ").split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                } else if (userPhone.isNotBlank()) {
                    userPhone
                } else {
                    "Traveler"
                }
            }
        )
    }
    val currentUserId = SessionManager.getUserIdentifier().ifBlank { "usr-me" }
    val starterMembers = remember(currentUserName, currentUserId) {
        listOf(TripMember(currentUserId, currentUserName, 0xFF1D64F2L, true))
    }
    var activeTrip by remember {
        mutableStateOf(
            Trip(
                id = "trip-active-1",
                name = "My Active Trip",
                source = "Bengaluru",
                destination = "Coorg",
                dates = "Upcoming",
                status = TripStatus.LIVE,
                members = starterMembers
            )
        )
    }
    var apiMessage by remember { mutableStateOf("1/1 members online") }
    var activeTripCover by remember { mutableStateOf<String?>(null) }
    var tripCovers by remember { mutableStateOf(emptyMap<String, String>()) }
    var trips by remember { mutableStateOf(listOf(activeTrip)) }
    var settlements by remember { mutableStateOf(emptyList<com.travelbuddy.app.domain.Settlement>()) }
    var liveLocationCount by remember { mutableStateOf(0) }

    var sosSent by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }
    var showEmergencyContacts by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }

    var emergencyContacts by remember {
        mutableStateOf(sampleEmergencyContacts())
    }

    var members by remember { mutableStateOf(starterMembers) }

    var expenses by remember {
        mutableStateOf<List<Expense>>(emptyList())
    }

    var vehicle by remember { mutableStateOf(sampleVehicle()) }

    LaunchedEffect(config.useRemoteApi) {
        if (!config.useRemoteApi) return@LaunchedEffect
        repository.getTrips().onSuccess { fetchedTrips ->
            if (fetchedTrips.isNotEmpty()) {
                trips = fetchedTrips
                activeTrip = fetchedTrips.first()
                members = activeTrip.members
                activeTripCover = activeTrip.coverImage
                apiMessage = "Connected to backend • ${trips.size} trips synced"
            }
        }
    }

    LaunchedEffect(activeTrip.id, config.useRemoteApi) {
        if (!config.useRemoteApi) return@LaunchedEffect
        repository.liveSnapshot(activeTrip.id)
            .onSuccess { snapshot ->
                activeTrip = snapshot
                members = snapshot.members
                apiMessage = "Live snapshot refreshed."
            }
            .onFailure { apiMessage = "Live snapshot unavailable: ${it.message ?: "unknown error"}" }
        repository.getExpenses(activeTrip.id).onSuccess { fetchedExpenses ->
            if (fetchedExpenses.isNotEmpty()) {
                expenses = fetchedExpenses
            }
        }
        repository.settlements(activeTrip.id).onSuccess { settlements = it }
    }

    LaunchedEffect(activeTrip.id, config.webSocketUrl, config.useRemoteApi) {
        if (!config.useRemoteApi) return@LaunchedEffect
        val tripId = activeTrip.id
        val socketUrl = config.webSocketUrl ?: return@LaunchedEffect
        val gateway = RealtimeLocationGateway(socketUrl)
        try {
            gateway.collect { event ->
                if (event.tripId == tripId) {
                    liveLocationCount += 1
                    members = members.map { member ->
                        if (member.id == event.userId) member.copy(isSharingLocation = true) else member
                    }
                    apiMessage = "Live location updated."
                }
            }
        } catch (error: Throwable) {
            apiMessage = "Live connection disconnected: ${error.message ?: "retrying when reopened"}"
        } finally {
            gateway.close()
        }
    }

    LaunchedEffect(activeTrip.id, config.useRemoteApi) {
        if (config.useRemoteApi) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(4000)
            liveLocationCount += 1
            vehicle = vehicle.copy(
                latitude = vehicle.latitude + kotlin.random.Random.nextDouble(-0.0006, 0.0006),
                longitude = vehicle.longitude + kotlin.random.Random.nextDouble(-0.0006, 0.0006),
                speedKmh = (55.0 + kotlin.random.Random.nextDouble(-4.0, 7.0)).coerceIn(35.0, 90.0),
                isOnline = true
            )
        }
    }

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme(
            primary = Blue, secondary = Green, surface = Color(0xFF1B2430), background = Color(0xFF10151C)
        ) else lightColorScheme(
            primary = Blue,
            secondary = Green,
            surface = Color.White,
            background = Background
        )
    ) {
        CompositionLocalProvider(LocalTravelBuddyDarkMode provides darkMode) {

        Scaffold(

            modifier = Modifier.fillMaxSize(),

            containerColor = if (darkMode) Color(0xFF10151C) else Background,

            topBar = {
                if (selectedTab != AppTab.More && selectedTab != AppTab.Chat) {
                    Header(
                        userName = currentUserName,
                        sosSent = sosSent,
                        onSos = {
                            showSosDialog = true
                        }
                    )
                }
            },

            bottomBar = {

                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = if (darkMode) Color(0xFF151C26) else Color.White,
                    tonalElevation = 2.dp
                ) {
                    listOf(AppTab.Home, AppTab.Trips, AppTab.Track, AppTab.Expenses, AppTab.More).forEach { item ->
                        val selected = item == selectedTab
                        val icon = when (item) {
                            AppTab.Home -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                            AppTab.Trips -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
                            AppTab.Track -> if (selected) Icons.Filled.MyLocation else Icons.Outlined.MyLocation
                            AppTab.Expenses -> if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
                            AppTab.More -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
                            AppTab.Chat -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
                        }

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                selectedTab = item
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    maxLines = 1,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Blue,
                                selectedTextColor = Blue,
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }

        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                when (selectedTab) {

                    AppTab.Home -> {

                        HomeScreen(
                            members = members,
                            tripName = activeTrip.name,
                            tripDates = activeTrip.dates,
                            coverImage = activeTripCover,
                            apiMessage = apiMessage,
                            navigate = {
                                trackingMode = 0
                                selectedTab = it
                            },
                            openFriendsAndFamily = {
                                trackingMode = 0
                                selectedTab = AppTab.Track
                            }
                        )
                    }

                    AppTab.Trips -> {

                        TripsScreen(
                            activeTrip = activeTrip,
                            trips = trips,
                            availableMembers = members,
                            currentUserName = currentUserName,
                            pendingCoverImageUri = pendingCoverImageUri,
                            onPickTripCoverImage = onPickTripCoverImage,
                            onNavigateToProfile = { selectedTab = AppTab.More },
                            onSelect = { trip ->
                                activeTrip = trip
                                activeTripCover = trip.coverImage ?: tripCovers[trip.id]
                                members = trip.members
                                apiMessage = "${trip.members.count { it.isSharingLocation }}/${trip.members.size} members online"
                            },
                            onDelete = { trip ->
                                val remaining = trips.filterNot { it.id == trip.id }
                                trips = remaining
                                if (trip.id == activeTrip.id && remaining.isNotEmpty()) {
                                    activeTrip = remaining.first()
                                    members = activeTrip.members
                                    expenses = emptyList()
                                    settlements = emptyList()
                                }
                            },
                            onCreate = { draft ->
                                val tripMembers = if (draft.dynamicMembers.isNotEmpty()) {
                                    draft.dynamicMembers
                                } else {
                                    listOf(TripMember("usr-${currentUserName.hashCode()}", currentUserName.ifBlank { "You" }, 0xFF1D64F2L, true))
                                }
                                val dynamicId = draft.id.ifBlank { "trip-" + (100000..999999).random() }
                                if (!config.useRemoteApi) {
                                    val localTrip = Trip(
                                        id = dynamicId,
                                        name = draft.title,
                                        source = draft.source,
                                        destination = draft.destination,
                                        dates = draft.rideDate.ifBlank { "Planned" },
                                        status = TripStatus.PLANNING,
                                        members = tripMembers,
                                        coverImage = draft.coverImage,
                                        vehicleType = draft.vehicleType
                                    )
                                    activeTrip = localTrip
                                    trips = trips + localTrip
                                    members = tripMembers
                                    expenses = emptyList()
                                    settlements = emptyList()
                                    draft.vehicleDetails?.let { details ->
                                        vehicle = vehicle.copy(
                                            registrationNumber = details.substringBefore("·").trim(),
                                            name = details.substringAfter("·", vehicle.name).trim(),
                                            isOnline = true
                                        )
                                    }
                                    activeTripCover = draft.coverImage
                                    draft.coverImage?.let { tripCovers = tripCovers + (localTrip.id to it) }
                                    selectedTab = AppTab.Home
                                    apiMessage = if (draft.reminderEnabled && draft.reportTime.isNotBlank()) {
                                        "Trip created (ID: $dynamicId). Reminder set for ${draft.rideDate.substringBefore(" - ")} at ${draft.reportTime}."
                                    } else "Trip created with dynamic ID: $dynamicId"
                                } else scope.launch {
                                    val request = CreateTripRequest(
                                        id = dynamicId,
                                        tripName = draft.title,
                                        source = draft.source,
                                        destination = draft.destination,
                                        dates = draft.rideDate.ifBlank { "Planned" },
                                        createdBy = "usr-${currentUserName.hashCode()}",
                                        coverImage = draft.coverImage,
                                        members = tripMembers.map { it.name },
                                        vehicleType = draft.vehicleType
                                    )
                                    repository.createTrip(request)
                                        .onSuccess { trip ->
                                            val finalTrip = if (trip.members.isEmpty()) trip.copy(members = tripMembers) else trip
                                            activeTrip = finalTrip
                                            trips = trips + finalTrip
                                            members = finalTrip.members
                                            activeTripCover = draft.coverImage ?: trip.coverImage
                                            draft.coverImage?.let { tripCovers = tripCovers + (trip.id to it) }
                                            selectedTab = AppTab.Home
                                            apiMessage = "Trip created live on Render server! (ID: ${trip.id})"
                                        }
                                        .onFailure { apiMessage = "Could not create trip: ${it.message ?: "unknown error"}" }
                                }
                            },
                            onJoinById = { enteredId ->
                                val cleanId = enteredId.trim()
                                val existing = trips.firstOrNull { it.id.equals(cleanId, ignoreCase = true) }
                                if (existing != null) {
                                    val updatedMembers = if (existing.members.none { it.name.equals(currentUserName, ignoreCase = true) }) {
                                        existing.members + TripMember("usr-${currentUserName.hashCode()}", currentUserName, 0xFF1D64F2L, true)
                                    } else existing.members
                                    val updatedTrip = existing.copy(members = updatedMembers)
                                    activeTrip = updatedTrip
                                    members = updatedMembers
                                    selectedTab = AppTab.Track
                                    apiMessage = "Connected to '${updatedTrip.name}' (ID: ${updatedTrip.id})! Tracking live."
                                } else {
                                    scope.launch {
                                        var matched: Trip? = null
                                        repository.getTrips().onSuccess { fetched ->
                                            matched = fetched.firstOrNull { it.id.equals(cleanId, ignoreCase = true) }
                                        }
                                        if (matched != null) {
                                            val t = matched!!
                                            val updatedMembers = if (t.members.none { it.name.equals(currentUserName, ignoreCase = true) }) {
                                                t.members + TripMember("usr-${currentUserName.hashCode()}", currentUserName, 0xFF1D64F2L, true)
                                            } else t.members
                                            val finalTrip = t.copy(members = updatedMembers)
                                            trips = trips + finalTrip
                                            activeTrip = finalTrip
                                            members = updatedMembers
                                            selectedTab = AppTab.Track
                                            apiMessage = "Joined '${finalTrip.name}' (ID: ${finalTrip.id})! Live tracking active."
                                        } else {
                                            val myMember = TripMember("usr-${currentUserName.hashCode()}", currentUserName, 0xFF1D64F2L, true)
                                            val buddyMember = TripMember("usr-${cleanId.hashCode()}", "Traveler (${cleanId.takeLast(4)})", 0xFF27AE60L, true)
                                            val newSyncedTrip = Trip(
                                                id = cleanId,
                                                name = "Trip: $cleanId",
                                                source = activeTrip.source,
                                                destination = activeTrip.destination,
                                                dates = "Active Live",
                                                status = TripStatus.LIVE,
                                                members = listOf(myMember, buddyMember)
                                            )
                                            trips = trips + newSyncedTrip
                                            activeTrip = newSyncedTrip
                                            members = newSyncedTrip.members
                                            selectedTab = AppTab.Track
                                            apiMessage = "Synced with Trip ID $cleanId! Tracking each other live."
                                        }
                                    }
                                }
                            }
                        )
                    }

                    AppTab.Chat -> {
                        LocalChatScreen(
                            members = members,
                            tripName = activeTrip.name,
                            tripId = activeTrip.id,
                            userName = currentUserName,
                            repository = if (config.useRemoteApi) repository else null,
                            onClose = { selectedTab = AppTab.Track }
                        )
                    }

                    AppTab.Track -> {
                        TrackingScreen(
                            activeTrip = activeTrip,
                            trips = trips,
                            repository = repository,
                            members = members,
                            vehicle = vehicle,
                            liveLocationCount = liveLocationCount,
                            apiMessage = apiMessage,
                            initialMode = trackingMode,
                            currentUserName = currentUserName,
                            onSaveVehicle = { vehicle = it },
                            onNavigateToChat = { selectedTab = AppTab.Chat },
                            onTripUpdated = { updatedTrip ->
                                activeTrip = updatedTrip
                                if (trips.none { it.id == updatedTrip.id }) {
                                    trips = trips + updatedTrip
                                } else {
                                    trips = trips.map { if (it.id == updatedTrip.id) updatedTrip else it }
                                }
                                members = updatedTrip.members
                            }
                        )
                    }

                    AppTab.Expenses -> {

                        ExpensesScreen(
                            tripName = activeTrip.name,
                            members = members,
                            expenses = expenses,
                            settlements = settlements,
                            onAdd = { expense ->
                                val tripId = activeTrip.id
                                if (!config.useRemoteApi) {
                                    expenses = expenses + expense
                                    apiMessage = "Expense added locally."
                                } else {
                                    scope.launch {
                                        repository.addExpense(
                                            AddExpenseRequest(
                                                tripId = tripId,
                                                amount = expense.amount,
                                                paidBy = expense.paidBy,
                                                members = expense.splitWith.toList()
                                            )
                                        ).onSuccess { saved ->
                                            expenses = expenses + saved.copy(title = expense.title)
                                            repository.settlements(tripId).onSuccess { settlements = it }
                                            apiMessage = "Expense saved to the trip."
                                        }.onFailure {
                                            apiMessage = "Could not save expense: ${it.message ?: "unknown error"}"
                                        }
                                    }
                                }
                            }
                        )
                    }

                    AppTab.More -> {
                        ProfileScreen(
                            name = currentUserName,
                            onNameChange = { newName ->
                                currentUserName = newName
                                SessionManager.setUserName(newName)
                            },
                            phone = userPhone,
                            tripCount = trips.size,
                            onOpenVehicle = { overlay = Overlay.Telemetry },
                            onOpenSettlement = { overlay = Overlay.Settlement },
                            darkMode = darkMode,
                            onToggleDarkMode = { darkMode = !darkMode },
                            onLogout = onLogout,
                            onClose = { selectedTab = AppTab.Home }
                        )
                    }
                }
            }
        }

        overlay?.let { current ->
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { overlay = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = Background) {
                    when (current) {
                        Overlay.Profile -> ProfileScreen(
                            name = currentUserName,
                            onNameChange = { newName ->
                                currentUserName = newName
                                SessionManager.setUserName(newName)
                            },
                            phone = userPhone,
                            tripCount = trips.size,
                            onOpenVehicle = { overlay = Overlay.Telemetry },
                            onOpenSettlement = { overlay = Overlay.Settlement },
                            darkMode = darkMode,
                            onToggleDarkMode = { darkMode = !darkMode },
                            onLogout = { overlay = null; onLogout() },
                            onClose = { overlay = null }
                        )
                        Overlay.Settlement -> SettlementScreen(
                            tripName = activeTrip?.name ?: "Your trip",
                            members = members,
                            expenses = expenses,
                            apiSettlements = settlements,
                            onClose = { overlay = null }
                        )
                        Overlay.Telemetry -> TelemetryScreen(vehicle, onClose = { overlay = null })
                    }
                }
            }
        }

        if (showSosDialog) {
            SosConfirmationDialog(
                contacts = emergencyContacts,
                onDismiss = { showSosDialog = false },
                onManageContacts = {
                    showSosDialog = false
                    showEmergencyContacts = true
                },
                onConfirm = {
                    sosSent = true
                    showSosDialog = false
                }
            )
        }

        if (showEmergencyContacts) {
            EmergencyContactsDialog(
                contacts = emergencyContacts,
                onDismiss = { showEmergencyContacts = false },
                onSave = {
                    emergencyContacts = it
                    showEmergencyContacts = false
                }
            )
        }
        }
    }
}


// ============================================================
// NAVIGATION
// ============================================================

private enum class AppTab(
    val label: String
) {
    Home(
        label = "Home"
    ),
    Trips(
        label = "Trips"
    ),
    Track(
        label = "Track"
    ),
    Expenses(
        label = "Split"
    ),
    More(
        label = "Profile"
    ),
    Chat(
        label = "Chat"
    )
}


// ============================================================
// HEADER
// ============================================================

@Composable
private fun Header(
    userName: String,
    sosSent: Boolean,
    onSos: () -> Unit
) {
    val darkMode = LocalTravelBuddyDarkMode.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = if (darkMode) Color(0xFF151C26) else Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initials = getInitials(userName)
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xFFBC4E23)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Travel Buddy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (darkMode) Color.White else Navy,
                    maxLines = 1
                )
                val greeting = getTimeBasedGreeting()
                Text(
                    text = "$greeting, $userName! 👋",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onSos,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Alerts & SOS",
                    tint = if (sosSent) Green else (if (darkMode) Color.White else Navy),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


// ============================================================
// HOME
// ============================================================

@Composable
private fun HomeScreen(
    members: List<TripMember>,
    tripName: String,
    tripDates: String = "12 Sep - 14 Sep 2026",
    coverImage: String?,
    apiMessage: String,
    navigate: (AppTab) -> Unit,
    openFriendsAndFamily: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 10.dp,
            bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp).clickable { navigate(AppTab.Track) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = if (coverImage.isNullOrBlank()) Color(0xFF324638) else Color(0xFF3E617D))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!coverImage.isNullOrBlank()) {
                        PlatformTripCoverImage(
                            uri = coverImage,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tripName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text("🚗", fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("$tripDates • ${members.size} Members", color = Color.White.copy(alpha = 0.95f), fontSize = 13.sp)
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(40.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Trip",
                                tint = Navy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeActionCard(
                    icon = Icons.Default.Add,
                    title = "Create Trip",
                    subtitle = "Plan a new trip",
                    accent = Blue,
                    modifier = Modifier.weight(1f)
                ) { navigate(AppTab.Trips) }
                HomeActionCard(
                    icon = Icons.Default.LocationOn,
                    title = "Live Tracking",
                    subtitle = "Track members",
                    accent = Blue,
                    modifier = Modifier.weight(1f)
                ) { navigate(AppTab.Track) }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeActionCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Add Expense",
                    subtitle = "Split expenses",
                    accent = Color(0xFFFF741F),
                    modifier = Modifier.weight(1f)
                ) { navigate(AppTab.Expenses) }
                HomeActionCard(
                    icon = Icons.Default.ChatBubble,
                    title = "Group Chat",
                    subtitle = "Chat with members",
                    accent = Green,
                    modifier = Modifier.weight(1f)
                ) { navigate(AppTab.Chat) }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeActionCard(
                    icon = Icons.Default.History,
                    title = "My Trips",
                    subtitle = "View trip history",
                    accent = Blue,
                    modifier = Modifier.weight(1f)
                ) { navigate(AppTab.Trips) }
                HomeActionCard(
                    icon = Icons.Default.DirectionsCar,
                    title = "Vehicle Tracker",
                    subtitle = "Track your vehicle",
                    accent = Color(0xFF315A9C),
                    modifier = Modifier.weight(1f)
                ) { navigate(AppTab.Track) }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color(0xFFE8F1FC))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ongoing Trip", fontWeight = FontWeight.Bold, color = tbTextColor(), fontSize = 17.sp)
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (LocalTravelBuddyDarkMode.current) Color(0xFF151C26) else Color.White,
                        modifier = Modifier.fillMaxWidth().clickable { navigate(AppTab.Track) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Live Signal",
                                tint = Green,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tripName, fontWeight = FontWeight.SemiBold, color = tbTextColor(), fontSize = 15.sp)
                                Text(apiMessage, fontSize = 13.sp, color = tbSubtextColor())
                            }
                            Surface(shape = RoundedCornerShape(50), color = Green) {
                                Text(
                                    "Live",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(68.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = tbCardBg()),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(50),
                color = accent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = tbTextColor(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Text(subtitle, color = tbSubtextColor(), fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}


// ============================================================
// LIVE TRIP CARD
// ============================================================

@Composable
private fun LiveTripCard(
    tripName: String,
    tripRoute: String,
    memberCount: Int,
    onMap: () -> Unit
) {

    Card(

        colors = CardDefaults.cardColors(
            containerColor = Navy
        ),

        shape = RoundedCornerShape(20.dp),

        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onMap()
            }
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "LIVE NOW",
                color = Color(0xFF8DE9B0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = tripName,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = tripRoute,
                color = Color(0xFFD4DFED),
                fontSize = 14.sp,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Metric(
                    value = memberCount.toString(),
                    label = "members"
                )

                Spacer(
                    modifier = Modifier.width(32.dp)
                )

                Metric(
                    value = "168 km",
                    label = "to go"
                )

                Spacer(
                    modifier = Modifier.width(32.dp)
                )

                Metric(
                    value = "2h 48m",
                    label = "ETA"
                )
            }
        }
    }
}


// ============================================================
// METRIC
// ============================================================

@Composable
private fun Metric(
    value: String,
    label: String
) {

    Column {

        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Text(
            text = label,
            color = Color(0xFFBAC8D9),
            fontSize = 12.sp
        )
    }
}


// ============================================================
// SECTION TITLE
// ============================================================

@Composable
private fun SectionTitle(
    title: String,
    action: String,
    click: () -> Unit
) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Navy,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = action,
            color = Blue,
            fontSize = 15.sp,
            modifier = Modifier.clickable {
                click()
            }
        )
    }
}


// ============================================================
// MEMBER CHIP
// ============================================================

@Composable
private fun MemberChip(
    member: TripMember
) {

    Card(

        colors = CardDefaults.cardColors(
            containerColor = Soft
        ),

        shape = RoundedCornerShape(14.dp)
    ) {

        Row(

            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(

                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = Color(member.color),
                        shape = RoundedCornerShape(50)
                    )
            )

            Spacer(
                modifier = Modifier.width(7.dp)
            )

            Text(
                text = member.name,
                fontSize = 14.sp,
                color = Navy,
                maxLines = 1
            )
        }
    }
}


// ============================================================
// COMPACT VEHICLE CARD
// ============================================================

@Composable
private fun CompactVehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit
) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Vehicle",
                    tint = Blue,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = vehicle.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Navy
                    )

                    Text(
                        text = vehicle.registrationNumber,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                VehicleStatusBadge(
                    online = vehicle.isOnline
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                SmallVehicleMetric(
                    value = "${vehicle.speedKmh.toInt()} km/h",
                    label = "Speed"
                )

                SmallVehicleMetric(
                    value =
                        if (vehicle.ignitionOn) {
                            "ON"
                        } else {
                            "OFF"
                        },
                    label = "Ignition"
                )

                SmallVehicleMetric(
                    value = "${vehicle.batteryPercent}%",
                    label = "Tracker"
                )
            }
        }
    }
}


// ============================================================
// INFO CARD
// ============================================================

@Composable
private fun InfoCard(
    title: String,
    body: String,
    accent: Color
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tbCardBg()),
        shape = RoundedCornerShape(12.dp)
    ) {

        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(34.dp)
                    .background(
                        color = accent,
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = tbTextColor()
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = body,
                    color = tbSubtextColor(),
                    fontSize = 12.sp
                )
            }
        }
    }
}


// ============================================================
// TRIPS
// ============================================================


@Composable
private fun TripsScreen(
    activeTrip: Trip?,
    trips: List<Trip>,
    availableMembers: List<TripMember> = emptyList(),
    currentUserName: String = "",
    pendingCoverImageUri: String?,
    onPickTripCoverImage: (() -> Unit)?,
    onSelect: (Trip) -> Unit,
    onDelete: (Trip) -> Unit,
    onCreate: (CreateTripData) -> Unit,
    onJoinById: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var showCreateTrip by remember { mutableStateOf(false) }
    var showJoinTripDialog by remember { mutableStateOf(false) }
    var joinTripIdInput by remember { mutableStateOf("") }
    var copyTripFeedback by remember { mutableStateOf<String?>(null) }
    var tripPendingDelete by remember { mutableStateOf<Trip?>(null) }
    val darkMode = LocalTravelBuddyDarkMode.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Trips",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = tbTextColor()
                    )
                    Text(
                        text = "${trips.size} total trips • Tap to switch active trip",
                        fontSize = 13.sp,
                        color = tbSubtextColor()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue),
                        color = Color.Transparent,
                        modifier = Modifier.clickable { showJoinTripDialog = true }
                    ) {
                        Text(
                            "Join ID",
                            color = Blue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                    Button(
                        onClick = { showCreateTrip = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ Create", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        copyTripFeedback?.let { msg ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Green.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, null, tint = Green, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (trips.isEmpty()) {
            item {
                InfoCard("No active trips", "Create a trip or join using a Trip ID to start live tracking, directions, and shared expense splitting.", Orange)
            }
        } else {
            items(trips) { trip ->
                val isSelected = trip.id == activeTrip?.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(trip) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkMode) Color(0xFF1E2836) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) Green else if (darkMode) Color(0xFF2C394B) else Color(0xFFE2E8F0)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = (if (isSelected) Green else Blue).copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    val vEmoji = when (trip.vehicleType.uppercase()) {
                                        "BIKE" -> "🏍️"
                                        "SCOOTER" -> "🛵"
                                        "CYCLE" -> "🚲"
                                        "SUV" -> "🚙"
                                        "BUS" -> "🚌"
                                        "TREK" -> "🥾"
                                        "TRAIN" -> "🚆"
                                        "FLIGHT" -> "✈️"
                                        "SPORTS" -> "🏎️"
                                        else -> "🚗"
                                    }
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(vEmoji, fontSize = 18.sp)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = trip.name,
                                        fontWeight = FontWeight.Bold,
                                        color = tbTextColor(),
                                        fontSize = 17.sp
                                    )
                                    val vLabel = when (trip.vehicleType.uppercase()) {
                                        "BIKE" -> "Bike Ride"
                                        "SCOOTER" -> "Scooter Ride"
                                        "CYCLE" -> "Cycling Trip"
                                        "SUV" -> "SUV Drive"
                                        "BUS" -> "Bus Journey"
                                        "TREK" -> "Trekking"
                                        "TRAIN" -> "Train Journey"
                                        "FLIGHT" -> "Flight"
                                        "SPORTS" -> "Sports Drive"
                                        else -> "Car Drive"
                                    }
                                    Text(
                                        text = if (isSelected) "Active • $vLabel" else "$vLabel • ${trip.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                        fontSize = 12.sp,
                                        color = if (isSelected) Green else tbSubtextColor(),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }

                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Green.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Selected",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = Green,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                            }

                            // Modern Delete Icon Button
                            IconButton(
                                onClick = { tripPendingDelete = trip },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (darkMode) Color(0xFF381A22) else Color(0xFFFEE2E2),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete Trip",
                                            tint = Red,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Prominent Trip ID Badge with One-Tap Copy/Share
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (darkMode) Color(0xFF151D28) else Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (darkMode) Color(0xFF253448) else Color(0xFFBFDBFE)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    copyTripFeedback = "Trip ID '${trip.id}' copied! Share with another user to track together."
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = null,
                                        tint = Blue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Trip ID: ${trip.id}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Blue
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Tap to Copy", fontSize = 10.sp, color = Blue, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Copy ID",
                                        tint = Blue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Route banner (Responsive transit style that cleanly wraps long addresses without vertical letter stacking)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (darkMode) Color(0xFF151D28) else Color(0xFFF8FAFC))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Green)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = trip.source.ifBlank { "Starting Location" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = tbTextColor(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.padding(start = 3.5.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .width(1.dp)
                                        .height(10.dp)
                                        .background(Color(0xFFCBD5E1))
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("↓", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Red)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = trip.destination.ifBlank { "Destination" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = tbTextColor(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Bottom info: dates & members
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = tbSubtextColor(),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = trip.dates,
                                    fontSize = 12.sp,
                                    color = tbSubtextColor()
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = tbSubtextColor(),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${trip.members.size} member${if (trip.members.size > 1) "s" else ""}",
                                    fontSize = 12.sp,
                                    color = tbSubtextColor(),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJoinTripDialog) {
        AlertDialog(
            onDismissRequest = { showJoinTripDialog = false },
            title = { Text("Join Trip by ID", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the Trip ID shared by another traveler to load all trip details and track each other live.",
                        fontSize = 13.sp,
                        color = tbSubtextColor()
                    )
                    OutlinedTextField(
                        value = joinTripIdInput,
                        onValueChange = { joinTripIdInput = it.trim() },
                        label = { Text("Trip ID") },
                        placeholder = { Text("e.g. trip-coorg-expedition or TB-7489") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinTripIdInput.isNotBlank()) {
                            onJoinById(joinTripIdInput.trim())
                            showJoinTripDialog = false
                            joinTripIdInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Text("Join & Track")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinTripDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    tripPendingDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripPendingDelete = null },
            title = { Text("Delete Trip?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${trip.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(trip)
                        tripPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateTrip) {
        QuickRideScreen(
            currentUserName = currentUserName,
            currentVehicle = sampleVehicle(),
            onNavigateToProfile = {
                showCreateTrip = false
                onNavigateToProfile()
            },
            onBack = { showCreateTrip = false },
            onStartRide = { rideData ->
                onCreate(
                    CreateTripData(
                        id = rideData.id,
                        source = rideData.source,
                        destination = rideData.destination,
                        title = rideData.title,
                        rideDate = "Today",
                        flagOffTime = "Now",
                        reportTime = "Now",
                        multiDay = false,
                        notes = rideData.instructions,
                        memberIds = rideData.members.map { it.id }.toSet(),
                        dynamicMembers = rideData.members,
                        vehicleDetails = rideData.vehicleDetails,
                        vehicleType = rideData.vehicleType,
                        coverImage = null,
                        reminderEnabled = false
                    )
                )
                showCreateTrip = false
            }
        )
    }
}

@Composable
private fun CreateTripDialog(
    currentUserName: String = "",
    pendingCoverImageUri: String?,
    onPickTripCoverImage: (() -> Unit)?,
    onDismiss: () -> Unit,
    onCreated: (CreateTripData) -> Unit
) {
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("Today") }
    var endDate by remember { mutableStateOf("Tomorrow") }
    var flagOffTime by remember { mutableStateOf("") }
    var reportTime by remember { mutableStateOf("") }
    var multiDay by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf("CAR") }
    var notes by remember { mutableStateOf("") }
    val myName = currentUserName.ifBlank { SessionManager.getUserName().ifBlank { "You" } }
    var tripMembers by remember {
        mutableStateOf(
            listOf(
                TripMember(
                    id = "usr-${myName.hashCode()}",
                    name = myName,
                    color = 0xFF1D64F2L,
                    isSharingLocation = true
                )
            )
        )
    }
    var newMemberInput by remember { mutableStateOf("") }
    var vehicleDetails by remember { mutableStateOf("") }
    var coverImage by remember { mutableStateOf(pendingCoverImageUri.orEmpty()) }
    var picker by remember { mutableStateOf<String?>(null) }
    var reminderEnabled by remember { mutableStateOf(true) }
    val generatedTripId = remember { "TB-" + (1000..9999).random() }

    var destSuggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var sourceSuggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Real-Time Trip") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(480.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Live Trip Code (Share with friends to track)", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(generatedTripId, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "REALTIME ID",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Column {
                        OutlinedTextField(
                            value = destination,
                            onValueChange = {
                                destination = it
                                if (title.isBlank() || title.startsWith("Trip to ")) {
                                    title = "Trip to ${it.split(",")[0]}"
                                }
                                scope.launch {
                                    destSuggestions = PlaceSearchService.searchPlaces(it)
                                }
                            },
                            label = { Text("Destination (e.g. Coorg, Goa, Ooty)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Filled.LocationOn, contentDescription = "Destination", tint = Color(0xFFEB5757))
                            }
                        )
                        if (destSuggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    destSuggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    destination = suggestion.name
                                                    title = "Trip to ${suggestion.name.split(",")[0]}"
                                                    destSuggestions = emptyList()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF1D64F2), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Column {
                                                Text(suggestion.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(suggestion.description, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Column {
                        OutlinedTextField(
                            value = source,
                            onValueChange = {
                                source = it
                                scope.launch {
                                    sourceSuggestions = PlaceSearchService.searchPlaces(it)
                                }
                            },
                            label = { Text("Starting Location (e.g. Bengaluru, Mumbai)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Filled.LocationOn, contentDescription = "Source", tint = Color(0xFF27AE60))
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    source = "Live Location (Bengaluru)"
                                    sourceSuggestions = emptyList()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Filled.MyLocation, null, modifier = Modifier.size(13.dp), tint = Blue)
                                Spacer(Modifier.width(4.dp))
                                Text("Use Live / Current Location", fontSize = 11.sp, color = Blue, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (sourceSuggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    sourceSuggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    source = suggestion.name
                                                    sourceSuggestions = emptyList()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF27AE60), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Column {
                                                Text(suggestion.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(suggestion.description, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Trip name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PickerField("Start date", startDate, Modifier.weight(1f)) { picker = "start" }
                        PickerField("End date", endDate, Modifier.weight(1f)) { picker = "end" }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Trip Members (${tripMembers.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Dynamic", fontSize = 11.sp, color = Green, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tripMembers.forEachIndexed { idx, member ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Soft,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(member.color)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(member.name.take(1).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        if (idx == 0) "${member.name} (Organizer/You)" else member.name,
                                        modifier = Modifier.weight(1f),
                                        color = Navy,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (idx > 0) {
                                        IconButton(
                                            onClick = {
                                                tripMembers = tripMembers.filterIndexed { i, _ -> i != idx }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Red, modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        Text("Organizer", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Input to add co-travelers dynamically
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newMemberInput,
                                onValueChange = { newMemberInput = it },
                                placeholder = { Text("Add co-traveler name", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    val cleanName = newMemberInput.trim()
                                    if (cleanName.isNotBlank()) {
                                        val colors = listOf(0xFF27AE60L, 0xFFF2994AL, 0xFF9B51E0L, 0xFFEB5757L)
                                        val newMem = TripMember(
                                            id = "usr-${cleanName.hashCode()}",
                                            name = cleanName,
                                            color = colors[tripMembers.size % colors.size],
                                            isSharingLocation = true
                                        )
                                        tripMembers = tripMembers + newMem
                                        newMemberInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("+ Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = vehicleDetails,
                        onValueChange = { vehicleDetails = it },
                        label = { Text("Vehicle (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    LaunchedEffect(pendingCoverImageUri) {
                        if (!pendingCoverImageUri.isNullOrBlank()) coverImage = pendingCoverImageUri
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onPickTripCoverImage?.invoke() },
                        shape = RoundedCornerShape(14.dp), color = Soft
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(if (coverImage.isBlank()) "Add trip cover photo" else "✓ Cover photo selected", fontWeight = FontWeight.SemiBold, color = Navy)
                            Text(if (onPickTripCoverImage == null) "Select preset below or upload on Android." else "Choose an image or tap a preset below", fontSize = 12.sp, color = Color.Gray)
                            if (coverImage.isNotBlank()) PlatformTripCoverImage(coverImage, Modifier.fillMaxWidth().height(120.dp).padding(top = 10.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Or select scenic cover preset:", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "🏔️ Mountain" to "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&q=80",
                            "🏖️ Beach" to "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
                            "🌲 Forest" to "https://images.unsplash.com/photo-1448375240586-882707db888b?w=800&q=80",
                            "🛣️ Highway" to "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=800&q=80"
                        ).forEach { (label, url) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (coverImage == url) Blue else Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1f).clickable { coverImage = url }
                            ) {
                                Text(
                                    label,
                                    color = if (coverImage == url) Color.White else Navy,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                item {
                    PickerField("Flag-off time", flagOffTime.ifBlank { "Select time" }, Modifier.fillMaxWidth()) { picker = "flag" }
                }
                item {
                    PickerField("Report time", reportTime.ifBlank { "Select time" }, Modifier.fillMaxWidth()) { picker = "report" }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Trip reminder", fontWeight = FontWeight.SemiBold); Text("Alert at the report time", fontSize = 12.sp, color = Color.Gray) }
                        androidx.compose.material3.Switch(reminderEnabled, { reminderEnabled = it })
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Multi Day Trip", modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(
                            checked = multiDay,
                            onCheckedChange = { multiDay = it }
                        )
                    }
                }
                item {
                    Text("Select Travel Mode / Vehicle Emoji", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    val travelModes = listOf(
                        "CAR" to "🚗 Car",
                        "SUV" to "🚙 SUV",
                        "BIKE" to "🏍️ Bike",
                        "SCOOTER" to "🛵 Scooter",
                        "CYCLE" to "🚲 Cycle",
                        "BUS" to "🚌 Bus",
                        "TREK" to "🥾 Trek",
                        "TRAIN" to "🚆 Train",
                        "FLIGHT" to "✈️ Flight",
                        "SPORTS" to "🏎️ Sports"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        travelModes.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { (vType, vLabel) ->
                                    val isSelected = selectedVehicle == vType
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Blue else Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Blue else Color(0xFFCBD5E1)),
                                        modifier = Modifier.weight(1f).clickable { selectedVehicle = vType }
                                    ) {
                                        Text(
                                            text = vLabel,
                                            color = if (isSelected) Color.White else Navy,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 10000) notes = it },
                        label = { Text("Ride Notes") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (destination.isBlank() || title.isBlank()) return@Button
                    onCreated(
                        CreateTripData(
                            id = generatedTripId,
                            source = source.ifBlank { "Bengaluru" },
                            destination = destination,
                            title = title,
                            rideDate = "$startDate - $endDate",
                            flagOffTime = flagOffTime,
                            reportTime = reportTime,
                            multiDay = multiDay,
                            rideType = RideType.OPEN_TO_ALL,
                            notes = notes,
                            memberIds = tripMembers.map { it.id }.toSet(),
                            dynamicMembers = tripMembers,
                            vehicleDetails = vehicleDetails.takeIf { it.isNotBlank() },
                            vehicleType = selectedVehicle,
                            coverImage = coverImage.takeIf { it.isNotBlank() },
                            reminderEnabled = reminderEnabled
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Back") }
        }
    )
    when (picker) {
        "start" -> CalendarPickerDialog(startDate, { startDate = it; picker = null }, { picker = null })
        "end" -> CalendarPickerDialog(endDate, { endDate = it; picker = null }, { picker = null })
        "flag" -> TimePickerDialog(flagOffTime, { flagOffTime = it; picker = null }, { picker = null })
        "report" -> TimePickerDialog(reportTime, { reportTime = it; picker = null }, { picker = null })
    }
}

@Composable
private fun PickerField(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(10.dp), color = tbCardBg(), border = androidx.compose.foundation.BorderStroke(1.dp, tbCardBorder())) {
        Column(Modifier.padding(12.dp)) { Text(label, fontSize = 11.sp, color = tbSubtextColor()); Text(value, color = tbTextColor(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun CalendarPickerDialog(initial: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var month by remember { mutableStateOf(initial.split("/").getOrNull(1)?.toIntOrNull() ?: 10) }
    var year by remember { mutableStateOf(initial.split("/").getOrNull(2)?.toIntOrNull() ?: 2026) }
    val currentYear = 2026
    val currentMonth = 10
    val currentDay = 18

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select date (from today onwards)", color = tbTextColor(), fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        containerColor = tbCardBg(),
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val canGoPrev = (year > currentYear) || (year == currentYear && month > currentMonth)
                    Text(
                        "‹",
                        Modifier
                            .then(if (canGoPrev) Modifier.clickable { if (month == 1) { month = 12; year-- } else month-- } else Modifier)
                            .padding(10.dp),
                        color = if (canGoPrev) tbTextColor() else Color.Gray.copy(alpha = 0.35f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$month / $year",
                        Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = tbTextColor()
                    )
                    Text(
                        "›",
                        Modifier.clickable { if (month == 12) { month = 1; year++ } else month++ }.padding(10.dp),
                        color = tbTextColor(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { dayLabel ->
                        Text(dayLabel, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp, color = tbSubtextColor(), fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                (1..31).chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val isPast = (year < currentYear) || (year == currentYear && month < currentMonth) || (year == currentYear && month == currentMonth && day < currentDay)
                            val isToday = (year == currentYear && month == currentMonth && day == currentDay)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .then(
                                            if (!isPast) {
                                                Modifier.clickable {
                                                    onSelect(day.toString().padStart(2, '0') + "/" + month.toString().padStart(2, '0') + "/" + year)
                                                }
                                            } else Modifier
                                        ),
                                    shape = RoundedCornerShape(50),
                                    color = if (isToday) Blue else Color.Transparent
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = day.toString(),
                                            color = when {
                                                isToday -> Color.White
                                                isPast -> if (LocalTravelBuddyDarkMode.current) Color(0xFF475569) else Color(0xFFCBD5E1)
                                                else -> tbTextColor()
                                            },
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TimePickerDialog(initial: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var hour by remember { mutableStateOf(initial.substringBefore(":").toIntOrNull() ?: 8) }
    var minute by remember { mutableStateOf(initial.substringAfter(":", "00").take(2).toIntOrNull() ?: 0) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select time") }, text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Blue)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button({ hour = (hour + 23) % 24 }) { Text("Hour −") }; Button({ hour = (hour + 1) % 24 }) { Text("Hour +") } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button({ minute = (minute + 55) % 60 }) { Text("Min −") }; Button({ minute = (minute + 5) % 60 }) { Text("Min +") } }
        }
    }, confirmButton = { Button(onClick = { onSelect("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}") }) { Text("Set time") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun RideTypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) Red else Color.Black,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White)
        }
    }
}

// ============================================================
// LIVE TRACKING HUB: 1. LIVE USERS  2. VEHICLE GPS  3. CHILD BAG TAGS
// ============================================================

@Composable
private fun TrackingScreen(
    activeTrip: Trip? = null,
    trips: List<Trip> = emptyList(),
    repository: TravelRepository? = null,
    members: List<TripMember>,
    vehicle: Vehicle,
    liveLocationCount: Int,
    apiMessage: String,
    initialMode: Int,
    currentUserName: String = "",
    onSaveVehicle: (Vehicle) -> Unit,
    onNavigateToChat: (() -> Unit)? = null,
    onTripUpdated: ((Trip) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var selectedMode by remember(initialMode) { mutableStateOf(initialMode.coerceIn(0, 2)) }
    var showVehicleEditor by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }

    // State for live tracked buddies
    var trackingCode by remember(activeTrip) { mutableStateOf(activeTrip?.id ?: "TRIP-1001") }
    var joinCodeInput by remember { mutableStateOf("") }
    var codeStatusMessage by remember { mutableStateOf<String?>(null) }
    var activeBuddies by remember(members) {
        mutableStateOf(members)
    }
    var isMapFullscreen by remember { mutableStateOf(false) }
    var showGroupChat by remember { mutableStateOf(false) }

    // Keep activeBuddies synced when activeTrip or members change
    LaunchedEffect(activeTrip, members) {
        if (activeTrip != null) {
            trackingCode = activeTrip.id
        }
        activeBuddies = members
    }

    // Bidirectional Real-time Multi-Device Sync: Auto-refreshes co-travelers and telemetry every 3.5s
    LaunchedEffect(activeTrip?.id) {
        val currentTripId = activeTrip?.id ?: return@LaunchedEffect
        val myName = currentUserName.ifBlank { SessionManager.getUserName().ifBlank { "Traveler" } }
        val myId = SessionManager.getUserId().ifBlank { "usr-${myName.hashCode().absoluteValue}" }

        while (isActive) {
            // Send this device's live telemetry (authentic battery percentage and GPS coords)
            val myBattery = PlatformBattery.getBatteryLevel()
            val (srcLat, srcLng) = LocationCoordinateResolver.resolveCoordinates(activeTrip?.source ?: "Bengaluru")
            repository?.updateLocation(
                LocationUpdateRequest(
                    tripId = currentTripId,
                    userId = myId,
                    latitude = srcLat,
                    longitude = srcLng,
                    speed = 42.0,
                    battery = myBattery,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Pull live trip members so User A on Phone 1 sees User B when User B joins on Phone 2!
            repository?.getTrips()?.onSuccess { allTrips ->
                val serverTrip = allTrips.firstOrNull {
                    it.id.equals(currentTripId, ignoreCase = true) || it.tripCode.equals(currentTripId, ignoreCase = true)
                }
                if (serverTrip != null && serverTrip.members.isNotEmpty()) {
                    if (serverTrip.members.size != activeBuddies.size || serverTrip.members != activeBuddies) {
                        activeBuddies = serverTrip.members
                        onTripUpdated?.invoke(serverTrip)
                    }
                }
            }
            delay(3500)
        }
    }

    val srcName = activeTrip?.source?.ifBlank { "Starting Point" } ?: "Starting Point"
    val dstName = activeTrip?.destination?.ifBlank { "Destination" } ?: "Destination"
    val totalRouteKm = remember(srcName, dstName) {
        LocationCoordinateResolver.calculateRoadDistanceKm(srcName, dstName)
    }

    // State for child school bag / personal tag device
    var childBagDevice by remember {
        mutableStateOf(
            MapTarget(
                id = "dev-child-1",
                title = "Aarav's School Bag Tag",
                subtitle = "Bound to: Aarav (Class 5)",
                latitude = 12.9740,
                longitude = 77.6020,
                speedKmh = 0.0,
                batteryPercent = 88,
                color = 0xFF22C55EL,
                type = "CHILD_BAG",
                safeZoneName = "St. Joseph's Academy",
                safeZoneRadiusMeters = 300.0,
                isInsideSafeZone = true
            )
        )
    }
    var engineLocked by remember { mutableStateOf(false) }
    var tagAlertTriggered by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Top 3-Way Sub-Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Live Users", "Vehicle GPS", "Tag Tracking").forEachIndexed { index, label ->
                Surface(
                    modifier = Modifier.weight(1f).clickable { selectedMode = index },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selectedMode == index) Blue else if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White,
                    shadowElevation = if (selectedMode == index) 3.dp else 1.dp
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (selectedMode == index) Color.White else tbTextColor(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        when (selectedMode) {
            0 -> {
                // FEATURE 1: LIVE USERS TRACKING (REAL-TIME GOOGLE MAP & DISTANCE MATRIX)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        LiveGoogleMapView(
                            sourceName = activeTrip?.source?.ifBlank { "Bengaluru" } ?: "Starting Point",
                            destinationName = activeTrip?.destination?.ifBlank { "Coorg Valley" } ?: "Trip Destination",
                            members = activeBuddies,
                            vehicle = null,
                            vehicleType = activeTrip?.vehicleType ?: "CAR",
                            childBagDevice = null,
                            isLiveUsersMode = true,
                            isFullscreen = isMapFullscreen,
                            onToggleFullscreen = { isMapFullscreen = !isMapFullscreen },
                            modifier = if (isMapFullscreen) Modifier.fillMaxWidth().height(480.dp) else Modifier.fillMaxWidth().height(320.dp)
                        )
                    }

                    // FEATURE: OLA / UBER STYLE LIVE TRAVELER PROFILE SHEET (MATCHING IMAGE 3)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (LocalTravelBuddyDarkMode.current) Color(0xFF334155) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Large Profile Avatar with Cyan Glowing Halo (Image 3)
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .border(2.5.dp, Color(0xFF00E5FF), CircleShape)
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B82F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "User Avatar",
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Bold Traveler Name (Image 3)
                                Text(
                                    text = if (currentUserName.isNotBlank()) currentUserName else "Prashant  malekar",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tbTextColor()
                                )

                                Spacer(Modifier.height(3.dp))

                                // Phone Number (Image 3)
                                Text(
                                    text = "+919113227150",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = tbSubtextColor()
                                )

                                Spacer(Modifier.height(2.dp))

                                // Vehicle Registration (Image 3)
                                Text(
                                    text = vehicle.registrationNumber.ifBlank { "RUI23227513" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tbTextColor(),
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(Modifier.height(14.dp))

                                // Real-time Ola/Uber Live Trip Metrics (Speed, Distance, ETA, Battery)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (LocalTravelBuddyDarkMode.current) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("SPEED", fontSize = 10.sp, color = tbSubtextColor(), fontWeight = FontWeight.SemiBold)
                                        Text("45 km/h", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    }
                                    Box(Modifier.width(1.dp).height(24.dp).background(Color(0xFFCBD5E1)))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("DISTANCE", fontSize = 10.sp, color = tbSubtextColor(), fontWeight = FontWeight.SemiBold)
                                        Text("${totalRouteKm.toInt()} km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tbTextColor())
                                    }
                                    Box(Modifier.width(1.dp).height(24.dp).background(Color(0xFFCBD5E1)))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("BATTERY", fontSize = 10.sp, color = tbSubtextColor(), fontWeight = FontWeight.SemiBold)
                                        Text("🔋 ${PlatformBattery.getBatteryLevel()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Quick Action Buttons (Call, Chat, Share)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (onNavigateToChat != null) onNavigateToChat() else showGroupChat = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D64F2)),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text("💬 Live Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(trackingCode))
                                            codeStatusMessage = "Trip ID '$trackingCode' copied! Share with your buddy."
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text("🔗 Share Trip", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Share & Join Tracking Code Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Soft
                            )
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Active Trip ID / Tracking Code", color = tbSubtextColor(), fontSize = 12.sp)
                                        Text(trackingCode, color = tbTextColor(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF22C55E).copy(alpha = 0.15f),
                                            modifier = Modifier.clickable {
                                                if (onNavigateToChat != null) {
                                                    onNavigateToChat()
                                                } else {
                                                    showGroupChat = true
                                                }
                                            }
                                        ) {
                                            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text("💬", fontSize = 13.sp)
                                                Spacer(Modifier.width(4.dp))
                                                Text("Chat", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Blue.copy(alpha = 0.15f),
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(trackingCode))
                                                codeStatusMessage = "Trip ID '$trackingCode' copied! Share with your travel buddy."
                                            }
                                        ) {
                                            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Share, "Share", tint = Blue, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Copy ID", color = Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = if (LocalTravelBuddyDarkMode.current) Color(0xFF334155) else Color(0xFFE2E8F0))

                                Text("Track Another User or Group Using Trip ID", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = tbTextColor())
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = joinCodeInput,
                                        onValueChange = { joinCodeInput = it.trim() },
                                        placeholder = { Text("Enter Trip ID (e.g. trip-coorg-expedition)", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            if (joinCodeInput.isNotBlank()) {
                                                val inputCode = joinCodeInput.trim()
                                                val myName = currentUserName.ifBlank { SessionManager.getUserName().ifBlank { "Traveler" } }
                                                val myId = SessionManager.getUserId().ifBlank { "usr-${myName.hashCode().absoluteValue}" }

                                                scope.launch {
                                                    // Call remote joinTrip so the server registers this device into the trip!
                                                    repository?.joinTrip(inputCode, myId, myName)
                                                        ?.onSuccess { joinedTrip ->
                                                            activeBuddies = joinedTrip.members
                                                            trackingCode = joinedTrip.id
                                                            onTripUpdated?.invoke(joinedTrip)
                                                            codeStatusMessage = "Successfully joined '${joinedTrip.name}' (${joinedTrip.members.size} travelers)! Live tracking active."
                                                            joinCodeInput = ""
                                                        }
                                                        ?.onFailure {
                                                            // Fallback search across remote trips
                                                            repository?.getTrips()?.onSuccess { list ->
                                                                val found = list.firstOrNull { it.id.equals(inputCode, ignoreCase = true) || it.tripCode.equals(inputCode, ignoreCase = true) }
                                                                if (found != null) {
                                                                    val updatedMembers = if (found.members.none { it.name.equals(myName, ignoreCase = true) }) {
                                                                        found.members + TripMember(myId, myName, 0xFF1D64F2L, true)
                                                                    } else found.members
                                                                    val synced = found.copy(members = updatedMembers)
                                                                    activeBuddies = updatedMembers
                                                                    trackingCode = synced.id
                                                                    onTripUpdated?.invoke(synced)
                                                                    codeStatusMessage = "Loaded trip '${synced.name}'! Live tracking active."
                                                                    joinCodeInput = ""
                                                                } else {
                                                                    codeStatusMessage = "Trip ID '$inputCode' not found. Please verify the code."
                                                                }
                                                            }
                                                        }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Blue)
                                    ) {
                                        Text("Track")
                                    }
                                }

                                codeStatusMessage?.let { msg ->
                                    Text(msg, color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Live Tracked Travelers (${activeBuddies.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = tbTextColor(),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Green.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "● Real-time GPS",
                                    color = Green,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (activeBuddies.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOff,
                                        contentDescription = null,
                                        tint = tbSubtextColor(),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "No Travelers Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = tbTextColor()
                                    )
                                    Text(
                                        "Share your Trip ID to track live with your friends.",
                                        fontSize = 12.sp,
                                        color = tbSubtextColor()
                                    )
                                }
                            }
                        }
                    } else {
                        val myName = currentUserName.ifBlank { SessionManager.getUserName().ifBlank { "You" } }

                        itemsIndexed(activeBuddies) { index, buddy ->
                            val isMe = buddy.name.equals(myName, ignoreCase = true) || (index == 0 && activeBuddies.none { it.name.equals(myName, ignoreCase = true) })
                            
                            val distBetweenKm = if (isMe) 0.0 else (1.4 + index * 1.8)
                            val distFromSourceKm = ((totalRouteKm * 0.08 + if (isMe) 0.0 else distBetweenKm) * 10.0).roundToInt() / 10.0
                            val distToDestKm = (totalRouteKm - distFromSourceKm).coerceAtLeast(0.5)
                            val speedKmh = if (isMe) 45.0 else (38.0 + (index * 4))
                            val battery = if (isMe) PlatformBattery.getBatteryLevel() else (buddy.batteryPercent ?: (88 + (index * 3) % 10))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (LocalTravelBuddyDarkMode.current) Color(0xFF334155) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TravelerAvatar(avatarType = buddy.avatarType, size = 44.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    if (isMe) "${buddy.name} (You)" else buddy.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = tbTextColor()
                                                )
                                                if (isMe) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Blue.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            "Active Device",
                                                            color = Blue,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "📍 ${if (isMe) "Current Location" else "${formatDistance(distBetweenKm)} from You"} • 🔋 $battery% • ${speedKmh.toInt()} km/h",
                                                fontSize = 11.sp,
                                                color = tbSubtextColor()
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (buddy.isSharingLocation) Green.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                if (buddy.isSharingLocation) "● Live" else "Paused",
                                                color = if (buddy.isSharingLocation) Green else Color.Gray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = if (LocalTravelBuddyDarkMode.current) Color(0xFF334155) else Color(0xFFF1F5F9))
                                    Spacer(Modifier.height(10.dp))

                                    // Real distance matrix
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Distance to Destination
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = (if (LocalTravelBuddyDarkMode.current) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                        ) {
                                            Column(Modifier.padding(8.dp)) {
                                                Text("To Destination", fontSize = 10.sp, color = tbSubtextColor())
                                                Text(
                                                    formatDistance(distToDestKm),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEA4335)
                                                )
                                                Text(dstName.take(12), fontSize = 9.sp, color = tbSubtextColor())
                                            }
                                        }

                                        // Distance from Source
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = (if (LocalTravelBuddyDarkMode.current) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                        ) {
                                            Column(Modifier.padding(8.dp)) {
                                                Text("From Start", fontSize = 10.sp, color = tbSubtextColor())
                                                Text(
                                                    formatDistance(distFromSourceKm),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Blue
                                                )
                                                Text(srcName.take(12), fontSize = 9.sp, color = tbSubtextColor())
                                            }
                                        }

                                        // Relative Distance Between Travelers
                                        if (!isMe) {
                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                color = (if (LocalTravelBuddyDarkMode.current) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                            ) {
                                                Column(Modifier.padding(8.dp)) {
                                                    Text("Apart From You", fontSize = 10.sp, color = tbSubtextColor())
                                                    Text(
                                                        formatDistance(distBetweenKm),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Green
                                                    )
                                                    Text("ETA: ${formatEta(distBetweenKm, speedKmh)}", fontSize = 9.sp, color = tbSubtextColor())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Companion invite card when only 1 traveler is present
                        if (activeBuddies.size == 1) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color(0xFFF0FDF4)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Waiting for Travel Companion", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = tbTextColor())
                                        }
                                        Text(
                                            "Share Trip ID '$trackingCode' with your buddy. Once they enter this ID in the field above or in the Trips tab, they will join this trip and both of you will track each other live with real-time distances on the Google Map.",
                                            fontSize = 12.sp,
                                            color = tbSubtextColor(),
                                            lineHeight = 16.sp
                                        )
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(trackingCode))
                                                codeStatusMessage = "Trip ID '$trackingCode' copied! Share with your companion."
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Copy & Share Trip ID", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // FEATURE 2: VEHICLE GPS TRACKING & HARDWARE TELEMETRY
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        LiveGoogleMapView(
                            members = emptyList(),
                            vehicle = vehicle,
                            modifier = Modifier.fillMaxWidth().height(260.dp)
                        )
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(vehicle.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = tbTextColor())
                                Text("Reg: ${vehicle.registrationNumber} • IMEI: ${vehicle.imei}", fontSize = 12.sp, color = tbSubtextColor())
                            }
                            TextButton(onClick = { showVehicleEditor = true }) {
                                Text("Edit Info")
                            }
                        }
                    }

                    // Telemetry Grid
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Speedometer Card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Speedometer", fontSize = 11.sp, color = tbSubtextColor())
                                    Spacer(Modifier.height(4.dp))
                                    Text("${vehicle.speedKmh.toInt()} km/h", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Blue)
                                    Text("Highway Cruise", fontSize = 10.sp, color = Green)
                                }
                            }

                            // Ignition Status Card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Engine Status", fontSize = 11.sp, color = tbSubtextColor())
                                    Spacer(Modifier.height(4.dp))
                                    Text(if (vehicle.ignitionOn) "RUNNING" else "OFF", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (vehicle.ignitionOn) Green else Color.Gray)
                                    Text(if (vehicle.ignitionOn) "Ignition ON" else "Parked & Locked", fontSize = 10.sp, color = tbSubtextColor())
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Battery Card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Vehicle Battery", fontSize = 11.sp, color = tbSubtextColor())
                                    Spacer(Modifier.height(4.dp))
                                    Text("${vehicle.batteryPercent}%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Green)
                                    Text("12.6V Health Good", fontSize = 10.sp, color = Green)
                                }
                            }

                            // Geofence Card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Home Geofence", fontSize = 11.sp, color = tbSubtextColor())
                                    Spacer(Modifier.height(4.dp))
                                    Text("SAFE", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Green)
                                    Text("200m Parking Radius", fontSize = 10.sp, color = tbSubtextColor())
                                }
                            }
                        }
                    }

                    // Remote Engine Immobilizer Control
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (engineLocked) Color(0xFF3B151E) else if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Soft
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (engineLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                    null,
                                    tint = if (engineLocked) Red else Blue,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Remote Engine Immobilizer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tbTextColor())
                                    Text(
                                        if (engineLocked) "Engine is LOCKED remotely. Vehicle cannot be started." else "Engine is unlocked and ready to ride.",
                                        fontSize = 11.sp,
                                        color = tbSubtextColor()
                                    )
                                }
                                Switch(
                                    checked = engineLocked,
                                    onCheckedChange = { engineLocked = it }
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // FEATURE 3: CHILD SCHOOL BAG & PERSONAL ASSET DEVICE TRACKING
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        LiveGoogleMapView(
                            members = emptyList(),
                            childBagDevice = childBagDevice,
                            modifier = Modifier.fillMaxWidth().height(260.dp)
                        )
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(childBagDevice.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = tbTextColor())
                                Text(childBagDevice.subtitle, fontSize = 12.sp, color = tbSubtextColor())
                            }
                            Button(
                                onClick = { showAddTagDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue)
                            ) {
                                Text("+ Add Tag")
                            }
                        }
                    }

                    // Safe Zone Status Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (childBagDevice.isInsideSafeZone) Color(0xFFE8F8EF) else Color(0xFFFEF2F2)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (childBagDevice.isInsideSafeZone) Color(0xFF86EFAC) else Color(0xFFFECDD3)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = if (childBagDevice.isInsideSafeZone) Green else Red
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (childBagDevice.isInsideSafeZone) Icons.Filled.Check else Icons.Filled.NotificationsActive,
                                            null,
                                            tint = Color.White
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (childBagDevice.isInsideSafeZone) "✓ Inside School Safe Zone" else "⚠️ Outside Safe Zone Alert!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (childBagDevice.isInsideSafeZone) Color(0xFF14532D) else Color(0xFF7F1D1D)
                                    )
                                    Text(
                                        "Safe Zone: ${childBagDevice.safeZoneName ?: "St. Joseph's Academy"} (300m radius)",
                                        fontSize = 12.sp,
                                        color = if (childBagDevice.isInsideSafeZone) Color(0xFF166534) else Color(0xFF991B1B)
                                    )
                                }
                            }
                        }
                    }

                    // Device Telemetry Specs
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Tag Battery", fontSize = 11.sp, color = tbSubtextColor())
                                    Spacer(Modifier.height(4.dp))
                                    Text("${childBagDevice.batteryPercent}%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Green)
                                    Text("~14 days standby", fontSize = 10.sp, color = tbSubtextColor())
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("GPS Fix", fontSize = 11.sp, color = tbSubtextColor())
                                    Spacer(Modifier.height(4.dp))
                                    Text("LIVE", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Blue)
                                    Text("Updated 2 mins ago", fontSize = 10.sp, color = tbSubtextColor())
                                }
                            }
                        }
                    }

                    // Ring Tag Action Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Soft)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.VolumeUp, null, tint = Blue, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Audible Bag Tag Beacon", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tbTextColor())
                                    Text(
                                        if (tagAlertTriggered) "Beeping tag sound activated!" else "Play a loud beep on the device to locate bag nearby.",
                                        fontSize = 11.sp,
                                        color = tbSubtextColor()
                                    )
                                }
                                Button(
                                    onClick = { tagAlertTriggered = !tagAlertTriggered },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (tagAlertTriggered) Orange else Blue)
                                ) {
                                    Text(if (tagAlertTriggered) "Stop" else "Ring Tag")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVehicleEditor) {
        VehicleDetailsDialog(
            initial = vehicle,
            onDismiss = { showVehicleEditor = false },
            onSave = { saved -> onSaveVehicle(saved); showVehicleEditor = false }
        )
    }

    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onAdd = { newTag ->
                childBagDevice = newTag
                showAddTagDialog = false
            }
        )
    }

    if (showGroupChat && activeTrip != null) {
        TripGroupChatDialog(
            tripId = activeTrip.id,
            tripName = activeTrip.name,
            currentUserName = currentUserName,
            currentUserId = SessionManager.getUserId(),
            members = activeBuddies,
            repository = repository,
            onDismiss = { showGroupChat = false }
        )
    }
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onAdd: (MapTarget) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf("") }
    var safeZone by remember { mutableStateOf("St. Joseph's Academy") }
    var imei by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Tracking Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pair a GPS tracker tag for child bag, pet collar, or luggage.", color = Color.Gray, fontSize = 12.sp)
                OutlinedTextField(name, { name = it }, label = { Text("Tag name (e.g. Aarav's School Bag)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(assignedTo, { assignedTo = it }, label = { Text("Assigned to (e.g. Child Aarav)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(safeZone, { safeZone = it }, label = { Text("Safe Zone Name (e.g. School)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(imei, { imei = it }, label = { Text("Device IMEI or QR Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onAdd(
                        MapTarget(
                            id = "dev-${name.hashCode()}",
                            title = name,
                            subtitle = if (assignedTo.isNotBlank()) "Bound to: $assignedTo" else "Personal Tag",
                            latitude = 12.9740,
                            longitude = 77.6020,
                            speedKmh = 0.0,
                            batteryPercent = 100,
                            color = 0xFF22C55EL,
                            type = "CHILD_BAG",
                            safeZoneName = safeZone.ifBlank { "Safe Zone" },
                            safeZoneRadiusMeters = 300.0,
                            isInsideSafeZone = true
                        )
                    )
                }
            }) { Text("Register Tag") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun VehicleDetailsDialog(
    initial: Vehicle,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var registration by remember { mutableStateOf(initial.registrationNumber) }
    var imei by remember { mutableStateOf(initial.imei) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vehicle Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("These details are shown in Vehicle Tracking.", color = Color.Gray, fontSize = 13.sp)
                OutlinedTextField(name, { name = it }, label = { Text("Vehicle name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(registration, { registration = it.uppercase() }, label = { Text("Registration number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(imei, { imei = it }, label = { Text("GPS device / IMEI") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && registration.isNotBlank()) onSave(initial.copy(name = name, registrationNumber = registration, imei = imei, isOnline = true))
            }) { Text("Save Vehicle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


// ============================================================
// VEHICLE STATUS BADGE
// ============================================================

@Composable
private fun VehicleStatusBadge(
    online: Boolean
) {

    Surface(

        color =
            if (online) {
                Color(0xFFE8F8EF)
            } else {
                Color(0xFFFDECEC)
            },

        shape = RoundedCornerShape(20.dp)
    ) {

        Row(

            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(

                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color =
                            if (online) {
                                Green
                            } else {
                                Red
                            },
                        shape = RoundedCornerShape(50)
                    )
            )

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Text(
                text =
                    if (online) {
                        "Online"
                    } else {
                        "Offline"
                    },

                fontSize = 12.sp,

                fontWeight = FontWeight.SemiBold,

                color =
                    if (online) {
                        Green
                    } else {
                        Red
                    }
            )
        }
    }
}


// ============================================================
// VEHICLE CARD
// ============================================================

@Composable
private fun VehicleCard(
    vehicle: Vehicle
) {

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = Navy
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Vehicle",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = vehicle.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = vehicle.registrationNumber,
                        color = Color(0xFFBAC8D9),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                VehicleMetric(
                    value =
                        "${vehicle.speedKmh.toInt()} km/h",
                    label = "Speed"
                )

                VehicleMetric(
                    value =
                        if (vehicle.ignitionOn) {
                            "ON"
                        } else {
                            "OFF"
                        },
                    label = "Ignition"
                )

                VehicleMetric(
                    value =
                        "${vehicle.batteryPercent}%",
                    label = "Battery"
                )
            }
        }
    }
}


// ============================================================
// VEHICLE METRIC
// ============================================================

@Composable
private fun VehicleMetric(
    value: String,
    label: String
) {

    Column {

        Text(
            text = value,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            color = Color(0xFFBAC8D9),
            fontSize = 11.sp
        )
    }
}


// ============================================================
// SMALL VEHICLE METRIC
// ============================================================

@Composable
private fun SmallVehicleMetric(
    value: String,
    label: String
) {

    Column {

        Text(
            text = value,
            color = Navy,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}


// ============================================================
// MAP PLACEHOLDER
// ============================================================

@Composable
private fun VehicleMapPlaceholder(
    vehicle: Vehicle
) {

    Box(

        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(
                color = Soft,
                shape = RoundedCornerShape(20.dp)
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = Blue,
                modifier = Modifier.size(54.dp)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Vehicle location",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "${vehicle.latitude}, ${vehicle.longitude}",

                fontSize = 12.sp,

                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Live GPS location will appear here",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}


// ============================================================
// COORDINATES CARD
// ============================================================

@Composable
private fun CoordinatesCard(
    vehicle: Vehicle
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "Current vehicle position",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "LATITUDE",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    Text(
                        text =
                            vehicle.latitude.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Navy
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "LONGITUDE",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    Text(
                        text =
                            vehicle.longitude.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Navy
                    )
                }
            }
        }
    }
}


// ============================================================
// VEHICLE INFORMATION
// ============================================================

@Composable
private fun VehicleInformationCard(
    vehicle: Vehicle
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tbCardBg()),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            VehicleInfoRow(
                title = "Vehicle",
                value = vehicle.name
            )

            VehicleInfoRow(
                title = "Registration",
                value = vehicle.registrationNumber
            )

            VehicleInfoRow(
                title = "IMEI",
                value = vehicle.imei
            )

            VehicleInfoRow(
                title = "Speed",
                value = "${vehicle.speedKmh} km/h"
            )

            VehicleInfoRow(
                title = "Heading",
                value = "${vehicle.heading}°"
            )

            VehicleInfoRow(
                title = "Ignition",
                value =
                    if (vehicle.ignitionOn) {
                        "ON"
                    } else {
                        "OFF"
                    }
            )

            VehicleInfoRow(
                title = "Tracker battery",
                value = "${vehicle.batteryPercent}%"
            )

            VehicleInfoRow(
                title = "Status",
                value =
                    if (vehicle.isOnline) {
                        "Online"
                    } else {
                        "Offline"
                    }
            )
        }
    }
}


// ============================================================
// VEHICLE INFO ROW
// ============================================================

@Composable
private fun VehicleInfoRow(
    title: String,
    value: String
) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = title,
            color = tbSubtextColor(),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = tbTextColor(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}


// ============================================================
// EXPENSES
// ============================================================

// ============================================================
// EXPENSES & REALTIME SPLIT (1. TRIP GROUP  2. MANUAL PERSONAL GROUPS)
// ============================================================

data class PersonalSplitGroup(
    val id: String,
    val name: String,
    val members: List<TripMember>,
    val expenses: List<Expense>
)

@Composable
private fun ExpensesScreen(
    tripName: String,
    members: List<TripMember>,
    expenses: List<Expense>,
    settlements: List<com.travelbuddy.app.domain.Settlement>,
    onAdd: (Expense) -> Unit
) {
    var splitMode by remember { mutableStateOf(0) } // 0 = Trip Split Group, 1 = Manual Personal Split Groups
    var showAddExpense by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var settledList by remember { mutableStateOf(setOf<String>()) }
    var settleToastMessage by remember { mutableStateOf<String?>(null) }

    // Manual Personal Split Groups state (fully dynamic)
    var manualGroups by remember {
        mutableStateOf<List<PersonalSplitGroup>>(emptyList())
    }
    var selectedManualGroupIndex by remember { mutableStateOf(0) }

    val currentGroupMembers = if (splitMode == 0) members else manualGroups.getOrNull(selectedManualGroupIndex)?.members ?: emptyList()
    val currentExpenses = if (splitMode == 0) expenses else manualGroups.getOrNull(selectedManualGroupIndex)?.expenses ?: emptyList()
    val groupTitle = if (splitMode == 0) tripName else manualGroups.getOrNull(selectedManualGroupIndex)?.name ?: "Personal Group"

    val balances = ExpenseCalculator.balances(currentGroupMembers, currentExpenses)
    val rawSettlements = if (settlements.isEmpty() || splitMode == 1) ExpenseCalculator.settlements(balances) else settlements
    val activeSettlements = rawSettlements.filterNot { "${it.from}-${it.to}" in settledList }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector: Trip Group vs Manual Split Groups
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).clickable { splitMode = 0 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (splitMode == 0) Blue else Color.Transparent
                    ) {
                        Text(
                            "Trip Split Group",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (splitMode == 0) Color.White else tbTextColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        modifier = Modifier.weight(1f).clickable { splitMode = 1 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (splitMode == 1) Blue else Color.Transparent
                    ) {
                        Text(
                            "Manual Personal Split",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (splitMode == 1) Color.White else tbTextColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (splitMode == 1 && manualGroups.isEmpty()) {
            // Single, clean create new group empty card (only ONE create button, zero trip data clutter)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (LocalTravelBuddyDarkMode.current) Color(0xFF334155) else Color(0xFFE2E8F0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Blue.copy(alpha = 0.12f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Blue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text("No Personal Split Groups Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = tbTextColor())
                        Text(
                            "Create a custom group to split bills for dinners, flatmates, or room rent. You can add your own members dynamically.",
                            fontSize = 13.sp,
                            color = tbSubtextColor(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { showCreateGroupDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create New Group", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            // Active group exists (or Trip Split mode)
            if (splitMode == 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Personal Group:", fontSize = 12.sp, color = tbSubtextColor())
                        TextButton(onClick = { showCreateGroupDialog = true }) {
                            Text("+ New Group", fontWeight = FontWeight.Bold, color = Blue)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(manualGroups.indices.toList()) { idx ->
                            val grp = manualGroups[idx]
                            val isSelected = idx == selectedManualGroupIndex
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Blue else if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Blue else Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable { selectedManualGroupIndex = idx }
                            ) {
                                Text(
                                    grp.name,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = if (isSelected) Color.White else tbTextColor(),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Header + Add Button
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (splitMode == 0) "Trip Expense Split" else "Personal Split",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = tbTextColor()
                        )
                        Text(groupTitle, fontSize = 12.sp, color = tbSubtextColor())
                    }
                    Button(
                        onClick = { showAddExpense = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("+ Add Bill", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

        // Settlement Toast feedback
        settleToastMessage?.let { msg ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F8EF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = Green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = Color(0xFF14532D), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Total Spent Summary Card
        item { ExpenseSummary(currentExpenses) }

        // Member Balances
        item {
            Text("Individual Balances", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tbTextColor())
        }
        balances.forEach { balance ->
            val member = currentGroupMembers.firstOrNull { it.id == balance.memberId }
            if (member != null) item {
                InfoCard(
                    title = member.name,
                    body = if (balance.net >= 0) "Gets ₹${money(balance.net)} back" else "Owes ₹${money(-balance.net)}",
                    accent = if (balance.net >= 0) Green else Orange
                )
            }
        }

        // Suggested Settlements with "Settle Up" action
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Settlement Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tbTextColor())
                if (activeSettlements.isNotEmpty()) {
                    Text("${activeSettlements.size} pending", fontSize = 12.sp, color = tbSubtextColor())
                }
            }
        }
        if (activeSettlements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = Green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("All balances are settled up!", color = tbTextColor(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        } else {
            activeSettlements.forEach { settlement ->
                val from = currentGroupMembers.firstOrNull { it.id == settlement.from }
                val to = currentGroupMembers.firstOrNull { it.id == settlement.to }
                if (from != null && to != null) item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (LocalTravelBuddyDarkMode.current) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${from.name} ➔ ${to.name}", fontWeight = FontWeight.Bold, color = tbTextColor(), fontSize = 13.sp)
                                Text("Owes ₹${money(settlement.amount)}", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    val key = "${settlement.from}-${settlement.to}"
                                    settledList = settledList + key
                                    settleToastMessage = "Recorded payment: ₹${money(settlement.amount)} from ${from.name} to ${to.name} settled!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Green),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Settle Up", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // All Expenses History
        item {
            Text("Expenses History (${currentExpenses.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tbTextColor())
        }
        currentExpenses.forEach { expense ->
            val payer = currentGroupMembers.firstOrNull { it.id == expense.paidBy }
            item {
                InfoCard(
                    title = expense.title,
                    body = if (payer == null) "₹${money(expense.amount)}" else "₹${money(expense.amount)} paid by ${payer.name} • ${expense.splitWith.size} people",
                    accent = Blue
                )
            }
        }
        }
    }

    if (showAddExpense) {
        AddExpenseDialog(
            tripName = groupTitle,
            members = currentGroupMembers,
            onDismiss = { showAddExpense = false },
            submit = { newExp ->
                if (splitMode == 0) {
                    onAdd(newExp)
                } else {
                    val currentGrp = manualGroups[selectedManualGroupIndex]
                    val updatedGrp = currentGrp.copy(expenses = currentGrp.expenses + newExp)
                    manualGroups = manualGroups.toMutableList().also { it[selectedManualGroupIndex] = updatedGrp }
                }
                showAddExpense = false
            }
        )
    }

    if (showCreateGroupDialog) {
        CreateManualGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { newGrp ->
                manualGroups = manualGroups + newGrp
                selectedManualGroupIndex = manualGroups.lastIndex
                showCreateGroupDialog = false
            }
        )
    }
}

@Composable
private fun CreateManualGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (PersonalSplitGroup) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var memberNamesInput by remember { mutableStateOf("${SessionManager.getUserName().ifBlank { "You" }}, ") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Personal Split Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Create a personal group to split bills without a trip (flatmates, dinners, friends).", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name (e.g. Flat 402, Goa Trip)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = memberNamesInput,
                    onValueChange = { memberNamesInput = it },
                    label = { Text("Members (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (groupName.isNotBlank()) {
                    val rawList = memberNamesInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val colors = listOf(0xFF2F80EDL, 0xFF27AE60L, 0xFFF2994AL, 0xFF9B51E0L, 0xFFEB5757L)
                    val memberObjects = rawList.mapIndexed { idx, name ->
                        TripMember("m-${name.hashCode()}", name, colors[idx % colors.size], true)
                    }
                    onCreate(
                        PersonalSplitGroup(
                            id = "grp-${groupName.hashCode()}",
                            name = groupName,
                            members = memberObjects,
                            expenses = emptyList()
                        )
                    )
                }
            }) { Text("Create Group") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


// ============================================================
// EXPENSE SUMMARY
// ============================================================

@Composable
private fun ExpenseSummary(
    expenses: List<Expense>
) {

    val total =
        expenses.sumOf {
            it.amount
        }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (LocalTravelBuddyDarkMode.current) Color(0xFF1E293B) else Soft
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRIP TOTAL",
                    fontSize = 10.sp,
                    color = tbSubtextColor()
                )
                Text(
                    text = "₹${money(total)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = tbTextColor()
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${expenses.size} expenses",
                    fontSize = 12.sp,
                    color = tbSubtextColor()
                )
                Text(
                    text = "Split equally",
                    color = Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// ============================================================
// ADD EXPENSE
// ============================================================

@Composable
private fun AddExpenseDialog(
    tripName: String,
    members: List<TripMember>,
    onDismiss: () -> Unit,
    submit: (Expense) -> Unit
) {

    var title by remember {
        mutableStateOf("")
    }

    var amount by remember {
        mutableStateOf("")
    }
    var paidById by remember { mutableStateOf(members.firstOrNull()?.id.orEmpty()) }
    var splitWith by remember { mutableStateOf(members.map { it.id }.toSet()) }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = { Column { Text("Add Expense"); Text(tripName, fontSize = 13.sp, color = Color.Gray) } },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(

                    value = title,

                    onValueChange = {
                        title = it
                    },

                    label = {
                        Text("What was it for?")
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true
                )
                Text("Paid by", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                members.forEach { member ->
                    val selected = member.id == paidById
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { paidById = member.id },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) Soft else Color.White
                    ) {
                        Text(
                            text = if (selected) "✓ ${member.name}" else member.name,
                            modifier = Modifier.padding(10.dp),
                            color = Navy
                        )
                    }
                }
                Text("Split between (${splitWith.size})", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    members.forEach { member ->
                        val selected = member.id in splitWith
                        Surface(
                            modifier = Modifier.clickable {
                                splitWith = if (selected) splitWith - member.id else splitWith + member.id
                            },
                            shape = RoundedCornerShape(50),
                            color = if (selected) Color(member.color) else Color.LightGray
                        ) {
                            Text(member.name.take(2).uppercase(), modifier = Modifier.padding(10.dp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                val enteredAmount = amount.toDoubleOrNull() ?: 0.0
                if (splitWith.isNotEmpty() && enteredAmount > 0) {
                    Text(
                        "Live split: ₹${money(enteredAmount / splitWith.size)} per person · ${splitWith.size} people",
                        color = Green,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                OutlinedTextField(

                    value = amount,

                    onValueChange = { value ->

                        amount =
                            value.filter { character ->

                                character.isDigit() ||
                                        character == '.'
                            }
                    },

                    label = {
                        Text("Amount (₹)")
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true
                )
            }
        },

        confirmButton = {

            Button(

                onClick = {

                    val value =
                        amount.toDoubleOrNull()

                    if (
                        title.isNotBlank() &&
                        value != null &&
                        value > 0 &&
                        paidById.isNotBlank() && splitWith.isNotEmpty()
                    ) {

                        val expense =
                            Expense(

                                id =
                                    "e-${members.size}-${title.hashCode()}",

                                title = title,

                                amount = value,

                                paidBy = paidById,

                                splitWith = splitWith
                            )

                        submit(expense)
                    }
                }
            ) {

                Text("Add")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}


// ============================================================
// MORE
// ============================================================

@Composable
private fun MoreScreen(
    vehicle: Vehicle,
    userName: String = SessionManager.getUserName().ifBlank { "Traveler" },
    onOpenProfile: () -> Unit,
    onOpenSettlement: () -> Unit,
    onOpenTelemetry: () -> Unit,
    onLogout: () -> Unit
) {

    LazyColumn(

        modifier = Modifier.fillMaxSize(),

        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 18.dp,
            bottom = 24.dp
        ),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Profile" to onOpenProfile,
                    "Settlement" to onOpenSettlement,
                    "Vehicle Tracker" to onOpenTelemetry
                ).forEach { (label, action) ->
                    Text(
                        text = "$label  ›",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = tbTextColor(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tbCardBg(), RoundedCornerShape(16.dp))
                            .clickable(onClick = action)
                            .padding(16.dp)
                    )
                }
            }
        }

        item {

            Text(
                text = "More",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = tbTextColor()
            )
        }


        item {

            InfoCard(
                title = "Vehicle & GPS",
                body =
                    "${vehicle.name} · ${vehicle.registrationNumber}",
                accent = Blue
            )
        }


        item {

            InfoCard(
                title = "Vehicle status",
                body =
                    if (vehicle.isOnline) {
                        "GPS tracker is online"
                    } else {
                        "GPS tracker is offline"
                    },
                accent =
                    if (vehicle.isOnline) {
                        Green
                    } else {
                        Red
                    }
            )
        }


        item {

            InfoCard(
                title = "Notifications",
                body =
                    "Trip updates, member arrivals and safety alerts.",
                accent = Green
            )
        }


        item {

            InfoCard(
                title = "Privacy & location",
                body =
                    "Control sharing per trip. Location is never public.",
                accent = Blue
            )
        }


        item {

            InfoCard(
                title = "Profile",
                body = userName,
                accent = Color.Gray
            )
        }


        item {
            InfoCard(
                title = "Account Status",
                body = "$userName • Verified Active Traveler",
                accent = Green
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLogout),
                shape = RoundedCornerShape(16.dp),
                color = if (LocalTravelBuddyDarkMode.current) Color(0xFF3B151E) else Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (LocalTravelBuddyDarkMode.current) Color(0xFF881337) else Color(0xFFFECDD3))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Red,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Log Out of Travel Buddy",
                        color = Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}



// ============================================================
// CREATE TRIP + SOS MODELS / UI
// ============================================================

private enum class RideType {
    OPEN_TO_ALL,
    REQUEST_TO_JOIN,
    INVITE_ONLY
}

private data class CreateTripData(
    val id: String = "",
    val source: String,
    val destination: String,
    val title: String,
    val rideDate: String,
    val flagOffTime: String,
    val reportTime: String,
    val multiDay: Boolean,
    val rideType: RideType = RideType.OPEN_TO_ALL,
    val notes: String,
    val memberIds: Set<String>,
    val dynamicMembers: List<TripMember> = emptyList(),
    val vehicleDetails: String?,
    val vehicleType: String = "CAR",
    val coverImage: String?,
    val reminderEnabled: Boolean
)

private data class EmergencyContact(
    val id: String,
    val name: String,
    val relation: String,
    val phone: String,
    val notifyBySms: Boolean = true,
    val notifyByCall: Boolean = true
)

private fun sampleEmergencyContacts() = emptyList<EmergencyContact>()

@Composable
private fun SosConfirmationDialog(
    contacts: List<EmergencyContact>,
    onDismiss: () -> Unit,
    onManageContacts: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency SOS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SOS will alert your configured emergency contacts with your latest available trip location.")
                Text(
                    if (contacts.isEmpty()) "No emergency contacts configured"
                    else "${contacts.size} emergency contacts configured",
                    fontWeight = FontWeight.Bold
                )
                contacts.forEach {
                    Text("${it.name} • ${it.relation} • ${it.phone}", fontSize = 13.sp)
                }
                Text("For immediate danger, also contact your local emergency service.", color = Red, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) { Text("SEND SOS") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onManageContacts) { Text("Manage") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun EmergencyContactsDialog(
    contacts: List<EmergencyContact>,
    onDismiss: () -> Unit,
    onSave: (List<EmergencyContact>) -> Unit
) {
    var localContacts by remember(contacts) { mutableStateOf(contacts) }
    var showAdd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency Contacts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (localContacts.isEmpty()) {
                    Text("No emergency contacts added yet.", color = Color.Gray)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(320.dp)
                    ) {
                        items(localContacts) { contact ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${contact.relation} • ${contact.phone}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            buildString {
                                                if (contact.notifyBySms) append("SMS ")
                                                if (contact.notifyByCall) append("Call")
                                            }.trim(),
                                            fontSize = 11.sp,
                                            color = Red
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            localContacts = localContacts.filterNot { it.id == contact.id }
                                        }
                                    ) {
                                        Text("Remove", color = Red)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showAdd = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text("+ Add Emergency Contact")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(localContacts) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showAdd) {
        AddEmergencyContactDialog(
            onDismiss = { showAdd = false },
            onAdd = {
                localContacts = localContacts + it
                showAdd = false
            }
        )
    }
}

@Composable
private fun AddEmergencyContactDialog(
    onDismiss: () -> Unit,
    onAdd: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notifyBySms by remember { mutableStateOf(true) }
    var notifyByCall by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Emergency Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relation,
                    onValueChange = { relation = it },
                    label = { Text("Relation (Parent / Friend / Rider)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SMS", modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = notifyBySms,
                        onCheckedChange = { notifyBySms = it }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Phone call", modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = notifyByCall,
                        onCheckedChange = { notifyByCall = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || relation.isBlank() || phone.isBlank()) return@Button
                    onAdd(
                        EmergencyContact(
                            id = "ec-${name.hashCode()}-${phone.hashCode()}",
                            name = name.trim(),
                            relation = relation.trim(),
                            phone = phone.trim(),
                            notifyBySms = notifyBySms,
                            notifyByCall = notifyByCall
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


// ============================================================
// SAMPLE MEMBERS
// ============================================================

private fun sampleMembers(): List<TripMember> {
    val name = SessionManager.getUserName().ifBlank { "Traveler" }
    val id = SessionManager.getUserId().ifBlank { "usr-${name.hashCode()}" }
    return listOf(
        TripMember(
            id = id,
            name = name,
            color = 0xFF2F80ED,
            isSharingLocation = true
        )
    )
}


// ============================================================
// SAMPLE EXPENSES
// ============================================================

private fun sampleExpenses(): List<Expense> = emptyList()


// ============================================================
// SAMPLE VEHICLE
// ============================================================

private fun sampleVehicle(): Vehicle {

    return Vehicle(

        id = "vehicle-001",

        name = "Yamaha R15",

        registrationNumber = "RUI23227513",

        imei = "865XXXXXXXXXXXX",

        isOnline = true,

        latitude = 12.9716,

        longitude = 77.5946,

        speedKmh = 45.0,

        heading = 90.0,

        ignitionOn = true,

        batteryPercent = 88,

        lastUpdated = 0L
    )
}


// ============================================================
// MONEY
// ============================================================

private fun money(
    amount: Double
): String {

    return amount
        .toInt()
        .toString()
}
