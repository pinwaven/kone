package poct.device.app.entity.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import poct.device.app.entity.Case

@Dao
interface CaseDao {

    @RawQuery
    suspend fun query(sql: SimpleSQLiteQuery): List<Case>

    @Query("SELECT * from tbl_case WHERE id = :id")
    suspend fun findById(id: String): Case?

    @Query("SELECT * from tbl_case WHERE qrCode = :qrCode")
    suspend fun findByQrCode(qrCode: String): Case?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: Case)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entity: Case)

    @Delete
    suspend fun delete(entity: Case)
}