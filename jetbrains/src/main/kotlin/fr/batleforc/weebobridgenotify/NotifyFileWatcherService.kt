package fr.batleforc.weebobridgenotify

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

// Surveillance par polling du fichier de notifications (équivalent de fs.watchFile
// côté VS Code) : seules les lignes ajoutées depuis la dernière position sont lues,
// et un fichier tronqué/recréé resynchronise la position à zéro.
@Service
class NotifyFileWatcherService : Disposable {
    private var future: ScheduledFuture<*>? = null
    private var file: Path? = null
    private var position: Long = 0

    @Synchronized
    fun start() {
        cancel()
        val target = NotifySettings.getInstance().notifyFile
        file = target
        try {
            if (!Files.exists(target)) {
                target.parent?.let { Files.createDirectories(it) }
                Files.createFile(target)
            }
            position = Files.size(target)
        } catch (e: Exception) {
            Notifier.error("impossible d'initialiser $target : ${e.message}")
            return
        }
        val interval = NotifySettings.getInstance().current.pollInterval.coerceAtLeast(100).toLong()
        future = AppExecutorUtil.getAppScheduledExecutorService()
            .scheduleWithFixedDelay({ poll() }, interval, interval, TimeUnit.MILLISECONDS)
    }

    fun restart() = start()

    @Synchronized
    private fun poll() {
        val target = file ?: return
        val size = try {
            if (!Files.exists(target)) return
            Files.size(target)
        } catch (_: Exception) {
            return
        }
        if (size < position) position = 0 // fichier tronqué/recréé
        if (size <= position) return
        try {
            RandomAccessFile(target.toFile(), "r").use { raf ->
                raf.seek(position)
                val buffer = ByteArray((size - position).toInt())
                raf.readFully(buffer)
                position = size
                String(buffer, Charsets.UTF_8)
                    .split('\n')
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        LineParser.parse(line)?.let { msg ->
                            ApplicationManager.getApplication().invokeLater { Notifier.show(msg) }
                        }
                    }
            }
        } catch (_: Exception) {
            // lecture ratée : on retentera au prochain tick sans avancer position
        }
    }

    @Synchronized
    private fun cancel() {
        future?.cancel(false)
        future = null
    }

    override fun dispose() {
        cancel()
    }
}
