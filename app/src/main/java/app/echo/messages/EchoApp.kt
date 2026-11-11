package app.echo.messages

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import app.echo.messages.billing.Billing
import app.echo.messages.data.EchoDb
import app.echo.messages.data.Prefs

/** The whole object graph: three singletons don't need a DI framework. */
class EchoApp : Application() {
    val db by lazy { EchoDb.create(this) }
    val prefs by lazy { Prefs(this) }
    val billing by lazy { Billing(this, BuildConfig.REMOVE_ADS_PRODUCT) { prefs.setAdsRemoved(it) } }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_DELETED, "Deleted messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tells you the moment someone deletes a message, with what it said."
            }
        )
    }

    companion object {
        const val CHANNEL_DELETED = "deleted"
        const val EXTRA_PKG = "pkg"
        const val EXTRA_CONVERSATION = "conversation"
    }
}

val Context.echo: EchoApp get() = applicationContext as EchoApp
