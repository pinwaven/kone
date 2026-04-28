package poct.device.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import poct.device.app.entity.CardConfig
import poct.device.app.entity.Case
import poct.device.app.entity.SysConfig
import poct.device.app.entity.User
import poct.device.app.entity.dao.CardConfigDao
import poct.device.app.entity.dao.CaseDao
import poct.device.app.entity.dao.SysConfigDao
import poct.device.app.entity.dao.UserDao
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDate
import java.time.LocalDateTime

@Database(
    entities = [User::class, Case::class, CardConfig::class, SysConfig::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(LocalDateConverter::class, LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun caseDao(): CaseDao
    abstract fun cardConfigDao(): CardConfigDao
    abstract fun sysConfigDao(): SysConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // val dbPath = AppFileUtils.getBaseFileDirPath() + "/database/nanovate.db"
                val dbPath = context.getDatabasePath("nanovate.db").absolutePath
                println("dbPath：$dbPath")

                //val dbPath = "nanovate.db"
                val MIGRATION_1_2 = object : Migration(1, 2) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `cutOff5` REAL NOT NULL DEFAULT 0.0")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `cutOff6` REAL NOT NULL DEFAULT 0.0")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `cutOff7` REAL NOT NULL DEFAULT 0.0")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `cutOff8` REAL NOT NULL DEFAULT 0.0")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `cutOffMax` REAL NOT NULL DEFAULT 0.0")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `noise1` REAL NOT NULL DEFAULT 0.0")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `noise2` REAL NOT NULL DEFAULT 0.0")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `noise3` REAL NOT NULL DEFAULT 0.0")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `noise4` REAL NOT NULL DEFAULT 0.0")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `noise5` REAL NOT NULL DEFAULT 0.0")
                    }
                }
                val MIGRATION_2_3 = object : Migration(2, 3) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `scope` REAL NOT NULL DEFAULT 1.0")
                    }
                }
                val MIGRATION_3_4 = object : Migration(3, 4) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tbl_case ADD COLUMN `caseType` INTEGER NOT NULL DEFAULT 1")
                        db.execSQL("ALTER TABLE tbl_card_config ADD COLUMN `typeScore` REAL NOT NULL DEFAULT 1.0")
                    }
                }
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    dbPath
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            CoroutineScope(Dispatchers.IO).launch {
                                // 初始化数据
                                INSTANCE!!.userDao().add(
                                    User(
                                        username = User.ROLE_ADMIN,
                                        role = User.ROLE_ADMIN,
                                        nickname = "Admin",
                                        pwd = "888888",
                                    )
                                )
                                INSTANCE!!.userDao().add(
                                    User(
                                        username = User.ROLE_DEV,
                                        role = User.ROLE_DEV,
                                        nickname = "Dev",
                                        pwd = "888888",
                                    )
                                )
                                INSTANCE!!.sysConfigDao().add(
                                    SysConfig(
                                        name = "scan",
                                        value = "y",
                                        gmtCreated = LocalDateTime.now(),
                                        gmtModified = LocalDateTime.now(),
                                    )
                                )
                            }
                        }
                    })
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}


internal class LocalDateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { AppLocalDateUtils.dateFromTimestamp(it) }
    }


    @TypeConverter
    fun toTimestamp(date: LocalDate?): Long? {
        return date?.let { AppLocalDateUtils.timestampFromDate(it) }
    }
}

internal class LocalDateTimeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { AppLocalDateUtils.dateTimeFromTimestamp(it) }
    }

    @TypeConverter
    fun toTimestamp(datetime: LocalDateTime?): Long? {
        return datetime?.let { AppLocalDateUtils.timestampFromDateTime(datetime) }
    }
}