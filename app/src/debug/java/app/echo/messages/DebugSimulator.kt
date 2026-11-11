package app.echo.messages

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person

/**
 * Debug builds only. Posts chat notifications the way WhatsApp does (stable timestamps, in-place
 * "This message was deleted"), so the whole capture → deletion → alert path can be tested on an emulator:
 *   adb shell am broadcast -n app.echo.messages/.DebugSimulator --ei step 1   (then 2, 3, 4)
 */
class DebugSimulator : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Debug chats", NotificationManager.IMPORTANCE_DEFAULT))
        val base = 1_762_900_000_000L
        val alex = Person.Builder().setName("Alex Rivera").build()
        val me = Person.Builder().setName("Me").build()
        when (intent.getIntExtra("step", 1)) {
            1 -> post(context, 1, "Alex Rivera", alex, me, false, listOf(
                Triple("Are you coming tonight?", base, alex),
                Triple("Sorry, I can’t make it 😕", base + 60_000, alex),
                Triple("Wait, maybe I can", base + 120_000, alex),
            ))
            // Alex deletes the second message: WhatsApp keeps its timestamp and swaps the text.
            2 -> post(context, 1, "Alex Rivera", alex, me, false, listOf(
                Triple("Are you coming tonight?", base, alex),
                Triple("This message was deleted", base + 60_000, alex),
                Triple("Wait, maybe I can", base + 120_000, alex),
            ))
            3 -> {
                val sam = Person.Builder().setName("Sam").build()
                val priya = Person.Builder().setName("Priya").build()
                post(context, 2, "Weekend trip", sam, me, true, listOf(
                    Triple("Booked the cabin for Saturday!", base + 200_000, sam),
                    Triple("I’ll bring the speaker", base + 260_000, priya),
                    Triple("Who’s driving?", base + 320_000, sam),
                ))
            }
            // Sam edits a message (same timestamp, new text) and Priya's message vanishes (deleted).
            4 -> {
                val sam = Person.Builder().setName("Sam").build()
                post(context, 2, "Weekend trip", sam, me, true, listOf(
                    Triple("Booked the cabin for Sunday!", base + 200_000, sam),
                    Triple("Who’s driving?", base + 320_000, sam),
                ))
            }
        }
    }

    private fun post(context: Context, id: Int, title: String, who: Person, me: Person, group: Boolean, messages: List<Triple<String, Long, Person>>) {
        val style = NotificationCompat.MessagingStyle(me).setGroupConversation(group)
        if (group) style.setConversationTitle(title)
        messages.forEach { (text, ts, person) -> style.addMessage(text, ts, person) }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_echo)
            .setContentTitle(if (group) title else who.name)
            .setContentText(messages.last().first)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, n)
    }

    companion object { const val CHANNEL = "debug_sim" }
}
