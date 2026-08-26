package poct.device.app.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.component.AppConfirmPassword
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppSwitch
import poct.device.app.component.AppTopBar
import poct.device.app.component.NanoEnvironmentSelector
import poct.device.app.component.wakeScreenOnTouch
import poct.device.app.entity.service.TestModeConfigService
import poct.device.app.utils.app.AppToastUtil
import poct.device.app.theme.bgColor
import poct.device.app.theme.borderColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.inputBgColor
import poct.device.app.theme.inputFontColor

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
            val testModeEnabled by AppParams.runtimeModeState.testModeEnabled.collectAsState()
            var reactionTimeSeconds by remember { mutableStateOf("300") }
            var absorbTimeMillis by remember { mutableStateOf("3000") }
            var scanTimeMillis by remember { mutableStateOf("14000") }
            var laserPower by remember { mutableStateOf("-25") }
            var editingField by remember { mutableStateOf<TestModeConfigField?>(null) }
            var editingOriginalValue by remember { mutableStateOf("") }
            var autoShowKeyboard by remember { mutableStateOf(true) }
            var testModePasswordVisible by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            fun testModeConfigValues() = TestModeConfigValues(
                reactionTimeSeconds = reactionTimeSeconds,
                absorbTimeMillis = absorbTimeMillis,
                scanTimeMillis = scanTimeMillis,
                laserPower = laserPower,
            )

            fun applyTestModeConfigValues(values: TestModeConfigValues) {
                reactionTimeSeconds = values.reactionTimeSeconds
                absorbTimeMillis = values.absorbTimeMillis
                scanTimeMillis = values.scanTimeMillis
                laserPower = values.laserPower
            }

            fun openTestModeConfigEdit(field: TestModeConfigField) {
                autoShowKeyboard = true
                editingOriginalValue = testModeConfigValues().valueOf(field)
                editingField = field
            }

            fun cancelTestModeConfigEdit() {
                val field = editingField
                if (field != null) {
                    applyTestModeConfigValues(
                        restoreTestModeConfigValue(
                            values = testModeConfigValues(),
                            field = field,
                            originalValue = editingOriginalValue,
                        )
                    )
                }
                editingField = null
                editingOriginalValue = ""
            }

            LaunchedEffect(Unit) {
                val config = TestModeConfigService.findBean()
                reactionTimeSeconds = config.reactionTimeSeconds
                absorbTimeMillis = config.absorbTimeMillis
                scanTimeMillis = config.scanTimeMillis
                laserPower = config.laserPower
            }

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
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.test_mode),
                        color = fontColor,
                        fontSize = 18.sp
                    )
                    AppSwitch(
                        checked = testModeEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF1677FF),
                            checkedBorderColor = Color(0xFF1677FF),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFBFC7D5),
                            uncheckedBorderColor = Color(0xFFBFC7D5)
                        ),
                        onCheckedChange = { checked ->
                            if (checked) {
                                testModePasswordVisible = true
                            } else {
                                AppParams.runtimeModeState.setTestModeEnabled(false)
                            }
                        }
                    )
                }
                if (testModeEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TestModeConfigRow(
                        label = stringResource(id = R.string.test_mode_reaction_time),
                        value = reactionTimeSeconds,
                        unit = stringResource(id = R.string.test_mode_reaction_time_unit),
                        onClick = {
                            openTestModeConfigEdit(TestModeConfigField.REACTION_TIME)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TestModeConfigRow(
                        label = stringResource(id = R.string.test_mode_absorb_time),
                        value = absorbTimeMillis,
                        unit = stringResource(id = R.string.test_mode_millisecond_unit),
                        onClick = {
                            openTestModeConfigEdit(TestModeConfigField.ABSORB_TIME)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TestModeConfigRow(
                        label = stringResource(id = R.string.test_mode_scan_time),
                        value = scanTimeMillis,
                        unit = stringResource(id = R.string.test_mode_millisecond_unit),
                        onClick = {
                            openTestModeConfigEdit(TestModeConfigField.SCAN_TIME)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TestModeConfigRow(
                        label = stringResource(id = R.string.test_mode_laser_power),
                        value = laserPower,
                        unit = "",
                        onClick = {
                            openTestModeConfigEdit(TestModeConfigField.LASER_POWER)
                        }
                    )
                }
            }
            val editingValue = when (editingField) {
                TestModeConfigField.REACTION_TIME -> reactionTimeSeconds
                TestModeConfigField.ABSORB_TIME -> absorbTimeMillis
                TestModeConfigField.SCAN_TIME -> scanTimeMillis
                TestModeConfigField.LASER_POWER -> laserPower
                null -> ""
            }
            val editingTitle = when (editingField) {
                TestModeConfigField.REACTION_TIME -> stringResource(id = R.string.test_mode_reaction_time)
                TestModeConfigField.ABSORB_TIME -> stringResource(id = R.string.test_mode_absorb_time)
                TestModeConfigField.SCAN_TIME -> stringResource(id = R.string.test_mode_scan_time)
                TestModeConfigField.LASER_POWER -> stringResource(id = R.string.test_mode_laser_power)
                null -> ""
            }
            val editingUnit = when (editingField) {
                TestModeConfigField.REACTION_TIME -> stringResource(id = R.string.test_mode_reaction_time_unit)
                TestModeConfigField.ABSORB_TIME,
                TestModeConfigField.SCAN_TIME -> stringResource(id = R.string.test_mode_millisecond_unit)
                TestModeConfigField.LASER_POWER,
                null -> ""
            }
            val editingRangeText = when (editingField) {
                TestModeConfigField.ABSORB_TIME -> stringResource(
                    id = R.string.test_mode_value_range_with_unit,
                    "0",
                    "300000",
                    stringResource(id = R.string.test_mode_millisecond_unit)
                )

                TestModeConfigField.SCAN_TIME -> stringResource(
                    id = R.string.test_mode_value_range_with_unit,
                    "1",
                    "300000",
                    stringResource(id = R.string.test_mode_millisecond_unit)
                )

                TestModeConfigField.LASER_POWER -> stringResource(
                    id = R.string.test_mode_value_range,
                    "-100",
                    "0"
                )

                TestModeConfigField.REACTION_TIME,
                null -> ""
            }
            TestModeNumberDialog(
                visible = editingField != null,
                title = editingTitle,
                value = editingValue,
                unit = editingUnit,
                rangeText = editingRangeText,
                signed = editingField == TestModeConfigField.LASER_POWER,
                autoShowKeyboard = autoShowKeyboard,
                onValueChange = { value ->
                    when (editingField) {
                        TestModeConfigField.REACTION_TIME -> reactionTimeSeconds = value.filter(Char::isDigit)
                        TestModeConfigField.ABSORB_TIME -> absorbTimeMillis = value.filter(Char::isDigit)
                        TestModeConfigField.SCAN_TIME -> scanTimeMillis = value.filter(Char::isDigit)
                        TestModeConfigField.LASER_POWER -> laserPower = value.filterSignedInt()
                        null -> Unit
                    }
                },
                onCancel = { cancelTestModeConfigEdit() },
                onConfirm = {
                    val field = editingField
                    editingField = null
                    editingOriginalValue = ""
                    coroutineScope.launch {
                        when (field) {
                            TestModeConfigField.REACTION_TIME -> {
                                TestModeConfigService.saveReactionTimeSeconds(reactionTimeSeconds)
                                reactionTimeSeconds =
                                    TestModeConfigService.normalizeReactionTimeSeconds(reactionTimeSeconds)
                            }

                            TestModeConfigField.ABSORB_TIME -> {
                                TestModeConfigService.saveAbsorbTimeMillis(absorbTimeMillis)
                                absorbTimeMillis =
                                    TestModeConfigService.normalizeAbsorbTimeMillis(absorbTimeMillis)
                            }

                            TestModeConfigField.SCAN_TIME -> {
                                TestModeConfigService.saveScanTimeMillis(scanTimeMillis)
                                scanTimeMillis =
                                    TestModeConfigService.normalizeScanTimeMillis(scanTimeMillis)
                            }

                            TestModeConfigField.LASER_POWER -> {
                                TestModeConfigService.saveLaserPower(laserPower)
                                laserPower = TestModeConfigService.normalizeLaserPower(laserPower)
                            }

                            null -> Unit
                        }
                    }
                }
            )
            AppConfirmPassword(
                visible = testModePasswordVisible,
                onCancel = { testModePasswordVisible = false },
                onConfirm = { password ->
                    if (AppParams.runtimeModeState.verifyTestModePassword(password)) {
                        testModePasswordVisible = false
                        AppParams.runtimeModeState.setTestModeEnabled(true)
                    } else {
                        AppToastUtil.shortShow(App.getContext().getString(R.string.msg_wrong_password))
                    }
                }
            )
        }
    }
}

@Composable
private fun TestModeConfigRow(
    label: String,
    value: String,
    unit: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = fontColor,
            fontSize = 16.sp
        )
        Text(
            text = if (unit.isEmpty()) value else "$value$unit",
            color = fontColor,
            fontSize = 16.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TestModeNumberDialog(
    visible: Boolean,
    title: String,
    value: String,
    unit: String,
    rangeText: String,
    signed: Boolean,
    autoShowKeyboard: Boolean,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) {
        return
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.parent as? DialogWindowProvider)?.window
                window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                    WindowInsetsControllerCompat(it, view).apply {
                        hide(WindowInsetsCompat.Type.navigationBars())
                        systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
        }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        var textFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    text = value,
                    selection = TextRange(value.length)
                )
            )
        }
        if (autoShowKeyboard) {
            LaunchedEffect(Unit) {
                delay(100)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
        Surface(
            modifier = Modifier
                .wakeScreenOnTouch()
                .width(300.dp)
                .height(if (rangeText.isEmpty()) 210.dp else 236.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        PaddingValues(
                            start = 15.dp,
                            end = 15.dp,
                            top = 24.dp,
                            bottom = 20.dp
                        )
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    text = title
                )
                if (rangeText.isNotEmpty()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = fontColor,
                        text = rangeText
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .focusRequester(focusRequester)
                            .background(inputBgColor)
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = inputFontColor,
                        ),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                        onValueChange = { incoming ->
                            val digits = if (signed) {
                                incoming.text.filterSignedInt()
                            } else {
                                incoming.text.filter(Char::isDigit)
                            }
                            textFieldValue = if (digits == incoming.text) {
                                incoming
                            } else {
                                TextFieldValue(
                                    text = digits,
                                    selection = TextRange(digits.length)
                                )
                            }
                            onValueChange(digits)
                        },
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                innerTextField()
                            }
                        }
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = unit,
                            color = fontColor,
                            fontSize = 16.sp
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = onCancel,
                        text = stringResource(id = R.string.btn_label_cancel),
                        fontSize = 14.sp
                    )
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = onConfirm,
                        text = stringResource(id = R.string.btn_label_ok),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

internal data class TestModeConfigValues(
    val reactionTimeSeconds: String,
    val absorbTimeMillis: String,
    val scanTimeMillis: String,
    val laserPower: String,
)

internal enum class TestModeConfigField {
    REACTION_TIME,
    ABSORB_TIME,
    SCAN_TIME,
    LASER_POWER,
}

internal fun TestModeConfigValues.valueOf(field: TestModeConfigField): String {
    return when (field) {
        TestModeConfigField.REACTION_TIME -> reactionTimeSeconds
        TestModeConfigField.ABSORB_TIME -> absorbTimeMillis
        TestModeConfigField.SCAN_TIME -> scanTimeMillis
        TestModeConfigField.LASER_POWER -> laserPower
    }
}

internal fun restoreTestModeConfigValue(
    values: TestModeConfigValues,
    field: TestModeConfigField,
    originalValue: String,
): TestModeConfigValues {
    return when (field) {
        TestModeConfigField.REACTION_TIME -> values.copy(reactionTimeSeconds = originalValue)
        TestModeConfigField.ABSORB_TIME -> values.copy(absorbTimeMillis = originalValue)
        TestModeConfigField.SCAN_TIME -> values.copy(scanTimeMillis = originalValue)
        TestModeConfigField.LASER_POWER -> values.copy(laserPower = originalValue)
    }
}

private fun String.filterSignedInt(): String {
    val trimmed = trim()
    val sign = if (trimmed.startsWith("-")) "-" else ""
    return sign + trimmed.filter(Char::isDigit)
}
