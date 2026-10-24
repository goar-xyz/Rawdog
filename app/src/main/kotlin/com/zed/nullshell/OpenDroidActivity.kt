package com.zed.nullshell

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import dev.opendroid.agent.AnthropicLlmClient
import dev.opendroid.agent.ChatMessage
import dev.opendroid.agent.FinishReason
import dev.opendroid.agent.LlmClient
import dev.opendroid.agent.OpenAiChatCompletionsLlmClient
import dev.opendroid.agent.OpenDroidQueryLoop
import dev.opendroid.agent.QueryLoopEvent
import dev.opendroid.agent.opendroidToolDefinitionsForSession
import dev.opendroid.app.LlmApiFormat
import dev.opendroid.app.OpenDroidSettings
import dev.opendroid.app.skills.OpenDroidSkillInfo
import dev.opendroid.app.skills.OpenDroidSkillRepository
import dev.opendroid.device.opendroidDeviceToolExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * OpenDroid Agent Activity: Interactive autonomous agent interface integrated into RawDog.
 * Provides real tool execution (Kali Linux shell commands, PRoot environment, and Android
 * device accessibility automation) driven by OpenAI, Anthropic, or local model APIs.
 */
class OpenDroidActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var activeJob: Job? = null

    private lateinit var settings: OpenDroidSettings
    private lateinit var skillsRepo: OpenDroidSkillRepository

    private lateinit var logContainer: LinearLayout
    private lateinit var logScroll: ScrollView
    private lateinit var inputField: EditText
    private lateinit var sendButton: TextView
    private lateinit var modelInfoLabel: TextView
    private lateinit var progressBar: ProgressBar

    private val conversationHistory = mutableListOf<ChatMessage>()

    private fun dp(v: Float): Int = Ui.dp(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = OpenDroidSettings(this)
        skillsRepo = OpenDroidSkillRepository(this)
        skillsRepo.ensureRoot()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            id = View.generateViewId()
        }
        setContentView(root)
        Ui.recolor(root)
        Ui.applyInsets(root)

        buildTopBar(root)
        buildModelInfoBar(root)
        buildLogArea(root)
        buildQuickChips(root)
        buildBottomInputBar(root)

        renderWelcomeMessage()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeJob?.cancel()
    }

    private fun buildTopBar(parent: LinearLayout) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
            setBackgroundColor(Ui.SURFACE)
        }

        val btnBack = Ui.iconButton(this, "<-").apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setOnClickListener { finish() }
        }
        bar.addView(btnBack)

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8f), 0, 0, 0)
        }
        val title = TextView(this).apply {
            text = "OpenDroid Agent"
            setTextColor(Ui.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
        }
        val subtitle = TextView(this).apply {
            text = "Autonomous Shell & Device Automation"
            setTextColor(Ui.DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        titleCol.addView(title)
        titleCol.addView(subtitle)
        bar.addView(titleCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val btnSkills = Ui.iconButton(this, "Skills").apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setOnClickListener { showSkillsDialog() }
        }
        bar.addView(btnSkills)

        val btnConfig = Ui.iconButton(this, "Config").apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setOnClickListener { showSettingsDialog() }
        }
        bar.addView(btnConfig)

        parent.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52f)))

        val line = View(this).apply { setBackgroundColor(Ui.LINE) }
        parent.addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
    }

    private fun buildModelInfoBar(parent: LinearLayout) {
        val infoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(6f), dp(14f), dp(6f))
            setBackgroundColor(Ui.SURFACE)
        }

        modelInfoLabel = TextView(this).apply {
            setTextColor(Ui.DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        infoRow.addView(modelInfoLabel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleSmall).apply {
            visibility = View.GONE
        }
        infoRow.addView(progressBar)

        parent.addView(infoRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val line = View(this).apply { setBackgroundColor(Ui.LINE) }
        parent.addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))

        updateModelInfo()
    }

    private fun updateModelInfo() {
        val fmt = settings.apiFormat.label.substringBefore(" (")
        val keySet = settings.apiKey.isNotBlank()
        val keyStatus = if (keySet) "[Key Set]" else "[No API Key]"
        modelInfoLabel.text = "Model: ${settings.model} ($fmt) $keyStatus"
    }

    private fun buildLogArea(parent: LinearLayout) {
        logScroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
        }

        logContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12f), dp(12f), dp(12f), dp(16f))
        }

        logScroll.addView(logContainer, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        parent.addView(logScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun buildQuickChips(parent: LinearLayout) {
        val chipsScroll = HorizontalScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Ui.SURFACE)
            setPadding(dp(6f), dp(4f), dp(6f), dp(4f))
        }

        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun addChip(label: String, prompt: String) {
            val chip = TextView(this).apply {
                text = label
                setTextColor(Ui.TEXT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setBackground(Ui.rounded(context, Ui.SURFACE2, 6f))
                setPadding(dp(10f), dp(5f), dp(10f), dp(5f))
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.setMargins(dp(4f), 0, dp(4f), 0)
                layoutParams = lp
                isClickable = true
                setOnClickListener {
                    inputField.setText(prompt)
                    inputField.setSelection(prompt.length)
                }
            }
            chipsRow.addView(chip)
        }

        addChip("Kali Info", "Check the Kali Linux environment status, kernel, and disk usage.")
        addChip("Network Scan", "Scan active local network interfaces and listening ports.")
        addChip("Running Processes", "List running processes and system memory usage.")
        addChip("Screen UI", "Inspect the current Android on-screen accessibility tree.")

        chipsScroll.addView(chipsRow)
        parent.addView(chipsScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val line = View(this).apply { setBackgroundColor(Ui.LINE) }
        parent.addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
    }

    private fun buildBottomInputBar(parent: LinearLayout) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(8f), dp(10f), dp(8f))
            setBackgroundColor(Ui.SURFACE)
        }

        inputField = EditText(this).apply {
            hint = "Ask OpenDroid or instruct a command..."
            setHintTextColor(Ui.HINT)
            setTextColor(Ui.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setBackground(Ui.rounded(context, Ui.BG, 10f))
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    executePrompt()
                    true
                } else false
            }
        }
        bar.addView(inputField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        sendButton = TextView(this).apply {
            text = "Send"
            setTextColor(Ui.ON_ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
            setBackground(Ui.rounded(context, Ui.ACCENT, 8f))
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.leftMargin = dp(8f)
            layoutParams = lp
            isClickable = true
            setOnClickListener { executePrompt() }
        }
        bar.addView(sendButton)

        parent.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun renderWelcomeMessage() {
        val welcome = """OpenDroid Agent is active in RawDog.
Tools available:
- shell: Direct execution inside Kali Linux (PRoot) or host shell.
- accessibility: Inspect UI nodes, tap, swipe, and automate device workflows.
- skills: Modular execution workflows (rawdog-kali, rawdog-network, android-device).

Configure your API key via [Config] above to begin queries."""
        appendSystemCard(welcome)
    }

    private fun executePrompt() {
        val prompt = inputField.text.toString().trim()
        if (prompt.isBlank()) return

        if (settings.apiKey.isBlank()) {
            Toast.makeText(this, "Please enter your API Key in Config first", Toast.LENGTH_LONG).show()
            showSettingsDialog()
            return
        }

        inputField.setText("")
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(inputField.windowToken, 0)

        appendUserCard(prompt)

        activeJob?.cancel()
        activeJob = scope.launch {
            runAgentLoop(prompt)
        }
    }

    private suspend fun runAgentLoop(prompt: String) {
        progressBar.visibility = View.VISIBLE
        sendButton.isEnabled = false
        sendButton.alpha = 0.5f

        val responseCard = appendAssistantCard()

        try {
            val llmClient: LlmClient = when (settings.apiFormat) {
                LlmApiFormat.ANTHROPIC_MESSAGES -> AnthropicLlmClient(
                    apiKey = settings.apiKey,
                    baseUrl = settings.baseUrl,
                )
                LlmApiFormat.OPENAI_CHAT_COMPLETIONS -> OpenAiChatCompletionsLlmClient(
                    apiKey = settings.apiKey,
                    baseUrl = settings.baseUrl,
                )
            }

            val allSkills = skillsRepo.listSkills()
            val tools = opendroidToolDefinitionsForSession()
            val executor = opendroidDeviceToolExecutor(this@OpenDroidActivity, skillsRepo)

            val skillCatalog = skillsRepo.catalogMarkdownBlock(allSkills, tools.map { it.name })
            val fullSystemPrompt = "${settings.systemPrompt}\n\n$skillCatalog"

            val loop = OpenDroidQueryLoop(
                llm = llmClient,
                tools = tools,
                toolExecutor = executor,
            )

            val fullText = StringBuilder()

            loop.run(
                systemPrompt = fullSystemPrompt,
                conversation = conversationHistory,
                userText = prompt,
                model = settings.model,
            ) { event ->
                withContext(Dispatchers.Main) {
                    when (event) {
                        is QueryLoopEvent.AssistantDelta -> {
                            fullText.append(event.text)
                            responseCard.text = fullText.toString()
                            scrollToBottom()
                        }
                        is QueryLoopEvent.ToolStarted -> {
                            appendToolCard(event.name, event.toolUseId, "Executing...")
                            scrollToBottom()
                        }
                        is QueryLoopEvent.ToolFinished -> {
                            appendToolResultCard(
                                event.name,
                                event.ok,
                                event.durationMs,
                                event.resultPreview
                            )
                            scrollToBottom()
                        }
                        is QueryLoopEvent.Finished -> {
                            when (val r = event.reason) {
                                is FinishReason.Error -> {
                                    appendErrorCard("Agent error: ${r.message}")
                                }
                                else -> { }
                            }
                        }
                        else -> { }
                    }
                }
            }

            if (fullText.isEmpty()) {
                responseCard.text = "(Action completed)"
            }
        } catch (e: Exception) {
            appendErrorCard("Execution failure: ${e.message}")
        } finally {
            progressBar.visibility = View.GONE
            sendButton.isEnabled = true
            sendButton.alpha = 1.0f
        }
    }

    private fun appendUserCard(userText: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(Ui.rounded(context, Ui.SURFACE2, 8f))
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(20f), dp(6f), 0, dp(6f))
            layoutParams = lp
        }

        val tag = TextView(this).apply {
            text = "YOU"
            setTextColor(Ui.ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val msg = TextView(this).apply {
            text = userText
            setTextColor(Ui.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }
        card.addView(tag)
        card.addView(msg)
        logContainer.addView(card)
        scrollToBottom()
    }

    private fun appendAssistantCard(): TextView {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(Ui.rounded(context, Ui.SURFACE, 8f))
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(6f), dp(20f), dp(6f))
            layoutParams = lp
        }

        val tag = TextView(this).apply {
            text = "OPENDROID AGENT"
            setTextColor(Ui.ON_ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val msg = TextView(this).apply {
            text = "Thinking..."
            setTextColor(Ui.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }
        card.addView(tag)
        card.addView(msg)
        logContainer.addView(card)
        scrollToBottom()
        return msg
    }

    private fun appendToolCard(name: String, toolId: String, status: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackground(Ui.rounded(context, Ui.SURFACE2, 6f))
            setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(10f), dp(4f), dp(10f), dp(4f))
            layoutParams = lp
        }

        val badge = TextView(this).apply {
            text = "[TOOL: $name]"
            setTextColor(Ui.ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val st = TextView(this).apply {
            text = " $status"
            setTextColor(Ui.DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        card.addView(badge)
        card.addView(st)
        logContainer.addView(card)
    }

    private fun appendToolResultCard(name: String, ok: Boolean, durationMs: Long, output: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(Ui.rounded(context, Ui.BG, 6f))
            setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(10f), 0, dp(10f), dp(6f))
            layoutParams = lp
        }

        val header = TextView(this).apply {
            text = "[OUTPUT: $name | ${durationMs}ms | ${if (ok) "OK" else "ERROR"}]"
            setTextColor(if (ok) Ui.ACCENT else Ui.DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val outText = TextView(this).apply {
            text = output.take(800)
            setTextColor(Ui.DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        card.addView(header)
        card.addView(outText)
        logContainer.addView(card)
    }

    private fun appendSystemCard(systemText: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(Ui.rounded(context, Ui.SURFACE, 8f))
            setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(6f), 0, dp(6f))
            layoutParams = lp
        }

        val msg = TextView(this).apply {
            text = systemText
            setTextColor(Ui.DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        card.addView(msg)
        logContainer.addView(card)
        scrollToBottom()
    }

    private fun appendErrorCard(msgText: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(Ui.rounded(context, Ui.SURFACE2, 8f))
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(6f), 0, dp(6f))
            layoutParams = lp
        }

        val t = TextView(this).apply {
            text = msgText
            setTextColor(Ui.ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        card.addView(t)
        logContainer.addView(card)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        logScroll.post {
            logScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showSettingsDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(12f), dp(20f), dp(12f))
        }

        fun fieldLabel(title: String): TextView = TextView(this).apply {
            text = title
            setTextColor(Ui.DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dp(8f), 0, dp(2f))
        }

        fun fieldEdit(initial: String, hintText: String): EditText = EditText(this).apply {
            setText(initial)
            hint = hintText
            setHintTextColor(Ui.HINT)
            setTextColor(Ui.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setBackground(Ui.rounded(context, Ui.BG, 6f))
            setPadding(dp(10f), dp(8f), dp(10f), dp(8f))
        }

        box.addView(fieldLabel("API KEY"))
        val editKey = fieldEdit(settings.apiKey, "sk-...")
        box.addView(editKey)

        box.addView(fieldLabel("API FORMAT / PROVIDER"))
        val rg = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        val rbOpenAi = RadioButton(this).apply {
            text = "OpenAI compatible (/v1/chat/completions)"
            setTextColor(Ui.TEXT)
            isChecked = settings.apiFormat == LlmApiFormat.OPENAI_CHAT_COMPLETIONS
        }
        val rbAnthropic = RadioButton(this).apply {
            text = "Anthropic Claude (/v1/messages)"
            setTextColor(Ui.TEXT)
            isChecked = settings.apiFormat == LlmApiFormat.ANTHROPIC_MESSAGES
        }
        rg.addView(rbOpenAi)
        rg.addView(rbAnthropic)
        box.addView(rg)

        box.addView(fieldLabel("BASE URL"))
        val editBase = fieldEdit(settings.baseUrl, "https://api.openai.com/v1")
        box.addView(editBase)

        box.addView(fieldLabel("MODEL NAME"))
        val editModel = fieldEdit(settings.model, "gpt-4o")
        box.addView(editModel)

        box.addView(fieldLabel("ANDROID ACCESSIBILITY AUTOMATION"))
        val btnA11y = Button(this).apply {
            text = "Open Accessibility Settings"
            setTextColor(Ui.TEXT)
            setBackground(Ui.rounded(context, Ui.SURFACE2, 6f))
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        box.addView(btnA11y)

        Dlg(this)
            .setTitle("Agent Configuration")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                settings.apiKey = editKey.text.toString().trim()
                settings.baseUrl = editBase.text.toString().trim()
                settings.model = editModel.text.toString().trim()
                settings.apiFormat = if (rbAnthropic.isChecked) {
                    LlmApiFormat.ANTHROPIC_MESSAGES
                } else {
                    LlmApiFormat.OPENAI_CHAT_COMPLETIONS
                }
                updateModelInfo()
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSkillsDialog() {
        scope.launch {
            val list = skillsRepo.listSkills()
            val names = list.map { "${it.displayName} (${it.skillId})" }.toTypedArray()

            Dlg(this@OpenDroidActivity)
                .setTitle("OpenDroid Skills")
                .setItems(names) { _, which ->
                    showSkillDetail(list[which])
                }
                .setPositiveButton("Close", null)
                .show()
        }
    }

    private fun showSkillDetail(skill: OpenDroidSkillInfo) {
        scope.launch {
            val md = skillsRepo.readSkillMarkdown(skill.skillId) ?: "(Empty skill)"

            val box = LinearLayout(this@OpenDroidActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
            }

            val tv = TextView(this@OpenDroidActivity).apply {
                text = md
                setTextColor(Ui.TEXT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            val sv = ScrollView(this@OpenDroidActivity).apply {
                addView(tv)
            }
            box.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300f)))

            Dlg(this@OpenDroidActivity)
                .setTitle(skill.displayName)
                .setView(box)
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
