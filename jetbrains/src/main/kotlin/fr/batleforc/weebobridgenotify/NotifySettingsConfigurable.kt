package fr.batleforc.weebobridgenotify

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel

class NotifySettingsConfigurable : BoundConfigurable("Weebo Bridge Notify") {
    override fun createPanel(): DialogPanel {
        val state = NotifySettings.getInstance().current
        return panel {
            row("Fichier surveillé :") {
                textField()
                    .columns(40)
                    .bindText(state::file)
                    .comment("Vide = ~/.ide-notify")
            }
            row("Intervalle de surveillance (ms) :") {
                intTextField(range = 100..600_000)
                    .bindIntText(state::pollInterval)
            }
            row {
                checkBox("Installer le CLI ide-notify dans ~/.local/bin au démarrage")
                    .bindSelected(state::installCli)
            }
        }
    }

    override fun apply() {
        super.apply()
        // Fichier ou intervalle changé : on relance la surveillance
        service<NotifyFileWatcherService>().restart()
    }
}
