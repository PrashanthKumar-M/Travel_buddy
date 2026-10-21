package com.travelbuddy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelbuddy.app.domain.TripMember
import com.travelbuddy.app.domain.Vehicle
import kotlinx.coroutines.launch

data class QuickRideData(
    val id: String = "",
    val source: String,
    val stops: List<String> = emptyList(),
    val destination: String,
    val title: String,
    val vehicleType: String,
    val vehicleDetails: String,
    val instructions: String,
    val date: String = "Today",
    val members: List<TripMember> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRideScreen(
    currentUserName: String,
    currentVehicle: Vehicle,
    onNavigateToProfile: () -> Unit,
    onBack: () -> Unit,
    onStartRide: (QuickRideData) -> Unit
) {
    val scope = rememberCoroutineScope()
    val darkMode = LocalTravelBuddyDarkMode.current

    // Form states matching user's attached Image 1 & Image 2
    var selectedVehicleType by remember { mutableStateOf("Bike") }
    var selectedVehicleLabel by remember {
        mutableStateOf(
            if (currentVehicle.name.isNotBlank()) "${currentVehicle.name} (${currentVehicle.registrationNumber})"
            else "Select Vehicle"
        )
    }
    var showVehicleDropdown by remember { mutableStateOf(false) }

    var startPoint by remember { mutableStateOf("134, Bengaluru, Karnataka, India") }
    var destinationPoint by remember { mutableStateOf("") }
    var stopsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var rideInstructions by remember { mutableStateOf("") }

    var destSuggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var isMapFullscreen by remember { mutableStateOf(false) }

    val primaryTextColor = if (darkMode) Color.White else Color(0xFF0F172A)
    val secondaryTextColor = if (darkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBackground = if (darkMode) Color(0xFF1E293B) else Color.White
    val cyanAccent = Color(0xFF0284C7)
    val cyanBorder = Color(0xFFBAE6FD)

    val vehicleOptions = listOf(
        "Bike (Yamaha R15 - RUI23227513)",
        "Motorcycle / Scooter",
        "Car (Sedan / Hatchback)",
        "SUV (Mahindra Thar / Creta)",
        "Bicycle / Cycle"
    )

    Scaffold(
        topBar = {
            // Dark Navy App Bar matching attached Image 1
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0B192C)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Quick Ride",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        containerColor = if (darkMode) Color(0xFF10151C) else Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ----------------------------------------------------
            // 1. SELECT VEHICLE SECTION
            // ----------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = cyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Select Vehicle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = primaryTextColor
                    )
                }

                // Dropdown Card with cyan border
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cyanBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showVehicleDropdown = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            tint = cyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = selectedVehicleLabel,
                            fontSize = 14.sp,
                            color = primaryTextColor,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            tint = cyanAccent
                        )
                    }
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = showVehicleDropdown,
                    onDismissRequest = { showVehicleDropdown = false }
                ) {
                    vehicleOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, fontSize = 13.sp) },
                            onClick = {
                                selectedVehicleLabel = opt
                                selectedVehicleType = when {
                                    opt.contains("Bike", true) || opt.contains("Motorcycle", true) -> "BIKE"
                                    opt.contains("Scooter", true) -> "SCOOTER"
                                    opt.contains("SUV", true) -> "SUV"
                                    opt.contains("Cycle", true) -> "CYCLE"
                                    else -> "CAR"
                                }
                                showVehicleDropdown = false
                            }
                        )
                    }
                }

                // Pill Button: "To Add Vehicle ?, Go to Profile..."
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.clickable { onNavigateToProfile() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "To Add Vehicle ?, Go to Profile...",
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 2. START POINT SECTION
            // ----------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(2.dp, cyanAccent, CircleShape)
                    )
                    Text(
                        text = "Start Point",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = primaryTextColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cyanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS",
                            tint = cyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = startPoint,
                            onValueChange = { startPoint = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor
                            )
                        )
                    }
                }
            }

            // ----------------------------------------------------
            // 3. STOPS SECTION
            // ----------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ForkRight,
                        contentDescription = null,
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Stops",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = primaryTextColor
                    )
                }

                // Render any added stops
                stopsList.forEachIndexed { index, stop ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}.", fontWeight = FontWeight.Bold, color = Color(0xFFEA580C), fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(stop, modifier = Modifier.weight(1f), fontSize = 13.sp, color = primaryTextColor)
                            IconButton(
                                onClick = { stopsList = stopsList.filterIndexed { i, _ -> i != index } },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // "Add Stop" Full Width Orange Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF7ED),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val defaultStops = listOf("Hebbal Flyover", "Yelahanka Old Town", "Nandi Upachar", "Devanahalli Toll")
                            val nextStop = defaultStops.getOrNull(stopsList.size) ?: "Stop ${stopsList.size + 1}"
                            stopsList = stopsList + nextStop
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Add Stop",
                            color = Color(0xFFEA580C),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ----------------------------------------------------
            // 4. DESTINATION POINT SECTION
            // ----------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = cyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Destination Point",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = primaryTextColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cyanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Flag",
                            tint = cyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = destinationPoint,
                            onValueChange = {
                                destinationPoint = it
                                scope.launch {
                                    destSuggestions = PlaceSearchService.searchPlaces(it)
                                }
                            },
                            placeholder = { Text("Enter Destination", color = secondaryTextColor, fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor
                            )
                        )
                    }
                }

                // Autocomplete Suggestions
                if (destSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cyanBorder)
                    ) {
                        Column(Modifier.padding(6.dp)) {
                            destSuggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            destinationPoint = suggestion.name
                                            destSuggestions = emptyList()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.LocationOn, null, tint = cyanAccent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(suggestion.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = primaryTextColor)
                                        Text(suggestion.description, fontSize = 10.sp, color = secondaryTextColor, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 5. TRAFFIC WARNING CARD
            // ----------------------------------------------------
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Alternate routes may vary depending on current traffic conditions.",
                        color = Color(0xFFB91C1C),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // ----------------------------------------------------
            // 6. REALISTIC GOOGLE MAP VIEW (Matching Image 1 & 2)
            // ----------------------------------------------------
            LiveGoogleMapView(
                sourceName = startPoint.ifBlank { "Bengaluru" },
                destinationName = destinationPoint.ifBlank { "Chikkajala" },
                members = listOf(TripMember("usr-${currentUserName.hashCode()}", currentUserName.ifBlank { "Prashant" }, 0xFF1D64F2L, true)),
                vehicle = currentVehicle,
                vehicleType = selectedVehicleType,
                childBagDevice = null,
                isLiveUsersMode = true,
                isFullscreen = isMapFullscreen,
                onToggleFullscreen = { isMapFullscreen = !isMapFullscreen },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )

            // ----------------------------------------------------
            // 7. RIDE TITLE / INSTRUCTIONS SECTION
            // ----------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Title,
                        contentDescription = null,
                        tint = cyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Ride Title",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = primaryTextColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cyanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = cyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = rideInstructions,
                            onValueChange = { rideInstructions = it },
                            placeholder = { Text("Add instructions for fellow riders...", color = secondaryTextColor, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ----------------------------------------------------
            // 8. START RIDE ACTION BUTTON
            // ----------------------------------------------------
            Button(
                onClick = {
                    val finalDest = destinationPoint.ifBlank { "Chikkajala, Bengaluru" }
                    val finalTitle = rideInstructions.ifBlank { "Ride to ${finalDest.split(",")[0]}" }
                    val dynamicId = "TB-" + (1000..9999).random()

                    onStartRide(
                        QuickRideData(
                            id = dynamicId,
                            source = startPoint,
                            stops = stopsList,
                            destination = finalDest,
                            title = finalTitle,
                            vehicleType = selectedVehicleType,
                            vehicleDetails = "${currentVehicle.name.ifBlank { "Yamaha R15" }} · ${currentVehicle.registrationNumber.ifBlank { "RUI23227513" }}",
                            instructions = rideInstructions,
                            date = "Today",
                            members = listOf(
                                TripMember(
                                    id = "usr-${currentUserName.hashCode()}",
                                    name = currentUserName.ifBlank { "Prashant" },
                                    color = 0xFF1D64F2L,
                                    isSharingLocation = true
                                )
                            )
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D64F2))
            ) {
                Text(
                    text = "Start Ride",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}
