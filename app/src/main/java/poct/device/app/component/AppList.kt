package poct.device.app.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.theme.bg2Color
import poct.device.app.theme.tipFontColor

/**
 */
@Composable
fun <T> AppList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    records: State<List<T>>,
    gap: Dp = 10.dp,
    recordRender: @Composable (record: T) -> Unit,
) {
    AppList(
        modifier = modifier,
        state = state,
        records = records.value,
        gap = gap
    ) { item: T, _: Int ->
        recordRender(item)
    }
}

/**
 */
@Composable
fun <T> AppList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    records: List<T>,
    gap: Dp = 10.dp,
    recordRender: @Composable (record: T) -> Unit,
) {
    AppList(
        modifier = modifier,
        state = state,
        records = records,
        gap = gap
    ) { item: T, _: Int ->
        recordRender(item)
    }
}


/**
 */
@Composable
fun <T> AppList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    records: State<List<T>>,
    gap: Dp = 10.dp,
    recordRender: @Composable (record: T, index: Int) -> Unit,
) {
    AppList(
        modifier = modifier,
        state = state,
        records = records.value,
        gap = gap
    ) { item: T, index: Int ->
        recordRender(item, index)
    }
}


/**
 */
@Composable
fun <T> AppList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    records: List<T>,
    gap: Dp = 10.dp,
    key: ((index: Int) -> Any)? = { records[it].toString() },
    recordRender: @Composable (record: T, index: Int) -> Unit,
) {
    if (records.isEmpty()) {
        AppListEmpty(modifier)
    } else {
        LazyColumn(
            modifier = modifier,
            state = state,
        ) {
            items(records.size, key = key) {
                recordRender(records[it], it)
                Spacer(modifier = Modifier.height(gap))
            }
        }
    }
}

@Composable
private fun AppListEmpty(modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(168.dp))
        Image(
            painter = painterResource(id = R.mipmap.zwsj_img),
            contentDescription = ""
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.no_record),
            fontSize = 16.sp,
            color = tipFontColor
        )
    }
}

@Preview
@Composable
fun AppListPreview() {
    val navController = rememberNavController()
    val list = ArrayList<CaseBean>()
//    list.add(CaseInfo(name = "sample"))
    AppPreviewWrapper {
        AppList<CaseBean>(
            modifier = Modifier
                .fillMaxSize()
                .background(bg2Color),
            records = list
        ) { _ ->
            Text(text = "sample")
        }
    }
}
