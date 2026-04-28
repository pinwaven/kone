package poct.device.app.utils.app

import poct.device.app.entity.User


object AppUserUtils {
    fun isDefault(username: String): Boolean {
       return username == User.ROLE_ADMIN || username == User.ROLE_DEV
    }
}