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

/**
 * "This message was deleted" as WhatsApp shows it, per language. Verified: English, Tamil (WhatsApp FAQ),
 * Spanish, Portuguese, Russian, Indonesian (press quoting the app). The rest are best-known wording;
 * if one is off, [isPlaceholderCollision] learns the real text on the device after two deletions.
 */
private val DELETED_PLACEHOLDERS = listOf(
    "this message was deleted", "you deleted this message",
    "se eliminó este mensaje", "este mensaje fue eliminado", "eliminaste este mensaje",
    "esta mensagem foi apagada", "essa mensagem foi apagada", "mensagem apagada", "você apagou esta mensagem",
    "это сообщение было удалено", "это сообщение удалено", "данное сообщение удалено", "вы удалили это сообщение",
    "pesan ini telah dihapus", "anda menghapus pesan ini",
    "இந்த மெசேஜ் நீக்கப்பட்டது",
    "ce message a été supprimé", "vous avez supprimé ce message",
    "diese nachricht wurde gelöscht", "du hast diese nachricht gelöscht",
    "questo messaggio è stato eliminato", "hai eliminato questo messaggio",
    "bu mesaj silindi", "bu mesajı sildiniz",
    "تم حذف هذه الرسالة", "لقد حذفت هذه الرسالة",
    "यह मैसेज डिलीट कर दिया गया था", "यह मैसेज डिलीट किया गया", "यह संदेश हटा दिया गया",
    "یہ پیغام حذف کر دیا گیا", "یہ پیغام حذف کر دیا گیا تھا",
    "এই মেসেজটি মুছে ফেলা হয়েছে", "এই বার্তাটি মুছে ফেলা হয়েছে",
    "हा मेसेज हटवला गेला", "हा संदेश हटवला गेला",
    "આ મેસેજ ડિલીટ કરવામાં આવ્યો હતો", "આ સંદેશ કાઢી નાખવામાં આવ્યો હતો",
    "ఈ మెసేజ్ తొలగించబడింది", "ఈ సందేశం తొలగించబడింది",
    "このメッセージは削除されました",
    "삭제된 메시지입니다", "이 메시지는 삭제되었습니다",
    "此消息已删除", "此消息已被删除", "此訊息已刪除", "此訊息已被刪除",
    "ข้อความนี้ถูกลบแล้ว",
    "tin nhắn này đã bị xóa", "tin nhắn này đã bị xoá",
    "ta wiadomość została usunięta",
    "це повідомлення видалено", "це повідомлення було видалено",
).map(::normalizePlaceholder)

/** Lowercase, no 🚫 icon, direction marks or end punctuation, single spaces: so small app differences still match. */
fun normalizePlaceholder(text: String): String =
    text.replace(Regex("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069]|🚫"), "")
        .lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimEnd('.', '。', '।', '!', ' ')

/**
 * True when an app replaced a message with its "deleted" placeholder (WhatsApp keeps the original timestamp).
 * `learned` holds placeholders discovered on this device. A prefix match also covers "…deleted by admin Sam".
 */
fun isDeletedPlaceholder(text: String, learned: Set<String> = emptySet(), exact: Boolean = false): Boolean {
    val t = normalizePlaceholder(text)
    if (t in learned) return true
    return DELETED_PLACEHOLDERS.any { t == it || (!exact && t.startsWith("$it ")) }
}

/**
 * A message's text changed in place. If other people's messages already changed to the exact same text,
 * it isn't an edit: it's this phone's "deleted" placeholder, in whatever language the phone uses.
 * shortcut: two people editing to the identical text would be misread as deletions; rare enough for now.
 */
fun isPlaceholderCollision(conversation: String, sender: String, alreadyChangedTo: List<Pair<String, String>>): Boolean =
    alreadyChangedTo.any { (c, s) -> c != conversation || s != sender }

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
