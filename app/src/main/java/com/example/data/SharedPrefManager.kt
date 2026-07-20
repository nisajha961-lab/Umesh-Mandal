package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SharedPrefManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("firing_for_cash_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    companion object {
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SOUND_ON = "sound_on"
        private const val KEY_LOCAL_COINS = "local_coins"
        private const val KEY_HIGH_SCORE = "high_score"
        
        // Ads metrics keys
        private const val KEY_BANNER_IMPS = "banner_imps"
        private const val KEY_INTERSTITIAL_IMPS = "interstitial_imps"
        private const val KEY_REWARDED_IMPS = "rewarded_imps"
        private const val KEY_ESTIMATED_REVENUE = "estimated_revenue"
        
        // Coin history for anti-bot sliding window
        private const val KEY_COIN_HISTORY = "coin_history"
    }

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var isSoundOn: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ON, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ON, value).apply()

    var localCoins: Int
        get() = prefs.getInt(KEY_LOCAL_COINS, 0)
        set(value) = prefs.edit().putInt(KEY_LOCAL_COINS, value).apply()

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()

    // Ads simulation counters
    var bannerImpressions: Int
        get() = prefs.getInt(KEY_BANNER_IMPS, 0)
        set(value) = {
            prefs.edit().putInt(KEY_BANNER_IMPS, value).apply()
            recalculateRevenue()
        }()

    var interstitialImpressions: Int
        get() = prefs.getInt(KEY_INTERSTITIAL_IMPS, 0)
        set(value) = {
            prefs.edit().putInt(KEY_INTERSTITIAL_IMPS, value).apply()
            recalculateRevenue()
        }()

    var rewardedImpressions: Int
        get() = prefs.getInt(KEY_REWARDED_IMPS, 0)
        set(value) = {
            prefs.edit().putInt(KEY_REWARDED_IMPS, value).apply()
            recalculateRevenue()
        }()

    var estimatedRevenue: Double
        get() = prefs.getFloat(KEY_ESTIMATED_REVENUE, 0.0f).toDouble()
        private set(value) = prefs.edit().putFloat(KEY_ESTIMATED_REVENUE, value.toFloat()).apply()

    private fun recalculateRevenue() {
        // eCPM: Rewarded: ₹70.00/1k, Interstitial: ₹30.00/1k, Banner: ₹10.00/1k
        val rev = (rewardedImpressions * 0.07) + (interstitialImpressions * 0.03) + (bannerImpressions * 0.01)
        estimatedRevenue = rev
    }

    // Sliding window of coin earnings to prevent bots
    // Elements are timestamps in milliseconds
    fun getCoinEarningHistory(): List<Long> {
        val json = prefs.getString(KEY_COIN_HISTORY, null) ?: return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, java.lang.Long::class.java)
            val adapter = moshi.adapter<List<Long>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCoinEarningHistory(history: List<Long>) {
        try {
            val type = Types.newParameterizedType(List::class.java, java.lang.Long::class.java)
            val adapter = moshi.adapter<List<Long>>(type)
            val json = adapter.toJson(history)
            prefs.edit().putString(KEY_COIN_HISTORY, json).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_LOCAL_COINS)
            .remove(KEY_HIGH_SCORE)
            .apply()
    }
}
