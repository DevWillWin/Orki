package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.network.EmailVerificationService
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthTab {
    EMAIL_OTP,
    GOOGLE,
    PASSWORD
}

private enum class OtpStep {
    REQUEST_CODE,
    VERIFY_CODE
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

    var selectedTab by remember { mutableStateOf(AuthTab.EMAIL_OTP) }
    var email by remember { mutableStateOf(suggestedEmail) }
    var name by remember { mutableStateOf(suggestedName) }
    var password by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }

    var otpStep by remember { mutableStateOf(OtpStep.REQUEST_CODE) }
    var isSendingCode by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusSuccessMessage by remember { mutableStateOf<String?>(null) }
    var fallbackDebugCode by remember { mutableStateOf<String?>(null) }

    // Resend Email API Key configuration
    var showApiKeyConfig by remember { mutableStateOf(false) }
    var resendApiKeyInput by remember { mutableStateOf(emailService.getResendApiKey()) }

    // Cooldown countdown timer
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds -= 1
        }
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

    fun triggerSendOtp() {
        val err = validateEmail(email)
        if (err != null) {
            errorMessage = err
            return
        }

        errorMessage = null
        isSendingCode = true

        scope.launch {
            val result = emailService.sendVerificationCode(
                email = email.trim(),
                recipientName = name.trim().ifEmpty { "User" }
            )
            isSendingCode = false
            if (result.success) {
                cooldownSeconds = 60
                otpStep = OtpStep.VERIFY_CODE
                statusSuccessMessage = if (result.isRealEmailDispatched) {
                    "📬 Real verification email sent to ${email.trim()}! Please check your Inbox and Spam folder."
                } else {
                    "Dispatched to ${email.trim()}. Check inbox/spam or configure Resend API key below."
                }
                // Only provide debug fallback code if real email wasn't dispatched (i.e. no key configured)
                fallbackDebugCode = if (!result.isRealEmailDispatched) result.generatedCode else null
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
                                text = "Authentication & Verification",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Secure identity verification for Orki AI",
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

                // Tab Switcher: Email OTP / Google / Password
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141715))
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // OTP Verification Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == AuthTab.EMAIL_OTP) EmeraldPrimary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                selectedTab = AuthTab.EMAIL_OTP
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Email OTP",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == AuthTab.EMAIL_OTP) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == AuthTab.EMAIL_OTP) EmeraldAccent else TextMuted
                        )
                    }

                    // Google Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == AuthTab.GOOGLE) EmeraldPrimary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                selectedTab = AuthTab.GOOGLE
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Google",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == AuthTab.GOOGLE) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == AuthTab.GOOGLE) EmeraldAccent else TextMuted
                        )
                    }

                    // Password Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == AuthTab.PASSWORD) EmeraldPrimary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                selectedTab = AuthTab.PASSWORD
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Password",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == AuthTab.PASSWORD) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == AuthTab.PASSWORD) EmeraldAccent else TextMuted
                        )
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

                // Success/Info Banner
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

                // CONTENT PER TAB
                when (selectedTab) {
                    AuthTab.GOOGLE -> {
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
                                text = "Sign in directly with your authenticated Google identity. Email verification is automatically granted with zero spam risk.",
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

                    AuthTab.EMAIL_OTP -> {
                        // Two-Step Verification Flow
                        if (otpStep == OtpStep.REQUEST_CODE) {
                            Column {
                                Text(
                                    text = "Step 1: Request 6-Digit Email Code",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "We will send a 6-digit verification code directly to your email inbox to verify ownership.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        errorMessage = null
                                    },
                                    label = { Text("Your real email address", fontSize = 12.sp) },
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
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Display name", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
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
                                    onClick = { triggerSendOtp() },
                                    enabled = !isSendingCode,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    if (isSendingCode) {
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
                                            text = "Send 6-Digit Code to Email",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Collapsible Resend Email Key Config Section
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF161A18))
                                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showApiKeyConfig = !showApiKeyConfig },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = EmeraldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (emailService.getResendApiKey().isNotBlank()) "Real Email Delivery: Active" else "Email Delivery Setup (Resend API)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (emailService.getResendApiKey().isNotBlank()) EmeraldAccent else TextPrimary
                                            )
                                        }

                                        Text(
                                            text = if (showApiKeyConfig) "Hide" else "Setup",
                                            fontSize = 11.sp,
                                            color = EmeraldAccent
                                        )
                                    }

                                    AnimatedVisibility(visible = showApiKeyConfig) {
                                        Column(modifier = Modifier.padding(top = 10.dp)) {
                                            Text(
                                                text = "To deliver verification codes straight to actual Gmail/Yahoo inboxes, enter your free Resend API key (3,000 free emails/mo):",
                                                fontSize = 11.sp,
                                                color = TextMuted,
                                                lineHeight = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedTextField(
                                                value = resendApiKeyInput,
                                                onValueChange = { resendApiKeyInput = it },
                                                label = { Text("Resend API Key (re_...)", fontSize = 11.sp) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Key,
                                                        contentDescription = null,
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
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

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://resend.com"))
                                                        context.startActivity(browserIntent)
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                        contentDescription = null,
                                                        tint = EmeraldAccent,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Get free key (resend.com)", fontSize = 11.sp, color = EmeraldAccent)
                                                }

                                                Button(
                                                    onClick = {
                                                        emailService.setResendApiKey(resendApiKeyInput.trim())
                                                        statusSuccessMessage = if (resendApiKeyInput.isNotBlank()) "API key saved! Real emails will now be sent." else "Cleared API key"
                                                        showApiKeyConfig = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Save Key", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Step 2: VERIFY OTP (CODE IS IN THE USER'S EMAIL, NOT DISPLAYED ON SCREEN)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        otpStep = OtpStep.REQUEST_CODE
                                        errorMessage = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Change email ($email)",
                                        fontSize = 12.sp,
                                        color = EmeraldAccent
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Email Sent Notice Box (Informs user to check Inbox and Spam)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF0D241A))
                                        .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.MarkEmailRead,
                                            contentDescription = null,
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Check Your Email Inbox",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "We sent a 6-digit code to $email.",
                                                fontSize = 12.sp,
                                                color = EmeraldAccent,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "💡 Please also check your Spam or Junk folder if you don't see it within 30 seconds.",
                                                fontSize = 11.sp,
                                                color = TextMuted,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }

                                // If user hasn't configured a Resend key yet, provide a helper note
                                if (fallbackDebugCode != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1A1F1C))
                                            .border(1.dp, Color(0xFF3B4840), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "No Resend key configured yet",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                            Text(
                                                text = "Autofill test code",
                                                fontSize = 10.sp,
                                                color = EmeraldAccent,
                                                modifier = Modifier.clickable {
                                                    otpCodeInput = fallbackDebugCode ?: ""
                                                    errorMessage = null
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Enter 6-Digit Verification Code",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = otpCodeInput,
                                    onValueChange = {
                                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                            otpCodeInput = it
                                            errorMessage = null
                                        }
                                    },
                                    placeholder = { Text("• • • • • •", letterSpacing = 4.sp, color = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 6.sp,
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
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Verify & Sign In Button
                                Button(
                                    onClick = {
                                        if (otpCodeInput.length != 6) {
                                            errorMessage = "Please enter all 6 digits of the code"
                                            return@Button
                                        }
                                        val (isVerified, verifyMessage) = emailService.verifyCode(email.trim(), otpCodeInput)
                                        if (!isVerified) {
                                            errorMessage = "❌ $verifyMessage"
                                            return@Button
                                        }

                                        // Success: Verified email from real mail
                                        val finalName = name.trim().ifEmpty { email.substringBefore("@") }
                                        onSignIn(email.trim(), finalName, "Email OTP")
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Text(
                                        text = "Verify Code & Sign In",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Resend Code Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (cooldownSeconds > 0) {
                                        Text(
                                            text = "Resend code in ${cooldownSeconds}s",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    } else {
                                        TextButton(
                                            onClick = { triggerSendOtp() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = EmeraldAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Resend email code",
                                                fontSize = 11.sp,
                                                color = EmeraldAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AuthTab.PASSWORD -> {
                        // Password Verification
                        Column {
                            Text(
                                text = "Sign In with Password",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Account passwords require at least 6 characters. Authentication is strictly validated.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
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
                                value = password,
                                onValueChange = {
                                    password = it
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
                                visualTransformation = PasswordVisualTransformation(),
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

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    val err = validateEmail(email)
                                    if (err != null) {
                                        errorMessage = err
                                        return@Button
                                    }
                                    if (password.length < 6) {
                                        errorMessage = "❌ Password must be at least 6 characters"
                                        return@Button
                                    }

                                    val finalName = name.trim().ifEmpty { email.substringBefore("@") }
                                    onSignIn(email.trim(), finalName, "Password")
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
                        }
                    }
                }
            }
        }
    }
}
