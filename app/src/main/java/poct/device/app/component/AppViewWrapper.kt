package poct.device.app.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import poct.device.app.state.ViewState

/**
 * *
 * @date ：2022/4/14
 * @desc：基础页面 统一处理正在加载、加载成功 和失败的状态
 */
@Composable
fun AppViewWrapper(
    viewState: State<ViewState>,
    onErrorClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    AppViewWrapper(viewState = viewState.value, onErrorClick = onErrorClick) {
        content()
    }
}

@Composable
@Deprecated("使用State<ViewState>替代")
fun AppViewWrapper(
    viewState: ViewState,
    onErrorClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    when (viewState) {
        is ViewState.Loading -> {
            AppViewLoading(msg = viewState.msg)
        }

        is ViewState.LoadingOver -> {
            AppViewLoading(
                msg = viewState.msg,
                content = content
            )
        }

        is ViewState.LoadError -> {
            //加载失败的页面 处理为无网络请求
            AppViewError(msg = viewState.msg, onClick = onErrorClick, content = content)
        }

        is ViewState.LoadSuccess -> {
            //正常数据
            content()
        }

        else -> {
            //
        }
    }

}