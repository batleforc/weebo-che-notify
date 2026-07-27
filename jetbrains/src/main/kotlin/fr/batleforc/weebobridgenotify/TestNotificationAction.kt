package fr.batleforc.weebobridgenotify

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class TestNotificationAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = NotifySettings.getInstance().notifyFile
        try {
            Files.writeString(
                file,
                "info|🔔 Notification de test weebo-bridge-notify (action Weebo Bridge Notify: Test)\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
        } catch (ex: Exception) {
            Notifier.error("écriture de la notification de test échouée : ${ex.message}")
        }
    }
}
