package poct.device.app.entity.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import poct.device.app.entity.User

@Dao
interface UserDao {
    @Query("SELECT * FROM tbl_user ORDER BY LOWER(username)")
    suspend fun findAll(): List<User>?

    @Query("SELECT * from tbl_user WHERE username = :username")
    suspend fun findByUsername(username: String): User?

    @Insert
    suspend fun add(user: User)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)
}