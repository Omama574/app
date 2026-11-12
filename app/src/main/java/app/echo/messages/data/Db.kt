package app.echo.messages.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["pkg", "conversation", "sender", "timestamp"], unique = true),
        Index(value = ["deletedAt"]),
    ],
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pkg: String,
    val conversation: String,
    /** "" = the phone's owner. */
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isGroup: Boolean,
    val deletedAt: Long? = null,
    /** The newer text when the sender edited the message; [text] keeps the original. */
    val editedText: String? = null,
) {
    val isDeleted get() = deletedAt != null
    val isMine get() = sender.isEmpty()
}

data class ConversationRow(
    val pkg: String,
    val conversation: String,
    val isGroup: Boolean,
    val lastSender: String,
    val lastText: String,
    val lastTime: Long,
    val lastDeleted: Boolean,
    val deletedCount: Int,
)

@Dao
interface MessageDao {
    /** Returns -1 when the message is already saved. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: Message): Long

    @Query("SELECT * FROM messages WHERE pkg = :pkg AND conversation = :conversation AND sender = :sender AND timestamp = :timestamp LIMIT 1")
    suspend fun find(pkg: String, conversation: String, sender: String, timestamp: Long): Message?

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun byId(id: Long): Message?

    /** Returns 1 only the first time, so we alert once per deletion. */
    @Query("UPDATE messages SET deletedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun markDeleted(id: Long, at: Long): Int

    @Query("UPDATE messages SET editedText = :text WHERE id = :id")
    suspend fun markEdited(id: Long, text: String)

    @Query("SELECT * FROM messages WHERE editedText = :text AND deletedAt IS NULL")
    suspend fun editedTo(text: String): List<Message>

    /** An "edit" that turned out to be the deleted placeholder: the original text is the message that was deleted. */
    @Query("UPDATE messages SET deletedAt = :at, editedText = NULL WHERE id = :id AND deletedAt IS NULL")
    suspend fun convertEditToDeleted(id: Long, at: Long)

    @Query(
        """
        SELECT m.pkg, m.conversation, m.isGroup, m.sender AS lastSender, m.text AS lastText,
               m.timestamp AS lastTime, (m.deletedAt IS NOT NULL) AS lastDeleted,
               (SELECT COUNT(*) FROM messages d
                 WHERE d.pkg = m.pkg AND d.conversation = m.conversation AND d.deletedAt IS NOT NULL) AS deletedCount
        FROM messages m
        WHERE m.id = (SELECT x.id FROM messages x
                       WHERE x.pkg = m.pkg AND x.conversation = m.conversation
                       ORDER BY x.timestamp DESC, x.id DESC LIMIT 1)
        ORDER BY m.timestamp DESC
        """
    )
    fun conversations(): Flow<List<ConversationRow>>

    @Query("SELECT * FROM messages WHERE pkg = :pkg AND conversation = :conversation ORDER BY timestamp, id")
    fun thread(pkg: String, conversation: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun deleted(): Flow<List<Message>>

    // shortcut: LIKE scan is fine for tens of thousands of rows; move to FTS4 if search gets slow.
    @Query(
        """
        SELECT * FROM messages
        WHERE text LIKE '%' || :q || '%' OR editedText LIKE '%' || :q || '%'
           OR sender LIKE '%' || :q || '%' OR conversation LIKE '%' || :q || '%'
        ORDER BY timestamp DESC LIMIT 200
        """
    )
    fun search(q: String): Flow<List<Message>>

    @Query("DELETE FROM messages WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM messages WHERE pkg = :pkg AND conversation = :conversation")
    suspend fun deleteConversation(pkg: String, conversation: String)

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Database(entities = [Message::class], version = 1, exportSchema = true)
abstract class EchoDb : RoomDatabase() {
    abstract fun messages(): MessageDao

    companion object {
        fun create(context: Context) =
            Room.databaseBuilder(context, EchoDb::class.java, "echo.db").build()
    }
}
