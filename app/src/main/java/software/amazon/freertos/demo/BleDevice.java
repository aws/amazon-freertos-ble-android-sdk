package software.amazon.freertos.demo;

import android.bluetooth.BluetoothDevice;

import java.util.Objects;

public class BleDevice {
    private String name;
    private String macAddr;
    private BluetoothDevice mBluetoothDevice;

    public BleDevice(String name, String macAddr, BluetoothDevice bluetoothDevice) {
        this.name = name;
        this.macAddr = macAddr;
        mBluetoothDevice = bluetoothDevice;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        mBluetoothDevice = bluetoothDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BleDevice bleDevice = (BleDevice) o;
        return Objects.equals(getMacAddr(), bleDevice.getMacAddr());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getName(), getMacAddr(), getBluetoothDevice());
    }
}
