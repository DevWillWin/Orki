package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.preferences.VoiceOptions
import com.example.ui.theme.AmberPro
import com.example.ui.theme.AmberProLight
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBright
import com.example.ui.theme.GreenHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    initialScript: String,
    initialUiLang: String,
    initialName: String,
    initialPersona: String,
    initialVoice: String,
    currentPlan: String = "Free",
    activePlayingText: String? = null,
    onPreviewVoice: (String) -> Unit,
    onDismiss: () -> Unit,
    onUpgradeClick: (() -> Unit)? = null,
    onSave: (script: String, uiLang: String, name: String, persona: String, voice: String) -> Unit
) {
    var script by remember { mutableStateOf(initialScript) }
    var uiLang by remember { mutableStateOf(initialUiLang) }
    var name by remember { mutableStateOf(initialName) }
    var persona by remember { mutableStateOf(initialPersona) }
    var voice by remember { mutableStateOf(initialVoice) }
    var lastPreviewedVoiceId by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

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
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header (ChatGPT & Claude Style: Clean Title with Close Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkSurfaceBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Preferences, speech voice & personalization",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 38.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Subtle divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DarkSurfaceBorder)
                )

                // Content Sections (Categorized Grouped Cards)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // SECTION 1: VOICE PREFERENCE (The Star Feature)
                    SettingsSection(title = "VOICE & SPEECH") {
                        Text(
                            text = "Choose the voice model used for audio playback & Live Talk.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VoiceOptions.ALL.forEach { profile ->
                                val isSelected = profile.id == voice
                                val isCurrentlyPlaying = (lastPreviewedVoiceId == profile.id && activePlayingText != null)

                                VoiceOptionCard(
                                    profileName = profile.name,
                                    profileDesc = profile.description,
                                    samplePhrase = profile.samplePhrase,
                                    isSelected = isSelected,
                                    isPlaying = isCurrentlyPlaying,
                                    onSelect = { voice = profile.id },
                                    onPlayPreview = {
                                        lastPreviewedVoiceId = profile.id
                                        onPreviewVoice(profile.id)
                                    }
                                )
                            }
                        }
                    }

                    // SECTION 2: PERSONALIZATION
                    SettingsSection(title = "PERSONALIZATION") {
                        // User Name
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "What should Orki call you?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = {
                                    Text("e.g., Rahul, Priya, Bodo Friend…", fontSize = 13.sp, color = TextMuted)
                                },
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
                                    focusedBorderColor = DarkSurfaceBorder,
                                    unfocusedBorderColor = DarkSurfaceBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = GreenBright,
                                    focusedContainerColor = DarkCanvas,
                                    unfocusedContainerColor = DarkCanvas
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Instructions / Persona
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Custom Instructions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Provide background or guidelines Orki should follow when answering.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            OutlinedTextField(
                                value = persona,
                                onValueChange = { persona = it },
                                placeholder = {
                                    Text(
                                        "e.g., Speak like a friendly tutor, keep answers short and simple…",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                },
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DarkSurfaceBorder,
                                    unfocusedBorderColor = DarkSurfaceBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = GreenBright,
                                    focusedContainerColor = DarkCanvas,
                                    unfocusedContainerColor = DarkCanvas
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 3: APP LANGUAGE & SCRIPT
                    SettingsSection(title = "LANGUAGE & SCRIPT") {
                        // Interface Language
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "App Interface Language",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkCanvas)
                                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SegmentedPill(
                                    label = "ब Devanagari",
                                    selected = uiLang == "deva",
                                    modifier = Modifier.weight(1f)
                                ) { uiLang = "deva" }

                                SegmentedPill(
                                    label = "R Roman",
                                    selected = uiLang == "roman",
                                    modifier = Modifier.weight(1f)
                                ) { uiLang = "roman" }

                                SegmentedPill(
                                    label = "EN English",
                                    selected = uiLang == "en",
                                    modifier = Modifier.weight(1f)
                                ) { uiLang = "en" }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // AI Output Response Script
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI Response Script",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (script == "deva") "बर' फरायनो" else "Roman script",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkCanvas)
                                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SegmentedPill(
                                    label = "ब Devanagari",
                                    selected = script == "deva",
                                    modifier = Modifier.weight(1f)
                                ) { script = "deva" }

                                SegmentedPill(
                                    label = "R Roman Bodo",
                                    selected = script == "roman",
                                    modifier = Modifier.weight(1f)
                                ) { script = "roman" }
                            }
                        }
                    }

                    // SECTION 4: MEMBERSHIP & PLAN STATUS
                    SettingsSection(title = "ACCOUNT & SUBSCRIPTION") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkCanvas)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Current Plan:",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (currentPlan == "Pro") AmberPro.copy(alpha = 0.2f)
                                                else DarkSurfaceVariant
                                            )
                                            .border(1.dp, DarkBorderSubtle, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = currentPlan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentPlan == "Pro") AmberPro else TextSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = if (currentPlan == "Pro") "Okafwr 2.1 Deep Reasoning Active"
                                    else if (currentPlan == "Plus") "Extended Context & Voice Access"
                                    else "Standard daily conversation quota",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            if (currentPlan != "Pro" && onUpgradeClick != null) {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onUpgradeClick()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Upgrade",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 5: AI IMAGE GENERATION (DUAL ENGINE)
                    SettingsSection(title = "AI IMAGE GENERATION (DUAL ENGINE)") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkCanvas)
                                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x2210B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = GreenHighlight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Primary Engine: Cloudflare Workers AI", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("Worker: orki-img-gen.devmightwin.workers.dev", fontSize = 10.sp, color = TextSecondary)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkCanvas)
                                    .border(1.dp, AmberPro.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x22F59E0B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = AmberPro,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Auto-Fallback Engine: Pollinations.AI", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("Active Fallback • Key: sk_hFio...JS7o", fontSize = 10.sp, color = AmberProLight)
                                }
                            }
                        }
                    }

                    // SECTION 6: AI VIDEO GENERATION (JSON2VIDEO + BYTEZ FALLBACK)
                    SettingsSection(title = "AI VIDEO GENERATION (DUAL ENGINE)") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkCanvas)
                                    .border(1.dp, Color(0x5506B6D4), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x2206B6D4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Primary Engine: Json2video AI Studio", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("600s Quota (~60 videos) • 8s Cinematic MP4 • Key: I8It...q4w", fontSize = 10.sp, color = Color(0xFF67E8F9))
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkCanvas)
                                    .border(1.dp, AmberPro.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x22F59E0B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = AmberPro,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Backup Engine: Bytez / Pollinations Video", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("1 Request at a time limit • Key: f55b...8b51", fontSize = 10.sp, color = AmberProLight)
                                }
                            }
                        }
                    }
                }

                // Sticky Bottom Action Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .border(
                            width = 1.dp,
                            color = DarkSurfaceBorder,
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(text = "Cancel", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onSave(script, uiLang, name.trim(), persona.trim(), voice)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBright),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Save Changes",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun VoiceOptionCard(
    profileName: String,
    profileDesc: String,
    samplePhrase: String,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlayPreview: () -> Unit
) {
    val borderColor = if (isSelected) EmeraldPrimary else DarkSurfaceBorder
    val bgModifier = if (isSelected) {
        Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0x2810B981), Color(0x1010B981))
            )
        )
    } else {
        Modifier.background(DarkCanvas)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(bgModifier)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio Circle
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 5.dp else 1.5.dp,
                        color = if (isSelected) EmeraldPrimary else TextMuted,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profileName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) EmeraldAccent else TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•  $samplePhrase",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Normal
                    )
                }
                Text(
                    text = profileDesc,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Preview Speaker Button (Preloaded Track Player)
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isPlaying) Color(0x3810B981) else DarkSurfaceVariant)
                .border(
                    1.dp,
                    if (isPlaying) EmeraldAccent else DarkSurfaceBorder,
                    RoundedCornerShape(8.dp)
                )
                .clickable(onClick = onPlayPreview)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isPlaying) {
                    AudioWaveAnimation()
                } else {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Preview voice",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = if (isPlaying) "Playing" else "Preview",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlaying) EmeraldAccent else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AudioWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h1.dp)
                .clip(CircleShape)
                .background(EmeraldAccent)
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h2.dp)
                .clip(CircleShape)
                .background(EmeraldAccent)
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h3.dp)
                .clip(CircleShape)
                .background(EmeraldAccent)
        )
    }
}

@Composable
private fun SegmentedPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) DarkSurfaceVariant else Color.Transparent)
            .border(
                1.dp,
                if (selected) DarkSurfaceBorder else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}
