package poct.device.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppFullScreenTheme(
    content: @Composable () -> Unit
) {

    MaterialTheme(
        typography = Typography,
        shapes = Shapes,
        colorScheme = lightColorScheme(
            primary = primaryColor,
            outline = primaryColor,
            outlineVariant = primaryColor,
        ),
        content = content
    )
}