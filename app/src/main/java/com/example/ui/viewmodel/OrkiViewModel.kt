package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.data.audio.AudioManager
import com.example.data.audio.AudioPlayerState
import com.example.data.billing.PlayBillingManager
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.network.ImageGenerationService
import com.example.data.network.VideoGenerationService
import com.example.data.network.OrkiApiService
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.VoiceOptions
import com.example.util.AttachedFile
import com.example.util.FileUploadHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class LiveTalkStatus {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

data class UiState(
    val currentConversationId: String? = null,
    val messages: List<ChatMessageEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val currentStreamingResponse: String = "",
    val isThinking: Boolean = false,
    val isRecording: Boolean = false,
    val liveTalkStatus: LiveTalkStatus = LiveTalkStatus.IDLE,
    val liveTalkTranscript: String = "",
    val script: String = "deva",
    val uiLanguage: String = "deva",
    val userName: String = "Guest",
    val userPersona: String = "",
    val currentPlan: String = "Guest",
    val dailyUsage: Int = 0,
    val dailyLimit: Int = 4,
    val dailyUploadUsage: Int = 0,
    val dailyUploadLimit: Int = 0,
    val attachedFile: AttachedFile? = null,
    val isProcessingFile: Boolean = false,
    val selectedModel: String = "orki-3.0",
    val selectedVoice: String = "female_mainao",
    val isIncognito: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isEmailVerified: Boolean = false,
    val authMethod: String = "Guest",
    val userEmail: String = "",
    val errorMessage: String? = null,
    val isGeneratingImage: Boolean = false,
    val imageGenProgress: Int = 0,
    val imageGenStage: String = "",
    val imageGenPrompt: String = "",
    val imageGenEngine: String = "Orki AI (Cloudflare)",
    val isImageGenFallback: Boolean = false,
    val isGeneratingVideo: Boolean = false,
    val videoGenProgress: Int = 0,
    val videoGenStage: String = "",
    val videoGenPrompt: String = "",
    val videoGenEngine: String = "Json2video AI Studio (Primary)",
    val isVideoGenFallback: Boolean = false,
    val triggerUpgradeDialog: Boolean = false,
    val triggerLoginDialog: Boolean = false
)

class OrkiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.conversationDao()
    private val prefs = UserPreferences(application)
    private val apiService = OrkiApiService()

    val audioManager = AudioManager(application, viewModelScope)
    val billingManager = PlayBillingManager(application, viewModelScope)
    val imageService = ImageGenerationService(application)
    val videoService = VideoGenerationService(application)

    private val activeUserEmailFlow = MutableStateFlow(if (prefs.isLoggedIn) prefs.userEmail.ifBlank { "" } else "")

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<ConversationEntity>> = activeUserEmailFlow
        .flatMapLatest { email ->
            if (email.isBlank()) {
                flowOf(emptyList()) // Guests or logged out users do not see previous user chats
            } else {
                dao.getConversationsForUser(email)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioPlayerState: StateFlow<AudioPlayerState> = audioManager.playerState
    val amplitudeFlow: StateFlow<Float> = audioManager.amplitudeFlow

    private val _uiState = MutableStateFlow(
        UiState(
            script = prefs.script,
            uiLanguage = prefs.uiLanguage,
            userName = if (prefs.isLoggedIn) prefs.userName.ifBlank { "User" } else "Guest",
            userPersona = prefs.userPersona,
            currentPlan = if (prefs.isLoggedIn) prefs.currentPlan else "Guest",
            dailyUsage = prefs.getDailyUsage(if (prefs.isLoggedIn) prefs.userEmail else ""),
            dailyLimit = prefs.getDailyLimit(if (prefs.isLoggedIn) prefs.currentPlan else "Guest"),
            dailyUploadUsage = prefs.getDailyUploadUsage(if (prefs.isLoggedIn) prefs.userEmail else ""),
            dailyUploadLimit = prefs.getDailyUploadLimit(if (prefs.isLoggedIn) prefs.currentPlan else "Guest"),
            selectedModel = prefs.selectedModel,
            selectedVoice = prefs.selectedVoice,
            isIncognito = prefs.isIncognito,
            isLoggedIn = prefs.isLoggedIn,
            isEmailVerified = prefs.isEmailVerified,
            authMethod = if (prefs.isLoggedIn) prefs.authMethod else "Guest",
            userEmail = if (prefs.isLoggedIn) prefs.userEmail else ""
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var activeStreamJob: Job? = null
    private var activeConversationJob: Job? = null

    // In-memory messages for incognito mode
    private val incognitoMessages = mutableListOf<ChatMessageEntity>()

    init {
        viewModelScope.launch {
            apiService.warmUpEdgeFunctions()
        }
        billingManager.startConnection()
        viewModelScope.launch {
            billingManager.purchasedPlan.collect { plan ->
                if (plan != null) {
                    upgradePlan(plan)
                }
            }
        }
        viewModelScope.launch {
            billingManager.billingMessage.collect { msg ->
                _uiState.value = _uiState.value.copy(errorMessage = msg)
            }
        }
    }

    fun startNewChat() {
        cancelGeneration()
        audioManager.stopPlayback()
        _uiState.value = _uiState.value.copy(
            currentConversationId = null,
            messages = emptyList(),
            currentStreamingResponse = ""
        )
    }

    fun selectConversation(id: String) {
        if (_uiState.value.isIncognito) return
        cancelGeneration()
        audioManager.stopPlayback()

        activeConversationJob?.cancel()
        _uiState.value = _uiState.value.copy(
            currentConversationId = id,
            currentStreamingResponse = ""
        )

        activeConversationJob = viewModelScope.launch {
            dao.getMessagesForConversation(id).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            dao.deleteConversation(id)
            if (_uiState.value.currentConversationId == id) {
                startNewChat()
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            dao.clearAllConversations()
            startNewChat()
        }
    }

    fun toggleIncognito() {
        val next = !_uiState.value.isIncognito
        prefs.isIncognito = next
        incognitoMessages.clear()
        _uiState.value = _uiState.value.copy(
            isIncognito = next,
            currentConversationId = null,
            messages = emptyList(),
            currentStreamingResponse = ""
        )
    }

    fun setModel(model: String) {
        if (model == "okafwr-2.1" && _uiState.value.currentPlan != "Pro") {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Okafwr 2.1 requires an active Pro subscription."
            )
            return
        }
        prefs.selectedModel = model
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun updateSettings(script: String, uiLang: String, name: String, persona: String, voice: String) {
        prefs.script = script
        prefs.uiLanguage = uiLang
        prefs.userName = name
        prefs.userPersona = persona
        prefs.selectedVoice = voice
        _uiState.value = _uiState.value.copy(
            script = script,
            uiLanguage = uiLang,
            userName = name,
            userPersona = persona,
            selectedVoice = voice
        )
    }

    fun previewVoice(voiceId: String) {
        val profile = VoiceOptions.get(voiceId)
        audioManager.playRawResource(
            resId = profile.rawResId,
            ttsText = profile.samplePhrase,
            pitch = profile.pitch,
            speed = profile.speed
        )
    }

    fun launchPlayBillingFlow(activity: Activity, plan: String, cycle: String) {
        billingManager.launchPurchaseFlow(activity, plan, cycle)
    }

    fun signIn(email: String, name: String, method: String = "Google", verified: Boolean = true) {
        val cleanEmail = email.trim()
        val displayName = if (name.isNotBlank()) name else (cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() })
        val userPlan = prefs.getPlanForEmail(cleanEmail) ?: "Free"

        prefs.hasExplicitlyLoggedIn = true
        prefs.isLoggedIn = true
        prefs.isEmailVerified = verified
        prefs.authMethod = method
        prefs.userEmail = cleanEmail
        prefs.userName = displayName
        prefs.currentPlan = userPlan

        cancelGeneration()
        audioManager.stopPlayback()
        activeUserEmailFlow.value = cleanEmail

        val limit = prefs.getDailyLimit(userPlan)
        val usage = prefs.getDailyUsage(cleanEmail)
        val uploadLimit = prefs.getDailyUploadLimit(userPlan)
        val uploadUsage = prefs.getDailyUploadUsage(cleanEmail)

        _uiState.value = _uiState.value.copy(
            isLoggedIn = true,
            isEmailVerified = verified,
            authMethod = method,
            userEmail = cleanEmail,
            userName = displayName,
            currentPlan = userPlan,
            dailyLimit = limit,
            dailyUsage = usage,
            dailyUploadLimit = uploadLimit,
            dailyUploadUsage = uploadUsage,
            currentConversationId = null,
            messages = emptyList(),
            currentStreamingResponse = ""
        )
    }

    fun signOut() {
        cancelGeneration()
        audioManager.stopPlayback()

        prefs.hasExplicitlyLoggedIn = false
        prefs.isLoggedIn = false
        prefs.isEmailVerified = false
        prefs.authMethod = "Guest"
        prefs.userEmail = ""
        prefs.userName = "Guest"
        prefs.currentPlan = "Guest"
        prefs.resetGuestUsage()

        activeUserEmailFlow.value = ""

        val guestLimit = prefs.getDailyLimit("Guest")
        val guestUploadLimit = prefs.getDailyUploadLimit("Guest")

        _uiState.value = _uiState.value.copy(
            isLoggedIn = false,
            isEmailVerified = false,
            authMethod = "Guest",
            userEmail = "",
            userName = "Guest",
            currentPlan = "Guest",
            dailyLimit = guestLimit,
            dailyUsage = 0,
            dailyUploadLimit = guestUploadLimit,
            dailyUploadUsage = 0,
            currentConversationId = null,
            messages = emptyList(),
            currentStreamingResponse = ""
        )
    }

    fun upgradePlan(plan: String) {
        prefs.currentPlan = plan
        if (_uiState.value.userEmail.isNotBlank()) {
            prefs.setPlanForEmail(_uiState.value.userEmail, plan)
        }
        val newLimit = prefs.getDailyLimit(plan)
        val newUploadLimit = prefs.getDailyUploadLimit(plan)
        _uiState.value = _uiState.value.copy(
            currentPlan = plan,
            dailyLimit = newLimit,
            dailyUploadLimit = newUploadLimit
        )
    }

    fun attachFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingFile = true)
            val attached = FileUploadHelper.processUri(getApplication(), uri)
            if (attached != null) {
                _uiState.value = _uiState.value.copy(
                    attachedFile = attached,
                    isProcessingFile = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isProcessingFile = false,
                    errorMessage = "Could not process file. Please select a valid Image, TXT, or PDF file."
                )
            }
        }
    }

    fun removeAttachedFile() {
        _uiState.value = _uiState.value.copy(attachedFile = null)
    }

    fun checkUploadQuota(): Boolean {
        if (_uiState.value.currentPlan == "Guest" || !_uiState.value.isLoggedIn) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "File attachments and uploads are reserved for registered users. Please sign in or create an account.",
                triggerLoginDialog = true
            )
            return false
        }
        val currentEmail = _uiState.value.userEmail
        if (!prefs.canUpload(_uiState.value.currentPlan, currentEmail)) {
            val limit = prefs.getDailyUploadLimit(_uiState.value.currentPlan)
            val plan = _uiState.value.currentPlan
            val msg = if (plan == "Free") {
                "Free upload limit reached ($limit/2 uploads used today). Upgrade to Plus or Pro for more uploads!"
            } else {
                "$plan upload limit of $limit reached today. Upgrade to Pro for maximum uploads!"
            }
            _uiState.value = _uiState.value.copy(
                errorMessage = msg,
                triggerUpgradeDialog = true
            )
            return false
        }
        return true
    }

    fun clearTriggerUpgradeDialog() {
        _uiState.value = _uiState.value.copy(triggerUpgradeDialog = false)
    }

    fun clearTriggerLoginDialog() {
        _uiState.value = _uiState.value.copy(triggerLoginDialog = false)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun checkQuota(): Boolean {
        val currentEmail = if (_uiState.value.isLoggedIn) _uiState.value.userEmail else ""
        val currentUsage = prefs.getDailyUsage(currentEmail)
        val limit = prefs.getDailyLimit(_uiState.value.currentPlan)
        if (currentUsage >= limit) {
            val isGuest = _uiState.value.currentPlan == "Guest" || !_uiState.value.isLoggedIn
            val msg = if (isGuest) {
                "Guest limit reached (4/4 messages). Please sign in or create a free account to continue chatting!"
            } else {
                "Daily limit of $limit messages reached. Upgrade your plan for more quota!"
            }
            _uiState.value = _uiState.value.copy(
                errorMessage = msg,
                triggerLoginDialog = isGuest,
                triggerUpgradeDialog = !isGuest
            )
            return false
        }
        return true
    }

    data class VisualIntent(val type: String, val prompt: String)

    private fun extractVisualIntent(rawText: String): VisualIntent? {
        val t = rawText.trim()
        if (t.isEmpty()) return null
        val lower = t.lowercase()

        // 1. Explicit Slash Commands
        val slashVideoRegex = Regex("^/(video|vid|clip)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val slashVideoMatch = slashVideoRegex.find(t)
        if (slashVideoMatch != null) {
            return VisualIntent("video", slashVideoMatch.groupValues[2].trim())
        }

        val slashImageRegex = Regex("^/(image|imagine|draw|img)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val slashImageMatch = slashImageRegex.find(t)
        if (slashImageMatch != null) {
            return VisualIntent("image", slashImageMatch.groupValues[2].trim())
        }

        // 2. Natural Video Request Intents (Handles 'can you create a video of', 'make a video', typos like 'vedio', etc.)
        val videoPattern = Regex(
            "^(?:hey\\s+orki\\s*,?\\s*|can\\s+you\\s+(?:please\\s+)?|could\\s+you\\s+(?:please\\s+)?|please\\s+|i\\s+want\\s+(?:you\\s+to\\s+|a\\s+)?|make\\s+me\\s+a\\s+|give\\s+me\\s+a\\s+|show\\s+me\\s+a\\s+)?(?:generate|create|ceeate|make|render|produce|show|give)?\\s*(?:an?\\s+)?(?:ai\\s+)?(?:video|clip|movie|animation|vedio|vidoe)\\s*(?:of|about|showing|with|for|depicting)?\\s*(.+)$",
            RegexOption.IGNORE_CASE
        )
        val videoMatch = videoPattern.find(t)
        if (videoMatch != null && videoMatch.groupValues[1].isNotBlank()) {
            val candidate = videoMatch.groupValues[1].trim()
            if (candidate.length >= 2) {
                return VisualIntent("video", candidate)
            }
        }

        // 3. Natural Image Request Intents (Handles 'create a inage of', 'draw me a', 'picture of', etc.)
        val imagePattern = Regex(
            "^(?:hey\\s+orki\\s*,?\\s*|can\\s+you\\s+(?:please\\s+)?|could\\s+you\\s+(?:please\\s+)?|please\\s+|i\\s+want\\s+(?:you\\s+to\\s+|a\\s+)?|make\\s+me\\s+a\\s+|give\\s+me\\s+a\\s+|show\\s+me\\s+a\\s+)?(?:generate|create|ceeate|make|render|produce|draw|paint|illustrate|show|give)?\\s*(?:an?\\s+)?(?:ai\\s+)?(?:image|inage|picture|piture|photo|artwork|drawing|illustration|painting|wallpaper)\\s*(?:of|about|showing|with|for|depicting)?\\s*(.+)$",
            RegexOption.IGNORE_CASE
        )
        val imageMatch = imagePattern.find(t)
        if (imageMatch != null && imageMatch.groupValues[1].isNotBlank()) {
            val candidate = imageMatch.groupValues[1].trim()
            if (candidate.length >= 2) {
                return VisualIntent("image", candidate)
            }
        }

        // 4. Drawing Commands (e.g. 'draw a cat', 'draw me a girl in dokhona')
        val drawPattern = Regex(
            "^(?:can\\s+you\\s+(?:please\\s+)?|could\\s+you\\s+(?:please\\s+)?|please\\s+)?(?:draw|paint|illustrate)\\s*(?:me\\s+)?(?:an?\\s+)?(.+)$",
            RegexOption.IGNORE_CASE
        )
        val drawMatch = drawPattern.find(t)
        if (drawMatch != null && drawMatch.groupValues[1].isNotBlank()) {
            val candidate = drawMatch.groupValues[1].trim()
            if (candidate.length >= 2) {
                return VisualIntent("image", candidate)
            }
        }

        // 5. Bodo Language Triggers
        if (lower.contains("भिदिअ बानाय") || lower.contains("बानाय भिदिअ")) {
            val clean = t.replace(Regex("भिदिअ|बानाय"), "").trim()
            if (clean.isNotEmpty()) return VisualIntent("video", clean)
        }
        if (lower.contains("छबि बानाय") || lower.contains("बानाय छबि")) {
            val clean = t.replace(Regex("छबि|बानाय"), "").trim()
            if (clean.isNotEmpty()) return VisualIntent("image", clean)
        }

        return null
    }

    fun sendMessage(userText: String) {
        val attachment = _uiState.value.attachedFile
        val trimmed = userText.trim()
        if (trimmed.isEmpty() && attachment == null) return
        if (_uiState.value.isGenerating) return

        val isGuest = _uiState.value.currentPlan == "Guest" || !_uiState.value.isLoggedIn
        if (isGuest) {
            val lower = trimmed.lowercase()
            val isVideoRequest = lower.contains("video") ||
                    lower.contains("animation") ||
                    lower.contains("animate") ||
                    lower.contains("movie") ||
                    lower.contains("clip")
            if (isVideoRequest) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Guest users cannot create videos. Please sign in or create an account to unlock video creations!",
                    triggerLoginDialog = true
                )
                return
            }
        }

        // Intelligent Visual Intent Detection (Natural language Image/Video requests + slash commands)
        val visualIntent = extractVisualIntent(trimmed)
        if (visualIntent != null && attachment == null) {
            if (visualIntent.type == "video") {
                generateVideo(visualIntent.prompt)
                return
            } else if (visualIntent.type == "image") {
                generateImage(visualIntent.prompt)
                return
            }
        }

        if (!checkQuota()) return

        if (attachment != null) {
            if (!checkUploadQuota()) return
        }

        val promptText = if (trimmed.isNotEmpty()) {
            trimmed
        } else when (attachment?.fileType) {
            "image" -> "Please analyze this image and explain what you see."
            "pdf" -> "Please summarize and explain the key details of this PDF document."
            "txt" -> "Please analyze and summarize this text file."
            else -> "Please analyze this attachment."
        }

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = _uiState.value.currentConversationId ?: "",
            role = "user",
            text = promptText,
            attachmentName = attachment?.name,
            attachmentType = attachment?.fileType,
            attachmentSize = attachment?.sizeBytes,
            attachmentUri = attachment?.uri?.toString(),
            timestamp = System.currentTimeMillis()
        )

        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            isGenerating = true,
            isThinking = true,
            currentStreamingResponse = "",
            attachedFile = null
        )

        executeStreamingChat(
            conversationSnapshot = updatedMessages,
            audioPayload = null,
            attachmentPayload = attachment,
            liveMode = false
        )
    }

    fun generateImage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value.isGenerating) return

        if (!checkQuota()) return

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = _uiState.value.currentConversationId ?: "",
            role = "user",
            text = "🎨 /image $trimmed",
            timestamp = System.currentTimeMillis()
        )

        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            isGenerating = true,
            isThinking = false,
            isGeneratingImage = true,
            imageGenProgress = 6,
            imageGenStage = "Connecting to Orki AI Worker...",
            imageGenPrompt = trimmed,
            currentStreamingResponse = ""
        )

        activeStreamJob = viewModelScope.launch {
            // Step 1: Gemini AI Middleman - Enhance the prompt with authentic cultural & visual details
            _uiState.value = _uiState.value.copy(
                imageGenProgress = 12,
                imageGenStage = "✨ Gemini AI: Enhancing prompt with cultural & visual details..."
            )

            val enhancedPrompt = try {
                apiService.enhanceVisualPrompt(trimmed, isVideo = false)
            } catch (_: Exception) {
                trimmed
            }

            _uiState.value = _uiState.value.copy(
                imageGenProgress = 25,
                imageGenStage = "Connecting to Orki AI GPU Cluster..."
            )

            // Live progressive ticker simulating real-time diffusion stages up to 96%
            val tickerJob = launch {
                val milestones = listOf(
                    40 to "Encoding prompt into text latents...",
                    60 to "Sampling diffusion steps (GPU Pass 1)...",
                    78 to "Denoising & refining composition...",
                    88 to "Enhancing details & color grading...",
                    94 to "Finalizing PNG artwork bytes..."
                )
                var currentPercent = 25
                for ((targetPercent, stageText) in milestones) {
                    _uiState.value = _uiState.value.copy(imageGenStage = stageText)
                    while (currentPercent < targetPercent) {
                        currentPercent += (1..3).random()
                        if (currentPercent > targetPercent) currentPercent = targetPercent
                        _uiState.value = _uiState.value.copy(imageGenProgress = currentPercent)
                        delay((180L..340L).random())
                    }
                }
                while (currentPercent < 97) {
                    delay(450)
                    currentPercent += 1
                    _uiState.value = _uiState.value.copy(imageGenProgress = currentPercent)
                }
            }

            try {
                val result = imageService.generateImage(
                    prompt = enhancedPrompt,
                    workerUrl = prefs.imageWorkerUrl,
                    apiKey = prefs.imageApiKey,
                    pollinationsKey = prefs.pollinationsApiKey,
                    onStatusUpdate = { stageText, isFallback ->
                        _uiState.value = _uiState.value.copy(
                            imageGenStage = stageText,
                            isImageGenFallback = isFallback,
                            imageGenEngine = if (isFallback) "Pollinations AI (Backup)" else "Orki AI (Cloudflare)"
                        )
                    }
                )
                tickerJob.cancel()

                result.onSuccess { generated ->
                    val imageFile = generated.file
                    _uiState.value = _uiState.value.copy(
                        imageGenProgress = 100,
                        imageGenStage = if (generated.isFallback) "Artwork generated via Pollinations AI backup!" else "Artwork generated! Rendering..."
                    )
                    delay(250)

                    val responseText = if (enhancedPrompt.equals(trimmed, ignoreCase = true)) {
                        if (generated.isFallback) {
                            "Here is your generated image for: \"$trimmed\"\n*(Generated via Pollinations AI backup engine)*"
                        } else {
                            "Here is your generated image for: \"$trimmed\""
                        }
                    } else {
                        if (generated.isFallback) {
                            "Here is your generated image for: \"$trimmed\"\n\n✨ **Enhanced prompt used:**\n$enhancedPrompt\n\n*(Generated via Pollinations AI backup engine)*"
                        } else {
                            "Here is your generated image for: \"$trimmed\"\n\n✨ **Enhanced prompt used:**\n$enhancedPrompt"
                        }
                    }

                    val assistantMessage = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = _uiState.value.currentConversationId ?: "",
                        role = "model",
                        text = responseText,
                        attachmentName = imageFile.name,
                        attachmentType = "generated_image",
                        attachmentSize = imageFile.length(),
                        attachmentUri = imageFile.absolutePath,
                        timestamp = System.currentTimeMillis()
                    )

                    val finalizedMessages = _uiState.value.messages + assistantMessage
                    val activeEmail = if (_uiState.value.isLoggedIn) _uiState.value.userEmail else ""
                    val newUsage = prefs.incrementDailyUsage(activeEmail)

                    _uiState.value = _uiState.value.copy(
                        messages = finalizedMessages,
                        isGenerating = false,
                        isThinking = false,
                        isGeneratingImage = false,
                        imageGenProgress = 0,
                        imageGenStage = "",
                        imageGenPrompt = "",
                        isImageGenFallback = false,
                        imageGenEngine = "Orki AI (Cloudflare)",
                        dailyUsage = newUsage
                    )

                    persistThread(finalizedMessages)
                }.onFailure { error ->
                    val assistantError = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = _uiState.value.currentConversationId ?: "",
                        role = "model",
                        text = "⚠️ Image generation failed: ${error.localizedMessage ?: "Unknown error"}. Both Cloudflare and Pollinations AI were attempted.",
                        timestamp = System.currentTimeMillis()
                    )
                    val finalizedMessages = _uiState.value.messages + assistantError
                    _uiState.value = _uiState.value.copy(
                        messages = finalizedMessages,
                        isGenerating = false,
                        isThinking = false,
                        isGeneratingImage = false,
                        imageGenProgress = 0,
                        imageGenStage = "",
                        imageGenPrompt = "",
                        isImageGenFallback = false,
                        imageGenEngine = "Orki AI (Cloudflare)",
                        errorMessage = "Image generation failed: ${error.localizedMessage}"
                    )
                    persistThread(finalizedMessages)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                tickerJob.cancel()
                throw e
            } catch (e: Exception) {
                tickerJob.cancel()
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    isThinking = false,
                    isGeneratingImage = false,
                    imageGenProgress = 0,
                    imageGenStage = "",
                    imageGenPrompt = "",
                    isImageGenFallback = false,
                    imageGenEngine = "Orki AI (Cloudflare)"
                )
            }
        }
    }

    fun generateVideo(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        // Concurrency Guard: Only 1 video request at a time
        if (_uiState.value.isGeneratingVideo) {
            val busyMessage = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = _uiState.value.currentConversationId ?: "",
                role = "model",
                text = "⚠️ Video generation server is currently busy with another render. On this tier, only 1 video can be processed at a time. Please wait a few moments and try again!",
                timestamp = System.currentTimeMillis()
            )
            val updatedMessages = _uiState.value.messages + busyMessage
            _uiState.value = _uiState.value.copy(messages = updatedMessages)
            viewModelScope.launch { persistThread(updatedMessages) }
            return
        }

        if (_uiState.value.isGenerating) return
        if (!checkQuota()) return

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = _uiState.value.currentConversationId ?: "",
            role = "user",
            text = "🎬 /video $trimmed",
            timestamp = System.currentTimeMillis()
        )

        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            isGenerating = true,
            isThinking = false,
            isGeneratingVideo = true,
            videoGenProgress = 10,
            videoGenStage = "Initializing Json2video AI Studio...",
            videoGenPrompt = trimmed,
            videoGenEngine = "Json2video AI Studio (Primary)",
            isVideoGenFallback = false,
            currentStreamingResponse = ""
        )

        activeStreamJob = viewModelScope.launch {
            try {
                // Step 1: Gemini AI Middleman - Enhance video prompt with camera direction & cultural details
                _uiState.value = _uiState.value.copy(
                    videoGenProgress = 15,
                    videoGenStage = "✨ Gemini AI: Enhancing video prompt & camera keyframes..."
                )

                val enhancedPrompt = try {
                    apiService.enhanceVisualPrompt(trimmed, isVideo = true)
                } catch (_: Exception) {
                    trimmed
                }

                _uiState.value = _uiState.value.copy(
                    videoGenProgress = 25,
                    videoGenStage = "Submitting scene & camera keyframes to Json2video..."
                )

                val result = videoService.generateVideo(
                    prompt = enhancedPrompt,
                    json2videoKey = prefs.json2videoApiKey,
                    bytezKey = prefs.bytezApiKey,
                    onStatusUpdate = { progress, stageText, isFallback ->
                        _uiState.value = _uiState.value.copy(
                            videoGenProgress = progress,
                            videoGenStage = stageText,
                            isVideoGenFallback = isFallback,
                            videoGenEngine = if (isFallback) "Bytez/Pollinations (Backup)" else "Json2video AI Studio (Primary)"
                        )
                    }
                )

                result.onSuccess { generated ->
                    val videoFile = generated.file
                    _uiState.value = _uiState.value.copy(
                        videoGenProgress = 100,
                        videoGenStage = if (generated.isFallback) "Video generated via backup engine!" else "Video render complete! Ready to play."
                    )
                    delay(300)

                    val responseText = if (enhancedPrompt.equals(trimmed, ignoreCase = true)) {
                        if (generated.isFallback) {
                            "Here is your 8s AI video for: \"$trimmed\"\n*(Generated via Backup Video engine)*"
                        } else {
                            "Here is your 8s AI video for: \"$trimmed\""
                        }
                    } else {
                        if (generated.isFallback) {
                            "Here is your 8s AI video for: \"$trimmed\"\n\n✨ **Enhanced prompt used:**\n$enhancedPrompt\n\n*(Generated via Backup Video engine)*"
                        } else {
                            "Here is your 8s AI video for: \"$trimmed\"\n\n✨ **Enhanced prompt used:**\n$enhancedPrompt"
                        }
                    }

                    val assistantMessage = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = _uiState.value.currentConversationId ?: "",
                        role = "model",
                        text = responseText,
                        attachmentName = generated.thumbnailUrl ?: videoFile.name,
                        attachmentType = "generated_video",
                        attachmentSize = videoFile.length(),
                        attachmentUri = videoFile.absolutePath,
                        timestamp = System.currentTimeMillis()
                    )

                    val finalizedMessages = _uiState.value.messages + assistantMessage
                    val activeEmail = if (_uiState.value.isLoggedIn) _uiState.value.userEmail else ""
                    val newUsage = prefs.incrementDailyUsage(activeEmail)

                    _uiState.value = _uiState.value.copy(
                        messages = finalizedMessages,
                        isGenerating = false,
                        isThinking = false,
                        isGeneratingVideo = false,
                        videoGenProgress = 0,
                        videoGenStage = "",
                        videoGenPrompt = "",
                        isVideoGenFallback = false,
                        videoGenEngine = "Json2video AI Studio (Primary)",
                        dailyUsage = newUsage
                    )

                    persistThread(finalizedMessages)
                }.onFailure { error ->
                    val assistantError = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = _uiState.value.currentConversationId ?: "",
                        role = "model",
                        text = "⚠️ Video generation failed: ${error.localizedMessage ?: "Unknown error"}. Please check your quota or try another prompt.",
                        timestamp = System.currentTimeMillis()
                    )
                    val finalizedMessages = _uiState.value.messages + assistantError
                    _uiState.value = _uiState.value.copy(
                        messages = finalizedMessages,
                        isGenerating = false,
                        isThinking = false,
                        isGeneratingVideo = false,
                        videoGenProgress = 0,
                        videoGenStage = "",
                        videoGenPrompt = "",
                        isVideoGenFallback = false,
                        videoGenEngine = "Json2video AI Studio (Primary)",
                        errorMessage = "Video generation failed: ${error.localizedMessage}"
                    )
                    persistThread(finalizedMessages)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    isThinking = false,
                    isGeneratingVideo = false,
                    videoGenProgress = 0,
                    videoGenStage = "",
                    videoGenPrompt = "",
                    isVideoGenFallback = false,
                    videoGenEngine = "Json2video AI Studio (Primary)"
                )
            }
        }
    }

    fun sendGeneratedImageToChat(prompt: String, imageFile: File) {
        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = _uiState.value.currentConversationId ?: "",
            role = "user",
            text = "🎨 /image $prompt",
            timestamp = System.currentTimeMillis()
        )
        val assistantMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = _uiState.value.currentConversationId ?: "",
            role = "model",
            text = "Here is your generated image for: \"$prompt\"",
            attachmentName = imageFile.name,
            attachmentType = "generated_image",
            attachmentSize = imageFile.length(),
            attachmentUri = imageFile.absolutePath,
            timestamp = System.currentTimeMillis()
        )
        val updated = _uiState.value.messages + userMessage + assistantMessage
        val activeEmail = if (_uiState.value.isLoggedIn) _uiState.value.userEmail else ""
        val newUsage = prefs.incrementDailyUsage(activeEmail)
        _uiState.value = _uiState.value.copy(
            messages = updated,
            dailyUsage = newUsage
        )
        viewModelScope.launch {
            persistThread(updated)
        }
    }

    fun startVoiceRecording(liveMode: Boolean = false) {
        if (_uiState.value.isGenerating) return
        if (!checkQuota()) return

        audioManager.stopPlayback()
        val started = audioManager.startRecording(
            autoSilenceDetection = liveMode,
            onSilenceDetected = {
                if (_uiState.value.liveTalkStatus == LiveTalkStatus.LISTENING) {
                    stopVoiceRecording(liveMode = true)
                }
            }
        )

        if (started) {
            _uiState.value = _uiState.value.copy(
                isRecording = true,
                liveTalkStatus = if (liveMode) LiveTalkStatus.LISTENING else LiveTalkStatus.IDLE,
                liveTalkTranscript = if (liveMode) "Listening…" else ""
            )
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Could not start audio recorder. Check microphone permissions."
            )
            if (liveMode) stopLiveTalk()
        }
    }

    fun stopVoiceRecording(liveMode: Boolean = false) {
        val file = audioManager.stopRecording()
        _uiState.value = _uiState.value.copy(isRecording = false)

        if (file == null || file.length() < 500) {
            if (liveMode && _uiState.value.liveTalkStatus != LiveTalkStatus.IDLE) {
                // Keep listening in Live Mode if nothing recorded
                startVoiceRecording(liveMode = true)
            }
            return
        }

        val base64Audio = audioManager.fileToBase64(file)
        file.delete()

        if (base64Audio == null) {
            if (liveMode) stopLiveTalk()
            return
        }

        handleAudioSend(base64Audio, "audio/mp4", liveMode)
    }

    private fun handleAudioSend(base64Audio: String, mimeType: String, liveMode: Boolean) {
        val userVoiceMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = _uiState.value.currentConversationId ?: "",
            role = "user",
            text = "🎙️ [Voice Note]",
            timestamp = System.currentTimeMillis()
        )

        val updatedMessages = _uiState.value.messages + userVoiceMsg
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            isGenerating = true,
            isThinking = true,
            currentStreamingResponse = "",
            liveTalkStatus = if (liveMode) LiveTalkStatus.THINKING else LiveTalkStatus.IDLE,
            liveTalkTranscript = if (liveMode) "Thinking…" else ""
        )

        executeStreamingChat(
            conversationSnapshot = updatedMessages,
            audioPayload = Pair(base64Audio, mimeType),
            attachmentPayload = null,
            liveMode = liveMode
        )
    }

    private fun executeStreamingChat(
        conversationSnapshot: List<ChatMessageEntity>,
        audioPayload: Pair<String, String>?,
        attachmentPayload: AttachedFile? = null,
        liveMode: Boolean = false
    ) {
        activeStreamJob?.cancel()
        activeStreamJob = viewModelScope.launch {
            val script = _uiState.value.script
            val scriptConstraint = if (script == "roman") {
                "\n[Constraint:1. Provide the reply in natural, colloquial Roman Bodo (using 'w', 'dong', and keeping English words like 'Spanish', 'phone' as-is).2. Follow immediately with '---TTS---' on a new line, then the EXACT same sentence in Bodo Devanagari script (बर' हांखो) for speech synthesis.Example format:Oi jwmwi! Spanish raylainw nagirdwng nama? ma khobor nwngna?---TTS---ओइ जोमै! स्पेनिस रायलायनो नागिरदों नामा? मा खबर नोंना?]"
            } else {
                "\n[Constraint: Reply strictly in Bodo Devanagari script (बर' हांखो). Never output Roman script.]"
            }
            val livePromptConstraint = if (liveMode) {
                "\n[LIVE VOICE MODE: You are conversing in a live audio call. Keep your answer strictly to 1 or 2 ultra-concise, natural spoken sentences in Bodo. No lists, no bullets, no markdown.]"
            } else ""

            val memoryDepth = when (_uiState.value.currentPlan) {
                "Guest" -> 4
                "Free" -> 6
                "Plus" -> 12
                "Pro" -> 24
                else -> 6
            }

            val windowed = conversationSnapshot.takeLast(memoryDepth)
            val contentsJson = JSONArray()

            windowed.forEachIndexed { index, msg ->
                val obj = JSONObject()
                obj.put("role", if (msg.role == "user") "user" else "model")
                val parts = JSONArray()

                val isLastMsg = (index == windowed.size - 1)
                if (isLastMsg && audioPayload != null) {
                    val audioObj = JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", audioPayload.second)
                            put("data", audioPayload.first)
                        })
                    }
                    parts.put(audioObj)
                    val promptText = "1. Transcribe accurately.\n2. Formulate conversational reply as Orki.\nConstraint: $scriptConstraint$livePromptConstraint\n\nOutput MUST strictly follow:\nTRANSCRIPTION: <exact words>\nRESPONSE: <reply>"
                    parts.put(JSONObject().apply { put("text", promptText) })
                } else if (isLastMsg && attachmentPayload != null) {
                    if ((attachmentPayload.fileType == "image" || attachmentPayload.fileType == "pdf") && attachmentPayload.base64Data != null) {
                        val fileObj = JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", attachmentPayload.mimeType)
                                put("data", attachmentPayload.base64Data)
                            })
                        }
                        parts.put(fileObj)
                        val promptText = "Attached file: ${attachmentPayload.name} (${attachmentPayload.fileType.uppercase()})\n${msg.text}" + scriptConstraint + livePromptConstraint
                        parts.put(JSONObject().apply { put("text", promptText) })
                    } else if (attachmentPayload.fileType == "txt") {
                        val promptText = "[Document: ${attachmentPayload.name}]\n" +
                                (attachmentPayload.textContent ?: "") +
                                "\n[End of Document]\n\n" + msg.text + scriptConstraint + livePromptConstraint
                        parts.put(JSONObject().apply { put("text", promptText) })
                    } else {
                        parts.put(JSONObject().apply { put("text", msg.text + scriptConstraint + livePromptConstraint) })
                    }
                } else {
                    val textWithConstraint = if (isLastMsg && msg.role == "user") {
                        msg.text + scriptConstraint + livePromptConstraint
                    } else {
                        msg.text
                    }
                    parts.put(JSONObject().apply { put("text", textWithConstraint) })
                }

                obj.put("parts", parts)
                contentsJson.put(obj)
            }

            var fullText = ""
            var transcription = ""

            try {
                apiService.streamChat(
                    plan = _uiState.value.currentPlan,
                    model = _uiState.value.selectedModel,
                    script = script,
                    userPersona = buildPersona(),
                    contentsJson = contentsJson,
                    isLiveMode = liveMode
                ).collect { chunk ->
                    fullText += chunk
                    _uiState.value = _uiState.value.copy(
                        isThinking = false,
                        currentStreamingResponse = fullText
                    )

                    if (liveMode) {
                        if (fullText.contains("RESPONSE:")) {
                            val parts = fullText.split("RESPONSE:")
                            val reply = parts.drop(1).joinToString("RESPONSE:").trimStart()
                            val parsed = OrkiApiService.parseDualResponse(reply)
                            _uiState.value = _uiState.value.copy(
                                liveTalkTranscript = parsed.displayText
                            )
                        } else if (fullText.contains("TRANSCRIPTION:")) {
                            val transcriptPart = fullText.replace("TRANSCRIPTION:", "").trim()
                            _uiState.value = _uiState.value.copy(
                                liveTalkTranscript = if (transcriptPart.isNotEmpty()) "\"$transcriptPart\"" else ""
                            )
                        }
                    }
                }

                // Finalize response
                var finalText = fullText
                if (fullText.contains("RESPONSE:")) {
                    val parts = fullText.split("RESPONSE:")
                    transcription = parts[0].replace("TRANSCRIPTION:", "").trim()
                    finalText = parts.drop(1).joinToString("RESPONSE:").trim()
                }

                val dual = OrkiApiService.parseDualResponse(finalText)

                // Update user transcript if voice
                val finalizedMessages = conversationSnapshot.toMutableList()
                if (audioPayload != null && transcription.isNotEmpty()) {
                    val lastUserIdx = finalizedMessages.indexOfLast { it.role == "user" }
                    if (lastUserIdx != -1) {
                        finalizedMessages[lastUserIdx] = finalizedMessages[lastUserIdx].copy(
                            text = "🎙️ \"$transcription\""
                        )
                    }
                }

                val modelMessage = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = _uiState.value.currentConversationId ?: "",
                    role = "model",
                    text = dual.displayText,
                    ttsText = dual.ttsText,
                    timestamp = System.currentTimeMillis()
                )
                finalizedMessages.add(modelMessage)

                val activeEmail = if (_uiState.value.isLoggedIn) _uiState.value.userEmail else ""
                val newUsage = prefs.incrementDailyUsage(activeEmail)
                val newUploadUsage = if (attachmentPayload != null) prefs.incrementDailyUploadUsage(activeEmail) else prefs.getDailyUploadUsage(activeEmail)
                _uiState.value = _uiState.value.copy(
                    messages = finalizedMessages,
                    isGenerating = false,
                    isThinking = false,
                    currentStreamingResponse = "",
                    dailyUsage = newUsage,
                    dailyUploadUsage = newUploadUsage
                )

                // Persist to Room if not incognito
                persistThread(finalizedMessages)

                if (liveMode && _uiState.value.liveTalkStatus != LiveTalkStatus.IDLE) {
                    speakAndResumeLiveTalk(dual.ttsText)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    isThinking = false,
                    currentStreamingResponse = "",
                    errorMessage = "Error: ${e.localizedMessage ?: "Failed to generate response"}"
                )
                if (liveMode) stopLiveTalk()
            }
        }
    }

    private suspend fun persistThread(messages: List<ChatMessageEntity>) {
        if (_uiState.value.isIncognito || messages.isEmpty()) return

        val activeEmail = if (_uiState.value.isLoggedIn) _uiState.value.userEmail.ifBlank { "" } else ""
        var convId = _uiState.value.currentConversationId
        val firstUserText = messages.firstOrNull { it.role == "user" }?.text ?: "New Conversation"
        val title = if (firstUserText.length > 28) firstUserText.take(28) + "…" else firstUserText

        if (convId == null) {
            convId = UUID.randomUUID().toString()
            val conv = ConversationEntity(
                id = convId,
                title = title,
                updatedAt = System.currentTimeMillis(),
                userEmail = activeEmail
            )
            dao.insertConversation(conv)
            _uiState.value = _uiState.value.copy(currentConversationId = convId)
        } else {
            val conv = ConversationEntity(
                id = convId,
                title = title,
                updatedAt = System.currentTimeMillis(),
                userEmail = activeEmail
            )
            dao.insertConversation(conv)
        }

        messages.forEach { msg ->
            dao.insertMessage(msg.copy(conversationId = convId))
        }
    }

    fun playTts(text: String) {
        val clean = OrkiApiService.cleanTextForTTS(text)
        if (clean.isEmpty()) return

        val voice = _uiState.value.selectedVoice
        val profile = VoiceOptions.get(voice)
        val cacheKey = "$voice:$clean"

        // 1. Check in-memory URL cache
        val cachedUrl = audioManager.ttsUrlCache[cacheKey]
        if (cachedUrl != null) {
            audioManager.playAudioUrl(
                url = cachedUrl,
                ttsText = text,
                pitch = profile.pitch,
                speed = profile.speed
            )
            return
        }

        // 2. Check persistent disk cache (instant 0-latency playback)
        val diskPath = audioManager.getCachedTtsPath(clean, voice)
        if (diskPath != null) {
            audioManager.ttsUrlCache[cacheKey] = diskPath
            audioManager.playAudioUrl(
                url = diskPath,
                ttsText = text,
                pitch = profile.pitch,
                speed = profile.speed
            )
            return
        }

        viewModelScope.launch {
            audioManager.setPlayerLoading(text) // Triggers loading state in pill player
            val result = apiService.fetchTtsAudioUrl(clean, voice = voice)
            result.onSuccess { url ->
                // Cache to disk and memory for instant future replays
                val savedDiskPath = audioManager.saveTtsToDiskCache(clean, voice, url)
                val finalPath = savedDiskPath ?: url
                audioManager.ttsUrlCache[cacheKey] = finalPath

                audioManager.playAudioUrl(
                    url = finalPath,
                    ttsText = text,
                    pitch = profile.pitch,
                    speed = profile.speed
                )
            }.onFailure { err ->
                audioManager.stopPlayback()
                val friendlyError = when {
                    err.message?.contains("timeout", ignoreCase = true) == true ->
                        "Server-a som la-gasino... khebseni try khalamfin salte ⏳"
                    err.message?.contains("connect", ignoreCase = true) == true || err.message?.contains("network", ignoreCase = true) == true ->
                        "Internet signal gwiya khuma... wifi aba data check khalam 📡"
                    else ->
                        "TTS error: Voice generate khalamnw haya swi. Khebseni try khalamfin."
                }
                _uiState.value = _uiState.value.copy(errorMessage = friendlyError)
            }
        }
    }

    fun startLiveTalk() {
        _uiState.value = _uiState.value.copy(
            liveTalkStatus = LiveTalkStatus.LISTENING,
            liveTalkTranscript = ""
        )
        startVoiceRecording(liveMode = true)
    }

    fun stopLiveTalk() {
        _uiState.value = _uiState.value.copy(
            liveTalkStatus = LiveTalkStatus.IDLE,
            liveTalkTranscript = ""
        )
        audioManager.stopRecording()
        audioManager.stopPlayback()
    }

    fun onLiveTalkOrbTapped() {
        val currentStatus = _uiState.value.liveTalkStatus
        if (currentStatus == LiveTalkStatus.SPEAKING) {
            // Interrupt assistant speech and immediately start listening again
            audioManager.stopPlayback()
            startVoiceRecording(liveMode = true)
        } else if (currentStatus == LiveTalkStatus.LISTENING) {
            // Force stop recording early (don't wait for silence timer)
            stopVoiceRecording(liveMode = true)
        }
    }

    private fun speakAndResumeLiveTalk(ttsText: String) {
        val clean = OrkiApiService.cleanTextForTTS(ttsText)
        if (clean.isEmpty()) {
            if (_uiState.value.liveTalkStatus != LiveTalkStatus.IDLE) {
                startVoiceRecording(liveMode = true)
            }
            return
        }

        val voice = _uiState.value.selectedVoice
        val profile = VoiceOptions.get(voice)
        val cacheKey = "$voice:$clean"

        _uiState.value = _uiState.value.copy(liveTalkStatus = LiveTalkStatus.SPEAKING)

        val cachedUrl = audioManager.ttsUrlCache[cacheKey]
        if (cachedUrl != null) {
            audioManager.playAudioUrl(
                url = cachedUrl,
                ttsText = ttsText,
                pitch = profile.pitch,
                speed = profile.speed
            ) {
                if (_uiState.value.liveTalkStatus != LiveTalkStatus.IDLE) {
                    startVoiceRecording(liveMode = true)
                }
            }
            return
        }

        viewModelScope.launch {
            val res = apiService.fetchTtsAudioUrl(clean, voice = voice)
            res.onSuccess { url ->
                audioManager.ttsUrlCache[cacheKey] = url
                if (_uiState.value.liveTalkStatus != LiveTalkStatus.IDLE) {
                    audioManager.playAudioUrl(
                        url = url,
                        ttsText = ttsText,
                        pitch = profile.pitch,
                        speed = profile.speed
                    ) {
                        if (_uiState.value.liveTalkStatus != LiveTalkStatus.IDLE) {
                            startVoiceRecording(liveMode = true)
                        }
                    }
                }
            }.onFailure {
                if (_uiState.value.liveTalkStatus != LiveTalkStatus.IDLE) {
                    startVoiceRecording(liveMode = true)
                }
            }
        }
    }

    fun cancelGeneration() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        _uiState.value = _uiState.value.copy(
            isGenerating = false,
            isThinking = false,
            isGeneratingImage = false,
            imageGenProgress = 0,
            imageGenStage = "",
            imageGenPrompt = "",
            isImageGenFallback = false,
            imageGenEngine = "Orki AI (Cloudflare)",
            isGeneratingVideo = false,
            videoGenProgress = 0,
            videoGenStage = "",
            videoGenPrompt = "",
            isVideoGenFallback = false,
            videoGenEngine = "Json2video AI Studio (Primary)"
        )
    }

    private fun buildPersona(): String {
        val namePart = if (_uiState.value.userName.isNotBlank()) {
            "The user's name is ${_uiState.value.userName}; address them by name naturally."
        } else ""
        return listOf(namePart, _uiState.value.userPersona).filter { it.isNotBlank() }.joinToString(" ")
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.destroy()
        billingManager.destroy()
    }
}
