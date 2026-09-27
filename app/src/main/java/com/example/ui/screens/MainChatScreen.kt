package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ChatMessageEntity
import com.example.data.preferences.UiTranslations
import com.example.ui.components.AttachmentPickerDialog
import com.example.ui.components.AudioPlayerPill
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.DrawerContent
import com.example.ui.components.LiveTalkOverlay
import com.example.ui.components.LoginDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.UpgradeDialog
import com.example.ui.theme.AmberPro
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleIncognito
import com.example.ui.theme.PurpleIncognitoLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LiveTalkStatus
import com.example.ui.viewmodel.OrkiViewModel
import kotlinx.coroutines.launch

@Composable
fun MainChatScreen(
    viewModel: OrkiViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val audioPlayerState by viewModel.audioPlayerState.collectAsStateWithLifecycle()
    val amplitude by viewModel.amplitudeFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }

    val strings = UiTranslations.get(uiState.uiLanguage)

    // Trigger upgrade dialog if quota exceeded
    LaunchedEffect(uiState.triggerUpgradeDialog) {
        if (uiState.triggerUpgradeDialog) {
            showUpgradeDialog = true
            viewModel.clearTriggerUpgradeDialog()
        }
    }

    // Photo Picker Launcher (PickVisualMedia - Zero-permission, Google Play Policy Compliant)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFile(uri)
        }
    }

    // Document Picker Launcher (OpenDocument - Zero-permission SAF for PDF and TXT)
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFile(uri)
        }
    }

    // Scroll to bottom when new messages arrive or when response streams
    LaunchedEffect(uiState.messages.size, uiState.currentStreamingResponse) {
        val totalCount = uiState.messages.size + if (uiState.currentStreamingResponse.isNotEmpty()) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    // Display error messages via snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    // Microphone Permission Launcher
    var pendingActionIsLiveMode by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (pendingActionIsLiveMode) {
                viewModel.startLiveTalk()
            } else {
                viewModel.startVoiceRecording(liveMode = false)
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required for voice features.")
            }
        }
    }

    fun requestMicAndExecute(liveMode: Boolean) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        pendingActionIsLiveMode = liveMode
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (liveMode) {
                viewModel.startLiveTalk()
            } else {
                if (uiState.isRecording) {
                    viewModel.stopVoiceRecording(liveMode = false)
                } else {
                    viewModel.startVoiceRecording(liveMode = false)
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentPlan = uiState.currentPlan,
                dailyUsage = uiState.dailyUsage,
                dailyLimit = uiState.dailyLimit,
                dailyUploadUsage = uiState.dailyUploadUsage,
                dailyUploadLimit = uiState.dailyUploadLimit,
                isIncognito = uiState.isIncognito,
                isLoggedIn = uiState.isLoggedIn,
                userEmail = uiState.userEmail,
                userName = uiState.userName,
                conversations = conversations,
                selectedConversationId = uiState.currentConversationId,
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = { id ->
                    viewModel.deleteConversation(id)
                },
                onNewChat = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onToggleIncognito = {
                    viewModel.toggleIncognito()
                },
                onOpenUpgrade = {
                    showUpgradeDialog = true
                    scope.launch { drawerState.close() }
                },
                onOpenSettings = {
                    showSettingsDialog = true
                    scope.launch { drawerState.close() }
                },
                onSignOut = {
                    viewModel.signOut()
                    scope.launch {
                        snackbarHostState.showSnackbar("Signed out. Switched to Guest account.")
                    }
                },
                onOpenLogin = {
                    showLoginDialog = true
                    scope.launch { drawerState.close() }
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = DarkCanvas,
            topBar = {
                // Sleek ChatGPT/Claude style Top App Bar with Model Switcher & New Chat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Drawer Hamburger
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .testTag("menu_button")
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Drawer",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Centered Floating Model Selector Pill (ChatGPT/Claude style)
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF161B18))
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .clickable { showModelDropdown = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.selectedModel == "okafwr-2.1") AmberPro else EmeraldAccent)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = if (uiState.selectedModel == "okafwr-2.1") "Okafwr 2.1 Pro" else "Orki 3.0",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Model",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false },
                            modifier = Modifier
                                .background(DarkSurface)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Orki 3.0", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldAccent)
                                        Text("Ultra-fast conversational Bodo model", fontSize = 11.sp, color = TextMuted)
                                    }
                                },
                                onClick = {
                                    viewModel.setModel("orki-3.0")
                                    showModelDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Okafwr 2.1", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AmberPro)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PRO", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = AmberPro)
                                        }
                                        Text("Advanced reasoning and deep memory", fontSize = 11.sp, color = TextMuted)
                                    }
                                },
                                onClick = {
                                    viewModel.setModel("okafwr-2.1")
                                    showModelDropdown = false
                                }
                            )
                        }
                    }

                    // Right Actions: New Chat Icon Button + Incognito/Plan Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (uiState.isIncognito) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PurpleIncognito.copy(alpha = 0.2f))
                                    .border(1.dp, PurpleIncognito, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = PurpleIncognitoLight,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Incognito",
                                        color = PurpleIncognitoLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // New Chat Button (ChatGPT/Claude top-right icon)
                        IconButton(
                            onClick = { viewModel.startNewChat() },
                            modifier = Modifier
                                .testTag("new_chat_button")
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                // ChatGPT / Claude style Floating Capsule Composer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Attached File Preview Pill
                    AnimatedVisibility(visible = uiState.attachedFile != null) {
                        uiState.attachedFile?.let { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141A17))
                                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (file.fileType) {
                                                    "image" -> Color(0x3310B981)
                                                    "pdf" -> Color(0x33EF4444)
                                                    else -> Color(0x3338BDF8)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (file.fileType) {
                                                "image" -> Icons.Default.Image
                                                "pdf" -> Icons.Default.PictureAsPdf
                                                else -> Icons.Default.Description
                                            },
                                            contentDescription = null,
                                            tint = when (file.fileType) {
                                                "image" -> EmeraldAccent
                                                "pdf" -> Color(0xFFF87171)
                                                else -> Color(0xFF38BDF8)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = file.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${file.formattedSize} • Ready to send",
                                            fontSize = 10.sp,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeAttachedFile() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove file",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF171917))
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Attach File '+' Button
                            IconButton(
                                onClick = {
                                    if (!viewModel.checkUploadQuota()) {
                                        showUpgradeDialog = true
                                    } else {
                                        showAttachMenu = true
                                    }
                                },
                                modifier = Modifier
                                    .testTag("attach_button")
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.attachedFile != null) Color(0x2810B981) else Color(0x1F27272A))
                            ) {
                                if (uiState.isProcessingFile) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = EmeraldAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Attach File",
                                        tint = if (uiState.attachedFile != null) EmeraldAccent else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Text Input Field
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        text = if (uiState.attachedFile != null) "Ask about this file or send…" else strings.inputPlaceholder,
                                        fontSize = 14.sp,
                                        color = TextMuted
                                    )
                                },
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input")
                            )

                            // Action buttons cluster inside capsule
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Live Mode Wave Pill Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x2810B981))
                                        .clickable { requestMicAndExecute(liveMode = true) }
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                        .testTag("live_talk_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Live Talk",
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Live",
                                            color = EmeraldAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Quick Voice Recording Mic Button
                                IconButton(
                                    onClick = { requestMicAndExecute(liveMode = false) },
                                    modifier = Modifier
                                        .testTag("mic_button")
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isRecording) Color(0xFFDC2626) else Color(0x1F27272A))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Record Voice",
                                        tint = if (uiState.isRecording) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Send or Stop Button
                                val canSend = inputText.isNotBlank() || uiState.attachedFile != null
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (uiState.isGenerating) {
                                            viewModel.cancelGeneration()
                                        } else if (canSend) {
                                            viewModel.sendMessage(inputText)
                                            inputText = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .testTag("send_button")
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.isGenerating) Color(0xFFDC2626)
                                            else if (canSend) EmeraldPrimary
                                            else Color(0x2627272A)
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                                        contentDescription = if (uiState.isGenerating) "Stop" else "Send",
                                        tint = if (canSend || uiState.isGenerating) Color.White else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Disclaimer text
                    Text(
                        text = strings.disclaimer,
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp)
                    )
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Messages or ChatGPT/Claude Empty State
                if (uiState.messages.isEmpty() && uiState.currentStreamingResponse.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Glowing AI Sparkle Emblem with signature emerald tint
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            EmeraldPrimary.copy(alpha = 0.35f),
                                            Color(0xFF0D1E16),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(1.5.dp, EmeraldAccent.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Orki AI",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = strings.welcomeTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = strings.welcomeSub,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Suggested Prompt Cards Grid (Claude / ChatGPT style)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val prompt1 = if (uiState.script == "roman") "Khulumbai! Mabwrwi dong?" else "खुलुमबाय! माबोरै दं?"
                            val sub1 = "Start casual friendly conversation"

                            val prompt2 = if (uiState.script == "roman") "Bodo rao-ao khobor ma?" else "बर' रावआव खबर मा?"
                            val sub2 = "Ask what's new in Bodo language & culture"

                            val prompt3 = if (uiState.script == "roman") "Mwnse thunlai khonthai lirna hwdw." else "मोनसे थुनलाइ खन्थाय लिरना हरदो।"
                            val sub3 = "Generate traditional Bodo poetry"

                            ClaudePromptCard(title = prompt1, subtitle = sub1) { viewModel.sendMessage(prompt1) }
                            ClaudePromptCard(title = prompt2, subtitle = sub2) { viewModel.sendMessage(prompt2) }
                            ClaudePromptCard(title = prompt3, subtitle = sub3) { viewModel.sendMessage(prompt3) }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            ChatMessageItem(
                                message = msg,
                                onPlayTts = { text -> viewModel.playTts(text) }
                            )
                        }

                        // Active Streaming Model Bubble (with subtle caret indicator)
                        if (uiState.currentStreamingResponse.isNotEmpty()) {
                            item {
                                val streamingEntity = ChatMessageEntity(
                                    id = "streaming",
                                    conversationId = uiState.currentConversationId ?: "",
                                    role = "model",
                                    text = uiState.currentStreamingResponse + " ▋"
                                )
                                ChatMessageItem(
                                    message = streamingEntity,
                                    onPlayTts = {}
                                )
                            }
                        }

                        // Thinking Indicator
                        if (uiState.isThinking && uiState.currentStreamingResponse.isEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 18.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0C241B))
                                            .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ThinkingDot(0)
                                        ThinkingDot(150)
                                        ThinkingDot(300)
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Audio Player Pill
                AudioPlayerPill(
                    state = audioPlayerState,
                    onTogglePlayPause = { viewModel.audioManager.togglePlayPause() },
                    onSeek = { fraction -> viewModel.audioManager.seekToFraction(fraction) },
                    onClose = { viewModel.audioManager.stopPlayback() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Full-Screen Live Talk Overlay
                LiveTalkOverlay(
                    visible = uiState.liveTalkStatus != LiveTalkStatus.IDLE,
                    status = uiState.liveTalkStatus,
                    transcript = uiState.liveTalkTranscript,
                    amplitude = amplitude,
                    onOrbTapped = { viewModel.onLiveTalkOrbTapped() },
                    onEndLiveTalk = { viewModel.stopLiveTalk() }
                )
            }
        }
    }

    // Settings Dialog with Voice Change Option
    if (showSettingsDialog) {
        SettingsDialog(
            initialScript = uiState.script,
            initialUiLang = uiState.uiLanguage,
            initialName = uiState.userName,
            initialPersona = uiState.userPersona,
            initialVoice = uiState.selectedVoice,
            currentPlan = uiState.currentPlan,
            activePlayingText = if (audioPlayerState.isPlaying) audioPlayerState.activeTtsText else null,
            onPreviewVoice = { voiceId ->
                viewModel.previewVoice(voiceId)
            },
            onDismiss = { showSettingsDialog = false },
            onUpgradeClick = { showUpgradeDialog = true },
            onSave = { script, uiLang, name, persona, voice ->
                viewModel.updateSettings(script, uiLang, name, persona, voice)
            }
        )
    }

    // Upgrade Dialog
    if (showUpgradeDialog) {
        UpgradeDialog(
            currentPlan = uiState.currentPlan,
            onDismiss = { showUpgradeDialog = false },
            onSelectPlanWithCycle = { plan, cycle ->
                val activity = context as? Activity
                if (activity != null) {
                    viewModel.launchPlayBillingFlow(activity, plan, cycle)
                } else {
                    viewModel.upgradePlan(plan)
                }
            }
        )
    }

    // Sign In / Login Dialog
    if (showLoginDialog) {
        LoginDialog(
            suggestedEmail = if (uiState.userEmail.isNotBlank()) uiState.userEmail else "devmightwin@gmail.com",
            suggestedName = if (uiState.userName.isNotBlank() && uiState.userName != "Guest") uiState.userName else "DevD",
            onDismiss = { showLoginDialog = false },
            onSignIn = { email, name, method ->
                viewModel.signIn(email, name, method = method, verified = true)
                scope.launch {
                    snackbarHostState.showSnackbar("Verified & signed in via $method ($email)")
                }
            }
        )
    }

    // Attachment Picker Dialog (Image, PDF, TXT)
    if (showAttachMenu) {
        AttachmentPickerDialog(
            currentPlan = uiState.currentPlan,
            dailyUploadUsage = uiState.dailyUploadUsage,
            dailyUploadLimit = uiState.dailyUploadLimit,
            onSelectImage = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSelectPdf = {
                docPickerLauncher.launch(arrayOf("application/pdf"))
            },
            onSelectText = {
                docPickerLauncher.launch(arrayOf("text/plain", "text/*"))
            },
            onDismiss = { showAttachMenu = false },
            onUpgradeClick = { showUpgradeDialog = true }
        )
    }
}

@Composable
private fun ClaudePromptCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141715))
            .border(1.dp, Color(0x3310B981), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = EmeraldAccent.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ThinkingDot(delayMs: Int) {
    val transition = rememberInfiniteTransition(label = "thinkingDot")
    val scale by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = delayMs),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    Box(
        modifier = Modifier
            .size(7.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(EmeraldAccent)
    )
}
