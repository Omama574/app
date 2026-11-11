package app.echo.messages

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.echo.messages.ads.Ads
import app.echo.messages.ui.EchoRoot
import app.echo.messages.ui.theme.EchoTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val openChat = MutableStateFlow<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        route(intent)
        echo.billing.connect()
        if (!echo.prefs.adsRemoved.value) Ads.gatherConsent(this)
        setContent { EchoTheme { EchoRoot(openChat) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        route(intent)
    }

    private fun route(intent: Intent?) {
        val pkg = intent?.getStringExtra(EchoApp.EXTRA_PKG) ?: return
        val conversation = intent.getStringExtra(EchoApp.EXTRA_CONVERSATION) ?: return
        openChat.value = pkg to conversation
    }
}
