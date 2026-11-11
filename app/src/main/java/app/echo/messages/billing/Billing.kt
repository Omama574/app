package app.echo.messages.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One non-consumable product, bought once, owned forever.
 * shortcut: purchases are trusted on-device (no server verification); add a server check only if piracy matters.
 */
class Billing(
    context: Context,
    private val productId: String,
    private val onOwned: (Boolean) -> Unit,
) : PurchasesUpdatedListener {

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private var details: ProductDetails? = null
    private val _price = MutableStateFlow<String?>(null)
    /** Localised price ("₹99", "$2.99"), or null until Play answers. */
    val price: StateFlow<String?> = _price

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingResponseCode.OK) return
                loadProduct()
                restore()
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(ProductType.INAPP)
                    .build()
            )
        ).build()
        client.queryProductDetailsAsync(params) { _, result ->
            details = result.productDetailsList.firstOrNull()
            _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    fun restore() {
        if (!client.isReady) return connect()
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()) { result, purchases ->
            // Only an OK answer can revoke (e.g. a refund); offline errors keep what we cached.
            if (result.responseCode == BillingResponseCode.OK) onOwned(handle(purchases))
        }
    }

    /** Returns false when Play hasn't sent the product yet (offline or not set up in Play Console). */
    fun buy(activity: Activity): Boolean {
        val product = details ?: return false.also { connect() }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build())
        ).build()
        return client.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingResponseCode.OK && handle(purchases.orEmpty())) onOwned(true)
        if (result.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) restore()
    }

    private fun handle(purchases: List<Purchase>): Boolean {
        val owned = purchases.firstOrNull { productId in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            ?: return false
        if (!owned.isAcknowledged) {
            // Unacknowledged purchases are refunded by Play after 3 days.
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(owned.purchaseToken).build()) {}
        }
        return true
    }
}
