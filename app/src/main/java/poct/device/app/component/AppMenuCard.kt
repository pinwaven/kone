package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.theme.bg2Color
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.menuCardHeaderFontColor
import poct.device.app.theme.primaryColor

@Composable
fun AppMenuCard(
    navController: NavController,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AppMenuCardHeader(title)
            content()
        }
    }
}

@Composable
fun AppMenuCardItem(
    navController: NavController,
    label: String,
    painter: Painter? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (painter != null) {
            Icon(
                modifier = Modifier
                    .size(36.dp),
                painter = painter,
                tint = filledFontColor,
                contentDescription = ""
            )
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = label,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AppMenuCardHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(primaryColor),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = menuCardHeaderFontColor,
            text = title
        )
    }
}

@Preview
@Composable
fun AppMenuCardPreview() {
    val navController = rememberNavController()
    val painter = painterResource(id = R.mipmap.home_dark)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg2Color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)

        ) {
            AppMenuCard(
                navController = navController,
                title = "测试菜单",
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Spacer(modifier = Modifier.height(23.dp))
                    // 3个
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AppMenuCardItem(
                            navController = navController, label = "菜单菜单1", painter = painter
                        )
                        AppMenuCardItem(
                            navController = navController, label = "菜单菜单2", painter = painter
                        )
                        AppMenuCardItem(
                            navController = navController, label = "菜单菜单3", painter = painter
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // 2个，两边对齐
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AppMenuCardItem(
                            navController = navController, label = "菜单菜单1", painter = painter
                        )
                        AppMenuCardItem(
                            navController = navController, label = "菜单菜单2", painter = painter
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // 2个左对齐
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AppMenuCardItem(
                            navController = navController, label = "菜单菜单1", painter = painter
                        )
                        AppMenuCardItem(
                            navController = navController, label = "菜单菜单2", painter = painter
                        )
                        AppMenuCardItem(
                            navController = navController,
                            label = ""
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // 1个居中
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AppMenuCardItem(
                            navController = navController, label = "菜单菜单1", painter = painter
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                },
            )

        }
    }
}