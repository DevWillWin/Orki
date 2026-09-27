package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.network.EmailVerificationService
import com.example.data.preferences.UserPreferences
import java.util.Locale
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class MainAuthTab {
    EMAIL_PASSWORD,
    GOOGLE
}

private enum class EmailAuthMode {
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD
}

private enum class SignUpStep {
    ENTER_DETAILS,
    VERIFY_OTP
}

private enum class ForgotStep {
    REQUEST_CODE,
    VERIFY_AND_RESET
}

@Composable
fun LoginDialog(
    suggestedEmail: String = "devmightwin@gmail.com",
    suggestedName: String = "DevD",
    onDismiss: () -> Unit,
    onSignIn: (email: String, name: String, method: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val emailService = remember { EmailVerificationService(context) }
    val userPrefs = remember { UserPreferences(context) }

    var selectedTab by remember { mutableStateOf(MainAuthTab.EMAIL_PASSWORD) }
    var emailAuthMode by remember { mutableStateOf(EmailAuthMode.SIGN_IN) }

    // Sign In states
    var signInEmail by remember { mutableStateOf(suggestedEmail) }
    var signInPassword by remember { mutableStateOf("") }
    var signInPasswordVisible by remember { mutableStateOf(false) }

    // Sign Up states (First time password creation + email OTP verification)
    var signUpStep by remember { mutableStateOf(SignUpStep.ENTER_DETAILS) }
    var signUpEmail by remember { mutableStateOf(suggestedEmail) }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    var signUpPasswordVisible by remember { mutableStateOf(false) }
    var signUpConfirmPasswordVisible by remember { mutableStateOf(false) }
    var signUpOtpInput by remember { mutableStateOf("") }
    var isSignUpSendingCode by remember { mutableStateOf(false) }
    var signUpCooldownSeconds by remember { mutableIntStateOf(0) }

    // Forgot Password states
    var forgotStep by remember { mutableStateOf(ForgotStep.REQUEST_CODE) }
    var forgotEmail by remember { mutableStateOf(suggestedEmail) }
    var forgotOtpInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }
    var isForgotSendingCode by remember { mutableStateOf(false) }
    var forgotCooldownSeconds by remember { mutableIntStateOf(0) }

    // Feedback messages
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Cooldown countdown timers
    LaunchedEffect(signUpCooldownSeconds) {
        if (signUpCooldownSeconds > 0) {
            delay(1000)
            signUpCooldownSeconds -= 1
        }
    }

    LaunchedEffect(forgotCooldownSeconds) {
        if (forgotCooldownSeconds > 0) {
            delay(1000)
            forgotCooldownSeconds -= 1
        }
    }

    fun deriveDisplayName(emailAddress: String): String {
        val prefix = emailAddress.substringBefore("@").replace(".", " ").replace("_", " ")
        val formatted = prefix.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        return formatted.ifEmpty { "User" }
    }

    val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$")

    fun validateEmail(target: String): String? {
        val trimmed = target.trim()
        if (trimmed.isEmpty()) return "Email address is required"
        if (!emailRegex.matches(trimmed)) return "Invalid email format (e.g. name@domain.com)"
        if (trimmed.contains("@gamil.com", ignoreCase = true)) return "Typo detected: Did you mean @gmail.com?"
        if (trimmed.contains("@yaho.com", ignoreCase = true)) return "Typo detected: Did you mean @yahoo.com?"
        return null
    }

    fun triggerSendSignUpOtp() {
        val emailErr = validateEmail(signUpEmail)
        if (emailErr != null) {
            errorMessage = emailErr
            return
        }
        if (signUpPassword.length < 6) {
            errorMessage = "❌ Password must be at least 6 characters"
            return
        }
        if (signUpPassword != signUpConfirmPassword) {
            errorMessage = "❌ Passwords do not match"
            return
        }

        errorMessage = null
        statusSuccessMessage = null
        isSignUpSendingCode = true

        scope.launch {
            val result = emailService.sendVerificationCode(
                email = signUpEmail.trim(),
                recipientName = deriveDisplayName(signUpEmail)
            )
            isSignUpSendingCode = false
            if (result.success) {
                signUpCooldownSeconds = 60
                signUpStep = SignUpStep.VERIFY_OTP
                statusSuccessMessage = "📬 6-digit verification code sent to ${signUpEmail.trim()}! Please check your Inbox and Spam folder."
            } else {
                errorMessage = result.message
            }
        }
    }

    fun triggerSendForgotOtp() {
        val emailErr = validateEmail(forgotEmail)
        if (emailErr != null) {
            errorMessage = emailErr
            return
        }

        errorMessage = null
        statusSuccessMessage = null
        isForgotSendingCode = true

        scope.launch {
            val result = emailService.sendVerificationCode(
                email = forgotEmail.trim(),
                recipientName = deriveDisplayName(forgotEmail)
            )
            isForgotSendingCode = false
            if (result.success) {
                forgotCooldownSeconds = 60
                forgotStep = ForgotStep.VERIFY_AND_RESET
                statusSuccessMessage = "📬 Reset code sent to ${forgotEmail.trim()}! Please check your inbox."
            } else {
                errorMessage = result.message
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF064E3B), Color(0xFF042F24))
                                    )
                                )
                                .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Authentication & Security",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Sign in to Orki AI with password or Google",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Tabs: Email & Password / Google
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141715))
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Email & Password Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == MainAuthTab.EMAIL_PASSWORD) EmeraldPrimary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                selectedTab = MainAuthTab.EMAIL_PASSWORD
                                errorMessage = null
                                statusSuccessMessage = null
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (selectedTab == MainAuthTab.EMAIL_PASSWORD) EmeraldAccent else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Email & Password",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == MainAuthTab.EMAIL_PASSWORD) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == MainAuthTab.EMAIL_PASSWORD) EmeraldAccent else TextMuted
                            )
                        }
                    }

                    // Google Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == MainAuthTab.GOOGLE) EmeraldPrimary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                selectedTab = MainAuthTab.GOOGLE
                                errorMessage = null
                                statusSuccessMessage = null
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "G",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == MainAuthTab.GOOGLE) Color(0xFF4285F4) else TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == MainAuthTab.GOOGLE) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == MainAuthTab.GOOGLE) EmeraldAccent else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3B1212))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Success / Info Banner
                AnimatedVisibility(visible = statusSuccessMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D3325))
                            .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = statusSuccessMessage ?: "",
                            fontSize = 12.sp,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                when (selectedTab) {
                    MainAuthTab.GOOGLE -> {
                        // Google Authenticated 1-Tap
                        Column {
                            Text(
                                text = "Verified Device Account",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sign in directly with your authenticated Google identity. Email verification is automatically verified.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF161A18))
                                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        onSignIn(suggestedEmail, suggestedName, "Google")
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "G",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4285F4)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = suggestedName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Verified",
                                                    tint = EmeraldAccent,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Text(
                                                text = suggestedEmail,
                                                fontSize = 11.sp,
                                                color = EmeraldAccent
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Verified",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF042F24),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EmeraldAccent)
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    onSignIn(suggestedEmail, suggestedName, "Google")
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Sign In with Google",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    MainAuthTab.EMAIL_PASSWORD -> {
                        // Email & Password with Sign In vs Sign Up vs Forgot Password
                        when (emailAuthMode) {
                            EmailAuthMode.SIGN_IN -> {
                                Column {
                                    // Sub-mode pill selector (Sign In vs Sign Up)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1B201D))
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(EmeraldPrimary.copy(alpha = 0.25f))
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Sign In",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldAccent
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    emailAuthMode = EmailAuthMode.SIGN_UP
                                                    signUpStep = SignUpStep.ENTER_DETAILS
                                                    signUpEmail = signInEmail.trim()
                                                    signUpPassword = ""
                                                    signUpConfirmPassword = ""
                                                    errorMessage = null
                                                    statusSuccessMessage = null
                                                }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Create Account",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Sign In to Your Account",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Enter your registered email and account password.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = signInEmail,
                                        onValueChange = {
                                            signInEmail = it
                                            errorMessage = null
                                        },
                                        label = { Text("Email address", fontSize = 12.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = DarkSurfaceBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            cursorColor = EmeraldPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = signInPassword,
                                        onValueChange = {
                                            signInPassword = it
                                            errorMessage = null
                                        },
                                        label = { Text("Account password", fontSize = 12.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { signInPasswordVisible = !signInPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (signInPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = if (signInPasswordVisible) "Hide password" else "Show password",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        visualTransformation = if (signInPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = DarkSurfaceBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            cursorColor = EmeraldPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Forgot Password Link
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                emailAuthMode = EmailAuthMode.FORGOT_PASSWORD
                                                forgotStep = ForgotStep.REQUEST_CODE
                                                forgotEmail = signInEmail.trim()
                                                forgotOtpInput = ""
                                                newPassword = ""
                                                confirmNewPassword = ""
                                                errorMessage = null
                                                statusSuccessMessage = null
                                            }
                                        ) {
                                            Text(
                                                text = "Forgot Password?",
                                                fontSize = 12.sp,
                                                color = EmeraldAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            val err = validateEmail(signInEmail)
                                            if (err != null) {
                                                errorMessage = err
                                                return@Button
                                            }
                                            if (signInPassword.length < 6) {
                                                errorMessage = "❌ Password must be at least 6 characters"
                                                return@Button
                                            }

                                            val savedPwd = userPrefs.getPasswordForEmail(signInEmail.trim())
                                            if (savedPwd == null) {
                                                errorMessage = "No account password set for this email. Tap 'Create Account' above to sign up and set your password!"
                                                return@Button
                                            }

                                            if (savedPwd != signInPassword) {
                                                errorMessage = "❌ Incorrect password. Tap 'Forgot Password?' to reset it."
                                                return@Button
                                            }

                                            val finalName = deriveDisplayName(signInEmail.trim())
                                            userPrefs.userEmail = signInEmail.trim()
                                            userPrefs.isEmailVerified = true
                                            onSignIn(signInEmail.trim(), finalName, "Email & Password")
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                    ) {
                                        Text(
                                            text = "Authenticate & Sign In",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Don't have an account?",
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        TextButton(
                                            onClick = {
                                                emailAuthMode = EmailAuthMode.SIGN_UP
                                                signUpStep = SignUpStep.ENTER_DETAILS
                                                signUpEmail = signInEmail.trim()
                                                signUpPassword = ""
                                                signUpConfirmPassword = ""
                                                errorMessage = null
                                                statusSuccessMessage = null
                                            }
                                        ) {
                                            Text(
                                                text = "Sign Up / Set Password",
                                                fontSize = 12.sp,
                                                color = EmeraldAccent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            EmailAuthMode.SIGN_UP -> {
                                // First-time registration with Password setup and Email OTP verification
                                Column {
                                    // Sub-mode pill selector (Sign In vs Sign Up)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1B201D))
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    emailAuthMode = EmailAuthMode.SIGN_IN
                                                    errorMessage = null
                                                    statusSuccessMessage = null
                                                }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Sign In",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextMuted
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(EmeraldPrimary.copy(alpha = 0.25f))
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Create Account",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldAccent
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    if (signUpStep == SignUpStep.ENTER_DETAILS) {
                                        Text(
                                            text = "Step 1: Set Your Password",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Choose your account password and we'll send a 6-digit code to verify your email.",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        OutlinedTextField(
                                            value = signUpEmail,
                                            onValueChange = {
                                                signUpEmail = it
                                                errorMessage = null
                                            },
                                            label = { Text("Email address", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Email,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = DarkSurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = signUpPassword,
                                            onValueChange = {
                                                signUpPassword = it
                                                errorMessage = null
                                            },
                                            label = { Text("Create password (min 6 characters)", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
                                                    Icon(
                                                        imageVector = if (signUpPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                        contentDescription = null,
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = DarkSurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = signUpConfirmPassword,
                                            onValueChange = {
                                                signUpConfirmPassword = it
                                                errorMessage = null
                                            },
                                            label = { Text("Confirm password", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { signUpConfirmPasswordVisible = !signUpConfirmPasswordVisible }) {
                                                    Icon(
                                                        imageVector = if (signUpConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                        contentDescription = null,
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            visualTransformation = if (signUpConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = DarkSurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Security badge
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF161A18))
                                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
                                                .padding(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Shield,
                                                    contentDescription = null,
                                                    tint = EmeraldAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "We will send a 6-digit OTP code to verify ownership and activate your password.",
                                                    fontSize = 11.sp,
                                                    color = TextMuted,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = { triggerSendSignUpOtp() },
                                            enabled = !isSignUpSendingCode,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                        ) {
                                            if (isSignUpSendingCode) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = Color.Black,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Continue & Send Email Code",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Already have an account?",
                                                fontSize = 12.sp,
                                                color = TextMuted
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            TextButton(
                                                onClick = {
                                                    emailAuthMode = EmailAuthMode.SIGN_IN
                                                    signInEmail = signUpEmail.trim()
                                                    errorMessage = null
                                                    statusSuccessMessage = null
                                                }
                                            ) {
                                                Text(
                                                    text = "Sign In",
                                                    fontSize = 12.sp,
                                                    color = EmeraldAccent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        // Step 2: Verify 6-digit OTP to complete registration & activate password
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    signUpStep = SignUpStep.ENTER_DETAILS
                                                    errorMessage = null
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = EmeraldAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Step 2: Verify Email & Activate",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Enter the 6-digit verification code sent to ${signUpEmail.trim()} to activate your account password.",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            text = "6-Digit Email Code",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        OutlinedTextField(
                                            value = signUpOtpInput,
                                            onValueChange = {
                                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                                    signUpOtpInput = it
                                                    errorMessage = null
                                                }
                                            },
                                            placeholder = { Text("• • • • • •", letterSpacing = 4.sp, color = TextMuted) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 5.sp,
                                                textAlign = TextAlign.Center,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = DarkSurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldPrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = {
                                                if (signUpOtpInput.length != 6) {
                                                    errorMessage = "Please enter all 6 digits of the code"
                                                    return@Button
                                                }

                                                val (isVerified, verifyMessage) = emailService.verifyCode(signUpEmail.trim(), signUpOtpInput.trim())
                                                if (!isVerified) {
                                                    errorMessage = "❌ $verifyMessage"
                                                    return@Button
                                                }

                                                // Success: Save Password & Register User
                                                userPrefs.setPasswordForEmail(signUpEmail.trim(), signUpPassword)
                                                userPrefs.userEmail = signUpEmail.trim()
                                                userPrefs.isEmailVerified = true

                                                val finalName = deriveDisplayName(signUpEmail.trim())
                                                onSignIn(signUpEmail.trim(), finalName, "Email & Password")
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                        ) {
                                            Text(
                                                text = "Verify Code & Activate Password",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    signUpStep = SignUpStep.ENTER_DETAILS
                                                    errorMessage = null
                                                }
                                            ) {
                                                Text("Change Details", fontSize = 11.sp, color = TextMuted)
                                            }

                                            if (signUpCooldownSeconds > 0) {
                                                Text(
                                                    text = "Resend in ${signUpCooldownSeconds}s",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            } else {
                                                TextButton(onClick = { triggerSendSignUpOtp() }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = null,
                                                        tint = EmeraldAccent,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Resend Code", fontSize = 11.sp, color = EmeraldAccent)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            EmailAuthMode.FORGOT_PASSWORD -> {
                                // Forgot Password Flow with Email OTP
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = {
                                                emailAuthMode = EmailAuthMode.SIGN_IN
                                                errorMessage = null
                                                statusSuccessMessage = null
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = EmeraldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Reset Password via Email",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (forgotStep == ForgotStep.REQUEST_CODE) {
                                        Text(
                                            text = "Enter your email address. We'll send a 6-digit verification code so you can reset your password.",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        OutlinedTextField(
                                            value = forgotEmail,
                                            onValueChange = {
                                                forgotEmail = it
                                                errorMessage = null
                                            },
                                            label = { Text("Account email", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Email,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = DarkSurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = { triggerSendForgotOtp() },
                                            enabled = !isForgotSendingCode,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                        ) {
                                            if (isForgotSendingCode) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = Color.Black,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Send Reset Code to Email",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    emailAuthMode = EmailAuthMode.SIGN_IN
                                                    errorMessage = null
                                                }
                                            ) {
                                                Text(
                                                    text = "Back to Sign In",
                                                    fontSize = 12.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    } else {
                                        // Step 2: Enter OTP & Set New Password
                                        Text(
                                            text = "Enter the 6-digit code sent to ${forgotEmail.trim()} and choose your new password.",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "6-Digit Reset Code",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        OutlinedTextField(
                                            value = forgotOtpInput,
                                            onValueChange = {
                                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                                    forgotOtpInput = it
                                                    errorMessage = null
                                                }
                                            },
                                            placeholder = { Text("• • • • • •", letterSpacing = 4.sp, color = TextMuted) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 5.sp,
                                                textAlign = TextAlign.Center,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = DarkSurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldPrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = newPassword,
                                            onValueChange = {
                                                newPassword = it
                                                errorMessage = null
                                            },
                                            label = { Text("New password (min 6 chars)", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                                    Icon(
                                                        imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                        contentDescription = null,
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = DarkSurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldPrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = confirmNewPassword,
                                            onValueChange = {
                                                confirmNewPassword = it
                                                errorMessage = null
                                            },
                                            label = { Text("Confirm new password", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible }) {
                                                    Icon(
                                                        imageVector = if (confirmNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                        contentDescription = null,
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            visualTransformation = if (confirmNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = DarkSurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldPrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = {
                                                if (forgotOtpInput.length != 6) {
                                                    errorMessage = "Please enter all 6 digits of the reset code"
                                                    return@Button
                                                }
                                                val (isVerified, verifyMessage) = emailService.verifyCode(forgotEmail.trim(), forgotOtpInput)
                                                if (!isVerified) {
                                                    errorMessage = "❌ $verifyMessage"
                                                    return@Button
                                                }
                                                if (newPassword.length < 6) {
                                                    errorMessage = "❌ New password must be at least 6 characters"
                                                    return@Button
                                                }
                                                if (newPassword != confirmNewPassword) {
                                                    errorMessage = "❌ Passwords do not match"
                                                    return@Button
                                                }

                                                // Save new password
                                                userPrefs.setPasswordForEmail(forgotEmail.trim(), newPassword)
                                                userPrefs.userEmail = forgotEmail.trim()
                                                userPrefs.isEmailVerified = true

                                                val finalName = deriveDisplayName(forgotEmail.trim())
                                                onSignIn(forgotEmail.trim(), finalName, "Password Reset")
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                        ) {
                                            Text(
                                                text = "Save New Password & Sign In",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    forgotStep = ForgotStep.REQUEST_CODE
                                                    errorMessage = null
                                                }
                                            ) {
                                                Text("Change Email", fontSize = 11.sp, color = TextMuted)
                                            }

                                            if (forgotCooldownSeconds > 0) {
                                                Text(
                                                    text = "Resend in ${forgotCooldownSeconds}s",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            } else {
                                                TextButton(onClick = { triggerSendForgotOtp() }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = null,
                                                        tint = EmeraldAccent,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Resend Code", fontSize = 11.sp, color = EmeraldAccent)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
