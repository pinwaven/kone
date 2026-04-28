package poct.device.app.entity.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import poct.device.app.entity.CardConfig

@Dao
interface CardConfigDao {
    @Query("SELECT * FROM tbl_card_config ORDER BY gmtCreated DESC")
    suspend fun findAll(): List<CardConfig>?

    @Query("SELECT * from tbl_card_config WHERE id = :id")
    suspend fun findById(id: String): CardConfig?

    @Query("SELECT * from tbl_card_config WHERE type=:type and code = :code")
    suspend fun findByIden(type: String, code: String): CardConfig?

    @Insert
    suspend fun add(entity: CardConfig)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entity: CardConfig)

    @Delete
    suspend fun delete(entity: CardConfig)
}