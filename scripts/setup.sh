adb shell dpm set-device-owner poct.device.app/.MyDeviceAdminReceiver

adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
