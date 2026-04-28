package poct.device.app.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "tbl_sys_config")
data class SysConfig(
    @PrimaryKey var name: String = "",
    var value: String = "",
    var gmtCreated: LocalDateTime,
    var gmtModified: LocalDateTime,
)