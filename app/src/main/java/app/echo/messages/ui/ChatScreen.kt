package app.echo.messages.ui

import app.echo.messages.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.echo.messages.data.Message
import kotlinx.coroutines.launch

private sealed interface ChatItem { val key: Any }
private data class DayRow(val label: String) : ChatItem { override val key get() = "day-$label" }
private data class MsgRow(val m: Message, val showName: Boolean) : ChatItem { override val key get() = m.id }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: EchoViewModel, pkg: String, conversation: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val messages by remember(pkg, conversation) { vm.thread(pkg, conversation) }.collectAsState(initial = null)
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Newest first because the list is reversed (sticks to the bottom like a messenger).
    val rows = remember(messages) {
        val list = messages.orEmpty()
        buildList<ChatItem> {
            list.forEachIndexed { i, m ->
                val prev = list.getOrNull(i - 1)
                if (prev == null || dayLabel(context, prev.timestamp) != dayLabel(context, m.timestamp)) add(DayRow(dayLabel(context, m.timestamp)))
                add(MsgRow(m, showName = m.isGroup && !m.isMine && prev?.sender != m.sender))
            }
        }.asReversed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)) } },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(conversation, pkg, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(conversation, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(appLabel(context, pkg), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_delete)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = { menu = false; confirmDelete = true },
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is DayRow -> DaySeparator(row.label)
                    is MsgRow -> Bubble(row.m, row.showName, snackbar)
                }
            }
            if (messages != null) item(key = "footer") { SavedFromNote() }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.chat_delete_title)) },
            text = { Text(stringResource(R.string.chat_delete_body, conversation, appLabel(context, pkg))) },
            confirmButton = {
                TextButton(onClick = { vm.deleteConversation(pkg, conversation); confirmDelete = false; onBack() }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun DaySeparator(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun SavedFromNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.chat_footer),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(m: Message, showName: Boolean, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.copied)

    val container = when {
        m.isDeleted -> c.tertiaryContainer
        m.isMine -> c.primaryContainer
        else -> c.surfaceContainerHigh
    }
    val onContainer = when {
        m.isDeleted -> c.onTertiaryContainer
        m.isMine -> c.onPrimaryContainer
        else -> c.onSurface
    }
    val shape = if (m.isMine) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    val shownText = m.editedText ?: m.text

    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.isMine) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = shape,
            color = container,
            border = if (m.isDeleted) BorderStroke(1.5.dp, c.tertiary) else null,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(top = if (showName) 8.dp else 0.dp),
        ) {
            Column(
                Modifier
                    .combinedClickable(onClick = {}, onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboard.setText(AnnotatedString(shownText))
                        scope.launch { snackbar.showSnackbar(copied) }
                    })
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (showName) {
                    Text(m.sender, style = MaterialTheme.typography.labelLarge, color = avatarColor(m.sender))
                    Spacer(Modifier.height(2.dp))
                }
                Text(shownText, style = MaterialTheme.typography.bodyLarge, color = onContainer)
                if (m.editedText != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.original, m.text), style = MaterialTheme.typography.bodySmall, color = onContainer.copy(alpha = 0.75f))
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    if (m.isDeleted) {
                        Icon(Icons.Rounded.Restore, null, tint = c.tertiary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.label_deleted), style = MaterialTheme.typography.labelSmall, color = c.tertiary)
                        Spacer(Modifier.width(6.dp))
                    } else if (m.editedText != null) {
                        Icon(Icons.Rounded.Edit, null, tint = onContainer.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.label_edited), style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.7f))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(clock(context, m.timestamp), style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.7f))
                }
            }
        }
    }
}
