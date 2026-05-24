package com.example.bauhausroute

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "farmland_inspections")
data class FarmlandInspectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val regionName: String,
    val imageUri: String,
    val latitude: Double?,
    val longitude: Double?,
    val severity: String,
    val processStatus: String,
    val trashCount: Int,
    val description: String,
    val timestampMillis: Long
)

@Dao
interface FarmlandInspectionDao {
    @Query("SELECT * FROM farmland_inspections ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<FarmlandInspectionEntity>>

    @Query("SELECT * FROM farmland_inspections WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<FarmlandInspectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FarmlandInspectionEntity)

    @Delete
    suspend fun delete(entity: FarmlandInspectionEntity)
}

@Database(
    entities = [FarmlandInspectionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FarmlandInspectionDatabase : RoomDatabase() {
    abstract fun farmlandInspectionDao(): FarmlandInspectionDao

    companion object {
        @Volatile
        private var instance: FarmlandInspectionDatabase? = null

        fun get(context: Context): FarmlandInspectionDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FarmlandInspectionDatabase::class.java,
                    "farmland_inspections.db"
                ).build().also { instance = it }
            }
        }
    }
}
