package app.echo.messages.ads

import android.app.Activity
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.echo.messages.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Consent first (required by Google in the EEA/UK), then the ads SDK. */
object Ads {
    val ready = MutableStateFlow(false)
    private var started = false

    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { if (info.canRequestAds()) start(activity) } },
            { if (info.canRequestAds()) start(activity) },
        )
        if (info.canRequestAds()) start(activity) // consent from an earlier session
    }

    private fun start(context: Context) {
        if (started) return
        started = true
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context.applicationContext) { ready.value = true }
        }
    }

    fun privacyOptionsRequired(context: Context) =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) = UserMessagingPlatform.showPrivacyOptionsForm(activity) {}
}

/**
 * One in-feed native ad, drawn as a card that's clearly an ad (badge + AdChoices) and never overlaps content.
 * Renders nothing until an ad has loaded, so there's no empty hole in the list.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ready by Ads.ready.collectAsState()
    var ad by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(ready) {
        if (!ready || ad != null) return@LaunchedEffect
        AdLoader.Builder(context, BuildConfig.NATIVE_AD_UNIT)
            .forNativeAd { ad = it }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) = Unit
            })
            .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }
    DisposableEffect(Unit) { onDispose { ad?.destroy() } }

    val loaded = ad ?: return
    val c = MaterialTheme.colorScheme
    val colors = AdColors(
        card = c.surfaceContainerLow.toArgb(),
        title = c.onSurface.toArgb(),
        body = c.onSurfaceVariant.toArgb(),
        badge = c.secondaryContainer.toArgb(),
        onBadge = c.onSecondaryContainer.toArgb(),
        cta = c.primary.toArgb(),
        onCta = c.onPrimary.toArgb(),
    )
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { buildAdView(it, colors).root },
        update = { (it.tag as AdViews).bind(loaded) },
    )
}

private data class AdColors(val card: Int, val title: Int, val body: Int, val badge: Int, val onBadge: Int, val cta: Int, val onCta: Int)

private class AdViews(val root: NativeAdView) {
    lateinit var icon: ImageView
    lateinit var headline: TextView
    lateinit var bodyText: TextView
    lateinit var media: MediaView
    lateinit var cta: TextView

    fun bind(ad: NativeAd) {
        headline.text = ad.headline
        bodyText.text = ad.body
        bodyText.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
        cta.text = ad.callToAction ?: "Open"
        ad.icon?.drawable?.let { icon.setImageDrawable(it); icon.visibility = View.VISIBLE } ?: run { icon.visibility = View.GONE }
        media.mediaContent = ad.mediaContent
        root.setNativeAd(ad)
    }
}

private fun buildAdView(context: Context, c: AdColors): AdViews {
    fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).toInt()
    fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    fun clip(view: View, radius: Int) {
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) = o.setRoundRect(0, 0, v.width, v.height, dp(radius).toFloat())
        }
        view.clipToOutline = true
    }

    val root = AdViews(NativeAdView(context))
    root.root.tag = root
    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(c.card, 24)
        setPadding(dp(16), dp(16), dp(16), dp(16))
    }
    val header = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    root.icon = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; clip(this, 12) }
    header.addView(root.icon, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(12) })
    val titles = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    val badge = TextView(context).apply {
        text = "Ad"
        setTextColor(c.onBadge)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        background = rounded(c.badge, 6)
        setPadding(dp(6), dp(1), dp(6), dp(1))
    }
    titles.addView(badge, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
    root.headline = TextView(context).apply {
        setTextColor(c.title); setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    titles.addView(root.headline)
    header.addView(titles, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(28) }) // room for AdChoices
    column.addView(header)
    root.bodyText = TextView(context).apply {
        setTextColor(c.body); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); maxLines = 2; ellipsize = TextUtils.TruncateAt.END
    }
    column.addView(root.bodyText, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })
    root.media = MediaView(context).apply { clip(this, 16) }
    column.addView(root.media, LinearLayout.LayoutParams(MATCH_PARENT, dp(160)).apply { topMargin = dp(12) })
    root.cta = TextView(context).apply {
        setTextColor(c.onCta); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        background = rounded(c.cta, 24)
    }
    column.addView(root.cta, LinearLayout.LayoutParams(MATCH_PARENT, dp(48)).apply { topMargin = dp(12) })
    root.root.addView(column, MATCH_PARENT, WRAP_CONTENT)
    root.root.headlineView = root.headline
    root.root.bodyView = root.bodyText
    root.root.iconView = root.icon
    root.root.mediaView = root.media
    root.root.callToActionView = root.cta
    return root
}
