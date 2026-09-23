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
        get() = prefs.getString("arki_username", "") ?: ""
        set(value) = prefs.edit().putString("arki_username", value).apply()

    var userPersona: String
        get() = prefs.getString("arki_persona", "") ?: ""
        set(value) = prefs.edit().putString("arki_persona", value).apply()

    var currentPlan: String
        get() = prefs.getString("arki_plan", "Free") ?: "Free"
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

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("user_logged_in", true)
        set(value) = prefs.edit().putBoolean("user_logged_in", value).apply()

    var isEmailVerified: Boolean
        get() = prefs.getBoolean("user_email_verified", true)
        set(value) = prefs.edit().putBoolean("user_email_verified", value).apply()

    var authMethod: String
        get() = prefs.getString("user_auth_method", "Google") ?: "Google"
        set(value) = prefs.edit().putString("user_auth_method", value).apply()

    var resendApiKey: String
        get() = prefs.getString("resend_api_key", "") ?: ""
        set(value) = prefs.edit().putString("resend_api_key", value).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "devmightwin@gmail.com") ?: "devmightwin@gmail.com"
        set(value) = prefs.edit().putString("user_email", value).apply()

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getDailyUsage(): Int {
        val today = getTodayString()
        val lastDate = prefs.getString("usage_date", "") ?: ""
        return if (lastDate == today) {
            prefs.getInt("usage_count", 0)
        } else {
            prefs.edit().putString("usage_date", today).putInt("usage_count", 0).apply()
            0
        }
    }

    fun incrementDailyUsage(): Int {
        val today = getTodayString()
        val lastDate = prefs.getString("usage_date", "") ?: ""
        val current = if (lastDate == today) prefs.getInt("usage_count", 0) else 0
        val updated = current + 1
        prefs.edit().putString("usage_date", today).putInt("usage_count", updated).apply()
        return updated
    }

    fun getDailyLimit(plan: String): Int {
        return when (plan) {
            "Guest" -> 5
            "Free" -> 15
            "Plus" -> 50
            "Pro" -> 150
            else -> 15
        }
    }
}
