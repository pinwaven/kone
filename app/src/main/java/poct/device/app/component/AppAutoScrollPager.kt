package poct.device.app.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import poct.device.app.R
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.primaryColor

@Composable
fun AppAutoScrollPager(
    images: List<Int>, // 图片资源列表
    modifier: Modifier = Modifier,
    autoScrollInterval: Long = 3000, // 自动滚动间隔（毫秒）
) {
    // 1. 安全检查
    if (images.isEmpty()) return

    // 2. 状态设置（关键修正点）
    val pagerState = rememberPagerState(
        initialPage = images.size * 1000, // 足够大的初始位置
        pageCount = { Int.MAX_VALUE }    // 伪无限页数
    )

    // 2. 派生当前实际页（触发重组的关键）
    val actualPage by remember {
        derivedStateOf { pagerState.currentPage % images.size }
    }

    // 3. 动态颜色定义
    val activeColor = filledFontColor
    val inactiveColor = remember(actualPage) {  // 根据当前页缓存
        primaryColor
    }

    // 自动滚动逻辑
    LaunchedEffect(images.size) {
        while (true) {
            delay(autoScrollInterval)
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
        ) { page ->
            Image(
                painter = painterResource(id = images[page % images.size]),
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Fit
            )
        }

        // 底部指示器
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(images.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (actualPage == index)
                                activeColor else inactiveColor,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Preview
@Composable
fun AppAutoScrollPagerPreview() {
    val images = listOf(
        R.drawable.splash_logo,
        R.drawable.splash_img,
    )

    AppAutoScrollPager(
        images = images,
        modifier = Modifier
            .fillMaxSize()
    )
}
