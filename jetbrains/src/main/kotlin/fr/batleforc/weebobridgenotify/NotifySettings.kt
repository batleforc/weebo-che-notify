package fr.batleforc.weebobridgenotify

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.nio.file.Path

@Service
@State(name = "WeeboBridgeNotifySettings", storages = [Storage("weeboBridgeNotify.xml")])
class NotifySettings : PersistentStateComponent<NotifySettings.State> {
    class State {
        // Fichier surveillé ; vide = ~/.ide-notify
        var file: String = ""

        // Intervalle de surveillance du fichier en millisecondes
        var pollInterval: Int = 1000

        // Installe le CLI ide-notify dans ~/.local/bin au démarrage
        var installCli: Boolean = true
    }

    var current: State = State()

    override fun getState(): State = current

    override fun loadState(state: State) {
        current = state
    }

    val notifyFile: Path
        get() = current.file.trim().let {
            if (it.isEmpty()) Path.of(System.getProperty("user.home"), ".ide-notify") else Path.of(it)
        }

    companion object {
        fun getInstance(): NotifySettings =
            ApplicationManager.getApplication().getService(NotifySettings::class.java)
    }
}
