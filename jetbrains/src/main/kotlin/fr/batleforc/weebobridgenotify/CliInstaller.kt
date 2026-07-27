package fr.batleforc.weebobridgenotify

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

// Installe le CLI ide-notify (embarqué dans le plugin) dans ~/.local/bin,
// présent dans le PATH : tout outil du pod peut alors appeler `ide-notify`
// sans connaître le chemin du plugin. Re-copié quand le contenu change
// (mise à jour du plugin).
object CliInstaller {
    fun installIfWanted() {
        if (!NotifySettings.getInstance().current.installCli) return
        try {
            val content = CliInstaller::class.java.getResourceAsStream("/bin/ide-notify")
                ?.use { it.readBytes() } ?: return
            val destDir = Path.of(System.getProperty("user.home"), ".local", "bin")
            val dest = destDir.resolve("ide-notify")
            val existing = try { Files.readAllBytes(dest) } catch (_: Exception) { null }
            if (existing == null || !existing.contentEquals(content)) {
                Files.createDirectories(destDir)
                Files.write(dest, content)
            }
            try {
                Files.setPosixFilePermissions(dest, PosixFilePermissions.fromString("rwxr-xr-x"))
            } catch (_: UnsupportedOperationException) {
                // système de fichiers non POSIX (Windows) : rien à faire
            }
        } catch (e: Exception) {
            Notifier.warn("installation du CLI ide-notify échouée : ${e.message}")
        }
    }
}
