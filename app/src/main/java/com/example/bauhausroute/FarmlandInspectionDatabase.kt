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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

@Entity(tableName = "farmland_inspections")
data class FarmlandInspectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val regionName: String,
    val coverImageUri: String,
    val detailImages: List<String> = emptyList(),
    val latitude: Double?,
    val longitude: Double?,
    val severity: String,
    val processStatus: String,
    val trashCount: Int,
    val description: String,
    val timestampMillis: Long
)

@Entity(tableName = "TeamProfile")
data class TeamProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamName: String,
    val contactName: String,
    val phone: String,
    val email: String,
    val teamSize: Int,
    val city: String,
    val district: String,
    val createdAtMillis: Long
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

    @Query("UPDATE farmland_inspections SET detailImages = :detailImages WHERE id = :id")
    suspend fun updateDetailImages(id: Long, detailImages: List<String>)
}

@Dao
interface TeamProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TeamProfileEntity)
}

class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { index -> array.getString(index) }
    }
}

@Database(
    entities = [FarmlandInspectionEntity::class, TeamProfileEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class FarmlandInspectionDatabase : RoomDatabase() {
    abstract fun farmlandInspectionDao(): FarmlandInspectionDao
    abstract fun teamProfileDao(): TeamProfileDao

    companion object {
        @Volatile
        private var instance: FarmlandInspectionDatabase? = null

        fun get(context: Context): FarmlandInspectionDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FarmlandInspectionDatabase::class.java,
                    "farmland_inspections.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE farmland_inspections ADD COLUMN imageUris TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE farmland_inspections_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        regionName TEXT NOT NULL,
                        coverImageUri TEXT NOT NULL,
                        detailImages TEXT NOT NULL DEFAULT '[]',
                        latitude REAL,
                        longitude REAL,
                        severity TEXT NOT NULL,
                        processStatus TEXT NOT NULL,
                        trashCount INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        timestampMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO farmland_inspections_new (
                        id,
                        regionName,
                        coverImageUri,
                        detailImages,
                        latitude,
                        longitude,
                        severity,
                        processStatus,
                        trashCount,
                        description,
                        timestampMillis
                    )
                    SELECT
                        id,
                        regionName,
                        imageUri,
                        '[]',
                        latitude,
                        longitude,
                        severity,
                        processStatus,
                        trashCount,
                        description,
                        timestampMillis
                    FROM farmland_inspections
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE farmland_inspections")
                db.execSQL("ALTER TABLE farmland_inspections_new RENAME TO farmland_inspections")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS TeamProfile (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        teamName TEXT NOT NULL,
                        contactName TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT NOT NULL,
                        teamSize INTEGER NOT NULL,
                        city TEXT NOT NULL,
                        district TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
