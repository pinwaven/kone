package poct.device.app.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "tbl_user")
data class User(
    @PrimaryKey var username: String = "",
    var nickname: String = "",
    var role: String = "",
    var pwd: String = "",
    var loginTime: LocalDateTime = LocalDate.of(1900, 1, 1).atStartOfDay(),
) {
    companion object {
        val Empty = User()

        const val ROLE_ADMIN = "admin"
        const val ROLE_CHECKER = "checker"
        const val ROLE_DEV = "dev"
    }
}