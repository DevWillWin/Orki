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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.components.ImageGenProgressCard
import com.example.ui.components.VideoGenProgressCard
import com.example.ui.components.LiveTalkOverlay
import com.example.ui.components.LoginDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.UpgradeDialog
import com.example.ui.components.bounceClick
import com.example.ui.theme.AmberPro
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBorder
import com.example.ui.theme.GreenBorderGlow
import com.example.ui.theme.GreenBright
import com.example.ui.theme.GreenHighlight
import com.example.ui.theme.GreenMuted
import com.example.ui.theme.GreenSurfaceElevated
import com.example.ui.theme.GreenSurfaceTint
import com.example.ui.theme.GreenTextMuted
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
    var isImageGenMode by remember { mutableStateOf(false) }
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

    // Trigger login dialog if guest restriction or login required
    LaunchedEffect(uiState.triggerLoginDialog) {
        if (uiState.triggerLoginDialog) {
            showLoginDialog = true
            viewModel.clearTriggerLoginDialog()
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

    // Scroll to bottom when new messages arrive or when response streams or image/video is generating
    LaunchedEffect(uiState.messages.size, uiState.currentStreamingResponse, uiState.isGeneratingImage, uiState.isGeneratingVideo) {
        val totalCount = uiState.messages.size +
            (if (uiState.currentStreamingResponse.isNotEmpty()) 1 else 0) +
            (if (uiState.isGeneratingImage) 1 else 0) +
            (if (uiState.isGeneratingVideo) 1 else 0)
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Drawer Hamburger
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .testTag("menu_button")
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                            .bounceClick()
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
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
                                .bounceClick { showModelDropdown = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.selectedModel == "okafwr-2.1") AmberPro else GreenHighlight)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.selectedModel == "okafwr-2.1") "Okafwr 2.1 Pro" else "Orki 3.0",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Orki 3.0", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.isIncognito) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PurpleIncognito.copy(alpha = 0.2f))
                                    .border(1.dp, PurpleIncognito, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = PurpleIncognitoLight,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                .bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = TextPrimary,
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Attached File Preview Pill
                    AnimatedVisibility(visible = uiState.attachedFile != null) {
                        uiState.attachedFile?.let { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (file.fileType) {
                                                    "image" -> Color(0x2210B981)
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
                                                "image" -> GreenHighlight
                                                "pdf" -> Color(0xFFF87171)
                                                else -> Color(0xFF38BDF8)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${file.formattedSize} • Ready to send",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeAttachedFile() },
                                    modifier = Modifier.size(24.dp).bounceClick()
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

                    // Removable AI Image Gen Mode Card (Just like uploaded file card)
                    AnimatedVisibility(visible = isImageGenMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, GreenBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x2210B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = GreenHighlight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "AI Image Generation",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Orki AI Worker • Type your prompt below",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GreenTextMuted
                                    )
                                }
                            }
                            IconButton(
                                onClick = { isImageGenMode = false },
                                modifier = Modifier.size(24.dp).bounceClick()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image Gen",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(26.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Attach File '+' Button
                            IconButton(
                                onClick = {
                                    showAttachMenu = true
                                },
                                modifier = Modifier
                                    .testTag("attach_button")
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.attachedFile != null || isImageGenMode) DarkSurfaceElevated else DarkSurface)
                                    .bounceClick()
                            ) {
                                if (uiState.isProcessingFile) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = TextPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Attach File",
                                        tint = if (isImageGenMode) GreenHighlight else TextSecondary,
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
                                        text = when {
                                            isImageGenMode -> "Describe the image to generate…"
                                            uiState.attachedFile != null -> "Ask about this file or send…"
                                            else -> strings.inputPlaceholder
                                        },
                                        fontSize = 14.sp,
                                        color = if (isImageGenMode) GreenHighlight.copy(alpha = 0.7f) else TextMuted
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Live Mode Wave Pill Button - Clean neutral secondary action (Requirement 6)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurfaceElevated)
                                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
                                        .bounceClick { requestMicAndExecute(liveMode = true) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("live_talk_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Live Talk",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Live",
                                            color = TextSecondary,
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isRecording) Color(0xFFDC2626) else DarkSurface)
                                        .bounceClick()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Record Voice",
                                        tint = if (uiState.isRecording) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Send or Stop Button - Solid Green Fill reserved for Primary CTA (Requirement 1 & 6)
                                val canSend = inputText.isNotBlank() || uiState.attachedFile != null
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (uiState.isGenerating) {
                                            viewModel.cancelGeneration()
                                        } else if (canSend) {
                                            if (isImageGenMode) {
                                                viewModel.generateImage(inputText.trim())
                                                inputText = ""
                                                isImageGenMode = false
                                            } else {
                                                viewModel.sendMessage(inputText)
                                                inputText = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .testTag("send_button")
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.isGenerating) Color(0xFFDC2626)
                                            else if (canSend) GreenBright
                                            else DarkSurfaceElevated
                                        )
                                        .bounceClick()
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                                        contentDescription = if (uiState.isGenerating) "Stop" else "Send",
                                        tint = if (canSend) Color.Black else if (uiState.isGenerating) Color.White else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Disclaimer text
                    Text(
                        text = strings.disclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
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
                        // AI Sparkle Emblem with neutral dark surface and subtle border
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkSurfaceBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Orki AI",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Bold larger headline for greeting
                        Text(
                            text = strings.welcomeTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle in neutral secondary gray (Requirement 2)
                        Text(
                            text = strings.welcomeSub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Smaller muted caption text (Requirement 3)
                        Text(
                            text = "Explore Bodo literature, ask questions, or practice conversational speech",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Suggested Prompt Cards Grid (Requirement 4 & 6)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val prompt1 = if (uiState.script == "roman") "Khulumbai! Mabwrwi dong?" else "खुलुमबाय! माबोरै दं?"
                            val sub1 = "Start casual friendly conversation"

                            val prompt2 = if (uiState.script == "roman") "Bodo rao-ao khobor ma?" else "बर' रावआव खबर मा?"
                            val sub2 = "Ask what's new in Bodo language & culture"

                            val prompt3 = if (uiState.script == "roman") "Mwnse thunlai khonthai lirna hwdw." else "मोनसे थुनलाइ खन्थाय लिरना हरदो।"
                            val sub3 = "Generate traditional Bodo poetry"

                            CustomSuggestionCard(
                                title = prompt1,
                                subtitle = sub1,
                                icon = Icons.Default.AutoAwesome
                            ) { viewModel.sendMessage(prompt1) }

                            CustomSuggestionCard(
                                title = prompt2,
                                subtitle = sub2,
                                icon = Icons.Default.Explore
                            ) { viewModel.sendMessage(prompt2) }

                            CustomSuggestionCard(
                                title = prompt3,
                                subtitle = sub3,
                                icon = Icons.Default.MenuBook
                            ) { viewModel.sendMessage(prompt3) }
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

                        // Real-time AI Image Generation Card with live progress percentage
                        if (uiState.isGeneratingImage) {
                            item(key = "image_gen_progress") {
                                ImageGenProgressCard(
                                    prompt = uiState.imageGenPrompt,
                                    progress = uiState.imageGenProgress,
                                    stage = uiState.imageGenStage,
                                    engineName = uiState.imageGenEngine,
                                    isFallback = uiState.isImageGenFallback,
                                    onCancel = { viewModel.cancelGeneration() }
                                )
                            }
                        }

                        // Real-time AI Video Generation Card with live progress percentage
                        if (uiState.isGeneratingVideo) {
                            item(key = "video_gen_progress") {
                                VideoGenProgressCard(
                                    prompt = uiState.videoGenPrompt,
                                    progress = uiState.videoGenProgress,
                                    stage = uiState.videoGenStage,
                                    engineName = uiState.videoGenEngine,
                                    isFallback = uiState.isVideoGenFallback,
                                    onCancel = { viewModel.cancelGeneration() }
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
                                            .background(GreenSurfaceTint)
                                            .border(1.dp, GreenHighlight.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = GreenHighlight,
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
            suggestedEmail = if (uiState.userEmail.isNotBlank()) uiState.userEmail else "",
            suggestedName = if (uiState.userName.isNotBlank() && uiState.userName != "Guest") uiState.userName else "",
            onDismiss = { showLoginDialog = false },
            onSignIn = { email, name, method ->
                viewModel.signIn(email, name, method = method, verified = true)
                scope.launch {
                    snackbarHostState.showSnackbar("Verified & signed in via $method ($email)")
                }
            }
        )
    }

    // Attachment Picker Dialog (Image, PDF, TXT, AI Image Gen, AI Video)
    if (showAttachMenu) {
        AttachmentPickerDialog(
            currentPlan = uiState.currentPlan,
            dailyUploadUsage = uiState.dailyUploadUsage,
            dailyUploadLimit = uiState.dailyUploadLimit,
            onSelectImage = {
                if (viewModel.checkUploadQuota()) {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            },
            onSelectPdf = {
                if (viewModel.checkUploadQuota()) {
                    docPickerLauncher.launch(arrayOf("application/pdf"))
                }
            },
            onSelectText = {
                if (viewModel.checkUploadQuota()) {
                    docPickerLauncher.launch(arrayOf("text/plain", "text/*"))
                }
            },
            onOpenImageGenerator = {
                isImageGenMode = true
            },
            onVideoCreationClick = {
                inputText = "/video "
                scope.launch {
                    snackbarHostState.showSnackbar("🎬 Type your prompt after /video and tap Send to render an 8s video!")
                }
            },
            onDismiss = { showAttachMenu = false },
            onUpgradeClick = { showUpgradeDialog = true }
        )
    }
}

/**
 * Custom Suggestion Card (Requirement 4)
 * - Rounded corners (16-20dp) -> 18dp
 * - Subtle shadow/glow in green
 * - Icon on the left
 * - Tinted dark-green background instead of just an outline
 * - Ripple effect on tap with spring bounce press feedback
 */
@Composable
private fun CustomSuggestionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .bounceClick(scaleDown = 0.98f, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon on the left in clean neutral circular container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkBorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
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
            .background(GreenHighlight)
    )
}
