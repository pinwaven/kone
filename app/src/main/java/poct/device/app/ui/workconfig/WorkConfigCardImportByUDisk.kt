package poct.device.app.ui.workconfig

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.R
import poct.device.app.bean.FileInfo
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppList
import poct.device.app.component.AppOutlinedButton
import poct.device.app.theme.bgColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.primaryColor
import poct.device.app.theme.tipFontColor


@Composable
fun WorkConfigCardImportByUDisk(
    visible: Boolean = false,
    step: State<String>,
    files: State<List<FileInfo>>,
    selectFiles: State<List<FileInfo>>,
    onCancel: () -> Unit = {},
    onSelectFile: (selectFile: FileInfo) -> Unit = {},
    onImportByUDisk: () -> Unit = {},
) {
    if(!visible) {
        return
    }
    var stepValue = step.value
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier.size(280.dp)
        ) {
            if (stepValue == WorkConfigCardViewModel.STEP_IMPORT_CHECK) {
                WorkConfigCardImportByUDiskCheck()
            } else if (stepValue == WorkConfigCardViewModel.STEP_IMPORT_FILE) {
                WorkConfigCardImportByUDiskFile(files, selectFiles, onSelectFile, onCancel, onImportByUDisk)
            } else if (stepValue == WorkConfigCardViewModel.STEP_IMPORT_ING) {
                WorkConfigCardImportByUDiskIng()
            } else if (stepValue == WorkConfigCardViewModel.STEP_IMPORT_DONE) {
                WorkConfigCardImportByUDiskDone(onCancel)
            }
        }
    }

}


@Composable
private fun WorkConfigCardImportByUDiskDone(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val rotate by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        Spacer(modifier = Modifier.height(72.dp))
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Image(
                painter = painterResource(id = R.mipmap.tips_suc_img),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.import_from_u_disk_ok),
                fontSize = 14.sp,
                color = fontColor,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(36.dp),
                    onClick = {
                        onCancel()
                    },
                    text = stringResource(id = R.string.btn_label_i_know)
                )
            }
        }
    }
}


@Composable
private fun WorkConfigCardImportByUDiskIng() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val rotate by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        Spacer(modifier = Modifier.height(72.dp))
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.lodding_white_icon),
                tint = primaryColor,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .rotate(rotate)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.import_from_u_disk_ing),
                fontSize = 14.sp,
                color = fontColor,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun WorkConfigCardImportByUDiskFile(files: State<List<FileInfo>>, selectFiles: State<List<FileInfo>>, onSelectFile: (selectFile: FileInfo) -> kotlin.Unit, onCancel: () -> Unit, onImportByUDisk: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(start = 15.dp, end = 15.dp, top = 24.dp, bottom = 20.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            text = stringResource(id = R.string.btn_label_import_u)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.import_from_u_disk_file),
            color = tipFontColor
        )
        Spacer(modifier = Modifier.height(18.dp))
        AppList<FileInfo>(
            records = files.value,
            gap = 12.dp,
            modifier = Modifier
                .height(112.dp)
                .padding(start = 8.dp)
        ) { it ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    modifier = Modifier.height(24.dp),
                    selected = (selectFiles.value.contains(it)),
                    onClick = {
                        onSelectFile(it)
                    })
                Text(text = it.name, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AppOutlinedButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(36.dp),
                onClick = { onCancel() },
                text = stringResource(id = R.string.btn_label_exit)
            )
            AppFilledButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(36.dp),
                onClick = { onImportByUDisk() },
                text = stringResource(id = R.string.btn_label_import)
            )
        }
    }
}

@Composable
private fun WorkConfigCardImportByUDiskCheck() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val rotate by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        Spacer(modifier = Modifier.height(72.dp))
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.lodding_white_icon),
                tint = primaryColor,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .rotate(rotate)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.check_u_disk),
                fontSize = 14.sp,
                color = fontColor,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}