package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiStrings(
    val welcomeTitle: String,
    val welcomeSub: String,
    val inputPlaceholder: String,
    val newChat: String,
    val conversations: String,
    val dailyQuota: String,
    val disclaimer: String
)

data class VoiceProfile(
    val id: String,
    val name: String,
    val description: String,
    val pitch: Float,
    val speed: Float,
    val rawResId: Int,
    val samplePhrase: String
)

object VoiceOptions {
    val ALL = listOf(
        VoiceProfile(
            id = "female_mainao",
            name = "Mainao",
            description = "Natural & Expressive Female (BRX_F)",
            pitch = 1.0f,
            speed = 1.0f,
            rawResId = com.example.R.raw.voice_preview_female_mainao,
            samplePhrase = "Ang ni mung a Mainao"
        ),
        VoiceProfile(
            id = "male_birphung",
            name = "Birphung",
            description = "Deep & Resonant Male (BRX_M)",
            pitch = 1.0f,
            speed = 1.0f,
            rawResId = com.example.R.raw.voice_preview_male_birphung,
            samplePhrase = "Ang ni mung a Birphung"
        )
    )

    fun get(id: String): VoiceProfile {
        return when (id) {
            "male_birphung", "male_somkhwr", "male" -> ALL[1]
            else -> ALL[0] // "female_mainao", "female_alari", "female", default
        }
    }
}


object UiTranslations {
    val en = UiStrings(
        welcomeTitle = "Welcome",
        welcomeSub = "Start chatting with Orki AI",
        inputPlaceholder = "Message Orki…",
        newChat = "New Chat",
        conversations = "Conversations",
        dailyQuota = "Daily Quota",
        disclaimer = "Orki AI can make mistakes. Always double check."
    )

    val deva = UiStrings(
        welcomeTitle = "बरायबाय",
        welcomeSub = "Orki AI जों सावरायनायखौ जागायदो",
        inputPlaceholder = "Orki नो लिरहर…",
        newChat = "गोदानै सावराय",
        conversations = "सावरायनायफोर",
        dailyQuota = "सानसेनि बाहायनाय",
        disclaimer = "Orki AI आबो गोरोन्थि खालामनो हागौ। खेबसे नायबिजिर फिन।"
    )

    val roman = UiStrings(
        welcomeTitle = "Boraibai",
        welcomeSub = "Orki AI jwng saorainaikow jagaidw",
        inputPlaceholder = "Orki nw lirhor…",
        newChat = "Gwdanwi Saorai",
        conversations = "Saorainaifwr",
        dailyQuota = "Sanseni Bahainai",
        disclaimer = "Orki AI bw gwrwnti khalamnw hagwu. kebse nai bijir fin."
    )

    fun get(lang: String): UiStrings = when (lang) {
        "deva" -> deva
        "roman" -> roman
        else -> en
    }
}

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("orki_ai_prefs", Context.MODE_PRIVATE)

    var script: String
        get() = prefs.getString("arki_script", "deva") ?: "deva"
        set(value) = prefs.edit().putString("arki_script", value).apply()

    var uiLanguage: String
        get() = prefs.getString("arki_ui_lang", "deva") ?: "deva"
        set(value) = prefs.edit().putString("arki_ui_lang", value).apply()

    var userName: String
        get() = prefs.getString("arki_username", "Guest") ?: "Guest"
        set(value) = prefs.edit().putString("arki_username", value).apply()

    var userPersona: String
        get() = prefs.getString("arki_persona", "") ?: ""
        set(value) = prefs.edit().putString("arki_persona", value).apply()

    var currentPlan: String
        get() = prefs.getString("arki_plan", "Guest") ?: "Guest"
        set(value) = prefs.edit().putString("arki_plan", value).apply()

    var selectedModel: String
        get() = prefs.getString("arki_model", "orki-3.0") ?: "orki-3.0"
        set(value) = prefs.edit().putString("arki_model", value).apply()

    var isIncognito: Boolean
        get() = prefs.getBoolean("arki_incognito", false)
        set(value) = prefs.edit().putBoolean("arki_incognito", value).apply()

    var selectedVoice: String
        get() = prefs.getString("arki_voice", "female_mainao") ?: "female_mainao"
        set(value) = prefs.edit().putString("arki_voice", value).apply()

    var hasExplicitlyLoggedIn: Boolean
        get() = prefs.getBoolean("has_explicitly_logged_in", false)
        set(value) = prefs.edit().putBoolean("has_explicitly_logged_in", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("user_logged_in", false) && prefs.getBoolean("has_explicitly_logged_in", false)
        set(value) = prefs.edit().putBoolean("user_logged_in", value).apply()

    var isEmailVerified: Boolean
        get() = prefs.getBoolean("user_email_verified", false)
        set(value) = prefs.edit().putBoolean("user_email_verified", value).apply()

    var authMethod: String
        get() = prefs.getString("user_auth_method", "Guest") ?: "Guest"
        set(value) = prefs.edit().putString("user_auth_method", value).apply()

    var resendApiKey: String
        get() = prefs.getString("resend_api_key", "") ?: ""
        set(value) = prefs.edit().putString("resend_api_key", value).apply()

    var imageWorkerUrl: String
        get() = prefs.getString("image_worker_url", "https://orki-img-gen.devmightwin.workers.dev") ?: "https://orki-img-gen.devmightwin.workers.dev"
        set(value) = prefs.edit().putString("image_worker_url", value).apply()

    var imageApiKey: String
        get() = prefs.getString("image_api_key", "Orki-Image-7xP6-kQ9m-81vL") ?: "Orki-Image-7xP6-kQ9m-81vL"
        set(value) = prefs.edit().putString("image_api_key", value).apply()

    var userEmail: String
        get() = if (isLoggedIn) (prefs.getString("user_email", "") ?: "") else ""
        set(value) = prefs.edit().putString("user_email", value).apply()

    fun getPasswordForEmail(email: String): String? {
        val clean = email.lowercase().trim()
        if (clean.isEmpty()) return null
        val key = "user_pwd_" + clean
        return prefs.getString(key, null)?.trim()
    }

    fun hasPassword(email: String): Boolean {
        return !getPasswordForEmail(email).isNullOrEmpty()
    }

    fun setPasswordForEmail(email: String, pwd: String) {
        val clean = email.lowercase().trim()
        if (clean.isNotEmpty()) {
            val key = "user_pwd_" + clean
            prefs.edit().putString(key, pwd.trim()).commit()
        }
    }

    fun getPlanForEmail(email: String): String? {
        val clean = email.lowercase().trim()
        if (clean.isEmpty()) return null
        return prefs.getString("plan_$clean", null)
    }

    fun setPlanForEmail(email: String, plan: String) {
        val clean = email.lowercase().trim()
        if (clean.isNotEmpty()) {
            prefs.edit().putString("plan_$clean", plan).apply()
        }
    }

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getUsageKey(prefix: String, email: String): String {
        val clean = email.lowercase().trim()
        return if (clean.isEmpty()) "guest_$prefix" else "u_${clean}_$prefix"
    }

    fun getDailyUsage(email: String = userEmail): Int {
        val today = getTodayString()
        val key = getUsageKey("usage", email)
        val lastDate = prefs.getString("${key}_date", "") ?: ""
        return if (lastDate == today) {
            prefs.getInt("${key}_count", 0)
        } else {
            prefs.edit().putString("${key}_date", today).putInt("${key}_count", 0).apply()
            0
        }
    }

    fun incrementDailyUsage(email: String = userEmail): Int {
        val today = getTodayString()
        val key = getUsageKey("usage", email)
        val lastDate = prefs.getString("${key}_date", "") ?: ""
        val current = if (lastDate == today) prefs.getInt("${key}_count", 0) else 0
        val updated = current + 1
        prefs.edit().putString("${key}_date", today).putInt("${key}_count", updated).apply()
        return updated
    }

    fun getDailyLimit(plan: String): Int {
        return when (plan) {
            "Guest" -> 4  // User request: guests can only write 4 messages
            "Free" -> 15
            "Plus" -> 50
            "Pro" -> 150
            else -> 4
        }
    }

    fun getDailyUploadUsage(email: String = userEmail): Int {
        val today = getTodayString()
        val key = getUsageKey("upload", email)
        val lastDate = prefs.getString("${key}_date", "") ?: ""
        return if (lastDate == today) {
            prefs.getInt("${key}_count", 0)
        } else {
            prefs.edit().putString("${key}_date", today).putInt("${key}_count", 0).apply()
            0
        }
    }

    fun incrementDailyUploadUsage(email: String = userEmail): Int {
        val today = getTodayString()
        val key = getUsageKey("upload", email)
        val lastDate = prefs.getString("${key}_date", "") ?: ""
        val current = if (lastDate == today) prefs.getInt("${key}_count", 0) else 0
        val updated = current + 1
        prefs.edit().putString("${key}_date", today).putInt("${key}_count", updated).apply()
        return updated
    }

    fun getDailyUploadLimit(plan: String): Int {
        return when (plan) {
            "Guest" -> 0  // Guests cannot upload attachments
            "Free" -> 2   // Free tier capped at 2 uploads
            "Plus" -> 20  // Plus tier: 20 uploads
            "Pro" -> 100  // Pro tier: 100 uploads
            else -> 0
        }
    }

    fun canUpload(plan: String, email: String = userEmail): Boolean {
        val limit = getDailyUploadLimit(plan)
        return getDailyUploadUsage(email) < limit
    }

    fun resetGuestUsage() {
        val today = getTodayString()
        val key = getUsageKey("usage", "")
        val uploadKey = getUsageKey("upload", "")
        prefs.edit()
            .putString("${key}_date", today)
            .putInt("${key}_count", 0)
            .putString("${uploadKey}_date", today)
            .putInt("${uploadKey}_count", 0)
            .apply()
    }
}
