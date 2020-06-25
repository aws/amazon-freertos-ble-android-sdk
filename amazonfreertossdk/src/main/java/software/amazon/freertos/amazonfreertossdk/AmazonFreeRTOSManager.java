/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.freertos.amazonfreertossdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.security.KeyStore;
import java.util.Arrays;

import android.os.ParcelUuid;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.NonNull;

import static software.amazon.freertos.amazonfreertossdk.AmazonFreeRTOSConstants.*;

public class AmazonFreeRTOSManager {

    private static final String TAG = "AmazonFreeRTOSManager";
    private Context mContext;

    private Handler mScanHandler;
    private HandlerThread mScanHandlerThread;
    private boolean mScanning = false;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private List<ScanFilter> mScanFilters = Arrays.asList(
            new ScanFilter.Builder().setServiceUuid(
                    new ParcelUuid(UUID.fromString(UUID_AmazonFreeRTOS))).build());

    private Map<String, AmazonFreeRTOSDevice> mAFreeRTOSDevices = new HashMap<>();

    private BleScanResultCallback mBleScanResultCallback;

    /**
     * Construct an AmazonFreeRTOSManager instance.
     *
     * @param context          The app context. Should be passed in by the app that creates a new instance
     *                         of AmazonFreeRTOSManager.
     * @param bluetoothAdapter BluetoothAdaptor passed in by the app.
     */
    public AmazonFreeRTOSManager(Context context, BluetoothAdapter bluetoothAdapter) {
        mContext = context;
        mBluetoothAdapter = bluetoothAdapter;
    }

    /**
     * Setting the criteria for which exact the BLE devices to scan for. This overrides the default
     * mScanFilters which is by default set to scan for UUID_AmazonFreeRTOS.
     *
     * @param filters The list of ScanFilter for BLE devices.
     */
    public void setScanFilters(List<ScanFilter> filters) {
        mScanFilters = filters;
    }

    /**
     * Start scanning of nearby BLE devices. It filters the scan result only with AmazonFreeRTOS
     * service UUID unless setScanFilters was explicitly called. It keeps scanning for a period of
     * AmazonFreeRTOSConstants.class#SCAN_PERIOD ms, then stops the scanning automatically.
     * The scan result is passed back through the BleScanResultCallback. If at the time of calling
     * this API, there's already an ongoing scanning, then this will return immediately without
     * starting another scan.
     *
     * @param scanResultCallback The callback to notify the calling app of the scanning result. The
     *                           callback will be triggered, every time it finds a BLE device
     *                           nearby that meets the ScanFilter criteria.
     */
    public void startScanDevices(final BleScanResultCallback scanResultCallback) {
        startScanDevices(scanResultCallback, SCAN_PERIOD);
    }

    /**
     * Start scanning nearby BLE devices for a total duration of scanDuration milliseconds.
     *
     * @param scanResultCallback The callback to notify the calling app of the scanning result.
     * @param scanDuration       The duration of scanning. Keep scanning if 0.
     */
    public void startScanDevices(final BleScanResultCallback scanResultCallback, long scanDuration) {
        if (scanResultCallback == null) {
            throw new IllegalArgumentException("BleScanResultCallback is null");
        }
        mBleScanResultCallback = scanResultCallback;
        if (mBluetoothAdapter != null) {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (mScanHandlerThread == null) {
                mScanHandlerThread = new HandlerThread("ScanBleDeviceThread");
                mScanHandlerThread.start();
                mScanHandler = new Handler(mScanHandlerThread.getLooper());
            }
            scanLeDevice(scanDuration);
        } else {
            Log.e(TAG, "BluetoothAdaptor is null, please enable bluetooth.");
        }
    }

    private void scanLeDevice(long duration) {
        if (mScanning) {
            Log.d(TAG, "Scanning is already in progress.");
            return;
        }
        // Stops scanning after a pre-defined scan period.
        if (duration != 0) {
            mScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanDevices();
                }
            }, duration);
        }
        Log.i(TAG, "Starting ble device scan");
        mScanning = true;

        ScanSettings scanSettings = new ScanSettings.Builder().build();
        mBluetoothLeScanner.startScan(mScanFilters, scanSettings, mScanCallback);
    }

    /**
     * Stop scanning of nearby BLE devices. If there's no ongoing BLE scanning, then it will return
     * immediately.
     */
    public void stopScanDevices() {
        if (!mScanning) {
            Log.w(TAG, "No ble device scan is currently in progress.");
            return;
        }
        Log.i(TAG, "Stopping ble device scan");
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanning = false;
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "Found ble device: " + result.getDevice().getAddress()
                    + " RSSI: " + result.getRssi());
            if (mBleScanResultCallback != null) {
                mBleScanResultCallback.onBleScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Error when scanning ble device. Error code: " + errorCode);
            if (mBleScanResultCallback != null) {
                mBleScanResultCallback.onBleScanFailed(errorCode);
            }
        }
    };

    /**
     * Connect to the BLE device, and notify the connection state via BleConnectionStatusCallback.
     *
     * @param connectionStatusCallback The callback to notify app whether the BLE connection is
     *                                 successful. Must not be null.
     * @param btDevice                 the BLE device to be connected to.
     * @param cp                       the AWSCredential used to connect to AWS IoT.
     * @param autoReconnect            auto reconnect to device after unexpected disconnect
     */
    public AmazonFreeRTOSDevice connectToDevice(@NonNull final BluetoothDevice btDevice,
                                                @NonNull final BleConnectionStatusCallback connectionStatusCallback,
                                                final AWSCredentialsProvider cp,
                                                final boolean autoReconnect) {
        AmazonFreeRTOSDevice aDevice = new AmazonFreeRTOSDevice(btDevice, mContext, cp);
        mAFreeRTOSDevices.put(btDevice.getAddress(), aDevice);
        aDevice.connect(connectionStatusCallback, autoReconnect);
        return aDevice;
    }

    /**
     * Connect to the BLE device, and notify the connection state via BleConnectionStatusCallback.
     *
     * @param connectionStatusCallback The callback to notify app whether the BLE connection is
     *                                 successful. Must not be null.
     * @param btDevice                 the BLE device to be connected to.
     * @param ks                       the KeyStore that contains certificate used to connect to AWS IoT.
     * @param autoReconnect            auto reconnect to device after unexpected disconnect
     */
    public AmazonFreeRTOSDevice connectToDevice(@NonNull final BluetoothDevice btDevice,
                                                @NonNull final BleConnectionStatusCallback connectionStatusCallback,
                                                final KeyStore ks,
                                                final boolean autoReconnect) {
        AmazonFreeRTOSDevice aDevice = new AmazonFreeRTOSDevice(btDevice, mContext, ks);
        mAFreeRTOSDevices.put(btDevice.getAddress(), aDevice);
        aDevice.connect(connectionStatusCallback, autoReconnect);
        return aDevice;
    }

    /**
     * Closing BLE connection for the AmazonFreeRTOSDevice, reset all variables, and disconnect from AWS IoT.
     *
     * @param aDevice The AmazonFreeRTOSDevice to be disconnected.
     */
    public void disconnectFromDevice(@NonNull final AmazonFreeRTOSDevice aDevice) {
        mAFreeRTOSDevices.remove(aDevice.getMBluetoothDevice().getAddress());
        aDevice.disconnect();
    }

    /**
     * Get the instance of AmazonFreeRTOSDevice given the mac address of the BLE device
     *
     * @param macAddr the mac address of the connected BLE device
     * @return the corresponding AmazonFreeRTOSDevice instance that represents the connected BLE device.
     */
    public AmazonFreeRTOSDevice getConnectedDevice(String macAddr) {
        return mAFreeRTOSDevices.get(macAddr);
    }
}
