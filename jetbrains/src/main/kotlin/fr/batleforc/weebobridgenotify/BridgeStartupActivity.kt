package fr.batleforc.weebobridgenotify

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

// La surveillance est globale à l'application (le fichier est par utilisateur,
// pas par projet) : le premier projet ouvert l'arme, les suivants ne font rien.
class BridgeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (!started.compareAndSet(false, true)) return
        service<NotifyFileWatcherService>().start()
        CliInstaller.installIfWanted()
        val properties = PropertiesComponent.getInstance()
        if (!properties.getBoolean(WELCOMED_KEY)) {
            properties.setValue(WELCOMED_KEY, true)
            Notifier.info("weebo-bridge-notify actif — les lignes ajoutées à ~/.ide-notify apparaîtront ici.")
        }
    }

    companion object {
        private val started = AtomicBoolean(false)
        private const val WELCOMED_KEY = "weeboBridgeNotify.welcomed"
    }
}
