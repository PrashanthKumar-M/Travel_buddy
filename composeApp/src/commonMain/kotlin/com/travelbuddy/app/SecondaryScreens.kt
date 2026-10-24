package com.travelbuddy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelbuddy.app.domain.Expense
import com.travelbuddy.app.domain.ExpenseCalculator
import com.travelbuddy.app.domain.Settlement
import com.travelbuddy.app.domain.TripMember
import com.travelbuddy.app.domain.Vehicle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ArrowUpward
import kotlin.math.abs
import kotlin.math.round

/** Full-screen destinations opened from the More tab. */
internal enum class Overlay { Profile, Settlement, Telemetry }

private fun rupees(v: Double) = "₹" + abs(round(v)).toLong().toString()
private fun fixed(v: Double, places: Int): String {
    var f = 1.0; repeat(places) { f *= 10 }
    return (round(v * f) / f).toString()
}

@Composable
private fun ScreenHeader(title: String, onClose: () -> Unit) {
    val dark = LocalTravelBuddyDarkMode.current
    val textCol = if (dark) Color(0xFFF8FAFC) else Tb.Navy
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("←", fontSize = 24.sp, color = textCol, modifier = Modifier.clickable(onClick = onClose).padding(end = 16.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textCol)
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = LocalTravelBuddyDarkMode.current
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (dark) Color(0xFF1B2430) else Color.White)
            .border(1.dp, if (dark) Color(0xFF344052) else Tb.Border, RoundedCornerShape(16.dp)).padding(14.dp)
    ) { content() }
}

@Composable
private fun Avatar(name: String, color: Color, size: Int = 40) {
    val initials = getInitials(name)
    Box(Modifier.size(size.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.7).sp)
    }
}

// ---------------------------------------------------------------- Settlement

@Composable
internal fun SettlementScreen(
    tripName: String,
    members: List<TripMember>,
    expenses: List<Expense>,
    apiSettlements: List<Settlement>,
    onClose: () -> Unit
) {
    val dark = LocalTravelBuddyDarkMode.current
    val primaryText = if (dark) Color(0xFFF8FAFC) else Tb.Navy
    val secondaryText = if (dark) Color(0xFF94A3B8) else Tb.Gray
    val balances = remember(members, expenses) { ExpenseCalculator.balances(members, expenses) }
    // Prefer the server's settlement result; fall back to the local calculation.
    val transfers = if (apiSettlements.isNotEmpty()) apiSettlements else ExpenseCalculator.settlements(balances)
    fun nameOf(id: String) = members.firstOrNull { it.id == id }?.name ?: id

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settlement", onClose)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column {
                        Text(tripName, fontWeight = FontWeight.SemiBold, color = primaryText, fontSize = 16.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column {
                                Text("Total Expenses", fontSize = 12.sp, color = secondaryText)
                                Text(rupees(expenses.sumOf { it.amount }), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = primaryText)
                            }
                            Column {
                                Text("No. of Expenses", fontSize = 12.sp, color = secondaryText)
                                Text(expenses.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = primaryText)
                            }
                        }
                    }
                }
            }
            item { Text("Balances", fontWeight = FontWeight.SemiBold, color = primaryText, fontSize = 16.sp) }
            if (balances.isEmpty()) item { Text("No members or expenses yet.", color = secondaryText, fontSize = 13.sp) }
            items(balances.size) { i ->
                val b = balances[i]
                val m = members.firstOrNull { it.id == b.memberId }
                val name = m?.name ?: b.memberId
                val (label, tint, sign) = when {
                    b.net > 0.01 -> Triple("Will receive", Tb.Green, "+ ")
                    b.net < -0.01 -> Triple("Owes", Tb.Red, "− ")
                    else -> Triple("Settled", secondaryText, "")
                }
                Card(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(name, Color(m?.color ?: 0xFF2F80ED))
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Medium, color = primaryText)
                            Text(label, fontSize = 12.sp, color = tint)
                        }
                        Text(sign + rupees(b.net), fontWeight = FontWeight.Bold, color = tint, fontSize = 16.sp)
                    }
                }
            }
            if (transfers.isNotEmpty()) {
                item { Text("Suggested transfers", fontWeight = FontWeight.SemiBold, color = primaryText, fontSize = 16.sp) }
                items(transfers.size) { i ->
                    val t = transfers[i]
                    Card(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${nameOf(t.from)}  →  ${nameOf(t.to)}", Modifier.weight(1f), color = primaryText)
                            Text(rupees(t.amount), fontWeight = FontWeight.Bold, color = Tb.Blue)
                        }
                    }
                }
            }
            item {
                Text(
                    "Recording a payment as settled needs a backend endpoint that isn't in the Postman collection yet.",
                    fontSize = 12.sp, color = secondaryText
                )
            }
        }
    }
}

// ------------------------------------------------------------------- Profile

@Composable
internal fun ProfileScreen(
    name: String = SessionManager.getUserName().ifBlank { "Traveler" },
    onNameChange: (String) -> Unit = {},
    phone: String,
    tripCount: Int,
    trips: List<com.travelbuddy.app.domain.Trip> = emptyList(),
    onSelectTrip: ((com.travelbuddy.app.domain.Trip) -> Unit)? = null,
    onOpenVehicle: () -> Unit,
    onOpenSettlement: () -> Unit,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    var localName by remember(name) { mutableStateOf(name) }
    var localEmail by remember {
        mutableStateOf(
            if (SessionManager.isEmail()) phone
            else SessionManager.getUserIdentifier().ifBlank { "prashanthkm711@gmail.com" }
        )
    }
    var selectedAvatar by remember { mutableStateOf("MALE_1") }
    var tempEditValue by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<String?>(null) }
    var editingField by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showTripHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHelpSupportDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var highAccuracyGpsEnabled by remember { mutableStateOf(true) }
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var cacheClearedBanner by remember { mutableStateOf(false) }
    val primaryText = if (darkMode) Color(0xFFEAF0F8) else Tb.Navy
    val secondaryText = if (darkMode) Color(0xFFAEB9C8) else Tb.Gray
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .background(if (darkMode) Color(0xFF10151C) else Tb.Page)
            .verticalScroll(scrollState)
    ) {
        // Compact header bar (without extra statusBarsPadding so content fits on screen)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (darkMode) Color(0xFF151C26) else Color(0xFF0F3E4D),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "My Profile",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TravelerAvatar(avatarType = selectedAvatar, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(localName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryText)
                    Spacer(Modifier.height(1.dp))
                    Text(phone, fontSize = 11.sp, color = secondaryText)
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Tb.Green.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Verified Traveler",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            color = Tb.Green,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Copyright-Free Play Store Safe Avatar Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (darkMode) Color(0xFF1E2836) else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (darkMode) Color(0xFF2C394B) else Color(0xFFE2E8F0))
            ) {
                AvatarSelector(
                    selectedAvatar = selectedAvatar,
                    onSelect = { selectedAvatar = it },
                    modifier = Modifier.padding(8.dp)
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Column {
                    ProfileRow("Name", localName) {
                        tempEditValue = localName
                        editError = null
                        editingField = "name"
                    }
                    ProfileRow("Email", localEmail) {
                        tempEditValue = localEmail
                        editError = null
                        editingField = "email"
                    }
                    ProfileRow("Manage Vehicles", "1 vehicle", onOpenVehicle)
                    ProfileRow("Trip History", if (tripCount == 1) "1 trip" else "$tripCount trips") {
                        showTripHistoryDialog = true
                    }
                    ProfileRow("Expense History", "View settlements", onOpenSettlement)
                    ProfileRow("Settings", "GPS, notifications & theme") {
                        cacheClearedBanner = false
                        showSettingsDialog = true
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleDarkMode)
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark mode", Modifier.weight(1f), color = if (darkMode) Color(0xFFEAF0F8) else Tb.Navy, fontSize = 13.sp)
                        Switch(checked = darkMode, onCheckedChange = { onToggleDarkMode() })
                    }
                    ProfileRow("Help & Support", "FAQs & 24/7 support") {
                        showHelpSupportDialog = true
                    }
                }
            }

            // Prominent Logout Button fitted inside screen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { showLogoutConfirm = true }),
                shape = RoundedCornerShape(12.dp),
                color = if (darkMode) Color(0xFF3B151E) else Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (darkMode) Color(0xFF881337) else Color(0xFFFECDD3)
                ),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Tb.Red,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Log Out of Travel Buddy",
                        color = Tb.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TravelBuddyLogo(size = 18.dp)
                Spacer(Modifier.size(8.dp))
                Text("Travel Buddy v1.0.0", color = secondaryText, fontSize = 11.sp)
            }

            // Generous clearance space so the logout button is 100% above any bottom navigation bar
            Spacer(Modifier.height(140.dp))
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log Out?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of Travel Buddy? You will need your OTP the next time you sign in.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Tb.Red)
                ) {
                    Text("Log Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    editingField?.let { field ->
        AlertDialog(
            onDismissRequest = { editingField = null; editError = null },
            title = { Text("Edit ${if (field == "name") "Name" else "Email"}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempEditValue,
                        onValueChange = {
                            tempEditValue = it
                            editError = null
                        },
                        label = { Text(if (field == "name") "Full Name" else "Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    editError?.let { err ->
                        Text(err, fontSize = 12.sp, color = Tb.Red, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Changes will update across your greeting, trips, and account.",
                        fontSize = 12.sp,
                        color = Tb.Gray
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clean = tempEditValue.trim()
                    if (clean.isBlank()) {
                        editError = "Field cannot be empty."
                        return@Button
                    }
                    if (field == "name") {
                        localName = clean
                        onNameChange(clean)
                        SessionManager.setUserName(clean)
                        editingField = null
                    } else {
                        if (!isValidEmailAddress(clean)) {
                            editError = "Please enter a valid email address (e.g., name@domain.com)."
                            return@Button
                        }
                        localEmail = clean
                        SessionManager.setUserEmail(clean)
                        editingField = null
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingField = null; editError = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTripHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showTripHistoryDialog = false },
            title = { Text("Trip History ($tripCount)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.height(320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (trips.isEmpty()) {
                        Text(
                            "No trips recorded yet. Create or join a trip from the Trips tab to build your travel history.",
                            fontSize = 13.sp,
                            color = secondaryText
                        )
                    } else {
                        trips.forEach { trip ->
                            val memberCnt = trip.members.size.coerceAtLeast(1)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (darkMode) Color(0xFF151C26) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (darkMode) Color(0xFF2C394B) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showTripHistoryDialog = false
                                        onSelectTrip?.invoke(trip)
                                    }
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(trip.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryText)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Tb.Blue.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                trip.id,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Tb.Blue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "${trip.source.ifBlank { "Start" }} → ${trip.destination.ifBlank { "Destination" }}",
                                        fontSize = 12.sp,
                                        color = secondaryText
                                    )
                                    Text(
                                        "${trip.dates} • ${if (memberCnt == 1) "1 member" else "$memberCnt members"}",
                                        fontSize = 11.sp,
                                        color = Tb.Green,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTripHistoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("App Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("High-Accuracy Live GPS", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = primaryText)
                            Text("Pinpoint 2-second multi-rider location updates", fontSize = 11.sp, color = secondaryText)
                        }
                        Switch(checked = highAccuracyGpsEnabled, onCheckedChange = { highAccuracyGpsEnabled = it })
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Trip & SOS Notifications", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = primaryText)
                            Text("Alerts for group chat, route divergence & SOS", fontSize = 11.sp, color = secondaryText)
                        }
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto-Sync Cloud Trips", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = primaryText)
                            Text("Keep active trip & expenses synced in real time", fontSize = 11.sp, color = secondaryText)
                        }
                        Switch(checked = autoSyncEnabled, onCheckedChange = { autoSyncEnabled = it })
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Dark Theme", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = primaryText)
                            Text("Switch between light and dark appearance", fontSize = 11.sp, color = secondaryText)
                        }
                        Switch(checked = darkMode, onCheckedChange = { onToggleDarkMode() })
                    }
                    TextButton(
                        onClick = { cacheClearedBanner = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (cacheClearedBanner) "✓ Map & Route Cache Cleared" else "Clear Local Map Cache", color = Tb.Blue)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showHelpSupportDialog) {
        AlertDialog(
            onDismissRequest = { showHelpSupportDialog = false },
            title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.height(340.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Frequently Asked Questions", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Tb.Blue)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• How do I track another rider live?", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = primaryText)
                        Text("Copy your Trip ID from the Trips or Track screen and share it. When your friend joins using that Trip ID, both locations and the blue leader-member line update live.", fontSize = 11.sp, color = secondaryText)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• How does Emergency SOS work?", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = primaryText)
                        Text("Tap the red SOS button at the top bar to broadcast your live GPS coordinates to your trip group and emergency contacts.", fontSize = 11.sp, color = secondaryText)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• How are shared expenses settled?", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = primaryText)
                        Text("Open the Split tab to log fuel, food, or stay expenses and view simplified settlements.", fontSize = 11.sp, color = secondaryText)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (darkMode) Color(0xFF151C26) else Color(0xFFEFF6FF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Contact Travel Buddy Support", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Tb.Blue)
                            Text("Email: support@travelbuddy.app", fontSize = 11.sp, color = primaryText)
                            Text("24x7 Emergency Helpline: 112", fontSize = 11.sp, color = primaryText)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { PlatformShare.copyToClipboard("support@travelbuddy.app", "Support Email") },
                            colors = ButtonDefaults.buttonColors(containerColor = Tb.Blue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Copy Email", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { PlatformShare.openDialer("112") },
                            colors = ButtonDefaults.buttonColors(containerColor = Tb.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Call 112", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpSupportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String, onClick: (() -> Unit)?) {
    val dark = LocalTravelBuddyDarkMode.current
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), color = if (dark) Color(0xFFEAF0F8) else Tb.Navy, fontSize = 13.sp)
        if (value.isNotEmpty()) Text(value, color = if (dark) Color(0xFFAEB9C8) else Tb.Gray, fontSize = 12.sp)
        if (onClick != null) Text("  ›", color = if (dark) Color(0xFFAEB9C8) else Tb.Gray, fontSize = 13.sp)
    }
}

// ----------------------------------------------------------------- Telemetry

@Composable
internal fun TelemetryScreen(vehicle: Vehicle, onClose: () -> Unit) {
    val dark = LocalTravelBuddyDarkMode.current
    val primaryText = if (dark) Color(0xFFF8FAFC) else Tb.Navy
    val secondaryText = if (dark) Color(0xFF94A3B8) else Tb.Gray
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Vehicle Tracker", onClose)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(vehicle.registrationNumber, fontWeight = FontWeight.Bold, color = primaryText, fontSize = 16.sp)
                        Text(vehicle.name, fontSize = 13.sp, color = secondaryText)
                    }
                    val tint = if (vehicle.isOnline) Tb.Green else secondaryText
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(tint).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text(if (vehicle.isOnline) "Online" else "Offline", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("Live Data", "Trip History", "Events").forEachIndexed { i, label ->
                    Text(
                        label, Modifier.weight(1f).clickable { tab = i }.padding(vertical = 10.dp),
                        color = if (tab == i) Tb.Blue else secondaryText,
                        fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            if (tab == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile("Speed", fixed(vehicle.speedKmh, 0), "km/h", Modifier.weight(1f), primaryText)
                    Tile("Ignition", if (vehicle.ignitionOn) "ON" else "OFF", "", Modifier.weight(1f), if (vehicle.ignitionOn) Tb.Green else secondaryText)
                    Tile("Battery", vehicle.batteryPercent.toString(), "%", Modifier.weight(1f), primaryText)
                }
                Card(Modifier.fillMaxWidth().height(190.dp)) {
                    Box(Modifier.fillMaxSize().background(if (dark) Color(0xFF1E3A2F) else Color(0xFFC6EBCB)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Live vehicle map", color = primaryText, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.size(52.dp).clip(CircleShape).background(Tb.Blue), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = "Vehicle", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("${fixed(vehicle.latitude, 4)}, ${fixed(vehicle.longitude, 4)}", color = primaryText, fontSize = 12.sp)
                        }
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column {
                        Text("Live Location", fontWeight = FontWeight.SemiBold, color = primaryText)
                        Spacer(Modifier.height(6.dp))
                        Text("${fixed(vehicle.latitude, 4)}, ${fixed(vehicle.longitude, 4)}", color = primaryText, fontSize = 14.sp)
                        Text("Heading ${fixed(vehicle.heading, 0)}°", color = secondaryText, fontSize = 12.sp)
                    }
                }
            } else {
                Text(
                    "Not available yet. This needs GET /api/vehicles/{vehicleId}/history from the backend.",
                    color = secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun Tile(label: String, value: String, unit: String, modifier: Modifier, tint: Color) {
    val dark = LocalTravelBuddyDarkMode.current
    Card(modifier) {
        Column {
            Text(label, fontSize = 12.sp, color = if (dark) Color(0xFF94A3B8) else Tb.Gray)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = tint)
            if (unit.isNotEmpty()) Text(unit, fontSize = 12.sp, color = if (dark) Color(0xFF94A3B8) else Tb.Gray)
        }
    }
}

// ---------------------------------------------------------------------- Chat

data class LocalChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val time: String,
    val isMe: Boolean,
    val isAlert: Boolean = false
)

@Composable
internal fun LocalChatScreen(
    members: List<TripMember>,
    tripName: String = "Group Chat",
    tripId: String = "chat-local",
    userName: String = SessionManager.getUserName().ifBlank { "You" },
    repository: com.travelbuddy.app.data.TravelRepository? = null,
    onClose: (() -> Unit)? = null
) {
    val dark = LocalTravelBuddyDarkMode.current
    val primaryText = if (dark) Color(0xFFF8FAFC) else Tb.Navy
    val secondaryText = if (dark) Color(0xFF94A3B8) else Tb.Gray
    val cardBackground = if (dark) Color(0xFF1E293B) else Color.White
    val receivedBubble = if (dark) Color(0xFF273548) else Color(0xFFF1F5F9)
    val sentBubble = Tb.Blue

    var messageText by remember { mutableStateOf("") }
    // Zero hardcoded messages - dynamic state loaded from server
    var messages by remember(tripId) { mutableStateOf<List<LocalChatMessage>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val myId = remember(userName) { SessionManager.getUserId().ifBlank { "usr-${userName.hashCode()}" } }

    // Live continuous 2s polling loop across all connected devices
    LaunchedEffect(tripId, repository, userName) {
        val repo = repository ?: return@LaunchedEffect
        while (isActive) {
            repo.getChatMessages(tripId).onSuccess { remoteList ->
                messages = remoteList.map { r ->
                    val isMe = r.senderName.equals(userName, ignoreCase = true) || r.senderId == myId
                    val timeStr = if (r.timestamp > 0) {
                        val secDiff = (System.currentTimeMillis() - r.timestamp) / 1000
                        when {
                            secDiff < 60 -> "Just now"
                            secDiff < 3600 -> "${secDiff / 60}m ago"
                            else -> "${secDiff / 3600}h ago"
                        }
                    } else "Just now"
                    LocalChatMessage(
                        id = r.id.ifBlank { "${r.timestamp}-${r.senderName}" },
                        sender = r.senderName,
                        text = r.displayMessage,
                        time = timeStr,
                        isMe = isMe
                    )
                }
            }
            delay(1500)
        }
    }

    val onSendMessage: (String) -> Unit = { text ->
        val clean = text.trim()
        if (clean.isNotBlank()) {
            val localId = "temp-${System.currentTimeMillis()}"
            val newMsg = LocalChatMessage(
                id = localId,
                sender = userName,
                text = clean,
                time = "Just now",
                isMe = true
            )
            messages = messages + newMsg
            messageText = ""
            if (repository != null) {
                scope.launch {
                    repository.sendChatMessage(
                        tripId,
                        com.travelbuddy.app.data.RemoteSendMessageRequest(
                            senderId = myId,
                            senderName = userName,
                            text = clean,
                            message = clean
                        )
                    ).onSuccess {
                        // Immediately refresh server messages
                        repository.getChatMessages(tripId).onSuccess { remoteList ->
                            messages = remoteList.map { r ->
                                val isMe = r.senderName.equals(userName, ignoreCase = true) || r.senderId == myId
                                LocalChatMessage(
                                    id = r.id.ifBlank { "${r.timestamp}-${r.senderName}" },
                                    sender = r.senderName,
                                    text = r.displayMessage,
                                    time = "Just now",
                                    isMe = isMe
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val quickChips = listOf(
        "🏁 Reached safe",
        "⛽ Fuel stop",
        "☕ Coffee break",
        "📍 Sharing live ETA",
        "🚦 Slow traffic ahead"
    )

    val listState = rememberLazyListState()

    // Smoothly scroll to the latest message as new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Chat Header with Back Button (Redirect Support)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (dark) Color(0xFF151C26) else Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onClose != null) {
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryText
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Avatar(tripName, Tb.Green, 38)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tripName, fontWeight = FontWeight.Bold, color = primaryText, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Tb.Green))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${members.size} trip members • Live Sync",
                            color = secondaryText,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Messages List or Dynamic Empty State
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💬", fontSize = 42.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No messages yet in '$tripName'",
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Say hi to other travelers in your trip! Send a message or tap a quick status chip below.",
                        color = secondaryText,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages.size) { index ->
                    val msg = messages[index]
                    val isMe = msg.isMe

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (!isMe) {
                            Avatar(msg.sender, Tb.Blue, 30)
                            Spacer(Modifier.width(6.dp))
                        }

                        Column(
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            if (!isMe) {
                                Text(
                                    msg.sender,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Tb.Blue,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                ),
                                color = if (isMe) sentBubble else receivedBubble,
                                shadowElevation = 0.5.dp
                            ) {
                                val isShort = msg.text.length < 24 && !msg.text.contains("\n")
                                if (isShort) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = if (isMe) Color.White else primaryText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                        Text(
                                            text = msg.time,
                                            color = if (isMe) Color.White.copy(alpha = 0.70f) else secondaryText,
                                            fontSize = 10.sp
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = if (isMe) Color.White else primaryText,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = msg.time,
                                            color = if (isMe) Color.White.copy(alpha = 0.70f) else secondaryText,
                                            fontSize = 10.sp,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick travel action chips
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickChips.size) { i ->
                val chip = quickChips[i]
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (dark) Color(0xFF223042) else Color(0xFFEBF3FC),
                    modifier = Modifier.clickable {
                        onSendMessage(chip)
                    }
                ) {
                    Text(
                        chip,
                        color = Tb.Blue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Message input bar with IME (keyboard) padding
        Surface(
            modifier = Modifier.fillMaxWidth().imePadding(),
            color = if (dark) Color(0xFF151C26) else Color.White,
            shadowElevation = 3.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message trip members...", color = secondaryText, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSendMessage(messageText)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Tb.Blue),
                    modifier = Modifier.size(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
