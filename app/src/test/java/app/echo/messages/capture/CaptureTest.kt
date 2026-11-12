package app.echo.messages.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureTest {

    private fun k(ts: Long, s: String = "Alex") = MessageKey(s, ts)

    @Test fun placeholderDetection() {
        assertTrue(isDeletedPlaceholder("This message was deleted"))
        assertTrue(isDeletedPlaceholder("🚫 This message was deleted"))
        assertTrue(isDeletedPlaceholder("  Esta mensagem foi apagada  "))
        assertFalse(isDeletedPlaceholder("I think this message was deleted by mistake"))
        assertFalse(isDeletedPlaceholder("hello"))
    }

    @Test fun placeholderAcrossLanguagesAndVariants() {
        assertTrue(isDeletedPlaceholder("Se eliminó este mensaje."))
        assertTrue(isDeletedPlaceholder("\u200E🚫 இந்த மெசேஜ் நீக்கப்பட்டது"))
        assertTrue(isDeletedPlaceholder("此消息已删除"))
        assertTrue(isDeletedPlaceholder("This message was deleted by admin Sam"))
        assertFalse(isDeletedPlaceholder("Mensagem apagada? Não entendi"))
    }

    @Test fun learnedPlaceholderMatches() {
        val learned = setOf(normalizePlaceholder("Mesej ini telah dipadam."))
        assertTrue(isDeletedPlaceholder("Mesej ini telah dipadam", learned))
        assertFalse(isDeletedPlaceholder("Mesej ini telah dipadam", emptySet()))
    }

    @Test fun collisionAcrossPeopleMeansPlaceholder() {
        assertTrue(isPlaceholderCollision("Family", "Sam", listOf("Work" to "Priya")))
        assertTrue(isPlaceholderCollision("Family", "Sam", listOf("Family" to "Priya")))
        assertFalse(isPlaceholderCollision("Family", "Sam", listOf("Family" to "Sam")))
        assertFalse(isPlaceholderCollision("Family", "Sam", emptyList()))
    }

    @Test fun deletedFromMiddleIsDetected() {
        val prev = listOf(k(1), k(2), k(3))
        val cur = listOf(k(1), k(3))
        assertEquals(listOf(k(2)), findRemoved(prev, cur))
    }

    @Test fun deletedNewestIsDetected() {
        assertEquals(listOf(k(3)), findRemoved(listOf(k(1), k(2), k(3)), listOf(k(1), k(2))))
    }

    @Test fun trimmingOldestIsNotDeletion() {
        val prev = listOf(k(1), k(2), k(3))
        val cur = listOf(k(2), k(3), k(4))
        assertEquals(emptyList<MessageKey>(), findRemoved(prev, cur))
    }

    @Test fun freshNotificationHasNoDeletions() {
        assertEquals(emptyList<MessageKey>(), findRemoved(emptyList(), listOf(k(5))))
        assertEquals(emptyList<MessageKey>(), findRemoved(listOf(k(5)), emptyList()))
    }

    @Test fun titleCleanup() {
        assertEquals("Family", cleanTitle("Family (3 messages)"))
        assertEquals("Alex", cleanTitle("Alex"))
    }

    @Test fun noiseFilter() {
        assertTrue(isNoise("Checking for new messages"))
        assertTrue(isNoise("5 new messages from 2 chats"))
        assertTrue(isNoise("3 messages from 2 chats"))
        assertFalse(isNoise("see you at 5"))
    }
}
