//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package poct.device.app.serial.v2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android_serialport_api.SerialPort;
import timber.log.Timber;
import tp.xmaihh.serialport.stick.AbsStickPackageHelper;
import tp.xmaihh.serialport.stick.BaseStickPackageHelper;
import tp.xmaihh.serialport.utils.ByteUtil;

public class SerialHelperV2 {
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private String sPort = "/dev/ttyS1";
    private int iBaudRate = 9600;
    private int stopBits = 1;
    private int dataBits = 8;
    private int parity = 0;
    private int flowCon = 0;
    private int flags = 0;
    private boolean _isOpen = false;
    private byte[] _bLoopData = new byte[]{48};
    private int iDelay = 500;
    private AbsStickPackageHelper mStickPackageHelper = new BaseStickPackageHelper();

    public SerialHelperV2(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public void open() throws SecurityException, IOException, InvalidParameterException {
        this.mSerialPort = new SerialPort(new File(this.sPort), this.iBaudRate, this.stopBits, this.dataBits, this.parity, this.flowCon, this.flags);
        this.mOutputStream = this.mSerialPort.getOutputStream();
        this.mInputStream = this.mSerialPort.getInputStream();
        this._isOpen = true;
    }

    public void close() {
        if (this.mSerialPort != null) {
            this.mSerialPort.close();
            this.mSerialPort = null;
        }

        this._isOpen = false;
    }

    public void reconnect() throws IOException {
        this.close();
        this.open();
    }

    public void send(byte[] bOutArray) {
        try {
            this.mOutputStream.write(bOutArray);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendHex(String sHex) {
        byte[] bOutArray = ByteUtil.HexToByteArr(sHex);
        this.send(bOutArray);
    }

    public void sendTxt(String sTxt) {
        byte[] bOutArray = sTxt.getBytes();
        this.send(bOutArray);
    }

    public byte[] readAllData() {
        int timeout = 1000;
        int interByteTimeout = 10;

        long startTime = System.currentTimeMillis();
        long lastRecvTime = 0;

        byte[] buffer = null;
        while (true) {
            long now = System.currentTimeMillis();

            if ((now - startTime) > timeout) {
                break;
            }

            if (lastRecvTime > 0 && (now - lastRecvTime) > interByteTimeout) {
                break;
            }

            try {
                if (SerialHelperV2.this.mInputStream == null) {
                    break;
                }

                byte[] bufferTmp = SerialHelperV2.this.getStickPackageHelper().execute(SerialHelperV2.this.mInputStream);
                if (bufferTmp != null && bufferTmp.length > 0) {
                    lastRecvTime = System.currentTimeMillis();
                    if (buffer == null) {
                        buffer = bufferTmp;
                    } else {
                        byte[] result = new byte[buffer.length + bufferTmp.length];
                        System.arraycopy(buffer, 0, result, 0, buffer.length);
                        System.arraycopy(bufferTmp, 0, result, buffer.length, bufferTmp.length);
                        buffer = result;
                    }
                }
            } catch (Throwable e) {
                Timber.tag("error").e(e);
                break;
            }
        }
        return buffer;
    }

    public int getBaudRate() {
        return this.iBaudRate;
    }

    public boolean setBaudRate(int iBaud) {
        if (this._isOpen) {
            return false;
        } else {
            this.iBaudRate = iBaud;
            return true;
        }
    }

    public boolean setBaudRate(String sBaud) {
        int iBaud = Integer.parseInt(sBaud);
        return this.setBaudRate(iBaud);
    }

    public int getStopBits() {
        return this.stopBits;
    }

    public boolean setStopBits(int stopBits) {
        if (this._isOpen) {
            return false;
        } else {
            this.stopBits = stopBits;
            return true;
        }
    }

    public int getDataBits() {
        return this.dataBits;
    }

    public boolean setDataBits(int dataBits) {
        if (this._isOpen) {
            return false;
        } else {
            this.dataBits = dataBits;
            return true;
        }
    }

    public int getParity() {
        return this.parity;
    }

    public boolean setParity(int parity) {
        if (this._isOpen) {
            return false;
        } else {
            this.parity = parity;
            return true;
        }
    }

    public int getFlowCon() {
        return this.flowCon;
    }

    public boolean setFlowCon(int flowCon) {
        if (this._isOpen) {
            return false;
        } else {
            this.flowCon = flowCon;
            return true;
        }
    }

    public String getPort() {
        return this.sPort;
    }

    public boolean setPort(String sPort) {
        if (this._isOpen) {
            return false;
        } else {
            this.sPort = sPort;
            return true;
        }
    }

    public boolean isOpen() {
        return this._isOpen;
    }

    public byte[] getbLoopData() {
        return this._bLoopData;
    }

    public void setbLoopData(byte[] bLoopData) {
        this._bLoopData = bLoopData;
    }

    public void setTxtLoopData(String sTxt) {
        this._bLoopData = sTxt.getBytes();
    }

    public void setHexLoopData(String sHex) {
        this._bLoopData = ByteUtil.HexToByteArr(sHex);
    }

    public int getiDelay() {
        return this.iDelay;
    }

    public void setiDelay(int iDelay) {
        this.iDelay = iDelay;
    }

    public AbsStickPackageHelper getStickPackageHelper() {
        return this.mStickPackageHelper;
    }

    public void setStickPackageHelper(AbsStickPackageHelper mStickPackageHelper) {
        this.mStickPackageHelper = mStickPackageHelper;
    }

    private class SendThread extends Thread {
        public boolean suspendFlag;

        private SendThread() {
            this.suspendFlag = true;
        }

        public void run() {
            super.run();

            while (!this.isInterrupted()) {
                synchronized (this) {
                    while (this.suspendFlag) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                SerialHelperV2.this.send(SerialHelperV2.this.getbLoopData());

                try {
                    Thread.sleep((long) SerialHelperV2.this.iDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        public void setSuspendFlag() {
            this.suspendFlag = true;
        }

        public synchronized void setResume() {
            this.suspendFlag = false;
            this.notify();
        }
    }
}
