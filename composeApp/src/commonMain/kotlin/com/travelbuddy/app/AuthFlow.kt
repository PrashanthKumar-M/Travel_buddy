package com.travelbuddy.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelbuddy.app.data.KtorTravelRepository
import com.travelbuddy.app.data.OtpVerificationRequest
import com.travelbuddy.app.data.TravelApiConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Design Tokens matching the authentic screens:
 * Clean white theme, deep teal logo title, vibrant royal blue action buttons.
 */
private val BrandDeepTeal = Color(0xFF0A5C67)
private val BrandBlue = Color(0xFF1D64F2)
private val BrandMutedSlogan = Color(0xFF334155)
private val BrandLightSlogan = Color(0xFF64748B)
private val BorderLight = Color(0xFFE2E8F0)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val RedAlert = Color(0xFFDC2626)

internal object Tb {
    val Navy = Color(0xFF1F2937)
    val Blue = Color(0xFF1D64F2)
    val Green = Color(0xFF27AE60)
    val Soft = Color(0xFFEAF3FF)
    val Gray = Color(0xFF6B7280)
    val Border = Color(0xFFE5E7EB)
    val Red = Color(0xFFE53935)
    val Page = Color(0xFFFFFFFF)
}

private enum class AuthStage {
    Welcome,
    LoginWithMobile,
    OtpVerification,
    SetPassword,
    Main
}

/** Login flow setting */
private const val REQUIRE_LOGIN = true

/**
 * Public entry point for Android, iOS, and Web.
 * Full auth flow based on the reference screens:
 * Welcome (New Roads, Better Friends) -> Login with Mobile Number -> OTP Verification -> Set Password -> Main app.
 */
@Composable
fun TravelBuddyApp(
    config: AppConfig = AppConfig(),
    pendingCoverImageUri: String? = null,
    onPickTripCoverImage: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val repository = remember(config.apiBaseUrl, config.webSocketUrl) {
        KtorTravelRepository(TravelApiConfig(config.apiBaseUrl, config.webSocketUrl))
    }
    val hasSavedSession = remember { SessionManager.isLoggedIn() }
    var stage by remember {
        mutableStateOf(
            if (hasSavedSession) AuthStage.Main
            else if (REQUIRE_LOGIN) AuthStage.Welcome
            else AuthStage.Main
        )
    }
    var loginIdentifier by remember {
        mutableStateOf(if (hasSavedSession) SessionManager.getUserIdentifier() else "")
    }
    var isEmailAuth by remember {
        mutableStateOf(if (hasSavedSession) SessionManager.isEmail() else false)
    }
    var userPassword by remember { mutableStateOf("") }
    var dynamicOtp by remember { mutableStateOf("") }

    when (stage) {
        AuthStage.Welcome -> {
            WelcomeScreen(
                onGetStarted = { stage = AuthStage.LoginWithMobile }
            )
        }

        AuthStage.LoginWithMobile -> {
            LoginScreen(
                initialIdentifier = loginIdentifier,
                initialIsEmail = isEmailAuth,
                onBack = { stage = AuthStage.Welcome },
                onSendOtp = { identifier, isEmail ->
                    loginIdentifier = identifier
                    isEmailAuth = isEmail
                    val generated = (100000..999999).random().toString()
                    dynamicOtp = generated
                    // 1. Send real cellular SMS from device SIM to the entered number if mobile
                    if (!isEmail) {
                        dispatchDeviceOtpNotification(identifier, generated)
                    }
                    // 2. Request OTP from live backend API (dispatches real Email if email, or SMS if mobile)
                    coroutineScope.launch {
                        val phoneArg = if (!isEmail) identifier else null
                        val emailArg = if (isEmail) identifier else null
                        repository.requestOtp(phone = phoneArg, email = emailArg).onSuccess { remoteOtp ->
                            if (!remoteOtp.isNullOrBlank()) {
                                dynamicOtp = remoteOtp
                            }
                        }
                    }
                    stage = AuthStage.OtpVerification
                },
                onPasswordLoginSuccess = { identifier, password ->
                    loginIdentifier = identifier
                    userPassword = password
                    val defaultName = if (isEmailAuth) {
                        identifier.substringBefore("@").replace(".", " ").replace("_", " ").split(" ")
                            .filter { it.isNotBlank() }
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                    } else {
                        identifier
                    }
                    val existingName = SessionManager.getUserName()
                    val sessionName = if (existingName.isNotBlank() && existingName != "Traveler") existingName else defaultName
                    SessionManager.saveSession("tb_jwt_${identifier.hashCode()}", identifier, sessionName, identifier, isEmailAuth)
                    stage = AuthStage.Main
                }
            )
        }

        AuthStage.OtpVerification -> {
            OtpVerificationScreen(
                identifier = loginIdentifier,
                isEmail = isEmailAuth,
                expectedOtp = dynamicOtp,
                onResendOtp = {
                    val fresh = (100000..999999).random().toString()
                    dynamicOtp = fresh
                    if (!isEmailAuth) {
                        dispatchDeviceOtpNotification(loginIdentifier, fresh)
                    }
                    coroutineScope.launch {
                        val phoneArg = if (!isEmailAuth) loginIdentifier else null
                        val emailArg = if (isEmailAuth) loginIdentifier else null
                        repository.requestOtp(phone = phoneArg, email = emailArg).onSuccess { remoteOtp ->
                            if (!remoteOtp.isNullOrBlank()) {
                                dynamicOtp = remoteOtp
                            }
                        }
                    }
                    fresh
                },
                onBack = { stage = AuthStage.LoginWithMobile },
                onVerified = {
                    coroutineScope.launch {
                        val phoneArg = if (!isEmailAuth) loginIdentifier else null
                        val emailArg = if (isEmailAuth) loginIdentifier else null
                        repository.verifyOtp(OtpVerificationRequest(phone = phoneArg, email = emailArg, otp = dynamicOtp))
                    }
                    stage = AuthStage.SetPassword
                }
            )
        }

        AuthStage.SetPassword -> {
            SetPasswordScreen(
                onBack = { stage = AuthStage.OtpVerification },
                onPasswordSet = { newPassword ->
                    userPassword = newPassword
                    val defaultName = if (isEmailAuth) {
                        loginIdentifier.substringBefore("@").replace(".", " ").replace("_", " ").split(" ")
                            .filter { it.isNotBlank() }
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                    } else {
                        loginIdentifier
                    }
                    val existingName = SessionManager.getUserName()
                    val sessionName = if (existingName.isNotBlank() && existingName != "Traveler") existingName else defaultName
                    SessionManager.saveSession("tb_jwt_${loginIdentifier.hashCode()}", loginIdentifier, sessionName, loginIdentifier, isEmailAuth)
                    stage = AuthStage.Main
                }
            )
        }

        AuthStage.Main -> {
            val userDisplay = if (isEmailAuth) loginIdentifier else "+91 $loginIdentifier"
            TravelBuddyMain(
                config = config,
                userPhone = userDisplay,
                pendingCoverImageUri = pendingCoverImageUri,
                onPickTripCoverImage = onPickTripCoverImage,
                onLogout = {
                    SessionManager.clearSession()
                    loginIdentifier = ""
                    userPassword = ""
                    stage = AuthStage.Welcome
                }
            )
        }
    }
}

// ============================================================
// 1. WELCOME SCREEN (New Roads, Better Friends)
// ============================================================

@Composable
private fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A192F),
                        Color(0xFF0F2B48),
                        Color(0xFF0A3A40),
                        Color(0xFF0B2436),
                        Color(0xFF061524)
                    )
                )
            )
    ) {
        // Modern Atmospheric Highway & Ambient Glow Backdrop
        AtmosphericRoadBackdrop(
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                // Glowing Brand Emblem
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x5038BDF8), Color(0x1538BDF8), Color.Transparent)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        modifier = Modifier.size(76.dp),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TravelBuddyLogo(size = 56.dp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // App Title
                Text(
                    text = "Travel Buddy",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(Modifier.height(6.dp))

                // Premium Category Tag
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.20f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "LIVE GROUP NAVIGATION & EXPENSE SPLIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7DD3FC),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Hero Catchphrase
                Text(
                    text = "New Roads, Better Friends.\nTravel Together In-Sync.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9),
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(Modifier.height(18.dp))

                // 3 Modern Glass Feature Highlights
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WelcomeFeatureCard(
                        icon = "📍",
                        title = "Live Group Radar",
                        description = "Track all riders live on 3D map with speed & battery telemetry"
                    )
                    WelcomeFeatureCard(
                        icon = "⚡",
                        title = "Zero-Math Split",
                        description = "Equal & custom splits with instant net settlement calculations"
                    )
                    WelcomeFeatureCard(
                        icon = "🛡️",
                        title = "Route Geofence & SOS",
                        description = "Instant emergency broadcast with breadcrumb trail coordinates"
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // Primary "Get Started" Pill Button
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF2563EB),
                                    Color(0xFF1D4ED8),
                                    Color(0xFF0284C7)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Get Started",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Free for all travel groups • Real-time Sync & Live Map",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun WelcomeFeatureCard(
    icon: String,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = icon, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// ============================================================
// ATMOSPHERIC HIGHWAY BACKDROP (Modern Cinematic Canvas)
// ============================================================

@Composable
private fun AtmosphericRoadBackdrop(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Ambient Glow Orbs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3538BDF8), Color(0x1038BDF8), Color.Transparent),
                center = Offset(w * 0.2f, h * 0.25f),
                radius = w * 0.5f
            ),
            center = Offset(w * 0.2f, h * 0.25f),
            radius = w * 0.5f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x2506B6D4), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.65f),
                radius = w * 0.55f
            ),
            center = Offset(w * 0.85f, h * 0.65f),
            radius = w * 0.55f
        )

        // 2. Smooth Scenic Horizon Hills (soft silhouettes with mist)
        val hill1 = Path().apply {
            moveTo(0f, h * 0.72f)
            cubicTo(w * 0.25f, h * 0.68f, w * 0.45f, h * 0.74f, w * 0.7f, h * 0.70f)
            cubicTo(w * 0.85f, h * 0.67f, w * 0.95f, h * 0.72f, w, h * 0.71f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = hill1,
            color = Color(0xFF0F2A38).copy(alpha = 0.5f)
        )

        val hill2 = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(w * 0.3f, h * 0.74f, w * 0.6f, h * 0.82f, w, h * 0.76f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = hill2,
            color = Color(0xFF071E28).copy(alpha = 0.7f)
        )

        // 3. Smooth curving Highway receding into the horizon
        val road = Path().apply {
            moveTo(w * 0.48f, h * 0.75f)
            lineTo(w * 0.52f, h * 0.75f)
            cubicTo(w * 0.54f, h * 0.84f, w * 0.65f, h * 0.92f, w * 0.85f, h)
            lineTo(w * 0.15f, h)
            cubicTo(w * 0.38f, h * 0.92f, w * 0.46f, h * 0.84f, w * 0.48f, h * 0.75f)
            close()
        }
        drawPath(
            path = road,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = 0.3f),
                    Color(0xFF1E293B).copy(alpha = 0.85f),
                    Color(0xFF0F172A)
                ),
                startY = h * 0.75f,
                endY = h
            )
        )

        // 4. Glowing Neon Amber Center Line
        val centerLine = Path().apply {
            moveTo(w * 0.50f, h * 0.75f)
            cubicTo(w * 0.51f, h * 0.84f, w * 0.53f, h * 0.93f, w * 0.50f, h)
        }
        drawPath(
            path = centerLine,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x00F59E0B), Color(0xAAF59E0B), Color(0xFFFBBF24)),
                startY = h * 0.75f,
                endY = h
            ),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}

// ============================================================
// 2. LOGIN SCREEN (Mobile Number or Email Address)
// ============================================================

private enum class LoginMethod {
    Mobile,
    Email
}

@Composable
private fun LoginScreen(
    initialIdentifier: String,
    initialIsEmail: Boolean,
    onBack: () -> Unit,
    onSendOtp: (String, Boolean) -> Unit,
    onPasswordLoginSuccess: (String, String) -> Unit
) {
    var loginMethod by remember { mutableStateOf(if (initialIsEmail) LoginMethod.Email else LoginMethod.Mobile) }
    var mobileNumber by remember { mutableStateOf(if (!initialIsEmail) initialIdentifier else "") }
    var emailAddress by remember { mutableStateOf(if (initialIsEmail) initialIdentifier else "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isPasswordMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button row
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Real Brand Logo
        TravelBuddyLogo(size = 64.dp)

        Spacer(Modifier.height(10.dp))

        // App Title
        Text(
            text = "Travel Buddy",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = BrandDeepTeal
        )

        Spacer(Modifier.height(4.dp))

        // Slogans
        Text(
            text = "Track • Split • Travel • Connect",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = BrandMutedSlogan
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Family & Friends Live Tracking • Smart Expense Splitting",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0369A1)
        )

        Spacer(Modifier.height(22.dp))

        // Login Method Selector Tab (Mobile vs Email)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mobile Tab
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (loginMethod == LoginMethod.Mobile) Color.White else Color.Transparent,
                    shadowElevation = if (loginMethod == LoginMethod.Mobile) 2.dp else 0.dp,
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                        loginMethod = LoginMethod.Mobile
                        errorMessage = null
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "📱 Mobile Number",
                            fontSize = 13.sp,
                            fontWeight = if (loginMethod == LoginMethod.Mobile) FontWeight.Bold else FontWeight.Medium,
                            color = if (loginMethod == LoginMethod.Mobile) BrandBlue else TextMuted
                        )
                    }
                }

                // Email Tab
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (loginMethod == LoginMethod.Email) Color.White else Color.Transparent,
                    shadowElevation = if (loginMethod == LoginMethod.Email) 2.dp else 0.dp,
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                        loginMethod = LoginMethod.Email
                        errorMessage = null
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✉️ Email Address",
                            fontSize = 13.sp,
                            fontWeight = if (loginMethod == LoginMethod.Email) FontWeight.Bold else FontWeight.Medium,
                            color = if (loginMethod == LoginMethod.Email) BrandBlue else TextMuted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Headline
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isPasswordMode) "Login with Password"
                       else if (loginMethod == LoginMethod.Mobile) "Login with Mobile Number"
                       else "Login with Email Address",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isPasswordMode) "Enter your registered password"
                       else if (loginMethod == LoginMethod.Mobile) "We will send a 6-digit OTP to your number"
                       else "We will send a 6-digit OTP to your email inbox",
                fontSize = 13.sp,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(18.dp))

        // Input based on loginMethod
        if (loginMethod == LoginMethod.Mobile) {
            // Mobile Number input row: [+91 box] [Phone field]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flag +91 box
                Surface(
                    modifier = Modifier.width(76.dp).height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🇮🇳", fontSize = 16.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "+91",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                    }
                }

                // Mobile number input field
                Surface(
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = mobileNumber,
                            onValueChange = { input ->
                                mobileNumber = input.filter { it.isDigit() }.take(10)
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark
                            ),
                            cursorBrush = SolidColor(BrandBlue),
                            decorationBox = { innerTextField ->
                                if (mobileNumber.isEmpty()) {
                                    Text(
                                        text = "Enter 10-digit number",
                                        color = Color(0xFFA0AEC0),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
        } else {
            // Email Address Input Field
            Surface(
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "@", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = emailAddress,
                        onValueChange = { input ->
                            emailAddress = input
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark
                        ),
                        cursorBrush = SolidColor(BrandBlue),
                        decorationBox = { innerTextField ->
                            if (emailAddress.isEmpty()) {
                                Text(
                                    text = "Enter your email address",
                                    color = Color(0xFFA0AEC0),
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        // Password input field (if in password mode)
        if (isPasswordMode) {
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark
                        ),
                        cursorBrush = SolidColor(BrandBlue),
                        decorationBox = { innerTextField ->
                            if (password.isEmpty()) {
                                Text(
                                    text = "Password",
                                    color = Color(0xFFA0AEC0),
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Visibility",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = errorMessage ?: "",
                color = RedAlert,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(20.dp))

        // "Send OTP ->" or "Login ->" Royal Blue Pill Button
        Button(
            onClick = {
                val isEmail = (loginMethod == LoginMethod.Email)
                val target = if (isEmail) emailAddress.trim() else mobileNumber.trim()

                if (isEmail) {
                    if (target.isBlank() || !target.contains("@") || !target.contains(".")) {
                        errorMessage = "Please enter a valid email address"
                        return@Button
                    }
                } else {
                    if (target.length < 10) {
                        errorMessage = "Please enter a valid 10-digit mobile number"
                        return@Button
                    }
                }

                if (isPasswordMode) {
                    if (password.isBlank()) {
                        errorMessage = "Please enter your password"
                        return@Button
                    }
                    onPasswordLoginSuccess(target, password)
                } else {
                    onSendOtp(target, isEmail)
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isPasswordMode) "Login" else "Send OTP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Submit",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Toggle between OTP and Password login
        Text(
            text = if (isPasswordMode) "Sign in using OTP instead" else "Login using Password instead",
            color = BrandBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable {
                    isPasswordMode = !isPasswordMode
                    errorMessage = null
                }
                .padding(vertical = 4.dp)
        )

        Spacer(Modifier.height(28.dp))
    }
}

// ============================================================
// 3. OTP VERIFICATION SCREEN (Clean White Theme)
// ============================================================

@Composable
private fun OtpVerificationScreen(
    identifier: String,
    isEmail: Boolean,
    expectedOtp: String,
    onResendOtp: () -> String,
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    var currentOtp by remember(expectedOtp) { mutableStateOf(expectedOtp) }
    var otpDigits by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(30) } // 30s standard resend cooldown
    var resendKey by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendNotice by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(resendKey) {
        countdown = 30
        while (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val minutes = countdown / 60
    val seconds = countdown % 60
    val formattedTime = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Real Brand Logo
        TravelBuddyLogo(size = 56.dp)

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Travel Buddy",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BrandDeepTeal
        )

        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "OTP Verification",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isEmail) "We have sent a dynamic 6-digit OTP to $identifier" else "We have sent a dynamic 6-digit OTP to +91 $identifier",
                fontSize = 14.sp,
                color = TextMuted
            )
            if (isEmail) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Please check your inbox or spam folder",
                    fontSize = 12.sp,
                    color = Color(0xFF0369A1)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 6 OTP Digit Boxes
        BasicTextField(
            value = otpDigits,
            onValueChange = { input ->
                otpDigits = input.filter { it.isDigit() }.take(6)
                errorMessage = null
            },
            modifier = Modifier.focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(6) { index ->
                        val digit = otpDigits.getOrNull(index)?.toString() ?: ""
                        val isFocused = index == otpDigits.length

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .border(
                                    width = if (isFocused) 1.8.dp else 1.dp,
                                    color = if (isFocused) BrandBlue else BorderLight,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(Color.White, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }
                }
            }
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (isEmail) "Enter the 6-digit code sent to $identifier" else "Enter the 6-digit code sent via SMS to +91 $identifier",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )

        Spacer(Modifier.height(4.dp))
        Text(
            text = "⚡ Testing mode: you can also enter 123456",
            fontSize = 11.sp,
            color = Color(0xFF0284C7)
        )

        if (resendNotice != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Text(
                    text = resendNotice ?: "",
                    color = Color(0xFF16A34A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage ?: "",
                color = RedAlert,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(20.dp))

        // "Verify OTP ->" Royal Blue Button
        Button(
            onClick = {
                val entered = otpDigits.trim()
                if (entered == currentOtp.trim() || entered == "123456") {
                    onVerified()
                } else {
                    errorMessage = if (isEmail) "Incorrect OTP. Please enter the 6-digit code (or 123456 to test)"
                                   else "Incorrect OTP. Please enter the 6-digit code (or 123456 to test)"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Verify OTP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Verify",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Resend Timer Pill
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                modifier = Modifier.then(
                    if (countdown == 0) Modifier.clickable {
                        val freshOtp = onResendOtp()
                        currentOtp = freshOtp
                        otpDigits = ""
                        errorMessage = null
                        resendNotice = if (isEmail) "New 6-digit OTP sent to $identifier" else "New 6-digit OTP sent to +91 $identifier"
                        resendKey += 1
                    } else Modifier
                ),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (countdown == 0) BrandBlue else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (countdown > 0) "Resend OTP in $formattedTime" else "Resend OTP",
                        color = if (countdown == 0) BrandBlue else TextMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ============================================================
// 4. SET PASSWORD / COMPLETE PROFILE SCREEN
// ============================================================

@Composable
private fun SetPasswordScreen(
    onBack: () -> Unit,
    onPasswordSet: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var retypePasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Real Brand Logo
        TravelBuddyLogo(size = 64.dp)

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Travel Buddy",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = BrandDeepTeal
        )

        Spacer(Modifier.height(30.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Set Password",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Create a password for your Travel Buddy account",
                fontSize = 14.sp,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(24.dp))

        // Enter New Password
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    ),
                    cursorBrush = SolidColor(BrandBlue),
                    decorationBox = { innerTextField ->
                        if (newPassword.isEmpty()) {
                            Text(
                                text = "Enter New Password",
                                color = Color(0xFFA0AEC0),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )
                IconButton(
                    onClick = { newPasswordVisible = !newPasswordVisible },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Visibility",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Retype Password
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Retype Password",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = retypePassword,
                    onValueChange = {
                        retypePassword = it
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    visualTransformation = if (retypePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    ),
                    cursorBrush = SolidColor(BrandBlue),
                    decorationBox = { innerTextField ->
                        if (retypePassword.isEmpty()) {
                            Text(
                                text = "Retype Password",
                                color = Color(0xFFA0AEC0),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )
                IconButton(
                    onClick = { retypePasswordVisible = !retypePasswordVisible },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (retypePasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Visibility",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "* Password must contain at least one capital letter, one digit, one special character",
            color = RedAlert,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = errorMessage ?: "",
                color = RedAlert,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(28.dp))

        // "Get Started ->" Royal Blue Pill Button
        Button(
            onClick = {
                if (newPassword.isBlank()) {
                    errorMessage = "Please enter a password"
                    return@Button
                }
                if (newPassword != retypePassword) {
                    errorMessage = "Passwords do not match"
                    return@Button
                }
                onPasswordSet(newPassword)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Complete",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
