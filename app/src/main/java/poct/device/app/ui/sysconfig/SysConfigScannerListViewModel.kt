package poct.device.app.ui.sysconfig

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ScannerInfo
import poct.device.app.event.AppScannerEvent
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import timber.log.Timber
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Collections


class SysConfigScannerListViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val devList = MutableStateFlow<List<ScannerInfo>>(Collections.emptyList())


    //获取系统蓝牙适配器
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            devList.value = emptyList()
            initBluetooth()
        }
    }

    /**
     * 初始化蓝牙
     */
    private suspend fun initBluetooth() {
        // 判断蓝牙是否打开
        Timber.w("===蓝牙打开状态：${isOpenBluetooth()}")
        //打开蓝牙意图
        if (!isOpenBluetooth()) {
            val manager = App.getContext().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter
            checkPermission()
            adapter.enable()
        } else {
            val manager = App.getContext().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            // 蓝牙适配器
            mBluetoothAdapter = manager.adapter
            // 扫描广播
            mBluetoothAdapter.startDiscovery()
        }
        delay(15000)
        if (viewState.value is ViewState.LoadingOver) {
            viewState.value = ViewState.LoadError(App.getContext().getString(R.string.msg_timeout))
        }
    }

    /**
     * 判断蓝牙是否打开, 如果未打开就直接打开蓝牙
     */
    private fun isOpenBluetooth(): Boolean {
        val manager = App.getContext().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter ?: return false
        return adapter.isEnabled
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                App.getContext(),
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            viewState.value =
                ViewState.LoadError(App.getContext().getString(R.string.user_permission))
        }
    }

    @Subscribe
    @SuppressLint("MissingPermission")
    fun handleScannerEvent(event: AppScannerEvent) {
        Timber.d("scanner event")
        val intent = event.intent
        Timber.d("scanner event${event.intent?.action}")
        viewModelScope.launch(Dispatchers.IO) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                //获取已绑定的设备
                getBondedDevice()
                val scannerInfoList = ArrayList<ScannerInfo>(devList.value)
                //获取周围蓝牙设备
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val addressList = scannerInfoList.map { it.address }
                if (addressList.indexOf(device?.address) == -1) { //防止重复添加
                    device?.fetchUuidsWithSdp()
                    if (device?.name != null) {
                        //过滤掉设备名称为null的设备
                        scannerInfoList.add(
                            ScannerInfo(
                                address = device.address,
                                name = device.name,
                                device = device,
                                connected = device.bondState == 12
                            )
                        )
                    }
                }
                devList.value = scannerInfoList
                viewState.value = ViewState.LoadSuccess()
                Timber.w("========devList${App.gson.toJson(scannerInfoList)}")
            } else if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val scannerInfoList = ArrayList<ScannerInfo>(devList.value)
                // 状态改变的广播
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return@launch
                device.fetchUuidsWithSdp()
                val addressList = scannerInfoList.map { it.address }
                val index = addressList.indexOf(device.address)
                if (index == -1) { //防止重复添加
                    if (device.name != null) {
                        //过滤掉设备名称为null的设备
                        scannerInfoList.add(
                            ScannerInfo(
                                address = device.address,
                                name = device.name,
                                device = device,
                                connected = device.bondState == 12
                            )
                        )
                    }
                } else {
                    scannerInfoList[index] = ScannerInfo(
                        address = device.address,
                        name = device.name,
                        device = device,
                        connected = device.bondState == 12
                    )
                }
                devList.value = scannerInfoList
                viewState.value = ViewState.LoadSuccess()
            }
        }
    }


    /**
     * 获取已绑定设备
     */
    private fun getBondedDevice() {
        Timber.w("=======Paired")
        checkPermission()
        val pairedDevices = mBluetoothAdapter.bondedDevices

        Timber.w("=======Paired=${App.gson.toJson(pairedDevices)}")
        if (pairedDevices.size > 0) {
            //如果获取的结果大于0，则开始逐个解析
            val scannerInfoList = ArrayList<ScannerInfo>(devList.value)
            val deviceList = scannerInfoList.map { it.address }
            for (device in pairedDevices) {
                device.fetchUuidsWithSdp()
                if (deviceList.indexOf(device.address) == -1) { //防止重复添加
                    if (device.name != null) { //过滤掉设备名称为null的设备
                        scannerInfoList.add(
                            ScannerInfo(
                                address = device.address,
                                name = device.name,
                                device = device,
                                connected = device.bondState == 12
                            )
                        )
                    }
                }
            }
            devList.value = scannerInfoList
        }
    }

    @SuppressLint("MissingPermission")
    fun createOrRemoveBond(type: Int, device: BluetoothDevice?) {
        viewModelScope.launch {
            var method: Method?
            try {
                when (type) {
                    1 -> {
                        Timber.w("=====${device}")
                        method = BluetoothDevice::class.java.getMethod("createBond")
                        method.invoke(device)
                    }

                    2 -> {
                        Timber.w("=====${device}")
                        method = BluetoothDevice::class.java.getMethod("removeBond")
                        method.invoke(device)
                    }
                }
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
            viewState.value = ViewState.LoadSuccess()
        }
    }

    companion object {

    }
}