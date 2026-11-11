package app.echo.messages.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import app.echo.messages.capture.CaptureService
import app.echo.messages.capture.MESSENGERS
import app.echo.messages.ui.theme.AvatarPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

// ---------- System helpers ----------

fun listenerEnabled(context: Context) =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** Opens Echo's own row in notification-access settings where the OS allows it. */
fun openListenerSettings(context: Context) {
    val component = ComponentName(context, CaptureService::class.java).flattenToString()
    val direct = if (Build.VERSION.SDK_INT >= 30) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
    } else null
    val fallback = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    runCatching { context.startActivity((direct ?: fallback).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

private val icons = ConcurrentHashMap<String, ImageBitmap>()
private val labels = ConcurrentHashMap<String, String>()

fun appLabel(context: Context, pkg: String): String = labels.getOrPut(pkg) {
    runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrElse { MESSENGERS[pkg] ?: if (pkg == "com.android.shell") "Test" else pkg }
}

@Composable
fun rememberAppIcon(pkg: String): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState(icons[pkg], pkg) {
        if (value == null) value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()?.also { icons[pkg] = it }
        }
    }
    return icon
}

// ---------- Time ----------

/** "now", "12m", "14:32", "Yesterday", "Mon", "12 Mar". */
fun shortTime(context: Context, ts: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - ts
    if (diff < 60_000) return "now"
    if (diff < 60 * 60_000) return "${diff / 60_000}m"
    val then = Calendar.getInstance().apply { timeInMillis = ts }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val sameDay = then.get(Calendar.YEAR) == today.get(Calendar.YEAR) && then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return clock(context, ts)
    today.add(Calendar.DAY_OF_YEAR, -1)
    if (then.get(Calendar.YEAR) == today.get(Calendar.YEAR) && then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) return "Yesterday"
    if (diff < 6 * 24 * 60 * 60_000L) return DateFormat.format("EEE", ts).toString()
    return DateFormat.format("d MMM", ts).toString()
}

fun clock(context: Context, ts: Long): String = DateFormat.getTimeFormat(context).format(ts)

fun dayLabel(ts: Long, now: Long = System.currentTimeMillis()): String {
    val then = Calendar.getInstance().apply { timeInMillis = ts }
    val day = Calendar.getInstance().apply { timeInMillis = now }
    fun same() = then.get(Calendar.YEAR) == day.get(Calendar.YEAR) && then.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    if (same()) return "Today"
    day.add(Calendar.DAY_OF_YEAR, -1)
    if (same()) return "Yesterday"
    return DateFormat.format("EEEE, d MMMM", ts).toString()
}

// ---------- Avatar ----------

fun avatarColor(name: String): Color = AvatarPalette[(name.hashCode() and Int.MAX_VALUE) % AvatarPalette.size]

private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && it.first().isLetterOrDigit() }
    return when {
        parts.isEmpty() -> name.trim().take(1).ifEmpty { "?" }
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/** Initials on a stable colour, with the messenger's icon as a small badge. */
@Composable
fun Avatar(name: String, pkg: String?, size: Dp = 52.dp) {
    Box(Modifier.size(size)) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(avatarColor(name)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials(name),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
        if (pkg != null) {
            val icon = rememberAppIcon(pkg)
            if (icon != null) {
                val badge = size * 0.4f
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(badge)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                )
            }
        }
    }
}

// ---------- Empty states ----------

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}
