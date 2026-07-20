package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ScoreRepository(
    private val leaderboardDao: LeaderboardDao,
    private val sharedPrefManager: SharedPrefManager,
    context: Context
) {
    val allScores: Flow<List<LeaderboardEntry>> = leaderboardDao.getAllScores()
    
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun saveScoreAndCoins(
        email: String,
        username: String,
        score: Int,
        coinsEarned: Int,
        isOnline: Boolean
    ) = withContext(Dispatchers.IO) {
        // Fetch current score in local Room db to see if it's a new high score
        val currentEntry = leaderboardDao.getScoreByEmail(email)
        val existingHighScore = currentEntry?.highScore ?: 0
        val existingCoins = currentEntry?.coins ?: 0

        val newHighScore = maxOf(existingHighScore, score)
        val newTotalCoins = existingCoins + coinsEarned

        // Save local shared prefs for current logged in user if match
        if (email == sharedPrefManager.userEmail) {
            sharedPrefManager.highScore = newHighScore
            sharedPrefManager.localCoins = newTotalCoins
        }

        val updatedEntry = LeaderboardEntry(
            email = email,
            username = username,
            highScore = newHighScore,
            coins = newTotalCoins,
            timestamp = System.currentTimeMillis(),
            pendingSync = true // mark pending sync
        )

        // Write to local cache (Room)
        leaderboardDao.insertOrUpdate(updatedEntry)

        // If online, immediately attempt to sync
        if (isOnline) {
            syncEntryWithFirebase(updatedEntry)
        }
    }

    suspend fun syncPendingWithFirebase() = withContext(Dispatchers.IO) {
        val pendings = leaderboardDao.getPendingSyncScores()
        Log.d("ScoreRepository", "Syncing ${pendings.size} pending entries with Firebase.")
        for (entry in pendings) {
            syncEntryWithFirebase(entry)
        }
    }

    private suspend fun syncEntryWithFirebase(entry: LeaderboardEntry) {
        try {
            val docRef = db.collection("leaderboard").document(entry.email)
            
            // Transactional update on Firebase to ensure scores and coins never get overwritten by lower values
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                
                val cloudScore = snapshot.getLong("highScore") ?: 0L
                val cloudCoins = snapshot.getLong("coins") ?: 0L

                val finalScore = maxOf(cloudScore, entry.highScore.toLong())
                // Ensure coins update transactionally: we take the maximum of local total coins or cloud total coins,
                // or compute it appropriately. Here, since Room's entry is local truth, if local is greater, use local,
                // otherwise use cloud. To be perfectly transactional, we check:
                val finalCoins = maxOf(cloudCoins, entry.coins.toLong())

                transaction.set(
                    docRef, mapOf(
                        "email" to entry.email,
                        "username" to entry.username,
                        "highScore" to finalScore,
                        "coins" to finalCoins,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }.await()

            // If transaction succeeded, mark as synced in local DB
            leaderboardDao.markSynced(entry.email)
            Log.d("ScoreRepository", "Successfully synced score transactionally for ${entry.email}")
        } catch (e: Exception) {
            Log.e("ScoreRepository", "Firebase sync failed for ${entry.email}: ${e.message}")
        }
    }

    suspend fun fetchGlobalRanksFromFirebase(isOnline: Boolean) = withContext(Dispatchers.IO) {
        if (!isOnline) return@withContext

        try {
            val querySnapshot = db.collection("leaderboard")
                .orderBy("highScore", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            for (doc in querySnapshot.documents) {
                val email = doc.getString("email") ?: continue
                val username = doc.getString("username") ?: email.substringBefore("@")
                val highScore = doc.getLong("highScore")?.toInt() ?: 0
                val coins = doc.getLong("coins")?.toInt() ?: 0

                // Sync back to local Room database
                val localEntry = leaderboardDao.getScoreByEmail(email)
                val localHighScore = localEntry?.highScore ?: 0
                val localCoins = localEntry?.coins ?: 0

                // Update local Room database if cloud score or coins is higher
                if (highScore > localHighScore || coins > localCoins) {
                    leaderboardDao.insertOrUpdate(
                        LeaderboardEntry(
                            email = email,
                            username = username,
                            highScore = maxOf(localHighScore, highScore),
                            coins = maxOf(localCoins, coins),
                            timestamp = System.currentTimeMillis(),
                            pendingSync = false
                        )
                    )
                }
            }
            Log.d("ScoreRepository", "Synced global leaderboard ranks into local cache Room.")
        } catch (e: Exception) {
            Log.e("ScoreRepository", "Failed to pull global ranks from Firebase: ${e.message}")
        }
    }
}
