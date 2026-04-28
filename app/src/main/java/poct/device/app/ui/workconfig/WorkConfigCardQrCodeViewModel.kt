package poct.device.app.ui.workconfig

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.AppParams
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.converter.CardConfigConverter
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppCardConfigUtils
import poct.device.app.utils.app.AppFileUtils
import poct.device.app.utils.app.AppQRCodeUtils
import java.io.File
import java.io.FileOutputStream

class WorkConfigCardQrCodeViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)

    // 记录
    val bean = MutableStateFlow(CardConfigBean.Empty)
    val curContent = MutableStateFlow<String>("")

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch {
            bean.value = AppParams.varCardConfigForPreview
            viewState.value = ViewState.LoadSuccess()
            val carConfig = CardConfigConverter.toEntity(bean.value)
            curContent.value = AppCardConfigUtils.toEncodeQrCode(carConfig)

            val bitmap = AppQRCodeUtils.createQRCodeBitmap(
                curContent.value,
                260,
                260,
                "UTF-8",
                "L",
                "0",
                Color.BLACK,
                Color.WHITE
            )
            val fileName = bean.value.type + "_" + bean.value.code + ".png"
            withContext(Dispatchers.IO) {
                saveBitmapToFile(bitmap, fileName)
            }
        }
    }

    fun saveBitmapToFile(bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val fileUrl = AppFileUtils.getQrCodeUrl() + fileName
            // 创建文件路径
            val filePath = File(fileUrl)
            if (!filePath.parentFile?.exists()!!) {
                filePath.parentFile?.mkdirs()
            }
            if (filePath.exists()) {
                filePath.delete()
            }
            // 使用FileOutputStream将Bitmap保存到文件中
            val fileOutputStream = FileOutputStream(filePath)
            // 将Bitmap压缩成JPEG格式并保存到文件中
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            // 刷新并关闭流
            fileOutputStream.flush()
            fileOutputStream.close()
            true // 返回true表示保存成功
        } catch (e: Exception) {
            e.printStackTrace()
            false // 返回false表示保存失败
        }
    }

    companion object {
    }
}