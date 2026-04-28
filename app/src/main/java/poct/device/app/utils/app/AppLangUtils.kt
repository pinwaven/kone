package poct.device.app.utils.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.yariksoffice.lingver.Lingver
import poct.device.app.App
import java.util.Locale


object AppLangUtils {
    fun init(app: Application) {
        Lingver.init(app)
    }

    fun setLanguage(context: Context, language: String) {
        val editor: SharedPreferences.Editor =
            context.getSharedPreferences("Language", Context.MODE_PRIVATE).edit()
        editor.putString("language", language)
        editor.apply()

        useLanguage(context)
    }
    fun useLanguage(context: Context) {
        Lingver.getInstance().setLocale(context, getLanguage(App.getContext()))
    }

    private fun getLocale(lang: String): Locale {
        return if (lang == "cn") {
            Locale.SIMPLIFIED_CHINESE
        } else {
            Locale.ENGLISH
        }
    }

    fun getLanguage(context: Context): String {
        val editor: SharedPreferences =
            context.getSharedPreferences("Language", Context.MODE_PRIVATE)
        return editor.getString("language", "cn") ?: "cn"
    }
}