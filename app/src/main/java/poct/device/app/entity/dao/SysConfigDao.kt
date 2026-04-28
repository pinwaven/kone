package poct.device.app.entity.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import poct.device.app.entity.SysConfig

@Dao
interface SysConfigDao {
    @Query("SELECT * FROM tbl_sys_config ORDER BY gmtCreated DESC")
    suspend fun findAll(): List<SysConfig>?

    @Query("SELECT * FROM tbl_sys_config WHERE name like :prefix")
    suspend fun findByPrefix(prefix: String): List<SysConfig>?

    @Query("SELECT * from tbl_sys_config WHERE name=:name")
    suspend fun findByName(name: String): SysConfig?

    @Insert
    suspend fun add(entity: SysConfig)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entity: SysConfig)

    @Delete
    suspend fun delete(entity: SysConfig)
}