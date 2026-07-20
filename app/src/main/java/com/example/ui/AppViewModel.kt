package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class AppViewModel(
    application: Application,
    private val repository: ScoreRepository,
    val sharedPrefs: SharedPrefManager,
    private val synth: AudioSynthEngine
) : AndroidViewModel(application) {

    // Login & Session State
    var userEmail by mutableStateOf(sharedPrefs.userEmail ?: "")
        private set
    var isLoggedIn by mutableStateOf(sharedPrefs.isLoggedIn)
        private set

    // Connectivity State
    var isOnline by mutableStateOf(false)
        private set
    var systemStatusText by mutableStateOf("SYS_ACTIVE: OFFLINE")
        private set

    // Coins & Scoring States (Reactive UI bindings)
    var localCoins by mutableStateOf(sharedPrefs.localCoins)
        private set
    var highScore by mutableStateOf(sharedPrefs.highScore)
        private set

    // Leaderboard List
    val leaderboardScores: StateFlow<List<LeaderboardEntry>> = repository.allScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Ad Simulation Metrics
    var bannerImpressions by mutableStateOf(sharedPrefs.bannerImpressions)
        private set
    var interstitialImpressions by mutableStateOf(sharedPrefs.interstitialImpressions)
        private set
    var rewardedImpressions by mutableStateOf(sharedPrefs.rewardedImpressions)
        private set
    var estimatedRevenue by mutableStateOf(sharedPrefs.estimatedRevenue)
        private set

    // Sliding Window Cooldown State for HUD
    var isCapReached by mutableStateOf(false)
        private set

    // Sound toggle state
    var isSoundOn by mutableStateOf(sharedPrefs.isSoundOn)
        private set

    // --- Space Shooter Canvas Game States ---
    var gameScore by mutableStateOf(0)
    var targetsDestroyedInSession by mutableStateOf(0)
    var coinsEarnedInSession by mutableStateOf(0)
    var currentRevivesUsed by mutableStateOf(0)
    var isGameActive by mutableStateOf(false)
    var isGameOver by mutableStateOf(false)
    
    // Game Entity Objects
    var playerX by mutableStateOf(0.5f) // Normalized 0f..1f
    var bullets = mutableStateOf<List<Bullet>>(emptyList())
    var targets = mutableStateOf<List<Target>>(emptyList())
    var particles = mutableStateOf<List<Particle>>(emptyList())

    // Ad simulations UI controllers
    var activeSimAdOverlay by mutableStateOf<String?>(null) // "INTERSTITIAL", "REWARDED_REVIVE", "REWARDED_DOUBLE", "NONE"
    var simAdProgressSeconds by mutableStateOf(0)

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var gameLoopJob: Job? = null

    init {
        connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        checkInitialNetworkState()
        registerNetworkMonitor()
        refreshLeaderboard()
        checkSlidingWindowCap()
    }

    private fun checkInitialNetworkState() {
        val activeNet = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNet)
        isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        updateStatusText()
        if (isOnline) {
            triggerSyncs()
        }
    }

    private fun registerNetworkMonitor() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
                updateStatusText()
                triggerSyncs()
            }

            override fun onLost(network: Network) {
                isOnline = false
                updateStatusText()
            }
        }

        try {
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e("AppViewModel", "Failed to register network callback", e)
        }
    }

    private fun updateStatusText() {
        systemStatusText = if (isOnline) "SYS_ACTIVE: ONLINE" else "SYS_ACTIVE: OFFLINE"
    }

    private fun triggerSyncs() {
        viewModelScope.launch {
            repository.syncPendingWithFirebase()
            repository.fetchGlobalRanksFromFirebase(isOnline)
        }
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            repository.fetchGlobalRanksFromFirebase(isOnline)
        }
    }

    // Audio Actions
    fun toggleSound() {
        sharedPrefs.isSoundOn = !sharedPrefs.isSoundOn
        isSoundOn = sharedPrefs.isSoundOn
        if (isSoundOn) {
            playTapSound()
        }
    }

    fun playTapSound() {
        synth.playUiTap()
    }

    // Auth actions
    fun loginUser(email: String): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false
        }
        playTapSound()
        sharedPrefs.userEmail = email
        sharedPrefs.isLoggedIn = true
        userEmail = email
        isLoggedIn = true
        localCoins = sharedPrefs.localCoins
        highScore = sharedPrefs.highScore
        triggerSyncs()
        return true
    }

    fun logoutUser() {
        playTapSound()
        sharedPrefs.clearSession()
        userEmail = ""
        isLoggedIn = false
        localCoins = 0
        highScore = 0
    }

    // Anti-Bot Capping Sliding Window check
    fun checkSlidingWindowCap(): Boolean {
        val history = sharedPrefs.getCoinEarningHistory()
        val now = System.currentTimeMillis()
        val windowMs = 30 * 60 * 1000 // 30 minutes
        val validHistory = history.filter { now - it < windowMs }
        sharedPrefs.saveCoinEarningHistory(validHistory)
        
        isCapReached = validHistory.size >= 20
        return isCapReached
    }

    // Record coin transaction with Anti-Bot checks
    fun recordCoinEarned(): Boolean {
        if (checkSlidingWindowCap()) {
            Log.w("AppViewModel", "Anti-Bot alert: Sliding window limit reached! Coin blocked.")
            return false
        }

        // Add coin
        val currentHistory = sharedPrefs.getCoinEarningHistory().toMutableList()
        currentHistory.add(System.currentTimeMillis())
        sharedPrefs.saveCoinEarningHistory(currentHistory)
        
        coinsEarnedInSession++
        val newCoins = localCoins + 1
        localCoins = newCoins
        sharedPrefs.localCoins = newCoins

        synth.playCoin()

        // Sync with db
        viewModelScope.launch {
            repository.saveScoreAndCoins(
                email = userEmail,
                username = userEmail.substringBefore("@"),
                score = highScore,
                coinsEarned = 1,
                isOnline = isOnline
            )
        }

        checkSlidingWindowCap()
        return true
    }

    // Force Adjust coins (Secret Admin Panel)
    fun adminSetCoins(amount: Int) {
        playTapSound()
        sharedPrefs.localCoins = amount
        localCoins = amount
        viewModelScope.launch {
            repository.saveScoreAndCoins(
                email = userEmail,
                username = userEmail.substringBefore("@"),
                score = highScore,
                coinsEarned = 0, // already updated in preferences
                isOnline = isOnline
            )
        }
    }

    // simulated ad triggers
    fun simulateBannerImpression() {
        sharedPrefs.bannerImpressions++
        bannerImpressions = sharedPrefs.bannerImpressions
        estimatedRevenue = sharedPrefs.estimatedRevenue
    }

    fun triggerInterstitialAd(onCompleted: () -> Unit) {
        activeSimAdOverlay = "INTERSTITIAL"
        simAdProgressSeconds = 3 // Interstitial duration
        sharedPrefs.interstitialImpressions++
        interstitialImpressions = sharedPrefs.interstitialImpressions
        estimatedRevenue = sharedPrefs.estimatedRevenue
        
        viewModelScope.launch {
            while (simAdProgressSeconds > 0) {
                delay(1000)
                simAdProgressSeconds--
            }
            activeSimAdOverlay = null
            onCompleted()
        }
    }

    fun triggerRewardedAd(type: String, onCompleted: () -> Unit) {
        activeSimAdOverlay = type
        simAdProgressSeconds = 15 // Rewarded ad duration
        sharedPrefs.rewardedImpressions++
        rewardedImpressions = sharedPrefs.rewardedImpressions
        estimatedRevenue = sharedPrefs.estimatedRevenue

        viewModelScope.launch {
            while (simAdProgressSeconds > 0) {
                delay(1000)
                simAdProgressSeconds--
            }
            activeSimAdOverlay = null
            onCompleted()
        }
    }

    // Diagnostic log exporter
    fun exportDiagnosticLogsJson(context: Context): File? {
        playTapSound()
        val dataMap = mapOf(
            "userEmail" to userEmail,
            "highScore" to highScore,
            "coins" to localCoins,
            "bannerImpressions" to bannerImpressions,
            "interstitialImpressions" to interstitialImpressions,
            "rewardedImpressions" to rewardedImpressions,
            "estimatedRevenue" to estimatedRevenue,
            "capReached" to isCapReached,
            "systemStatus" to systemStatusText,
            "platform" to "Android Native Kotlin/Compose"
        )
        return try {
            val jsonString = com.squareup.moshi.Moshi.Builder()
                .build()
                .adapter(Map::class.java)
                .toJson(dataMap)
            
            val file = File(context.cacheDir, "firing_for_cash_diagnostics.json")
            file.writeText(jsonString)
            file
        } catch (e: Exception) {
            Log.e("AppViewModel", "Failed to export diagnostics JSON", e)
            null
        }
    }

    // --- Space Shooter Canvas Engine Methods ---
    fun launchNewGame() {
        playTapSound()
        gameScore = 0
        targetsDestroyedInSession = 0
        coinsEarnedInSession = 0
        currentRevivesUsed = 0
        isGameOver = false
        isGameActive = true
        bullets.value = emptyList()
        targets.value = emptyList()
        particles.value = emptyList()
        playerX = 0.5f
        checkSlidingWindowCap()
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastBulletTime = 0L
            var lastTargetTime = 0L
            val loopDelay = 16L // ~60fps

            while (isGameActive && !isGameOver) {
                val now = System.currentTimeMillis()

                // 1. Auto-shoot bullet
                if (now - lastBulletTime > 300L) {
                    shootBullet()
                    lastBulletTime = now
                }

                // 2. Spawn targets randomly
                if (now - lastTargetTime > 800L) {
                    spawnTarget()
                    lastTargetTime = now
                }

                // 3. Update game ticks
                updateGameEntities()

                delay(loopDelay)
            }
        }
    }

    private fun shootBullet() {
        synth.playShoot()
        val currentBullets = bullets.value.toMutableList()
        // Spawn bullet centered from player's ship coordinates
        currentBullets.add(Bullet(x = playerX, y = 0.85f))
        bullets.value = currentBullets
    }

    private fun spawnTarget() {
        val currentTargets = targets.value.toMutableList()
        currentTargets.add(
            Target(
                x = (0.05f + Math.random() * 0.9f).toFloat(),
                y = 0.0f,
                speed = (0.005f + Math.random() * 0.008f).toFloat(),
                radius = (0.02f + Math.random() * 0.03f).toFloat() // normalized radius
            )
        )
        targets.value = currentTargets
    }

    private fun updateGameEntities() {
        val currentBullets = bullets.value.map { it.copy(y = it.y - 0.025f) }.filter { it.y > 0f }
        val currentTargets = targets.value.map { it.copy(y = it.y + it.speed) }
        val currentParticles = particles.value.map {
            it.copy(
                x = it.x + it.vx,
                y = it.y + it.vy,
                life = it.life - 0.05f
            )
        }.filter { it.life > 0f }

        val survivingTargets = currentTargets.toMutableList()
        val survivingBullets = currentBullets.toMutableList()
        val newExplosions = mutableListOf<Particle>()

        // Collision Check: Bullets + Targets
        val targetIterator = survivingTargets.iterator()
        while (targetIterator.hasNext()) {
            val target = targetIterator.next()
            
            // Check ship collision first
            // Player is at playerX, y=0.88f with nominal bounding circle
            val distToShip = Math.hypot((target.x - playerX).toDouble(), (target.y - 0.88f).toDouble())
            if (distToShip < (target.radius + 0.04f)) {
                triggerGameOverState()
                return
            }

            // Check bullet collision
            val bulletIterator = survivingBullets.iterator()
            var hit = false
            while (bulletIterator.hasNext()) {
                val bullet = bulletIterator.next()
                val distToBullet = Math.hypot((target.x - bullet.x).toDouble(), (target.y - bullet.y).toDouble())
                if (distToBullet < (target.radius + 0.015f)) {
                    hit = true
                    bulletIterator.remove()
                    break
                }
            }

            if (hit) {
                targetIterator.remove()
                triggerExplosion(target.x, target.y, newExplosions)
                gameScore += 100
                targetsDestroyedInSession++
                
                // Track Virtual Coins: 20 targets destroyed = 1 Virtual Coin
                if (targetsDestroyedInSession % 20 == 0) {
                    recordCoinEarned()
                }
            }
        }

        // Clean out-of-bounds targets
        val finalTargets = survivingTargets.filter {
            if (it.y >= 1.0f) {
                // target missed! Doesn't end game, but clean up
                false
            } else {
                true
            }
        }

        bullets.value = survivingBullets
        targets.value = finalTargets
        particles.value = currentParticles + newExplosions
    }

    private fun triggerExplosion(x: Float, y: Float, list: MutableList<Particle>) {
        synth.playExplosion()
        for (i in 0 until 12) {
            val angle = Math.random() * 2.0 * Math.PI
            val velocity = (0.005f + Math.random() * 0.01f).toFloat()
            list.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * velocity).toFloat(),
                    vy = (Math.sin(angle) * velocity).toFloat(),
                    colorType = (0..2).random(),
                    life = 1.0f
                )
            )
        }
    }

    private fun triggerGameOverState() {
        synth.playGameOver()
        isGameActive = false
        isGameOver = true
        gameLoopJob?.cancel()

        // Sync local High Score
        if (gameScore > highScore) {
            highScore = gameScore
            sharedPrefs.highScore = gameScore
            viewModelScope.launch {
                repository.saveScoreAndCoins(
                    email = userEmail,
                    username = userEmail.substringBefore("@"),
                    score = gameScore,
                    coinsEarned = 0,
                    isOnline = isOnline
                )
            }
        }

        // Trigger Interstitial simulation automatically
        triggerInterstitialAd {
            Log.d("AppViewModel", "Simulated Interstitial Ad Closed after game over.")
        }
    }

    fun handleRevive() {
        if (currentRevivesUsed >= 4) return
        
        triggerRewardedAd("REWARDED_REVIVE") {
            currentRevivesUsed++
            isGameOver = false
            isGameActive = true
            targets.value = emptyList()
            bullets.value = emptyList()
            startGameLoop()
        }
    }

    fun handleDoubleCoins() {
        triggerRewardedAd("REWARDED_DOUBLE") {
            // double the coins earned during this specific session
            val additionalCoins = coinsEarnedInSession
            coinsEarnedInSession *= 2
            
            // save to account securely
            for (i in 0 until additionalCoins) {
                recordCoinEarned()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // ignore
            }
        }
        gameLoopJob?.cancel()
    }
}

// Simple Game Entities
data class Bullet(val x: Float, val y: Float)
data class Target(val x: Float, val y: Float, val speed: Float, val radius: Float)
data class Particle(val x: Float, val y: Float, val vx: Float, val vy: Float, val colorType: Int, val life: Float)

// AppViewModel Factory helper
class AppViewModelFactory(
    private val application: Application,
    private val repository: ScoreRepository,
    private val sharedPrefs: SharedPrefManager,
    private val synth: AudioSynthEngine
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(application, repository, sharedPrefs, synth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
