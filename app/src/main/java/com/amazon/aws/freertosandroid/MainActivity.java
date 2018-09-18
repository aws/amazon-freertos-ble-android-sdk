package com.amazon.aws.freertosandroid;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants;
import com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSManager;
import com.amazon.aws.amazonfreertossdk.BleConnectionStatusCallback;
import com.amazon.aws.amazonfreertossdk.BleScanResultCallback;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.client.AWSMobileClient;


public class MainActivity extends AppCompatActivity {

    /*
     * Replace with the MAC address of the ble device we want the app to connect.
     * At least one of BLE_DEVICE_MAC_ADDR or BLE_DEVICE_NAME should be set.
     */
    private static final String BLE_DEVICE_MAC_ADDR = "Replace with MAC address of the BLE device";
    /*
     * Replace with the friendly Bluetooth name of the ble device we want the app to connect.
     * At least one of BLE_DEVICE_MAC_ADDR or BLE_DEVICE_NAME should be set.
     */
    private static final String BLE_DEVICE_NAME = "Replace with device name";
    /*
     * Replace with the desired MTU value to set between the BLE device and the Android device.
     * Note: this is only required if you want to set MTU that is different than the original MTU
     * value on the BLE device. However, even after you set the MTU, the actual MTU value may be
     * smaller than what you set, because it is limited by the maximum MTU the devices can support.
     * Please refer to API documentation for AmazonFreeRTOSManager.class#setMtu.
     */
    private static final int MTU = 512;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private static final String TAG = "MainActivity";
    private Switch connectSwitch, mqttProxySwitch;
    private Button scanButton, discoverButton;
    private Button mtuButton;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;

    private AmazonFreeRTOSManager mAmazonFreeRTOSManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sign out button
        Button signOutButton = (Button) findViewById(R.id.signoutButton);
        signOutButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IdentityManager.getDefaultIdentityManager().signOut();
            }
        });

        //Preparing bluetooth
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Log.i(TAG, "Bluetooth is enabled.");
        }
        // requesting user to grant permission.
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);

        //Creating AmazonFreeRTOSManager
        AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance().getCredentialsProvider();
        mAmazonFreeRTOSManager = new AmazonFreeRTOSManager(this, mBluetoothAdapter, credentialsProvider);

        scanButton = (Button)findViewById(R.id.scanbutton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "scan button clicked.");
                mAmazonFreeRTOSManager.startScanBleDevices(new BleScanResultCallback() {
                    @Override
                    public void onBleScanResult(ScanResult result) {
                        if (BLE_DEVICE_MAC_ADDR.equals(result.getDevice().getAddress()) ||
                                BLE_DEVICE_NAME.equals(result.getDevice().getName())) {
                            mBluetoothDevice = result.getDevice();
                            connectSwitch.setEnabled(true);
                        }
                    }
                });
            }
        });

        final BleConnectionStatusCallback connectionStatusCallback = new BleConnectionStatusCallback() {
            @Override
            public void onBleConnectionStatusChanged(AmazonFreeRTOSConstants.BleConnectionState connectionStatus) {
                Log.i(TAG, "BLE connection status changed to: " + connectionStatus);
                if (connectionStatus == AmazonFreeRTOSConstants.BleConnectionState.BLE_CONNECTED) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            discoverButton.setEnabled(true);
                            mqttProxySwitch.setEnabled(true);
                            mtuButton.setEnabled(true);
                        }
                    });
                } else if (connectionStatus == AmazonFreeRTOSConstants.BleConnectionState.BLE_DISCONNECTED) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resetButtons();
                        }
                    });
                }
            }
        };

        connectSwitch = (Switch)findViewById(R.id.connectswitch);
        connectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                Log.i(TAG, "connect switch isChecked: " + (isChecked ? "ON":"OFF"));
                if (isChecked) {
                    mAmazonFreeRTOSManager.connectToDevice(mBluetoothDevice, connectionStatusCallback);
                } else {
                    resetButtons();
                    close();
                }
            }
        });

        discoverButton = (Button)findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.i(TAG, "discover button clicked.");
                discoverServices();
            }
        });

        mtuButton = (Button) findViewById(R.id.mtuButton);
        mtuButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.i(TAG, "mtu button clicked.");
                if (mAmazonFreeRTOSManager != null) {
                    mAmazonFreeRTOSManager.setMtu(MTU);
                }
            }
        });

        mqttProxySwitch = (Switch)findViewById(R.id.proxyControlSwitch);
        mqttProxySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                Log.i(TAG, "mqtt proxy switch isChecked: " + (isChecked ? "ON":"OFF"));
                if (isChecked) {
                    enableMqttProxy();
                } else {
                    disableMqttProxy();
                }
            }
        });

        resetButtons();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "successfully enabled bluetooth");
                mBluetoothAdapter = mBluetoothManager.getAdapter();
            }
            else {
                Log.w(TAG, "Failed to enable bluetooth");
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "ACCESS_FINE_LOCATION granted.");
                } else {
                    Log.w(TAG, "ACCESS_FINE_LOCATION denied");
                    scanButton.setEnabled(false);
                }
            }
        }
    }

    private void discoverServices() {
        mAmazonFreeRTOSManager.discoverServices();
    }

    private void enableMqttProxy() {
        mAmazonFreeRTOSManager.enableMqttProxy(true);
    }

    private void disableMqttProxy() {
        mAmazonFreeRTOSManager.enableMqttProxy(false);
    }

    private void close() {
        mAmazonFreeRTOSManager.close();
    }

    private void resetButtons() {
        connectSwitch.setChecked(false);
        discoverButton.setEnabled(false);
        mqttProxySwitch.setChecked(false);
        mqttProxySwitch.setEnabled(false);
        mtuButton.setEnabled(false);
    }
}
