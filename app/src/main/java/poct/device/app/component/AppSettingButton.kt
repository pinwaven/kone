package poct.device.app.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.theme.filledFontColor


/**
 * start与end同时设置，仅start有效
 * top与bottom同时设置，仅top有效
 */
@Composable
fun AppSettingButton(
    navController: NavController,
    start: Dp? = null,
    end: Dp? = null,
    top: Dp? = null,
    bottom: Dp? = null,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = this.minWidth
        val height = this.maxHeight
        val iconSize = 32.dp
        var buttonStart = 0.dp
        var iconStart = 0.dp
        var buttonTop = 0.dp
        end?.let {
            buttonStart = width - it - iconSize
            iconStart = buttonStart - 56.dp
        }
        start?.let {
            buttonStart = start
            iconStart = buttonStart
        }
        bottom?.let {
            buttonTop = height - bottom - iconSize
        }
        top?.let { buttonTop = top }

        IconButton(
            modifier = Modifier.padding(start = buttonStart, top = buttonTop),
            onClick = {
                navController.navigate(RouteConfig.SETTING_MAIN)
            }
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(id = R.mipmap.xtpz_icon),
                tint = filledFontColor,
                contentDescription = ""
            )
        }
    }
}

@Preview
@Composable
fun AppSettingButtonPreview() {
    val navController = rememberNavController()
    AppSettingButton(navController = navController, end = 38.dp, bottom = 40.dp)
}