package app.echo.messages.capture

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.echo.messages.BuildConfig
import app.echo.messages.EchoApp
import app.echo.messages.MainActivity
import app.echo.messages.R
import app.echo.messages.data.Message
import app.echo.messages.echo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Saves every message notification from supported messengers and notices when one gets deleted. */
class CaptureService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    /** Last message list seen per notification, to spot messages that disappear. Memory only. */
    private val snapshots = HashMap<String, List<MessageKey>>()

    override fun onListenerConnected() {
        scope.launch {
            val days = echo.prefs.keepDays.value
            if (days > 0) echo.db.messages().deleteOlderThan(System.currentTimeMillis() - days * DAY_MS)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val simulated = BuildConfig.DEBUG && sbn.packageName == packageName && sbn.notification.channelId == "debug_sim"
        if (!simulated && !isWatched(sbn.packageName)) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val messages = read(sbn)
        if (messages.isEmpty()) return
        scope.launch { lock.withLock { record(sbn.key, messages) } }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Read or swiped away: the next notification starts fresh, so don't compare against this one.
        scope.launch { lock.withLock { snapshots.remove(sbn.key) } }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun isWatched(pkg: String): Boolean =
        (pkg in MESSENGERS || (BuildConfig.DEBUG && pkg == "com.android.shell")) &&
            pkg !in echo.prefs.mutedApps.value

    private fun read(sbn: StatusBarNotification): List<Incoming> {
        val n = sbn.notification
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        if (style != null) {
            val me = style.user.name?.toString()
            val conversation = cleanTitle(style.conversationTitle?.toString() ?: title)
            return style.messages.mapNotNull { m ->
                val text = m.text?.toString() ?: return@mapNotNull null
                val name = m.person?.name?.toString()
                val sender = if (name == null || name == me) "" else name
                Incoming(sbn.packageName, conversation.ifBlank { sender }, sender, text, m.timestamp, style.isGroupConversation)
            }.filterNot { isNoise(it.text) || it.conversation.isBlank() }
        }
        val text = (n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: n.extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val who = cleanTitle(title)
        if (who.isBlank() || isNoise(text)) return emptyList()
        val time = if (n.`when` > 0) n.`when` else sbn.postTime
        return listOf(Incoming(sbn.packageName, who, who, text, time, isGroup = false))
    }

    private suspend fun record(key: String, messages: List<Incoming>) {
        val dao = echo.db.messages()
        val now = System.currentTimeMillis()
        val shown = ArrayList<MessageKey>(messages.size)
        for (m in messages) {
            shown += m.key
            val saved = dao.find(m.pkg, m.conversation, m.sender, m.timestamp)
            if (saved == null) {
                // Nothing to recover for a placeholder we never saw the original of; anything else is a new message.
                if (isDeletedPlaceholder(m.text, echo.prefs.learnedPlaceholders.value, exact = true)) continue
                dao.insert(Message(pkg = m.pkg, conversation = m.conversation, sender = m.sender, text = m.text, timestamp = m.timestamp, isGroup = m.isGroup))
                continue
            }
            if (saved.text == m.text || saved.editedText == m.text) continue
            if (isDeletedPlaceholder(m.text, echo.prefs.learnedPlaceholders.value)) {
                if (dao.markDeleted(saved.id, now) == 1) alert(saved)
                continue
            }
            val sameChange = dao.editedTo(m.text).filter { it.id != saved.id }
            if (isPlaceholderCollision(saved.conversation, saved.sender, sameChange.map { it.conversation to it.sender })) {
                // Different people's messages all became this exact text: it's the placeholder in this phone's language.
                echo.prefs.learnPlaceholder(normalizePlaceholder(m.text))
                sameChange.forEach { dao.convertEditToDeleted(it.id, now) }
                if (dao.markDeleted(saved.id, now) == 1) alert(saved)
            } else {
                dao.markEdited(saved.id, m.text)
            }
        }
        val first = messages.first()
        for (gone in findRemoved(snapshots[key].orEmpty(), shown)) {
            dao.find(first.pkg, first.conversation, gone.sender, gone.timestamp)?.let { if (dao.markDeleted(it.id, now) == 1) alert(it) }
        }
        snapshots[key] = shown
    }

    private fun alert(m: Message) {
        if (!echo.prefs.alertOnDelete.value) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val who = m.sender.ifBlank { m.conversation }
        val open = PendingIntent.getActivity(
            this, m.id.toInt(),
            Intent(this, MainActivity::class.java)
                .putExtra(EchoApp.EXTRA_PKG, m.pkg)
                .putExtra(EchoApp.EXTRA_CONVERSATION, m.conversation)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, EchoApp.CHANNEL_DELETED)
            .setSmallIcon(R.drawable.ic_stat_echo)
            .setColor(ContextCompat.getColor(this, R.color.echo_brand))
            .setContentTitle(getString(R.string.alert_title, who))
            .setContentText(m.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(m.text))
            .setSubText(if (m.isGroup) m.conversation else null)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(m.id.toInt(), notification)
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
