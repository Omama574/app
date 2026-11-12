package app.echo.messages.ui

import app.echo.messages.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.echo.messages.ads.NativeAdCard
import app.echo.messages.data.ConversationRow
import app.echo.messages.data.Message
import kotlinx.coroutines.launch

/** The ad sits after this many chats: seen, but never the first thing. */
private const val AD_POSITION = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: EchoViewModel,
    onOpenChat: (String, String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val conversations by vm.conversations.collectAsState()
    val deleted by vm.deleted.collectAsState()
    val adsRemoved by vm.prefs.adsRemoved.collectAsState()
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.search_messages)) }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pager.currentPage, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(
                    selected = pager.currentPage == 0,
                    onClick = { scope.launch { pager.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.tab_chats), style = MaterialTheme.typography.titleSmall) },
                )
                Tab(
                    selected = pager.currentPage == 1,
                    onClick = { scope.launch { pager.animateScrollToPage(1) } },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.tab_deleted), style = MaterialTheme.typography.titleSmall)
                            val count = deleted?.size ?: 0
                            if (count > 0) {
                                Spacer(Modifier.width(6.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.tertiary) { Text("$count") }
                            }
                        }
                    },
                )
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                if (page == 0) ChatsTab(conversations, showAd = !adsRemoved, vm, onOpenChat, onSettings)
                else DeletedTab(deleted, onOpenChat)
            }
        }
    }
}

@Composable
private fun ChatsTab(
    conversations: List<ConversationRow>?,
    showAd: Boolean,
    vm: EchoViewModel,
    onOpenChat: (String, String) -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "status") { StatusBanners(vm) }
        when {
            conversations == null -> Unit
            conversations.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = Icons.Outlined.Forum,
                    title = stringResource(R.string.chats_empty_title),
                    body = stringResource(R.string.chats_empty_body),
                )
            }
            else -> conversations.forEachIndexed { index, row ->
                if (showAd && index == AD_POSITION) item(key = "ad") {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        NativeAdCard()
                        TextButton(onClick = onSettings, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.remove_ads)) }
                    }
                }
                item(key = "${row.pkg}/${row.conversation}") {
                    ConversationItem(row) { onOpenChat(row.pkg, row.conversation) }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(row: ConversationRow, onClick: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(row.conversation, row.pkg)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.conversation, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(shortTime(context, row.lastTime), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val preview = when {
                    row.lastSender.isEmpty() -> stringResource(R.string.you_prefix, row.lastText)
                    row.isGroup -> stringResource(R.string.sender_prefix, row.lastSender, row.lastText)
                    else -> row.lastText
                }
                if (row.lastDeleted) {
                    Icon(Icons.Rounded.Restore, null, tint = c.tertiary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.lastDeleted) c.tertiary else c.onSurfaceVariant,
                    fontStyle = if (row.lastDeleted) FontStyle.Italic else FontStyle.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (row.deletedCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = c.tertiaryContainer, contentColor = c.onTertiaryContainer) {
                        Text(stringResource(R.string.deleted_count, row.deletedCount), modifier = Modifier.padding(horizontal = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletedTab(deleted: List<Message>?, onOpenChat: (String, String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            deleted == null -> Unit
            deleted.isEmpty() -> item {
                EmptyState(
                    icon = Icons.Outlined.AutoDelete,
                    title = stringResource(R.string.deleted_empty_title),
                    body = stringResource(R.string.deleted_empty_body),
                )
            }
            else -> {
                item {
                    Text(
                        stringResource(R.string.deleted_header, deleted.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                items(deleted, key = { it.id }) { m -> DeletedCard(m) { onOpenChat(m.pkg, m.conversation) } }
            }
        }
    }
}

@Composable
private fun DeletedCard(m: Message, onClick: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    val who = m.sender.ifBlank { m.conversation }
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = c.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(who, m.pkg, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(who, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val app = appLabel(context, m.pkg)
                    val deletedWhen = shortTime(context, m.deletedAt ?: m.timestamp)
                    Text(
                        if (m.isGroup) stringResource(R.string.deleted_meta_group, m.conversation, app, deletedWhen) else stringResource(R.string.deleted_meta, app, deletedWhen),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
                color = c.tertiaryContainer,
                border = BorderStroke(1.dp, c.tertiary.copy(alpha = 0.5f)),
            ) {
                Text(m.text, style = MaterialTheme.typography.bodyLarge, color = c.onTertiaryContainer, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.sent_at, clock(context, m.timestamp)), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
        }
    }
}

/** Only shown when something needs the user: Echo paused, or alerts not allowed yet. */
@Composable
private fun StatusBanners(vm: EchoViewModel) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(true) }
    var canNotify by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        enabled = listenerEnabled(context)
        canNotify = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        onPauseOrDispose {}
    }
    val alertOnDelete by vm.prefs.alertOnDelete.collectAsState()
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = it }

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!enabled) {
            Banner(
                icon = Icons.Outlined.PauseCircle,
                title = stringResource(R.string.paused_title),
                body = stringResource(R.string.paused_body),
                action = stringResource(R.string.turn_on),
                onAction = { openListenerSettings(context) },
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            )
        } else if (alertOnDelete && !canNotify) {
            Banner(
                icon = Icons.Outlined.NotificationsActive,
                title = stringResource(R.string.alerts_banner_title),
                body = stringResource(R.string.alerts_banner_body),
                action = stringResource(R.string.allow_alerts),
                onAction = { askNotify.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Banner(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    content: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(shape = RoundedCornerShape(24.dp), color = container, contentColor = content, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(action) }
            }
        }
    }
}
