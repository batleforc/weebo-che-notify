package fr.batleforc.weebobridgenotify

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

object Notifier {
    private const val TITLE = "Weebo Bridge Notify"

    // info/warn s'effacent seuls (BALLOON), error reste affichée (STICKY_BALLOON)
    private fun group(sticky: Boolean) = NotificationGroupManager.getInstance()
        .getNotificationGroup(if (sticky) "Weebo Bridge Notify Errors" else "Weebo Bridge Notify")

    fun anyProject(): Project? = ProjectManager.getInstance().openProjects.firstOrNull { !it.isDisposed }

    fun show(msg: NotifyMessage) {
        val type = when (msg.level) {
            "error" -> NotificationType.ERROR
            "warn" -> NotificationType.WARNING
            else -> NotificationType.INFORMATION
        }
        val project = anyProject()
        val notification = group(msg.level == "error").createNotification(TITLE, msg.message, type)
        for (action in msg.actions) {
            notification.addAction(NotificationAction.createSimpleExpiring(action.label) {
                ActionRunner.run(anyProject(), action)
            })
        }
        notification.notify(project)
    }

    fun info(text: String) = show(NotifyMessage("info", text, emptyList()))
    fun warn(text: String) = show(NotifyMessage("warn", text, emptyList()))
    fun error(text: String) = show(NotifyMessage("error", text, emptyList()))
}
