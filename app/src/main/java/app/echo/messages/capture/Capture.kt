package app.echo.messages.capture

/** One message as read from a notification. `sender == ""` means the phone's owner (a reply sent from the shade). */
data class Incoming(
    val pkg: String,
    val conversation: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isGroup: Boolean,
) {
    val key get() = MessageKey(sender, timestamp)
}

data class MessageKey(val sender: String, val timestamp: Long)

/** Messaging apps Echo saves. Labels are fallbacks; the UI shows the installed app's real name. */
val MESSENGERS: Map<String, String> = mapOf(
    "com.whatsapp" to "WhatsApp",
    "com.whatsapp.w4b" to "WhatsApp Business",
    "org.telegram.messenger" to "Telegram",
    "org.telegram.messenger.web" to "Telegram",
    "org.thunderdog.challegram" to "Telegram X",
    "com.instagram.android" to "Instagram",
    "com.facebook.orca" to "Messenger",
    "com.facebook.mlite" to "Messenger Lite",
    "org.thoughtcrime.securesms" to "Signal",
    "com.google.android.apps.messaging" to "Messages",
    "com.samsung.android.messaging" to "Samsung Messages",
    "com.viber.voip" to "Viber",
    "jp.naver.line.android" to "LINE",
    "com.discord" to "Discord",
    "com.snapchat.android" to "Snapchat",
    "com.kakao.talk" to "KakaoTalk",
    "com.imo.android.imoim" to "imo",
    "com.Slack" to "Slack",
    "com.microsoft.teams" to "Teams",
    "com.google.android.apps.dynamite" to "Google Chat",
)

// shortcut: placeholder texts for the most common app languages only; add more as users report them.
private val DELETED_PLACEHOLDERS = listOf(
    "this message was deleted",
    "you deleted this message",
    "se eliminó este mensaje",
    "este mensaje fue eliminado",
    "esta mensagem foi apagada",
    "mensagem apagada",
    "ce message a été supprimé",
    "diese nachricht wurde gelöscht",
    "pesan ini telah dihapus",
    "questo messaggio è stato eliminato",
    "bu mesaj silindi",
    "это сообщение удалено",
    "यह मैसेज डिलीट किया गया",
)

/** True when an app replaced a message with its "deleted" placeholder (WhatsApp keeps the original timestamp). */
fun isDeletedPlaceholder(text: String): Boolean {
    val t = text.trim().removePrefix("‎").removePrefix("🚫").trim().lowercase()
    return DELETED_PLACEHOLDERS.any { t == it || t.startsWith("$it.") }
}

/**
 * Messages that vanished from a conversation's notification between two updates.
 * Apps also drop the *oldest* messages when the list gets long, so only messages newer than
 * the oldest one still shown count as deleted.
 */
fun findRemoved(previous: List<MessageKey>, current: List<MessageKey>): List<MessageKey> {
    if (previous.isEmpty() || current.isEmpty()) return emptyList()
    val oldestShown = current.minOf { it.timestamp }
    val now = current.toSet()
    return previous.filter { it !in now && it.timestamp > oldestShown }
}

/** "Family (3 messages)" → "Family". shortcut: also strips a real trailing "(…)" from a name; fine until someone complains. */
fun cleanTitle(title: String): String =
    title.replace(Regex("""\s*\(\d+[^)]*\)\s*$"""), "").trim()

/** Texts that are app chrome, not messages. */
fun isNoise(text: String): Boolean {
    val t = text.trim().lowercase()
    return t.isEmpty() ||
        t == "checking for new messages" ||
        Regex("""^\d+ new messages?( from \d+ chats?)?$""").matches(t) ||
        Regex("""^\d+ messages? from \d+ chats?$""").matches(t)
}
