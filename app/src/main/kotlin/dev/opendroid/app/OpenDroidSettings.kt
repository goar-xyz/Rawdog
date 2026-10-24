package dev.opendroid.app

import android.content.Context
import android.content.SharedPreferences

enum class LlmApiFormat(val prefValue: String, val label: String) {
    OPENAI_CHAT_COMPLETIONS("openai", "OpenAI (/v1/chat/completions)"),
    ANTHROPIC_MESSAGES("anthropic", "Anthropic Claude (/v1/messages)"),
    ;

    companion object {
        fun fromPref(raw: String?): LlmApiFormat {
            val v = raw?.trim().orEmpty()
            return when (v) {
                ANTHROPIC_MESSAGES.prefValue -> ANTHROPIC_MESSAGES
                else -> OPENAI_CHAT_COMPLETIONS
            }
        }
    }
}

class OpenDroidSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("opendroid_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE, "").orEmpty().ifBlank { DEFAULT_BASE_URL }
        set(value) = prefs.edit().putString(KEY_BASE, value.trim()).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, "").orEmpty().ifBlank { DEFAULT_MODEL }
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    var apiFormat: LlmApiFormat
        get() = LlmApiFormat.fromPref(prefs.getString(KEY_FORMAT, LlmApiFormat.OPENAI_CHAT_COMPLETIONS.prefValue))
        set(value) = prefs.edit().putString(KEY_FORMAT, value.prefValue).apply()

    var autoExecuteShell: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SHELL, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SHELL, value).apply()

    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM, "").orEmpty().ifBlank { DEFAULT_SYSTEM_PROMPT }
        set(value) = prefs.edit().putString(KEY_SYSTEM, value).apply()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_API = "api_key"
        private const val KEY_BASE = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_FORMAT = "format"
        private const val KEY_AUTO_SHELL = "auto_shell"
        private const val KEY_SYSTEM = "system_prompt"

        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "gpt-4o"
        const val DEFAULT_SYSTEM_PROMPT = """You are OpenDroid, the autonomous AI agent integrated into RawDog Linux Workspace.
You have real tools to execute tasks:
1. 'shell': Execute bash/sh commands directly in RawDog's Kali Linux userspace (PRoot) or host shell. Inspect files, install tools, run networking and security utilities, compile code, and check system status.
2. Device Automation tools (when accessibility is enabled): 'get_ui_tree', 'get_focused_package', 'tap', 'swipe', 'input_text', 'key_system', 'launch_app', 'list_apps', 'capture_screenshot'.
3. 'load_skill': Load reusable workflows from SKILL.md files.

Always be concise, effective, and direct. When asked to perform system or coding tasks, use the 'shell' tool to run the necessary commands and inspect real output."""
    }
}
