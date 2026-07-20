package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey val email: String,
    val username: String,
    val highScore: Int,
    val coins: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = false
)

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY highScore DESC, timestamp ASC")
    fun getAllScores(): Flow<List<LeaderboardEntry>>

    @Query("SELECT * FROM leaderboard WHERE email = :email LIMIT 1")
    suspend fun getScoreByEmail(email: String): LeaderboardEntry?

    @Query("SELECT * FROM leaderboard WHERE pendingSync = 1")
    suspend fun getPendingSyncScores(): List<LeaderboardEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: LeaderboardEntry)

    @Query("UPDATE leaderboard SET pendingSync = 0 WHERE email = :email")
    suspend fun markSynced(email: String)

    @Query("DELETE FROM leaderboard")
    suspend fun clearAll()
}

@Database(entities = [LeaderboardEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leaderboardDao(): LeaderboardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "firing_for_cash_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
