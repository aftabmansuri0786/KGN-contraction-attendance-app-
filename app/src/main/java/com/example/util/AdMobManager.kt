package com.example.util

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobConfig {
    private const val TAG = "AdMobConfig"

    // Official Google Test AdMob IDs (Safe for development without self-click bans)
    const val TEST_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Live Production IDs: Place your real AdMob unit IDs here when ready for release.
    // By default, if left blank, the app will safely fall back to Google Test Ads.
    var LIVE_BANNER_AD_UNIT_ID: String = ""
    var LIVE_INTERSTITIAL_AD_UNIT_ID: String = ""

    val bannerAdUnitId: String
        get() = LIVE_BANNER_AD_UNIT_ID.ifBlank { TEST_BANNER_AD_UNIT_ID }

    val interstitialAdUnitId: String
        get() = LIVE_INTERSTITIAL_AD_UNIT_ID.ifBlank { TEST_INTERSTITIAL_AD_UNIT_ID }

    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                MobileAds.initialize(context) { status ->
                    Log.d(TAG, "AdMob MobileAds initialized successfully: $status")
                }
                isInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MobileAds", e)
            }
        }
    }
}

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.bannerAdUnitId,
    adSize: AdSize = AdSize.BANNER
) {
    if (LocalInspectionMode.current) {
        // Preview mode placeholder
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier.padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Google AdMob Banner Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var adLoadFailed by remember { mutableStateOf(false) }

    val adView = remember {
        AdView(context).apply {
            setAdSize(adSize)
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    isAdLoaded = true
                    adLoadFailed = false
                    Log.d("AdMobBanner", "Ad loaded successfully: $adUnitId")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    isAdLoaded = false
                    adLoadFailed = true
                    Log.w("AdMobBanner", "Ad failed to load: ${error.message} (code: ${error.code})")
                }
            }
        }
    }

    DisposableEffect(adView) {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        onDispose {
            adView.destroy()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Transparent subtle "Ad / विज्ञापन" compliance label
        Text(
            text = "Ad • विज्ञापन",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 2.dp)
        )

        AndroidView(
            factory = { adView },
            modifier = Modifier.testTag("admob_banner_view")
        )
    }
}
