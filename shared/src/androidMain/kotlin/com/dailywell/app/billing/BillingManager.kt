package com.dailywell.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "dailywell_premium_monthly"
        const val PRODUCT_ANNUAL = "dailywell_premium_annual"
        const val PRODUCT_LIFETIME = "dailywell_premium_lifetime"
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to reconnect
                startConnection()
            }
        })
    }

    private fun queryProducts() {
        val subscriptionList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val inAppList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        // Query subscriptions
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subscriptionList)
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { _, productDetailsList ->
            val currentProducts = _products.value.toMutableList()
            currentProducts.addAll(productDetailsList)
            _products.value = currentProducts
        }

        // Query in-app purchases
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppList)
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { _, productDetailsList ->
            val currentProducts = _products.value.toMutableList()
            currentProducts.addAll(productDetailsList)
            _products.value = currentProducts
        }
    }

    private fun queryPurchases() {
        // Check subscriptions
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { _, purchases ->
            handlePurchaseList(purchases)
        }

        // Check in-app purchases
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { _, purchases ->
            handlePurchaseList(purchases)
        }
    }

    private fun handlePurchaseList(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                _isPremium.value = true
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _isPremium.value = true
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val productDetails = _products.value.find { it.productId == productId } ?: return

        _purchaseState.value = PurchaseState.Loading

        val productDetailsParams = if (productDetails.productType == BillingClient.ProductType.SUBS) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        } else {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun restorePurchases() {
        _purchaseState.value = PurchaseState.Loading
        queryPurchases()
        _purchaseState.value = if (_isPremium.value) {
            PurchaseState.Success
        } else {
            PurchaseState.NothingToRestore
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchaseList(it) }
                _purchaseState.value = PurchaseState.Success
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Cancelled
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(result.debugMessage)
            }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    // For testing without Play Store
    fun setPremiumForTesting(isPremium: Boolean) {
        _isPremium.value = isPremium
    }
}

sealed class PurchaseState {
    data object Idle : PurchaseState()
    data object Loading : PurchaseState()
    data object Success : PurchaseState()
    data object Cancelled : PurchaseState()
    data object NothingToRestore : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}
