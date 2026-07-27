package fr.batleforc.weebobridgenotify

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import java.awt.datatransfer.StringSelection

// Exécute un call to action attaché à une notification.
// Types supportés : url, file (+ line), command (action IDE par id), shell (terminal visible), copy.
object ActionRunner {
    fun run(project: Project?, action: NotifyAction) {
        try {
            when {
                action.type == "url" && action.url != null -> BrowserUtil.browse(action.url)
                action.type == "file" && action.path != null -> openFile(project, action)
                action.type == "command" && action.command != null -> runIdeAction(action.command)
                action.type == "shell" && action.command != null -> runShell(project, action)
                action.type == "copy" && action.text != null -> {
                    CopyPasteManager.getInstance().setContents(StringSelection(action.text))
                    Notifier.info("copié dans le presse-papier ✅")
                }
                else -> Notifier.warn("action inconnue ou incomplète (${action.type ?: "?"})")
            }
        } catch (e: Exception) {
            Notifier.error("action « ${action.label} » échouée : ${e.message}")
        }
    }

    private fun openFile(project: Project?, action: NotifyAction) {
        val target = project ?: Notifier.anyProject()
        if (target == null) {
            Notifier.warn("aucun projet ouvert pour afficher ${action.path}")
            return
        }
        ApplicationManager.getApplication().invokeLater {
            val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(action.path!!)
            if (file == null) {
                Notifier.warn("fichier introuvable : ${action.path}")
                return@invokeLater
            }
            val line = (action.line ?: 0).let { if (it > 0) it - 1 else 0 }
            OpenFileDescriptor(target, file, line, 0).navigate(true)
        }
    }

    // Équivalent JetBrains des commandes VS Code : l'id d'une action IDE
    // (ex. "About", "CheckForUpdate", visible via Help > Find Action).
    private fun runIdeAction(actionId: String) {
        val manager = ActionManager.getInstance()
        val ideAction = manager.getAction(actionId)
        if (ideAction == null) {
            Notifier.warn("action IDE inconnue : $actionId")
            return
        }
        ApplicationManager.getApplication().invokeLater {
            manager.tryToExecute(ideAction, null, null, null, true)
        }
    }

    private fun runShell(project: Project?, action: NotifyAction) {
        val target = project ?: Notifier.anyProject()
        if (target == null) {
            Notifier.warn("aucun projet ouvert pour lancer un terminal")
            return
        }
        ApplicationManager.getApplication().invokeLater {
            val tab = TerminalToolWindowTabsManager.getInstance(target)
                .createTabBuilder()
                .workingDirectory(target.basePath)
                .tabName(action.label)
                .requestFocus(true)
                .createTab()
            tab.view.createSendTextBuilder().shouldExecute().send(action.command!!)
        }
    }
}
