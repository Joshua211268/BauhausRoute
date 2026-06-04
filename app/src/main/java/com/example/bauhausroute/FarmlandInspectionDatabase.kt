package com.example.bauhausroute

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val userId: Long = 0,
    val email: String,
    val password: String,
    val userRole: UserRole
)

@Entity(
    tableName = "farmland_inspections",
    foreignKeys = [
        ForeignKey(
            entity = UserAccountEntity::class,
            parentColumns = ["userId"],
            childColumns = ["ownerUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ownerUserId")]
)
data class FarmerFarmlandEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val farmlandId: Long = 0,
    val ownerUserId: Long = LEGACY_LOCAL_USER_ID,
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
) {
    val id: Long
        get() = farmlandId
}

@Entity(
    tableName = "team_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserAccountEntity::class,
            parentColumns = ["userId"],
            childColumns = ["assignedUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("assignedUserId")]
)
data class TeamProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val teamId: Long = 0,
    val assignedUserId: Long = LEGACY_LOCAL_USER_ID,
    val teamName: String,
    val contactName: String,
    val phone: String,
    val email: String,
    val teamSize: Int,
    val city: String,
    val district: String,
    val createdAtMillis: Long
)

@Entity(
    tableName = "cleaner_tasks",
    foreignKeys = [
        ForeignKey(
            entity = UserAccountEntity::class,
            parentColumns = ["userId"],
            childColumns = ["assignedUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("assignedUserId")]
)
data class CleanerTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val taskId: Long = 0,
    val assignedUserId: Long,
    val teamName: String,
    val contactName: String,
    val phone: String,
    val teamSize: Int,
    val serviceArea: String,
    val taskImageUris: List<String> = emptyList(),
    val severityLevel: String,
    val status: String,
    val garbageCount: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(account: UserAccountEntity): Long

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE email = :email AND password = :password LIMIT 1")
    suspend fun authenticate(email: String, password: String): UserAccountEntity?

    @Transaction
    suspend fun save(account: UserAccountEntity): Long {
        val existing = findByEmail(account.email)
        if (existing != null) {
            return existing.userId
        }
        val insertedId = insert(account)
        if (insertedId != -1L) {
            return insertedId
        }
        return findByEmail(account.email)?.userId
            ?: error("Unable to save account for ${account.email}")
    }
}

@Dao
interface FarmerDao {
    @Query("SELECT * FROM farmland_inspections WHERE ownerUserId = :ownerUserId ORDER BY timestampMillis DESC")
    fun observeForOwner(ownerUserId: Long): Flow<List<FarmerFarmlandEntity>>

    @Query("SELECT * FROM farmland_inspections WHERE id = :farmlandId AND ownerUserId = :ownerUserId LIMIT 1")
    fun observeById(farmlandId: Long, ownerUserId: Long): Flow<FarmerFarmlandEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FarmerFarmlandEntity)

    @Delete
    suspend fun delete(entity: FarmerFarmlandEntity)

    @Query("UPDATE farmland_inspections SET detailImages = :detailImages WHERE id = :farmlandId AND ownerUserId = :ownerUserId")
    suspend fun updateDetailImages(
        farmlandId: Long,
        ownerUserId: Long,
        detailImages: List<String>
    )
}

@Dao
interface CleanerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamProfile(entity: TeamProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(entity: CleanerTaskEntity)

    @Query("SELECT * FROM cleaner_tasks WHERE assignedUserId = :assignedUserId ORDER BY timestampMillis DESC")
    fun observeTasksForUser(assignedUserId: Long): Flow<List<CleanerTaskEntity>>

    @Query("SELECT * FROM team_profiles WHERE assignedUserId = :assignedUserId LIMIT 1")
    suspend fun findTeamProfile(assignedUserId: Long): TeamProfileEntity?

    @Query("UPDATE cleaner_tasks SET taskImageUris = :taskImageUris WHERE taskId = :taskId AND assignedUserId = :assignedUserId")
    suspend fun updateTaskImages(
        taskId: Long,
        assignedUserId: Long,
        taskImageUris: List<String>
    )
}

class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { index -> array.getString(index) }
    }
}

class UserRoleConverter {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching {
        UserRole.valueOf(value)
    }.getOrDefault(UserRole.FARMER)
}

@Database(
    entities = [
        UserAccountEntity::class,
        FarmerFarmlandEntity::class,
        TeamProfileEntity::class,
        CleanerTaskEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(StringListConverter::class, UserRoleConverter::class)
abstract class FarmlandInspectionDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun farmerDao(): FarmerDao
    abstract fun cleanerDao(): CleanerDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
                        id, regionName, coverImageUri, detailImages, latitude, longitude,
                        severity, processStatus, trashCount, description, timestampMillis
                    )
                    SELECT
                        id, regionName, imageUri, '[]', latitude, longitude,
                        severity, processStatus, trashCount, description, timestampMillis
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_accounts (
                        userId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        email TEXT NOT NULL,
                        password TEXT NOT NULL,
                        userRole TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_user_accounts_email ON user_accounts (email)"
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO user_accounts (userId, email, password, userRole)
                    VALUES ($LEGACY_LOCAL_USER_ID, 'legacy.local@device', '', 'FARMER')
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE farmland_inspections_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ownerUserId INTEGER NOT NULL DEFAULT $LEGACY_LOCAL_USER_ID,
                        regionName TEXT NOT NULL,
                        coverImageUri TEXT NOT NULL,
                        detailImages TEXT NOT NULL DEFAULT '[]',
                        latitude REAL,
                        longitude REAL,
                        severity TEXT NOT NULL,
                        processStatus TEXT NOT NULL,
                        trashCount INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        timestampMillis INTEGER NOT NULL,
                        FOREIGN KEY(ownerUserId) REFERENCES user_accounts(userId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO farmland_inspections_new (
                        id, ownerUserId, regionName, coverImageUri, detailImages, latitude,
                        longitude, severity, processStatus, trashCount, description, timestampMillis
                    )
                    SELECT
                        id, $LEGACY_LOCAL_USER_ID, regionName, coverImageUri, detailImages, latitude,
                        longitude, severity, processStatus, trashCount, description, timestampMillis
                    FROM farmland_inspections
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE farmland_inspections")
                db.execSQL("ALTER TABLE farmland_inspections_new RENAME TO farmland_inspections")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_farmland_inspections_ownerUserId ON farmland_inspections (ownerUserId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE team_profiles (
                        teamId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        assignedUserId INTEGER NOT NULL DEFAULT $LEGACY_LOCAL_USER_ID,
                        teamName TEXT NOT NULL,
                        contactName TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT NOT NULL,
                        teamSize INTEGER NOT NULL,
                        city TEXT NOT NULL,
                        district TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        FOREIGN KEY(assignedUserId) REFERENCES user_accounts(userId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO team_profiles (
                        teamId, assignedUserId, teamName, contactName, phone, email,
                        teamSize, city, district, createdAtMillis
                    )
                    SELECT
                        id, $LEGACY_LOCAL_USER_ID, teamName, contactName, phone, email,
                        teamSize, city, district, createdAtMillis
                    FROM TeamProfile
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE TeamProfile")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_team_profiles_assignedUserId ON team_profiles (assignedUserId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE cleaner_tasks (
                        taskId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        assignedUserId INTEGER NOT NULL,
                        teamName TEXT NOT NULL,
                        contactName TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        teamSize INTEGER NOT NULL,
                        serviceArea TEXT NOT NULL,
                        taskImageUris TEXT NOT NULL DEFAULT '[]',
                        severityLevel TEXT NOT NULL,
                        status TEXT NOT NULL,
                        garbageCount INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        description TEXT NOT NULL,
                        timestampMillis INTEGER NOT NULL,
                        FOREIGN KEY(assignedUserId) REFERENCES user_accounts(userId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_cleaner_tasks_assignedUserId ON cleaner_tasks (assignedUserId)"
                )
            }
        }
    }
}

const val LEGACY_LOCAL_USER_ID = 1L
