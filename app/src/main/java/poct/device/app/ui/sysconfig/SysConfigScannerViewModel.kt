package poct.device.app.ui.sysconfig

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
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
import java.util.Collections

class SysConfigScannerViewModel : ViewModel() {
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
     * 判断蓝牙是否打开, 如果未打开就直接打开蓝牙
     */
    private fun isOpenBluetooth(): Boolean {
        val manager = App.getContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter ?: return false
        return adapter.isEnabled
    }

    private fun checkPermission()  {
        if (ActivityCompat.checkSelfPermission(
                App.getContext(),
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            viewState.value = ViewState.LoadError(App.getContext().getString(R.string.user_permission))
        }
    }
    /**
     * 初始化蓝牙
     */
    private suspend fun initBluetooth() {
        // 判断蓝牙是否打开
        Timber.w("===蓝牙打开状态：${isOpenBluetooth()}")
        checkPermission()
        //打开蓝牙意图
        if(!isOpenBluetooth()) {
            val manager = App.getContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter
            adapter.enable()
        }else{
            val manager = App.getContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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

    @Subscribe
    @SuppressLint("MissingPermission")
    fun handleScannerEvent(event: AppScannerEvent) {
        Timber.d("scanner event")
        viewModelScope.launch(Dispatchers.IO) {
            getBondedDevice()
        }
    }

    /**
     * 获取已绑定设备
     */
    private fun getBondedDevice() {
        Timber.w("==============蓝牙加载已绑定设备================")
        checkPermission()
        val pairedDevices = mBluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            //如果获取的结果大于0，则开始逐个解析
            val scannerInfoList = ArrayList<ScannerInfo>(devList.value)
            val deviceList = scannerInfoList.map { it.address }
            for (device in pairedDevices) {
                device.fetchUuidsWithSdp()
                if (deviceList.indexOf(device.address) == -1) { //防止重复添加
                    if (device.name != null && device.bondState==12) { //过滤掉设备名称为null的设备
                        scannerInfoList.add(ScannerInfo(address = device.address, name = device.name, device=device, connected = device.bondState==12 ))
                    }
                }
            }
            devList.value = scannerInfoList
        }
        viewState.value = ViewState.LoadSuccess()
        Timber.w("==============蓝牙加载已绑定设备================")
    }
}