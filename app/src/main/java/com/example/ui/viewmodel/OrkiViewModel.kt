package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioManager
import com.example.data.audio.AudioPlayerState
import com.example.data.billing.PlayBillingManager
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.network.OrkiApiService
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.VoiceOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val userName: String = "",
    val userPersona: String = "",
    val currentPlan: String = "Free",
    val dailyUsage: Int = 0,
    val dailyLimit: Int = 20,
    val selectedModel: String = "orki-3.0",
    val selectedVoice: String = "female_mainao",
    val isIncognito: Boolean = false,
    val isLoggedIn: Boolean = true,
    val isEmailVerified: Boolean = true,
    val authMethod: String = "Google",
    val userEmail: String = "devmightwin@gmail.com",
    val errorMessage: String? = null
)

class OrkiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.conversationDao()
    private val prefs = UserPreferences(application)
    private val apiService = OrkiApiService()

    val audioManager = AudioManager(application, viewModelScope)
    val billingManager = PlayBillingManager(application, viewModelScope)

    val conversations: StateFlow<List<ConversationEntity>> = dao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioPlayerState: StateFlow<AudioPlayerState> = audioManager.playerState
    val amplitudeFlow: StateFlow<Float> = audioManager.amplitudeFlow

    private val _uiState = MutableStateFlow(
        UiState(
            script = prefs.script,
            uiLanguage = prefs.uiLanguage,
            userName = prefs.userName,
            userPersona = prefs.userPersona,
            currentPlan = prefs.currentPlan,
            dailyUsage = prefs.getDailyUsage(),
            dailyLimit = prefs.getDailyLimit(prefs.currentPlan),
            selectedModel = prefs.selectedModel,
            selectedVoice = prefs.selectedVoice,
            isIncognito = prefs.isIncognito,
            isLoggedIn = prefs.isLoggedIn,
            isEmailVerified = prefs.isEmailVerified,
            authMethod = prefs.authMethod,
            userEmail = prefs.userEmail
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
        prefs.isLoggedIn = true
        prefs.isEmailVerified = verified
        prefs.authMethod = method
        prefs.userEmail = email
        if (name.isNotBlank()) {
            prefs.userName = name
        }
        _uiState.value = _uiState.value.copy(
            isLoggedIn = true,
            isEmailVerified = verified,
            authMethod = method,
            userEmail = email,
            userName = if (name.isNotBlank()) name else prefs.userName
        )
    }

    fun signOut() {
        prefs.isLoggedIn = false
        prefs.isEmailVerified = false
        prefs.authMethod = "Guest"
        prefs.userEmail = ""
        _uiState.value = _uiState.value.copy(
            isLoggedIn = false,
            isEmailVerified = false,
            authMethod = "Guest",
            userEmail = "",
            userName = "Guest"
        )
    }

    fun upgradePlan(plan: String) {
        prefs.currentPlan = plan
        val newLimit = prefs.getDailyLimit(plan)
        _uiState.value = _uiState.value.copy(
            currentPlan = plan,
            dailyLimit = newLimit
        )
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun checkQuota(): Boolean {
        val currentUsage = prefs.getDailyUsage()
        val limit = prefs.getDailyLimit(_uiState.value.currentPlan)
        if (currentUsage >= limit) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Daily limit of $limit messages reached. Upgrade your plan for more quota!"
            )
            return false
        }
        return true
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _uiState.value.isGenerating) return
        if (!checkQuota()) return

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = _uiState.value.currentConversationId ?: "",
            role = "user",
            text = trimmed,
            timestamp = System.currentTimeMillis()
        )

        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            isGenerating = true,
            isThinking = true,
            currentStreamingResponse = ""
        )

        executeStreamingChat(updatedMessages, null)
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

        executeStreamingChat(updatedMessages, Pair(base64Audio, mimeType), liveMode)
    }

    private fun executeStreamingChat(
        conversationSnapshot: List<ChatMessageEntity>,
        audioPayload: Pair<String, String>?,
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

                val newUsage = prefs.incrementDailyUsage()
                _uiState.value = _uiState.value.copy(
                    messages = finalizedMessages,
                    isGenerating = false,
                    isThinking = false,
                    currentStreamingResponse = "",
                    dailyUsage = newUsage
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

        var convId = _uiState.value.currentConversationId
        val firstUserText = messages.firstOrNull { it.role == "user" }?.text ?: "New Conversation"
        val title = if (firstUserText.length > 28) firstUserText.take(28) + "…" else firstUserText

        if (convId == null) {
            convId = UUID.randomUUID().toString()
            val conv = ConversationEntity(id = convId, title = title, updatedAt = System.currentTimeMillis())
            dao.insertConversation(conv)
            _uiState.value = _uiState.value.copy(currentConversationId = convId)
        } else {
            val conv = ConversationEntity(id = convId, title = title, updatedAt = System.currentTimeMillis())
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
            isThinking = false
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
