package poct.device.app.ui.countdown

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import poct.device.app.App
import poct.device.app.R
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.theme.fontColor
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.max

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun CountdownMain(navController: NavController) {
    // 秒
    var totalTime1 by remember { mutableIntStateOf(241) }
    var totalMinutes1 by remember { mutableDoubleStateOf(ceil(totalTime1 / 60.0)) }

    // 秒
    var totalTime2 by remember {
        mutableIntStateOf(241)
    }

    val totalMinutes2 by remember(totalTime2) {
        derivedStateOf {
            max(0.0, ceil(totalTime2 / 60.0))
        }
    }
    AppScaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = "Countdown",
                homeEnabled = false,
                loginInfoEnabled = false
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .padding(top = 25.dp)
                    .fillMaxSize()
            ) {
                Column {
                    Text(
                        text = "test",
                        color = fontColor,
                        fontSize = 50.sp
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        for (i in 0..totalTime1) {
                            delay(1000)
                            totalTime1--

                            val totalMinutes1Tmp = ceil(totalTime1 / 60.0)
                            totalMinutes1 = if (totalMinutes1Tmp < 1) {
                                1.0
                            } else {
                                totalMinutes1Tmp
                            }
                        }
                    }
                    Text(
                        text = App.getContext().getString(R.string.work_ing_time2)
                            .format(DecimalFormat("#").format(totalTime1)),
                        color = fontColor,
                        fontSize = 50.sp
                    )
                    Text(
                        text = App.getContext().getString(R.string.work_ing_time1)
                            .format(DecimalFormat("#").format(totalMinutes1)),
                        color = fontColor,
                        fontSize = 50.sp
                    )


                    LaunchedEffect(totalTime2) {
                        if (totalTime2 > 0) {
                            delay(1000)
                            totalTime2--
                        }
                    }
                    Text(
                        text = App.getContext().getString(R.string.work_ing_time2)
                            .format(DecimalFormat("#").format(totalTime2)),
                        color = fontColor,
                        fontSize = 50.sp
                    )
                    Text(
                        text = App.getContext().getString(R.string.work_ing_time1)
                            .format(DecimalFormat("#").format(totalMinutes2)),
                        color = fontColor,
                        fontSize = 50.sp
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun CountdownMainPreview() {
    CountdownMain(rememberNavController())
}