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
    var tempEditValue by remember { mutableStateOf("") }
    var editingField by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val primaryText = if (darkMode) Color(0xFFEAF0F8) else Tb.Navy
    val secondaryText = if (darkMode) Color(0xFFAEB9C8) else Tb.Gray
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .background(if (darkMode) Color(0xFF10151C) else Tb.Page)
            .verticalScroll(scrollState)
            .padding(bottom = 120.dp)
    ) {
        // Compact modern header bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (darkMode) Color(0xFF151C26) else Color(0xFF0F3E4D),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "My Profile",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            }
        }

        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(localName, Color(0xFFBC4E23), 64)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(localName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryText)
                    Spacer(Modifier.height(2.dp))
                    Text(phone, fontSize = 13.sp, color = secondaryText)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Tb.Green.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Verified Traveler",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Tb.Green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column {
                    ProfileRow("Name", localName) {
                        tempEditValue = localName
                        editingField = "name"
                    }
                    ProfileRow("Email", localEmail) {
                        tempEditValue = localEmail
                        editingField = "email"
                    }
                    ProfileRow("Manage Vehicles", "1 vehicle", onOpenVehicle)
                    ProfileRow("Trip History", "$tripCount trips", null)
                    ProfileRow("Expense History", "View settlements", onOpenSettlement)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleDarkMode)
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark mode", Modifier.weight(1f), color = primaryText)
                        Switch(checked = darkMode, onCheckedChange = { onToggleDarkMode() })
                    }
                    ProfileRow("Settings", "", null)
                    ProfileRow("Help & Support", "", null)
                }
            }

            Spacer(Modifier.height(4.dp))

            // Prominent, Well-Assigned Logout Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { showLogoutConfirm = true }),
                shape = RoundedCornerShape(16.dp),
                color = if (darkMode) Color(0xFF3B151E) else Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (darkMode) Color(0xFF881337) else Color(0xFFFECDD3)
                ),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Tb.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Log Out of Travel Buddy",
                        color = Tb.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TravelBuddyLogo(size = 20.dp)
                Spacer(Modifier.size(8.dp))
                Text("Travel Buddy v0.1.0 • Ready for Play Store", color = secondaryText, fontSize = 12.sp)
            }
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
            onDismissRequest = { editingField = null },
            title = { Text("Edit ${if (field == "name") "Name" else "Email"}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempEditValue,
                        onValueChange = { tempEditValue = it },
                        label = { Text(if (field == "name") "Full Name" else "Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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
                    if (clean.isNotBlank()) {
                        if (field == "name") {
                            localName = clean
                            onNameChange(clean)
                            SessionManager.setUserName(clean)
                        } else {
                            localEmail = clean
                            SessionManager.setUserEmail(clean)
                        }
                    }
                    editingField = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingField = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String, onClick: (() -> Unit)?) {
    val dark = LocalTravelBuddyDarkMode.current
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), color = if (dark) Color(0xFFEAF0F8) else Tb.Navy)
        if (value.isNotEmpty()) Text(value, color = if (dark) Color(0xFFAEB9C8) else Tb.Gray, fontSize = 13.sp)
        if (onClick != null) Text("  ›", color = if (dark) Color(0xFFAEB9C8) else Tb.Gray)
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
    tripName: String = "Coorg Ride",
    tripId: String = "coorg-ride",
    userName: String = SessionManager.getUserName().ifBlank { "You" },
    repository: com.travelbuddy.app.data.TravelRepository? = null
) {
    val dark = LocalTravelBuddyDarkMode.current
    val primaryText = if (dark) Color(0xFFF8FAFC) else Tb.Navy
    val secondaryText = if (dark) Color(0xFF94A3B8) else Tb.Gray
    val cardBackground = if (dark) Color(0xFF1E293B) else Color.White
    val receivedBubble = if (dark) Color(0xFF273548) else Color(0xFFF1F5F9)
    val sentBubble = Tb.Blue

    var messageText by remember { mutableStateOf("") }
    var messages by remember(userName) {
        mutableStateOf(
            listOf(
                LocalChatMessage("1", "Netra", "Reached the coffee estate entrance! Weather is amazing ☕🌲", "10:14 AM", false),
                LocalChatMessage("2", "Karuna", "Taking fuel stop at Shell Highway, on my way in 10 mins ⛽", "10:18 AM", false),
                LocalChatMessage("3", userName, "Just crossed the toll. Vehicle tracking shows 12 km left! 🚗", "10:22 AM", true),
                LocalChatMessage("4", "Suresh", "Group ETA is 11:30 AM. Tag tracking is active for bags.", "10:24 AM", false)
            )
        )
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(tripId, repository, userName) {
        if (repository != null) {
            repository.getChatMessages(tripId).onSuccess { remoteList ->
                if (remoteList.isNotEmpty()) {
                    messages = remoteList.map { r ->
                        LocalChatMessage(
                            id = r.id,
                            sender = r.senderName,
                            text = r.message,
                            time = "Live",
                            isMe = r.senderId == "usr-1" || r.senderName.equals(userName, ignoreCase = true)
                        )
                    }
                }
            }
        }
    }

    val onSendMessage: (String) -> Unit = { text ->
        val clean = text.trim()
        if (clean.isNotBlank()) {
            val newMsg = LocalChatMessage(
                id = (messages.size + 1).toString(),
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
                            senderId = "usr-1",
                            senderName = userName,
                            message = clean
                        )
                    )
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

    Column(Modifier.fillMaxSize()) {
        // Chat Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (dark) Color(0xFF151C26) else Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(tripName, Tb.Green, 42)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tripName, fontWeight = FontWeight.Bold, color = primaryText, fontSize = 17.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Tb.Green))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${members.count { it.isSharingLocation }}/${members.size} members live • Active Group Chat",
                            color = secondaryText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages.size) { index ->
                val msg = messages[index]
                val isMe = msg.isMe

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    if (!isMe) {
                        Avatar(msg.sender, Tb.Blue, 32)
                        Spacer(Modifier.width(8.dp))
                    }

                    Column(
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                        modifier = Modifier.fillMaxWidth(0.82f)
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
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    msg.text,
                                    color = if (isMe) Color.White else primaryText,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    msg.time,
                                    color = if (isMe) Color.White.copy(alpha = 0.75f) else secondaryText,
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.End)
                                )
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

        // Message input bar
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            color = if (dark) Color(0xFF151C26) else Color.White,
            shadowElevation = 3.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message trip members...", color = secondaryText, fontSize = 14.sp) },
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
                    modifier = Modifier.size(46.dp),
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
