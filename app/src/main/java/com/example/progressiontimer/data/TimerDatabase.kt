package com.example.progressiontimer.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "event_folders")
data class EventFolder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isHigherBetter: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "time_records",
    foreignKeys = [
        ForeignKey(
            entity = EventFolder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("folderId")],
)
data class TimeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: Int,
    val timeInMillis: Long,
    val dateRecorded: Long = System.currentTimeMillis(),
)

@Dao
interface TimerDao {
    @Insert
    suspend fun insertFolder(folder: EventFolder): Long

    @Insert
    suspend fun insertTime(record: TimeRecord): Long

    @Query("SELECT COUNT(*) FROM event_folders")
    suspend fun folderCount(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM event_folders")
    suspend fun maxSortOrder(): Int

    @Query("SELECT * FROM event_folders ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeFolders(): Flow<List<EventFolder>>

    @Query("SELECT * FROM event_folders WHERE id = :folderId LIMIT 1")
    fun observeFolder(folderId: Int): Flow<EventFolder?>

    @Query("SELECT * FROM time_records ORDER BY dateRecorded ASC")
    fun observeAllRecords(): Flow<List<TimeRecord>>

    @Query("SELECT * FROM time_records WHERE folderId = :folderId ORDER BY dateRecorded ASC")
    fun observeRecords(folderId: Int): Flow<List<TimeRecord>>

    @Query("DELETE FROM time_records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: Int)

    @Query("DELETE FROM event_folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Int)

    @Query("UPDATE event_folders SET sortOrder = :sortOrder WHERE id = :folderId")
    suspend fun updateFolderSortOrder(folderId: Int, sortOrder: Int)
}

@Database(
    entities = [EventFolder::class, TimeRecord::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "progression_timer.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
