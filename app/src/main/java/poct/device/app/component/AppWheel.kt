package poct.device.app.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.theme.bgColor
import poct.device.app.theme.sepColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <V> AppWheel(
    modifier: Modifier = Modifier,
    value: V,
    // key, value映射表
    options: Map<V, String>,
    fontSize: TextUnit = 14.sp,
    onValueChange: (it: V) -> Unit = {},
) {
    val indexList: List<Int> = genIndexList(options.size)
    val keyList = options.keys.toList()
    val valueIndex = keyList.indexOf(value)
    val defaultIndex = if (valueIndex < 0) 0 else valueIndex
    var curIndex by rememberSaveable { mutableIntStateOf(defaultIndex) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val height = this.maxHeight
            val width = this.maxWidth
            val listState: LazyListState = rememberLazyListState(
                initialFirstVisibleItemIndex = defaultIndex
            )
            if (listState.isScrollInProgress) {
                DisposableEffect(Unit) {
                    onDispose {
                        //onScroll Done
                        if (curIndex != listState.firstVisibleItemIndex) {
                            curIndex = listState.firstVisibleItemIndex
                            onValueChange(keyList[curIndex])
                        }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(listState),
            ) {
                items(indexList) {
                    Row(
                        modifier = Modifier
                            .width(width)
                            .height(height / 3)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val text = if (it < 0) "" else options[keyList[it]]!!
                        Text(text = text, fontSize = fontSize)
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(modifier = Modifier.height(height / 3))
                AppDivider(color = sepColor, size = 1.dp)
                Spacer(modifier = Modifier.height(height / 3))
                AppDivider(color = sepColor, size = 1.dp)
            }
        }
    }
}

private fun genIndexList(size: Int): List<Int> {
    val list = ArrayList<Int>(size + 2)
    list.add(-1)
    for (i in 0 until size) {
        list.add(i)
    }
    list.add(-2)
    return list
}


@Preview
@Composable
fun AppWheelPreview() {
    val options = mapOf(1 to "男", 2 to "女", 3 to "A", 4 to "B", 5 to "C")
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AppWheel<Int>(
            modifier = Modifier
                .width(300.dp)
                .height(200.dp),
            value = 1,
            options = options,
            onValueChange = {}
        )
    }
}
