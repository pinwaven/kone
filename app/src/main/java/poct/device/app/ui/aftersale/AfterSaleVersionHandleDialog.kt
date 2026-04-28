package poct.device.app.ui.aftersale


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.R
import poct.device.app.bean.VersionBean
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.theme.bgColor
import poct.device.app.theme.tipFontColor

@Composable
fun AfterSaleVersionHandleDialog(
    version: VersionBean,
    mode: String = "upgrade",
    onUpgradeCancel: () -> Unit,
    onUpgradeNow: () -> Unit,
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier.size(280.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()

                    .background(bgColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(id = R.string.after_sale_version_new),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.sys_fun_xtpz_info_f_software),
                                fontSize = 12.sp,
                                color = tipFontColor
                            )
                            Text(
                                text = version.software,
                                fontSize = 12.sp,
                                color = tipFontColor
                            )
                        }
                        Spacer(modifier = Modifier.width(36.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.sys_fun_xtpz_info_f_hardware),
                                fontSize = 12.sp,
                                color = tipFontColor
                            )
                            Text(
                                text = version.hardware,
                                fontSize = 12.sp,
                                color = tipFontColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 15.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.after_sale_version_remark),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .scrollable(rememberScrollState(), Orientation.Vertical)
                        ) {
                            if (version.softwareRemark.isNotBlank()) {
                                Text(
                                    text = stringResource(id = R.string.after_sale_version_remark_software),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = version.softwareRemark,
                                    fontSize = 12.sp,
                                    color = tipFontColor,
                                    lineHeight = 22.sp
                                )
                            }
                            if (version.hardwareRemark.isNotBlank()) {
                                Text(
                                    text = stringResource(id = R.string.after_sale_version_remark_hardware),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = version.hardwareRemark,
                                    fontSize = 12.sp,
                                    color = tipFontColor,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                    }
                    Spacer(modifier = Modifier.height(19.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = genDate(bean = version),
                            fontSize = 12.sp,
                            color = tipFontColor
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (mode == "upgrade") {
                        Row(
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AppOutlinedButton(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(36.dp),
                                onClick = { onUpgradeCancel() },
                                text = stringResource(id = R.string.after_sale_upgrade_cancel)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            AppFilledButton(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(36.dp),
                                onClick = { onUpgradeNow() },
                                text = stringResource(id = R.string.after_sale_upgrade_now)
                            )
                        }
                    }
                }

                // 关闭图标
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 246.dp)
                ) {
                    Icon(
                        modifier = Modifier.clickable { onUpgradeCancel() },
                        painter = painterResource(id = R.mipmap.pop_icon_guanbi),
                        contentDescription = ""
                    )
                }
            }
        }
    }
}

@Composable
private fun genDate(bean: VersionBean): String {
    val label = when (bean.state) {
        0 -> stringResource(id = R.string.after_sale_f_effect_date)
        2 -> stringResource(id = R.string.after_sale_f_handle_date)
        else -> stringResource(id = R.string.after_sale_f_expired_date)
    }
    val value = when (bean.state) {
        0 -> bean.lapseTime
        1 -> bean.handleTime
        2 -> bean.handleTime
        else -> bean.lapseTime
    }
    return "${label}${value}"
}
