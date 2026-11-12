package app.echo.messages.ui

import app.echo.messages.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.echo.messages.data.Message

@Composable
fun SearchScreen(vm: EchoViewModel, onOpenChat: (String, String) -> Unit, onBack: () -> Unit) {
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TextField(
            value = query,
            onValueChange = { vm.query.value = it },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            leadingIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search)) }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .focusRequester(focus),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when {
            query.isBlank() -> EmptyState(Icons.Outlined.TravelExplore, stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_body))
            results.isEmpty() -> EmptyState(Icons.Outlined.SearchOff, stringResource(R.string.search_none_title), stringResource(R.string.search_none_body, query.trim()))
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { m -> ResultRow(m, query.trim()) { onOpenChat(m.pkg, m.conversation) } }
            }
        }
    }
}

@Composable
private fun ResultRow(m: Message, q: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    val text = m.editedText ?: m.text
    val highlighted = remember(text, q) {
        buildAnnotatedString {
            var start = 0
            while (true) {
                val i = text.indexOf(q, start, ignoreCase = true)
                if (i < 0 || q.isEmpty()) { append(text.substring(start)); break }
                append(text.substring(start, i))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, background = c.primaryContainer, color = c.onPrimaryContainer)) {
                    append(text.substring(i, i + q.length))
                }
                start = i + q.length
            }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(m.conversation, m.pkg, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.conversation, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(shortTime(context, m.timestamp), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
            }
            // Prefix templates end with the message; an empty message leaves just the prefix.
            val who = when {
                m.isMine -> stringResource(R.string.you_prefix, "")
                m.isGroup -> stringResource(R.string.sender_prefix, m.sender, "")
                else -> ""
            }
            Text(
                buildAnnotatedString { append(who); append(highlighted) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (m.isDeleted) c.tertiary else c.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (m.isDeleted) Text(stringResource(R.string.label_deleted), style = MaterialTheme.typography.labelSmall, color = c.tertiary)
        }
    }
}
