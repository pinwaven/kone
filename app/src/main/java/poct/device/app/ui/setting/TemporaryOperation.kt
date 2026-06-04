package poct.device.app.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppSwitch
import poct.device.app.component.AppTopBar
import poct.device.app.component.NanoEnvironmentSelector
import poct.device.app.theme.bgColor
import poct.device.app.theme.fontColor

@Composable
fun TemporaryOperation(navController: NavController) {
    AppScaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = stringResource(id = R.string.after_sale_temp),
                backEnabled = true,
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(24.dp)
        ) {
            val venueModeEnabled by AppParams.runtimeModeState.venueModeEnabled.collectAsState()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "环境切换",
                        color = fontColor,
                        fontSize = 18.sp
                    )
                    NanoEnvironmentSelector()
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.venue_mode),
                        color = fontColor,
                        fontSize = 18.sp
                    )
                    AppSwitch(
                        checked = venueModeEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF1677FF),
                            checkedBorderColor = Color(0xFF1677FF),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFBFC7D5),
                            uncheckedBorderColor = Color(0xFFBFC7D5)
                        ),
                        onCheckedChange = { AppParams.runtimeModeState.setVenueModeEnabled(it) }
                    )
                }
            }
        }
    }
}
