package app.echo.messages.ui

import app.echo.messages.R
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.echo.messages.BuildConfig
import app.echo.messages.ads.Ads
import app.echo.messages.capture.MESSENGERS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: EchoViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val prefs = vm.prefs
    val adsRemoved by prefs.adsRemoved.collectAsState()
    val price by vm.billing.price.collectAsState()
    val alertOnDelete by prefs.alertOnDelete.collectAsState()
    val keepDays by prefs.keepDays.collectAsState()
    val muted by prefs.mutedApps.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    val installed = remember {
        MESSENGERS.keys.filter { context.packageManager.getLaunchIntentForPackage(it) != null }
            .distinctBy { appLabel(context, it) }
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)) } },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                RemoveAdsCard(
                    removed = adsRemoved,
                    price = price,
                    onBuy = {
                        if (!vm.buyRemoveAds(activity)) Toast.makeText(context, context.getString(R.string.play_unreachable), Toast.LENGTH_SHORT).show()
                    },
                    onRestore = { vm.billing.restore(); Toast.makeText(context, context.getString(R.string.checking_purchases), Toast.LENGTH_SHORT).show() },
                )
            }
            item {
                Section(stringResource(R.string.set_alerts)) {
                    SwitchRow(stringResource(R.string.set_alert_title), stringResource(R.string.set_alert_body), alertOnDelete) { prefs.setAlertOnDelete(it) }
                }
            }
            item {
                Section(stringResource(R.string.set_apps)) {
                    if (installed.isEmpty()) {
                        Text(stringResource(R.string.set_no_apps), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    }
                    installed.forEach { pkg ->
                        AppSwitchRow(pkg, enabled = pkg !in muted) { prefs.setAppEnabled(pkg, it) }
                    }
                }
            }
            item {
                Section(stringResource(R.string.set_keep)) {
                    val options = listOf(30 to stringResource(R.string.days_30), 90 to stringResource(R.string.days_90), 0 to stringResource(R.string.forever))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                        options.forEachIndexed { i, (days, label) ->
                            SegmentedButton(
                                selected = keepDays == days,
                                onClick = { prefs.setKeepDays(days) },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            ) { Text(label) }
                        }
                    }
                }
            }
            item {
                Section(stringResource(R.string.set_privacy)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            stringResource(R.string.set_privacy_body),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    LinkRow(Icons.Outlined.PrivacyTip, stringResource(R.string.privacy_policy)) {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_URL))) }
                    }
                    if (!adsRemoved && Ads.privacyOptionsRequired(context)) {
                        LinkRow(Icons.Outlined.PrivacyTip, stringResource(R.string.ad_choices)) { Ads.showPrivacyOptions(activity) }
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { confirmClear = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.delete_all_saved), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_title)) },
            text = { Text(stringResource(R.string.clear_body)) },
            confirmButton = { TextButton(onClick = { vm.clearAll(); confirmClear = false }) { Text(stringResource(R.string.delete_all)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun RemoveAdsCard(removed: Boolean, price: String?, onBuy: () -> Unit, onRestore: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(28.dp), color = c.primaryContainer, contentColor = c.onPrimaryContainer) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            if (removed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Favorite, null, tint = c.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.ads_removed), style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.ads_thanks), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(stringResource(R.string.remove_ads_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.remove_ads_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBuy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(if (price != null) stringResource(R.string.remove_ads_price, price) else stringResource(R.string.remove_ads), style = MaterialTheme.typography.titleSmall)
                }
                TextButton(onClick = onRestore, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.restore_purchase)) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun SwitchRow(title: String, body: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun AppSwitchRow(pkg: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!enabled) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = rememberAppIcon(pkg)
        if (icon != null) Image(icon, null, Modifier.size(36.dp)) else Spacer(Modifier.size(36.dp))
        Spacer(Modifier.width(16.dp))
        Text(appLabel(context, pkg), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = null)
    }
}

@Composable
private fun LinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
