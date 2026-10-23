package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMemoryDao {
    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    suspend fun getMemoriesList(): List<UserMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: UserMemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: UserMemoryEntity)

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 30")
    suspend fun getRecentMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Database(entities = [UserMemoryEntity::class, ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class AhiDatabase : RoomDatabase() {
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AhiDatabase? = null

        fun getDatabase(context: Context): AhiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AhiDatabase::class.java,
                    "ahi_companion.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
