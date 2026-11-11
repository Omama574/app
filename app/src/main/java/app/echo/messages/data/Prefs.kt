package app.echo.messages.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small settings store; each value is a StateFlow so Compose and the service see changes instantly. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("echo", Context.MODE_PRIVATE)

    private fun bool(key: String, default: Boolean) = MutableStateFlow(sp.getBoolean(key, default))

    private val _adsRemoved = bool("ads_removed", false)
    val adsRemoved: StateFlow<Boolean> = _adsRemoved
    fun setAdsRemoved(v: Boolean) { sp.edit().putBoolean("ads_removed", v).apply(); _adsRemoved.value = v }

    private val _alertOnDelete = bool("alert_on_delete", true)
    val alertOnDelete: StateFlow<Boolean> = _alertOnDelete
    fun setAlertOnDelete(v: Boolean) { sp.edit().putBoolean("alert_on_delete", v).apply(); _alertOnDelete.value = v }

    /** 0 = keep forever. */
    private val _keepDays = MutableStateFlow(sp.getInt("keep_days", 90))
    val keepDays: StateFlow<Int> = _keepDays
    fun setKeepDays(v: Int) { sp.edit().putInt("keep_days", v).apply(); _keepDays.value = v }

    /** Messaging apps the user switched off. Everything supported is on by default. */
    private val _mutedApps = MutableStateFlow(sp.getStringSet("muted_apps", emptySet()).orEmpty().toSet())
    val mutedApps: StateFlow<Set<String>> = _mutedApps
    fun setAppEnabled(pkg: String, enabled: Boolean) {
        val next = if (enabled) _mutedApps.value - pkg else _mutedApps.value + pkg
        sp.edit().putStringSet("muted_apps", next).apply()
        _mutedApps.value = next
    }
}
