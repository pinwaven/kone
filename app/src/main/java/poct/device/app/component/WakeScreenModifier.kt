package poct.device.app.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import poct.device.app.AppParams
import poct.device.app.MainActivity

/**
 * Compose Dialog 在独立 window，触摸不会经过 Activity.dispatchTouchEvent，
 * 闲置调暗后点击对话框无法恢复亮度。给对话框根节点加此修饰，
 * 按下时唤醒屏幕。Initial pass + 不消费事件，不影响内部点击。
 */
fun Modifier.wakeScreenOnTouch(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press) {
                (AppParams.curActivity as? MainActivity)?.onUserTouch()
            }
        }
    }
}
