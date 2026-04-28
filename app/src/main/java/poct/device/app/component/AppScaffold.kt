package poct.device.app.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import poct.device.app.theme.bg2Color
import poct.device.app.theme.primaryColor

@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    background: Color = bg2Color,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = primaryColor,
        topBar = topBar,
        bottomBar = bottomBar
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            color = background,
            content = content
        )
    }
}