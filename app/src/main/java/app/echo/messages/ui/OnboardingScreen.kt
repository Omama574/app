package app.echo.messages.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    // Coming back from Settings with access granted finishes onboarding by itself.
    LifecycleResumeEffect(Unit) {
        if (listenerEnabled(context)) onDone()
        onPauseOrDispose {}
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        RevealDemo()
        Spacer(Modifier.height(40.dp))
        Text("Read messages, even after they’re deleted", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "Echo quietly keeps a copy of every message you receive, so “This message was deleted” never hides anything from you again.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Benefit(Icons.Outlined.Forum, "WhatsApp, Telegram, Instagram, Signal and more")
        Benefit(Icons.Outlined.Lock, "Stays on this phone. No account, no sign-up.")
        Benefit(Icons.Outlined.VolunteerActivism, "Remove ads with one payment. Never a subscription.")
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = { openListenerSettings(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) { Text("Turn on Echo", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(12.dp))
        Text(
            "Next, Android asks to let Echo read notifications. That’s how it sees messages. Echo can only keep messages that arrive after you turn it on.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Benefit(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** The product in two seconds: a deleted message flips back to what it said. */
@Composable
private fun RevealDemo() {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800); revealed = true
            delay(2600); revealed = false
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Example: a deleted message shown again in Echo" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DemoBubble { Text("Are you coming tonight?", style = MaterialTheme.typography.bodyLarge) }
        AnimatedContent(
            targetState = revealed,
            transitionSpec = { (fadeIn(spring()) + scaleIn(spring(dampingRatio = 0.7f), initialScale = 0.92f)) togetherWith fadeOut() },
            label = "reveal",
        ) { show ->
            if (!show) {
                DemoBubble(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Text(
                        "🚫 This message was deleted",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.None,
                    )
                }
            } else {
                DemoBubble(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
                ) {
                    Column {
                        Text("Sorry, I can’t make it 😕", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Restore, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Deleted · saved by Echo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoBubble(
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        color = color,
        border = border,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
        modifier = Modifier.widthIn(max = 300.dp),
    ) { Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { content() } }
}
