package fr.batleforc.weebobridgenotify

// Call to action attaché à une notification.
// Types supportés : url, file (+ line), command (action IDE), shell (terminal visible), copy.
data class NotifyAction(
    val label: String,
    val type: String?,
    val url: String? = null,
    val path: String? = null,
    val line: Int? = null,
    val command: String? = null,
    val text: String? = null,
)

data class NotifyMessage(
    val level: String,
    val message: String,
    val actions: List<NotifyAction>,
)
