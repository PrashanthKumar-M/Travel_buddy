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
import kotlinx.coroutines.delay

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

/** Demo credentials */
private const val REQUIRE_LOGIN = true
private const val DEMO_PHONE = "9876543210"
private const val DEMO_OTP_6 = "123456"
private const val DEMO_OTP_4 = "1234"

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
    var stage by remember { mutableStateOf(if (REQUIRE_LOGIN) AuthStage.Welcome else AuthStage.Main) }
    var phoneDigits by remember { mutableStateOf(DEMO_PHONE) }
    var userPassword by remember { mutableStateOf("") }

    when (stage) {
        AuthStage.Welcome -> {
            WelcomeScreen(
                onGetStarted = { stage = AuthStage.LoginWithMobile }
            )
        }

        AuthStage.LoginWithMobile -> {
            LoginWithMobileScreen(
                initialPhone = phoneDigits,
                onBack = { stage = AuthStage.Welcome },
                onSendOtp = { phone ->
                    phoneDigits = phone
                    stage = AuthStage.OtpVerification
                },
                onPasswordLoginSuccess = { phone, password ->
                    phoneDigits = phone
                    userPassword = password
                    stage = AuthStage.Main
                }
            )
        }

        AuthStage.OtpVerification -> {
            OtpVerificationScreen(
                phone = phoneDigits,
                onBack = { stage = AuthStage.LoginWithMobile },
                onVerified = {
                    stage = AuthStage.SetPassword
                }
            )
        }

        AuthStage.SetPassword -> {
            SetPasswordScreen(
                onBack = { stage = AuthStage.OtpVerification },
                onPasswordSet = { newPassword ->
                    userPassword = newPassword
                    stage = AuthStage.Main
                }
            )
        }

        AuthStage.Main -> {
            TravelBuddyMain(
                config = config,
                userPhone = "+91 $phoneDigits",
                pendingCoverImageUri = pendingCoverImageUri,
                onPickTripCoverImage = onPickTripCoverImage,
                onLogout = { stage = AuthStage.Welcome }
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
            .background(Color.White)
    ) {
        // Scenic winding road with mist and mountain hills
        ScenicMountainRoadBackdrop(
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Real Brand Logo
            TravelBuddyLogo(size = 76.dp)

            Spacer(Modifier.height(14.dp))

            // App Name
            Text(
                text = "Travel Buddy",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = BrandDeepTeal
            )

            Spacer(Modifier.height(4.dp))

            // Slogan 1
            Text(
                text = "Track • Split • Travel • Connect",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BrandMutedSlogan
            )

            Spacer(Modifier.height(4.dp))

            // Family & Friends tracking + Split slogan
            Text(
                text = "Family & Friends Live Tracking • Smart Expense Splitting",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0369A1)
            )

            Spacer(Modifier.height(24.dp))

            // Main Headline
            Text(
                text = "New Roads, Better Friends\nTrack Live & Split Fairly",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // "Get Started ->" Royal Blue Pill Button
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
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
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Get Started",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// ============================================================
// SCENIC MOUNTAIN ROAD CANVAS (Custom vector background)
// ============================================================

@Composable
private fun ScenicMountainRoadBackdrop(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Sky Gradient: Soft morning mist to sunrise glow
        val skyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF7F8F9),
                Color(0xFFEDF3F1),
                Color(0xFFDFEAE6),
                Color(0xFFD3E1DD)
            ),
            startY = 0f,
            endY = h * 0.45f
        )
        drawRect(brush = skyBrush, size = Size(w, h))

        // 2. Distant mountain peaks (misty grey-green)
        val mtnFar = Path().apply {
            moveTo(0f, h * 0.32f)
            cubicTo(w * 0.25f, h * 0.26f, w * 0.5f, h * 0.30f, w * 0.75f, h * 0.24f)
            lineTo(w, h * 0.27f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = mtnFar,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFBFD4CC), Color(0xFF8AA69A)),
                startY = h * 0.24f,
                endY = h * 0.50f
            )
        )

        // 3. Middle mountain slope covered in trees
        val mtnMid = Path().apply {
            moveTo(0f, h * 0.22f)
            cubicTo(w * 0.2f, h * 0.32f, w * 0.35f, h * 0.48f, w * 0.55f, h * 0.65f)
            lineTo(0f, h * 0.75f)
            close()
        }
        drawPath(
            path = mtnMid,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4C7362), Color(0xFF2E4E41)),
                start = Offset(0f, h * 0.25f),
                end = Offset(w * 0.6f, h * 0.7f)
            )
        )

        // Right hill with forest
        val mtnRight = Path().apply {
            moveTo(w * 0.65f, h * 0.35f)
            cubicTo(w * 0.8f, h * 0.45f, w * 0.9f, h * 0.55f, w, h * 0.6f)
            lineTo(w, h)
            lineTo(w * 0.6f, h)
            close()
        }
        drawPath(
            path = mtnRight,
            color = Color(0xFF6B8A7A)
        )

        // 4. Winding Mountain Asphalt Road
        // Curves from lower left/center sweeping toward the right and curving into the mountain
        val roadPath = Path().apply {
            moveTo(-w * 0.1f, h)
            cubicTo(
                w * 0.25f, h * 0.82f,
                w * 0.55f, h * 0.74f,
                w * 0.42f, h * 0.58f
            )
            lineTo(w * 0.52f, h * 0.56f)
            cubicTo(
                w * 0.72f, h * 0.74f,
                w * 0.65f, h * 0.85f,
                w * 0.95f, h
            )
            close()
        }
        drawPath(
            path = roadPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF4A5568), Color(0xFF2D3748), Color(0xFF1A202C)),
                startY = h * 0.56f,
                endY = h
            )
        )

        // Road Outer Stone Barrier / Guard Wall
        val wallPath = Path().apply {
            moveTo(w * 0.52f, h * 0.56f)
            cubicTo(
                w * 0.73f, h * 0.74f,
                w * 0.66f, h * 0.85f,
                w * 0.97f, h
            )
        }
        drawPath(
            path = wallPath,
            color = Color(0xFF718096),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )

        // Road Center Broken Lane Lines (Yellow/White)
        val centerLine = Path().apply {
            moveTo(w * 0.47f, h * 0.57f)
            cubicTo(
                w * 0.48f, h * 0.68f,
                w * 0.45f, h * 0.80f,
                w * 0.42f, h
            )
        }
        drawPath(
            path = centerLine,
            color = Color(0xFFFBBF24),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // 5. Motorcyclist / Rider silhouette leaning into the turn
        val riderX = w * 0.38f
        val riderY = h * 0.72f
        val riderScale = w * 0.0016f

        // Motorbike body
        val bikePath = Path().apply {
            moveTo(riderX - 25f * riderScale, riderY + 30f * riderScale)
            lineTo(riderX + 25f * riderScale, riderY + 28f * riderScale)
            lineTo(riderX + 35f * riderScale, riderY + 15f * riderScale)
            lineTo(riderX - 10f * riderScale, riderY + 10f * riderScale)
            close()
        }
        drawPath(path = bikePath, color = Color(0xFF1E293B))

        // Rear Wheel
        drawCircle(
            color = Color(0xFF0F172A),
            radius = 16f * riderScale,
            center = Offset(riderX - 22f * riderScale, riderY + 32f * riderScale)
        )
        // Front Wheel
        drawCircle(
            color = Color(0xFF0F172A),
            radius = 16f * riderScale,
            center = Offset(riderX + 28f * riderScale, riderY + 30f * riderScale)
        )
        // Red Taillight
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 4f * riderScale,
            center = Offset(riderX - 26f * riderScale, riderY + 18f * riderScale)
        )

        // Rider Torso and Helmet
        val torsoPath = Path().apply {
            moveTo(riderX - 12f * riderScale, riderY + 14f * riderScale)
            lineTo(riderX + 8f * riderScale, riderY + 8f * riderScale)
            lineTo(riderX + 16f * riderScale, riderY - 14f * riderScale)
            lineTo(riderX - 4f * riderScale, riderY - 10f * riderScale)
            close()
        }
        drawPath(path = torsoPath, color = Color(0xFF334155))

        // Rider Helmet
        drawCircle(
            color = Color(0xFF0F172A),
            radius = 10f * riderScale,
            center = Offset(riderX + 6f * riderScale, riderY - 24f * riderScale)
        )

        // 6. Top soft mist overlay for crisp text readability
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xF0FFFFFF),
                    Color(0xD9FFFFFF),
                    Color(0x66FFFFFF),
                    Color(0x00FFFFFF)
                ),
                startY = 0f,
                endY = h * 0.42f
            ),
            size = Size(w, h * 0.42f)
        )

        // Bottom gradient overlay for button contrast
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x00FFFFFF),
                    Color(0xCCFFFFFF),
                    Color(0xF2FFFFFF)
                ),
                startY = h * 0.78f,
                endY = h
            ),
            topLeft = Offset(0f, h * 0.78f),
            size = Size(w, h * 0.22f)
        )
    }
}

// ============================================================
// 2. LOGIN WITH MOBILE NUMBER SCREEN (Image 1)
// ============================================================

@Composable
private fun LoginWithMobileScreen(
    initialPhone: String,
    onBack: () -> Unit,
    onSendOtp: (String) -> Unit,
    onPasswordLoginSuccess: (String, String) -> Unit
) {
    var mobileNumber by remember { mutableStateOf(initialPhone) }
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

        Spacer(Modifier.height(12.dp))

        // Real Brand Logo
        TravelBuddyLogo(size = 68.dp)

        Spacer(Modifier.height(12.dp))

        // App Title
        Text(
            text = "Travel Buddy",
            fontSize = 30.sp,
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
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Family & Friends Live Tracking • Smart Expense Splitting",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0369A1)
        )

        Spacer(Modifier.height(32.dp))

        // Headline (Left-aligned as in screenshot)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isPasswordMode) "Login with Password" else "Login with Mobile Number",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isPasswordMode) "Enter your registered password" else "We will send a 6-digit OTP to your number",
                fontSize = 14.sp,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(24.dp))

        // Mobile Number input row: [+91 box] [Phone field]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag +91 box
            Surface(
                modifier = Modifier
                    .width(76.dp)
                    .height(54.dp),
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
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
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
                                    text = "9876543210",
                                    color = Color(0xFFA0AEC0),
                                    fontSize = 16.sp
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
                if (mobileNumber.length < 10) {
                    errorMessage = "Please enter a valid 10-digit mobile number"
                    return@Button
                }
                if (isPasswordMode) {
                    if (password.isBlank()) {
                        errorMessage = "Please enter your password"
                        return@Button
                    }
                    onPasswordLoginSuccess(mobileNumber, password)
                } else {
                    onSendOtp(mobileNumber)
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
    phone: String,
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    var otpDigits by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(99) } // 01:39
    var resendKey by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(resendKey) {
        countdown = 99
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
                text = "OTP Verification",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "We have sent a 6-digit OTP to +91 $phone",
                fontSize = 14.sp,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(28.dp))

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
            text = "Demo OTP: $DEMO_OTP_6 (or $DEMO_OTP_4)",
            fontSize = 12.sp,
            color = Color(0xFFA0AEC0)
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage ?: "",
                color = RedAlert,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(28.dp))

        // "Verify OTP ->" Royal Blue Button
        Button(
            onClick = {
                if (otpDigits == DEMO_OTP_6 || otpDigits == DEMO_OTP_4 || otpDigits.length >= 4) {
                    onVerified()
                } else {
                    errorMessage = "Incorrect OTP. Try entering $DEMO_OTP_6"
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
                    if (countdown == 0) Modifier.clickable { resendKey += 1 } else Modifier
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
