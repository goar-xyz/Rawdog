package dev.opendroid.app.skills

import android.content.Context
import dev.opendroid.agent.SkillContentLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class OpenDroidSkillInfo(
    val skillId: String,
    val displayName: String,
    val description: String,
    val allowedTools: List<String>,
)

class OpenDroidSkillRepository(
    context: Context,
) : SkillContentLoader {
    private val root = File(context.filesDir, "skills").also { it.mkdirs() }

    init {
        initDefaultSkills()
    }

    fun ensureRoot() {
        root.mkdirs()
        initDefaultSkills()
    }

    private fun initDefaultSkills() {
        val kaliSkill = File(File(root, "rawdog-kali").also { it.mkdirs() }, "SKILL.md")
        if (!kaliSkill.exists()) {
            kaliSkill.writeText(
                """---
name: RawDog Kali Tools
description: Manage packages, system diagnostics, and shell tools in Kali Linux userspace.
allowed-tools: shell
---
# RawDog Kali Linux Workspace Skill
Use this skill when performing tasks inside RawDog's Kali Linux userspace.
- Check environment: `uname -a && cat /etc/os-release`
- Install packages: `apt update && apt install -y <package>`
- Disk and memory: `df -h / && free -m`
- Running processes: `ps aux`
- Network status: `ip a || ifconfig`
""".trimIndent(),
                Charsets.UTF_8
            )
        }

        val netSkill = File(File(root, "rawdog-network").also { it.mkdirs() }, "SKILL.md")
        if (!netSkill.exists()) {
            netSkill.writeText(
                """---
name: Network Diagnostics
description: Scan ports, ping hosts, check local network routing and connectivity.
allowed-tools: shell
---
# Network Diagnostics Skill
- Ping host: `ping -c 4 <host>`
- Scan ports: `nmap -sT -p 1-1000 <target>`
- Trace route: `traceroute <host>`
- Listening ports: `ss -tulpn || netstat -tulpn`
""".trimIndent(),
                Charsets.UTF_8
            )
        }

        val deviceSkill = File(File(root, "android-device").also { it.mkdirs() }, "SKILL.md")
        if (!deviceSkill.exists()) {
            deviceSkill.writeText(
                """---
name: Android Device Control
description: Inspect on-screen UI elements, tap, swipe, input text, and launch apps.
allowed-tools: get_ui_tree tap swipe input_text key_system launch_app list_apps
---
# Android Device Automation Skill
1. Use `get_ui_tree` to inspect current visible screen elements.
2. Use `tap` with target coordinates (x, y) to click buttons or input fields.
3. Use `input_text` to type into focused text inputs.
4. Use `key_system` with HOME, BACK, or RECENTS to navigate Android system.
""".trimIndent(),
                Charsets.UTF_8
            )
        }
    }

    suspend fun listSkills(): List<OpenDroidSkillInfo> = withContext(Dispatchers.IO) {
        root.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.mapNotNull { dir ->
                val skillFile = File(dir, "SKILL.md")
                if (!skillFile.isFile) return@mapNotNull null
                runCatching {
                    val content = skillFile.readText(Charsets.UTF_8)
                    val fm = SkillFrontmatter.parse(content)
                    val id = dir.name
                    OpenDroidSkillInfo(
                        skillId = id,
                        displayName = fm["name"]?.takeIf { it.isNotBlank() } ?: id,
                        description = fm["description"]?.takeIf { it.isNotBlank() }
                            ?: "(Description not provided in frontmatter)",
                        allowedTools = fm["allowed-tools"]
                            ?.split(" ", "\t")
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?: emptyList(),
                    )
                }.getOrNull()
            }
            .orEmpty()
            .sortedBy { it.skillId.lowercase() }
    }

    suspend fun readSkillMarkdown(skillId: String): String? = withContext(Dispatchers.IO) {
        resolveFile(skillId)?.takeIf { it.isFile }?.readText(Charsets.UTF_8)
    }

    suspend fun saveSkillMarkdown(skillId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val dir = OpenDroidSkillPaths.resolveSkillDir(root, skillId) ?: return@withContext false
        dir.mkdirs()
        val f = File(dir, "SKILL.md")
        f.writeText(content, Charsets.UTF_8)
        true
    }

    suspend fun deleteSkill(skillId: String): Boolean = withContext(Dispatchers.IO) {
        val dir = OpenDroidSkillPaths.resolveSkillDir(root, skillId) ?: return@withContext false
        dir.deleteRecursively()
    }

    fun catalogMarkdownBlock(skills: List<OpenDroidSkillInfo>, toolNames: List<String>): String {
        val toolsLine = toolNames.joinToString(", ")
        val header = buildString {
            appendLine("## Capabilities and Available Skills")
            appendLine("Available Tools: `$toolsLine`. Call tools as needed.")
            appendLine()
            if (skills.isEmpty()) {
                appendLine("No local skills currently registered.")
            } else {
                appendLine("Available Skills: If the user task matches a skill description below, call `load_skill` with `name` set to the exact `skill_id`:")
                appendLine()
                for (s in skills) {
                    append("- **`${s.skillId}`** - ${s.displayName}: ${s.description}")
                    if (s.allowedTools.isNotEmpty()) {
                        append(" (recommended tools: ${s.allowedTools.joinToString(", ")})")
                    }
                    appendLine()
                }
                appendLine()
                appendLine("When no specific skill matches, proceed directly with general reasoning and available tools.")
            }
        }
        return header.trimEnd()
    }

    private fun resolveFile(skillId: String): File? {
        val dir = OpenDroidSkillPaths.resolveSkillDir(root, skillId) ?: return null
        return File(dir, "SKILL.md")
    }

    override suspend fun load(name: String): String? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null
        resolveFile(name.trim())?.takeIf { it.isFile }?.readText(Charsets.UTF_8)
    }
}
