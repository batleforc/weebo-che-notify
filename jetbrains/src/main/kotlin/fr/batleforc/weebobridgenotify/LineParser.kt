package fr.batleforc.weebobridgenotify

import com.google.gson.JsonElement
import com.google.gson.JsonParser

// Même format de ligne que l'extension VS Code :
//   - "info|message", "warn|message", "error|message" (sans préfixe = info)
//   - ligne JSON {level, message, actions: [{label, type, ...}]}
object LineParser {
    private val LEVELS = setOf("info", "warn", "error")
    private val PREFIX = Regex("^(info|warn|error)\\|([\\s\\S]*)$")

    fun parse(line: String): NotifyMessage? {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("{")) {
            try {
                val obj = JsonParser.parseString(trimmed).asJsonObject
                val level = obj.get("level")?.asStringOrNull()?.takeIf { it in LEVELS } ?: "info"
                val message = obj.get("message")?.asStringOrNull() ?: ""
                val actions = obj.get("actions")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { parseAction(it) } ?: emptyList()
                if (message.isBlank() && actions.isEmpty()) return null
                return NotifyMessage(level, message, actions)
            } catch (_: Exception) {
                // pas du JSON valide : affiché comme texte brut ci-dessous
            }
        }
        val m = PREFIX.find(line)
        val level = m?.groupValues?.get(1) ?: "info"
        val message = m?.groupValues?.get(2) ?: line
        if (message.isBlank()) return null
        return NotifyMessage(level, message, emptyList())
    }

    private fun parseAction(el: JsonElement): NotifyAction? {
        if (!el.isJsonObject) return null
        val o = el.asJsonObject
        val label = o.get("label")?.asStringOrNull() ?: return null
        return NotifyAction(
            label = label,
            type = o.get("type")?.asStringOrNull(),
            url = o.get("url")?.asStringOrNull(),
            path = o.get("path")?.asStringOrNull(),
            line = o.get("line")?.asIntOrNull(),
            command = o.get("command")?.asStringOrNull(),
            text = o.get("text")?.asStringOrNull(),
        )
    }

    private fun JsonElement.asStringOrNull(): String? =
        if (isJsonPrimitive) try { asString } catch (_: Exception) { null } else null

    private fun JsonElement.asIntOrNull(): Int? =
        if (isJsonPrimitive && asJsonPrimitive.isNumber) try { asInt } catch (_: Exception) { null } else null
}
