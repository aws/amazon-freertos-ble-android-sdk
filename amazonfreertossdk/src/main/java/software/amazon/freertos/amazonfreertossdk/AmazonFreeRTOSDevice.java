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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import software.amazon.freertos.amazonfreertossdk.deviceinfo.BrokerEndpoint;
import software.amazon.freertos.amazonfreertossdk.deviceinfo.Mtu;
import software.amazon.freertos.amazonfreertossdk.deviceinfo.Version;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttMessageDeliveryCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import lombok.Getter;
import lombok.NonNull;
import software.amazon.freertos.amazonfreertossdk.mqttproxy.*;
import software.amazon.freertos.amazonfreertossdk.networkconfig.*;

import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static software.amazon.freertos.amazonfreertossdk.AmazonFreeRTOSConstants.*;
import static software.amazon.freertos.amazonfreertossdk.AmazonFreeRTOSConstants.AmazonFreeRTOSError.BLE_DISCONNECTED_ERROR;
import static software.amazon.freertos.amazonfreertossdk.BleCommand.CommandType.DISCOVER_SERVICES;
import static software.amazon.freertos.amazonfreertossdk.BleCommand.CommandType.NOTIFICATION;
import static software.amazon.freertos.amazonfreertossdk.BleCommand.CommandType.READ_CHARACTERISTIC;
import static software.amazon.freertos.amazonfreertossdk.BleCommand.CommandType.REQUEST_MTU;
import static software.amazon.freertos.amazonfreertossdk.BleCommand.CommandType.WRITE_CHARACTERISTIC;
import static software.amazon.freertos.amazonfreertossdk.BleCommand.CommandType.WRITE_DESCRIPTOR;

public class AmazonFreeRTOSDevice {

    private static final String TAG = "FRD";
    private static final boolean VDBG = false;
    private Context mContext;

    @Getter
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private BleConnectionStatusCallback mBleConnectionStatusCallback;
    private NetworkConfigCallback mNetworkConfigCallback;
    private DeviceInfoCallback mDeviceInfoCallback;
    private BleConnectionState mBleConnectionState = BleConnectionState.BLE_DISCONNECTED;
    private String mAmazonFreeRTOSLibVersion = "NA";
    private String mAmazonFreeRTOSDeviceType = "NA";
    private String mAmazonFreeRTOSDeviceId = "NA";
    private boolean mGattAutoReconnect = false;
    private BroadcastReceiver mBondStateCallback = null;
    private int mMtu = 0;

    private boolean rr = false;
    private Queue<BleCommand> mMqttQueue = new LinkedList<>();
    private Queue<BleCommand> mNetworkQueue = new LinkedList<>();
    private Queue<BleCommand> mIncomingQueue = new LinkedList<>();
    private boolean mBleOperationInProgress = false;
    private boolean mRWinProgress = false;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private byte[] mValueWritten;
    private Semaphore mutex = new Semaphore(1);
    private Semaphore mIncomingMutex = new Semaphore(1);

    //Buffer for receiving messages from device
    private ByteArrayOutputStream mTxLargeObject = new ByteArrayOutputStream();
    private ByteArrayOutputStream mTxLargeNw = new ByteArrayOutputStream();
    //Buffer for sending messages to device.
    private int mTotalPackets = 0;
    private int mPacketCount = 1;
    private int mMessageId = 0;
    private int mMaxPayloadLen = 0;

    private AWSIotMqttManager mIotMqttManager;
    private MqttConnectionState mMqttConnectionState = MqttConnectionState.MQTT_Disconnected;

    private AWSCredentialsProvider mAWSCredential;
    private KeyStore mKeystore;

    /**
     * Construct an AmazonFreeRTOSDevice instance.
     *
     * @param context The app context. Should be passed in by the app that creates a new instance
     *                of AmazonFreeRTOSDevice.
     * @param device  BluetoothDevice returned from BLE scan result.
     * @param cp      AWS credential for connection to AWS IoT. If null is passed in,
     *                then it will not be able to do MQTT proxy over BLE as it cannot
     *                connect to AWS IoT.
     */
    AmazonFreeRTOSDevice(@NonNull BluetoothDevice device, @NonNull Context context, AWSCredentialsProvider cp) {
        this(device, context, cp, null);
    }

    /**
     * Construct an AmazonFreeRTOSDevice instance.
     *
     * @param context The app context. Should be passed in by the app that creates a new instance
     *                of AmazonFreeRTOSDevice.
     * @param device  BluetoothDevice returned from BLE scan result.
     * @param ks      the KeyStore which contains the certificate used to connect to AWS IoT.
     */
    AmazonFreeRTOSDevice(@NonNull BluetoothDevice device, @NonNull Context context, KeyStore ks) {
        this(device, context, null, ks);
    }

    private AmazonFreeRTOSDevice(@NonNull BluetoothDevice device, @NonNull Context context,
                                 AWSCredentialsProvider cp, KeyStore ks) {
        mContext = context;
        mBluetoothDevice = device;
        mAWSCredential = cp;
        mKeystore = ks;
    }

    void connect(@NonNull final BleConnectionStatusCallback connectionStatusCallback,
                 final boolean autoReconnect) {
        mBleConnectionStatusCallback = connectionStatusCallback;
        mHandlerThread = new HandlerThread("BleCommandHandler"); //TODO: unique thread name for each device?
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mGattAutoReconnect = autoReconnect;
        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback, TRANSPORT_LE);
    }

    private void cleanUp() {
        // If ble connection is lost, clear any pending ble command.
        mMqttQueue.clear();
        mNetworkQueue.clear();
        mIncomingQueue.clear();
        mMessageId = 0;
        mMtu = 0;
        mMaxPayloadLen = 0;
        mTxLargeObject.reset();
        mTotalPackets = 0;
        mPacketCount = 1;
    }

    /**
     * User initiated disconnect
     */
    void disconnect() {
        if (mBluetoothGatt != null) {
            mGattAutoReconnect = false;
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * Sends a ListNetworkReq command to the connected BLE device. The available WiFi networks found
     * by the connected BLE device will be returned in the callback as a ListNetworkResp. Each found
     * WiFi network should trigger the callback once. For example, if there are 10 available networks
     * found by the BLE device, this callback will be triggered 10 times, each containing one
     * ListNetworkResp that represents that WiFi network. In addition, the order of the callbacks will
     * be triggered as follows: the saved networks will be returned first, in decreasing order of their
     * preference, as denoted by their index. (The smallest non-negative index denotes the highest
     * preference, and is therefore returned first.) For example, the saved network with index 0 will
     * be returned first, then the saved network with index 1, then index 2, etc. After all saved
     * networks have been returned, the non-saved networks will be returned, in the decreasing order
     * of their RSSI value, a network with higher RSSI value will be returned before one with lower
     * RSSI value.
     *
     * @param listNetworkReq The ListNetwork request
     * @param callback       The callback which will be triggered once the BLE device sends a ListNetwork
     *                       response.
     */
    public void listNetworks(ListNetworkReq listNetworkReq, NetworkConfigCallback callback) {
        mNetworkConfigCallback = callback;
        byte[] listNetworkReqBytes = listNetworkReq.encode();
        sendDataToDevice(UUID_NETWORK_SERVICE, UUID_NETWORK_RX, UUID_NETWORK_RXLARGE, listNetworkReqBytes);
    }

    /**
     * Sends a SaveNetworkReq command to the connected BLE device. The SaveNetworkReq contains the
     * network credential. A SaveNetworkResp will be sent by the BLE device and triggers the callback.
     * To get the updated order of all networks, call listNetworks again.
     *
     * @param saveNetworkReq The SaveNetwork request.
     * @param callback       The callback that is triggered once the BLE device sends a SaveNetwork response.
     */
    public void saveNetwork(SaveNetworkReq saveNetworkReq, NetworkConfigCallback callback) {
        mNetworkConfigCallback = callback;
        byte[] saveNetworkReqBytes = saveNetworkReq.encode();
        sendDataToDevice(UUID_NETWORK_SERVICE, UUID_NETWORK_RX, UUID_NETWORK_RXLARGE, saveNetworkReqBytes);
    }

    /**
     * Sends an EditNetworkReq command to the connected BLE device. The EditNetwork request is used
     * to update the preference of a saved network. It contains the current index of the saved network
     * to be updated, and the desired new index of the save network to be updated to. Both the current
     * index and the new index must be one of those saved networks. Behavior is undefined if an index
     * of an unsaved network is provided in the EditNetworkReq.
     * To get the updated order of all networks, call listNetworks again.
     *
     * @param editNetworkReq The EditNetwork request.
     * @param callback       The callback that is triggered once the BLE device sends an EditNetwork response.
     */
    public void editNetwork(EditNetworkReq editNetworkReq, NetworkConfigCallback callback) {
        mNetworkConfigCallback = callback;
        byte[] editNetworkReqBytes = editNetworkReq.encode();
        sendDataToDevice(UUID_NETWORK_SERVICE, UUID_NETWORK_RX, UUID_NETWORK_RXLARGE, editNetworkReqBytes);
    }

    /**
     * Sends a DeleteNetworkReq command to the connected BLE device. The saved network with the index
     * specified in the delete network request will be deleted, making it a non-saved network again.
     * To get the updated order of all networks, call listNetworks again.
     *
     * @param deleteNetworkReq The DeleteNetwork request.
     * @param callback         The callback that is triggered once the BLE device sends a DeleteNetwork response.
     */
    public void deleteNetwork(DeleteNetworkReq deleteNetworkReq, NetworkConfigCallback callback) {
        mNetworkConfigCallback = callback;
        byte[] deleteNetworkReqBytes = deleteNetworkReq.encode();
        sendDataToDevice(UUID_NETWORK_SERVICE, UUID_NETWORK_RX, UUID_NETWORK_RXLARGE, deleteNetworkReqBytes);
    }

    /**
     * Get the current mtu value between device and Android phone. This method returns immediately.
     * The request to get mtu value is asynchronous through BLE command. The response will be delivered
     * through DeviceInfoCallback.
     *
     * @param callback The callback to notify app of current mtu value.
     */
    public void getMtu(DeviceInfoCallback callback) {
        mDeviceInfoCallback = callback;
        if (!getMtu() && mDeviceInfoCallback != null) {
            mDeviceInfoCallback.onError(BLE_DISCONNECTED_ERROR);
        }
    }

    /**
     * Get the current broker endpoint on the device. This broker endpoint is used to connect to AWS
     * IoT, hence, this is also the AWS IoT endpoint. This method returns immediately.
     * The request is sent asynchronously through BLE command. The response will be delivered
     * through DeviceInfoCallback.
     *
     * @param callback The callback to notify app of current broker endpoint on device.
     */
    public void getBrokerEndpoint(DeviceInfoCallback callback) {
        mDeviceInfoCallback = callback;
        if (!getBrokerEndpoint() && mDeviceInfoCallback != null) {
            mDeviceInfoCallback.onError(BLE_DISCONNECTED_ERROR);
        }
    }

    /**
     * Get the AmazonFreeRTOS library software version running on the device. This method returns
     * immediately. The request is sent asynchronously through BLE command. The response will be
     * delivered through DeviceInfoCallback.
     *
     * @param callback The callback to notify app of current software version.
     */
    public void getDeviceVersion(DeviceInfoCallback callback) {
        mDeviceInfoCallback = callback;
        if (!getDeviceVersion() && mDeviceInfoCallback != null) {
            mDeviceInfoCallback.onError(BLE_DISCONNECTED_ERROR);
        }
    }

    /**
     * Try to read a characteristic from the Gatt service. If pairing is enabled, it will be triggered
     * by this action.
     */
    private void probe() {
        getDeviceVersion();
    }

    /**
     * Initialize the Gatt services
     */
    private void initialize() {
        getDeviceType();
        getDeviceId();
        getMtu();
        sendBleCommand(new BleCommand(WRITE_DESCRIPTOR, UUID_MQTT_PROXY_TX, UUID_MQTT_PROXY_SERVICE));
        sendBleCommand(new BleCommand(WRITE_DESCRIPTOR, UUID_MQTT_PROXY_TXLARGE, UUID_MQTT_PROXY_SERVICE));
        sendBleCommand(new BleCommand(WRITE_DESCRIPTOR, UUID_NETWORK_TX, UUID_NETWORK_SERVICE));
        sendBleCommand(new BleCommand(WRITE_DESCRIPTOR, UUID_NETWORK_TXLARGE, UUID_NETWORK_SERVICE));
    }

    private void enableService(final String serviceUuid, final boolean enable) {
        byte[] ready = new byte[1];
        if (enable) {
            ready[0] = 1;
        } else {
            ready[0] = 0;
        }
        switch (serviceUuid) {
            case UUID_NETWORK_SERVICE:
                Log.i(TAG, (enable ? "Enabling" : "Disabling") + " Wifi provisioning");
                sendBleCommand(new BleCommand(WRITE_CHARACTERISTIC, UUID_NETWORK_CONTROL, UUID_NETWORK_SERVICE, ready));
                break;
            case UUID_MQTT_PROXY_SERVICE:
                if (mKeystore != null || mAWSCredential != null) {
                    Log.i(TAG, (enable ? "Enabling" : "Disabling") + " MQTT Proxy");
                    sendBleCommand(new BleCommand(WRITE_CHARACTERISTIC, UUID_MQTT_PROXY_CONTROL, UUID_MQTT_PROXY_SERVICE, ready));
                }
                break;
            default:
                Log.w(TAG, "Unknown service. Ignoring.");
        }
    }

    private void processIncomingQueue() {
        try {
            mIncomingMutex.acquire();
            while (mIncomingQueue.size() != 0) {
                BleCommand bleCommand = mIncomingQueue.poll();
                Log.d(TAG, "Processing incoming queue. size: " + mIncomingQueue.size());
                byte[] responseBytes = bleCommand.getData();
                String cUuid = bleCommand.getCharacteristicUuid();
                switch (cUuid) {
                    case UUID_MQTT_PROXY_TX:
                        handleMqttTxMessage(responseBytes);
                        break;
                    case UUID_MQTT_PROXY_TXLARGE:
                        try {
                            mTxLargeObject.write(responseBytes);
                            sendBleCommand(new BleCommand(READ_CHARACTERISTIC,
                                    UUID_MQTT_PROXY_TXLARGE, UUID_MQTT_PROXY_SERVICE));
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to concatenate byte array.", e);
                        }
                        break;
                    case UUID_NETWORK_TX:
                        handleNwTxMessage(responseBytes);
                        break;
                    case UUID_NETWORK_TXLARGE:
                        try {
                            mTxLargeNw.write(responseBytes);
                            sendBleCommand(new BleCommand(READ_CHARACTERISTIC,
                                    UUID_NETWORK_TXLARGE, UUID_NETWORK_SERVICE));
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to concatenate byte array.", e);
                        }
                        break;
                    default:
                        Log.e(TAG, "Unknown characteristic " + cUuid);
                }
            }
            mIncomingMutex.release();
        } catch (InterruptedException e) {
            Log.e(TAG, "Incoming mutex error, ", e);
        }
    }

    private void registerBondStateCallback() {

        mBondStateCallback = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                        int prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                        int now = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        Log.d(TAG, "Bond state changed from " + prev + " to " + now);
                        /**
                         * If the state changed from bonding to bonded, then we have a valid bond created
                         * for the device.
                         * If discovery is not performed initiate discovery.
                         * If services are discovered start initialization by reading device version characteristic.
                         */
                        if (prev == BluetoothDevice.BOND_BONDING && now == BluetoothDevice.BOND_BONDED) {
                            List<BluetoothGattService> services = mBluetoothGatt.getServices();
                            if (services == null || services.isEmpty()) {
                                discoverServices();
                            } else {
                                probe();
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        };

        final IntentFilter bondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mBondStateCallback, bondFilter);
    }

    private void unRegisterBondStateCallback() {
        if (mBondStateCallback != null) {
            try {
                mContext.unregisterReceiver(mBondStateCallback);
                mBondStateCallback = null;
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "Caught exception while unregistering broadcast receiver" );
            }
        }
    }


    /**
     * This is the callback for all BLE commands sent from SDK to device. The response of BLE
     * command is included in the callback, together with the status code.
     */
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    Log.i(TAG, "BLE connection state changed: " + status + "; new state: "
                            + AmazonFreeRTOSConstants.BleConnectionState.values()[newState]);
                    if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                        int bondState = mBluetoothDevice.getBondState();

                        Log.i(TAG, "Connected to GATT server.");
                        mBleConnectionState = AmazonFreeRTOSConstants.BleConnectionState.BLE_CONNECTED;
                        mBleConnectionStatusCallback.onBleConnectionStatusChanged(mBleConnectionState);

                        // If the device is already bonded or will not bond we can call discoverServices() immediately
                        if (mBluetoothDevice.getBondState() != BOND_BONDING) {
                            discoverServices();
                        } else {
                            registerBondStateCallback();
                        }
                    } else {
                        Log.i(TAG, "Disconnected from GATT server due to error or peripheral initiated disconnect.");

                        mBleConnectionState = AmazonFreeRTOSConstants.BleConnectionState.BLE_DISCONNECTED;

                        if (mMqttConnectionState != AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Disconnected) {
                            disconnectFromIot();
                        }

                        unRegisterBondStateCallback();

                        cleanUp();

                        mBleConnectionStatusCallback.onBleConnectionStatusChanged(mBleConnectionState);

                        /**
                         * If auto reconnect is enabled, start a reconnect procedure in background
                         * using connect() method. Else close the GATT.
                         * Auto reconnect will be disabled when user initiates disconnect.
                         */
                        if (!mGattAutoReconnect) {
                            gatt.close();
                            mBluetoothGatt = null;
                        } else {
                            mBluetoothGatt.connect();
                        }
                    }

                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "Discovered Ble gatt services successfully. Bonding state: "
                                + mBluetoothDevice.getBondState());
                        describeGattServices(mBluetoothGatt.getServices());

                        /**
                         *  Trigger bonding if needed, by reading device version characteristic, if bonding is not already
                         *  in progress by the stack.
                         */
                        if (mBluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDING) {
                            probe();
                        }
                    } else {
                        Log.e(TAG, "onServicesDiscovered received: " + status);
                        disconnect();
                    }
                    processNextBleCommand();
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    byte[] responseBytes = characteristic.getValue();
                    Log.d(TAG, "->->-> Characteristic changed for: "
                            + uuidToName.get(characteristic.getUuid().toString())
                            + " with data: " + bytesToHexString(responseBytes));
                    BleCommand incomingCommand = new BleCommand(NOTIFICATION,
                            characteristic.getUuid().toString(),
                            characteristic.getService().getUuid().toString(), responseBytes);
                    mIncomingQueue.add(incomingCommand);

                    if (!mRWinProgress) {
                        processIncomingQueue();
                    }
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                              int status) {
                    Log.d(TAG, "onDescriptorWrite for characteristic: "
                            + uuidToName.get(descriptor.getCharacteristic().getUuid().toString())
                            + "; Status: " + (status == 0 ? "Success" : status));
                    processNextBleCommand();
                }

                @Override
                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                    Log.i(TAG, "onMTUChanged : " + mtu + " status: " + (status == 0 ? "Success" : status));
                    mMtu = mtu;
                    mMaxPayloadLen = Math.max(mMtu - 3, 0);
                    // The BLE service should be initialized at this stage
                    if (mBleConnectionState == BleConnectionState.BLE_INITIALIZING) {
                        mBleConnectionState = AmazonFreeRTOSConstants.BleConnectionState.BLE_INITIALIZED;
                        mBleConnectionStatusCallback.onBleConnectionStatusChanged(mBleConnectionState);
                    }
                    enableService(UUID_NETWORK_SERVICE, true);
                    enableService(UUID_MQTT_PROXY_SERVICE, true);
                    processNextBleCommand();
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    mRWinProgress = false;
                    Log.d(TAG, "->->-> onCharacteristicRead status: " + (status == 0 ? "Success. " : status));

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        // On the first successful read we enable the services
                        if (mBleConnectionState == BleConnectionState.BLE_CONNECTED) {
                            Log.d(TAG, "GATT services initializing...");
                            mBleConnectionState = AmazonFreeRTOSConstants.BleConnectionState.BLE_INITIALIZING;
                            mBleConnectionStatusCallback.onBleConnectionStatusChanged(mBleConnectionState);
                            initialize();
                        }
                        byte[] responseBytes = characteristic.getValue();
                        Log.d(TAG, "->->-> onCharacteristicRead: " + bytesToHexString(responseBytes));
                        switch (characteristic.getUuid().toString()) {
                            case UUID_MQTT_PROXY_TXLARGE:
                                try {
                                    mTxLargeObject.write(responseBytes);
                                    if (responseBytes.length < mMaxPayloadLen) {
                                        byte[] largeMessage = mTxLargeObject.toByteArray();
                                        Log.d(TAG, "MQTT Large object received from device successfully: "
                                                + bytesToHexString(largeMessage));
                                        handleMqttTxMessage(largeMessage);
                                        mTxLargeObject.reset();
                                    } else {
                                        sendBleCommand(new BleCommand(READ_CHARACTERISTIC,
                                                UUID_MQTT_PROXY_TXLARGE, UUID_MQTT_PROXY_SERVICE));
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "Failed to concatenate byte array.", e);
                                }
                                break;
                            case UUID_NETWORK_TXLARGE:
                                try {
                                    mTxLargeNw.write(responseBytes);
                                    if (responseBytes.length < mMaxPayloadLen) {
                                        byte[] largeMessage = mTxLargeNw.toByteArray();
                                        Log.d(TAG, "NW Large object received from device successfully: "
                                                + bytesToHexString(largeMessage));
                                        handleNwTxMessage(largeMessage);
                                        mTxLargeNw.reset();
                                    } else {
                                        sendBleCommand(new BleCommand(READ_CHARACTERISTIC,
                                                UUID_NETWORK_TXLARGE, UUID_NETWORK_SERVICE));
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "Failed to concatenate byte array.", e);
                                }
                                break;
                            case UUID_DEVICE_MTU:
                                Mtu currentMtu = new Mtu();
                                currentMtu.mtu = new String(responseBytes);
                                Log.i(TAG, "Default MTU is set to: " + currentMtu.mtu);
                                try {
                                    mMtu = Integer.parseInt(currentMtu.mtu);
                                    mMaxPayloadLen = Math.max(mMtu - 3, 0);
                                    if (mDeviceInfoCallback != null) {
                                        mDeviceInfoCallback.onObtainMtu(mMtu);
                                    }
                                    setMtu(mMtu);
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Cannot parse default MTU value.");
                                }
                                break;
                            case UUID_IOT_ENDPOINT:
                                BrokerEndpoint currentEndpoint = new BrokerEndpoint();
                                currentEndpoint.brokerEndpoint = new String(responseBytes);
                                Log.i(TAG, "Current broker endpoint is set to: "
                                        + currentEndpoint.brokerEndpoint);
                                if (mDeviceInfoCallback != null) {
                                    mDeviceInfoCallback.onObtainBrokerEndpoint(currentEndpoint.brokerEndpoint);
                                }
                                break;
                            case UUID_DEVICE_VERSION:
                                Version currentVersion = new Version();
                                currentVersion.version = new String(responseBytes);
                                if (!currentVersion.version.isEmpty()) {
                                    mAmazonFreeRTOSLibVersion = currentVersion.version;
                                }
                                Log.i(TAG, "Ble software version on device is: " + currentVersion.version);
                                if (mDeviceInfoCallback != null) {
                                    mDeviceInfoCallback.onObtainDeviceSoftwareVersion(currentVersion.version);
                                }
                                break;
                            case UUID_DEVICE_PLATFORM:
                                String platform = new String(responseBytes);
                                if (!platform.isEmpty()) {
                                    mAmazonFreeRTOSDeviceType = platform;
                                }
                                Log.i(TAG, "Device type is: " + mAmazonFreeRTOSDeviceType);
                                break;
                            case UUID_DEVICE_ID:
                                String devId = new String(responseBytes);
                                if (!devId.isEmpty()) {
                                    mAmazonFreeRTOSDeviceId = devId;
                                }
                                Log.i(TAG, "Device id is: " + mAmazonFreeRTOSDeviceId);
                                break;
                            default:
                                Log.w(TAG, "Unknown characteristic read. ");
                        }
                    }
                    processIncomingQueue();
                    processNextBleCommand();
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  int status) {
                    mRWinProgress = false;
                    byte[] value = characteristic.getValue();
                    Log.d(TAG, "onCharacteristicWrite for: "
                            + uuidToName.get(characteristic.getUuid().toString())
                            + "; status: " + (status == 0 ? "Success" : status) + "; value: " + bytesToHexString(value));
                    if (Arrays.equals(mValueWritten, value)) {
                        processIncomingQueue();
                        processNextBleCommand();
                    } else {
                        Log.e(TAG, "values don't match!");
                    }
                }
            };


    /**
     * Handle mqtt messages received from device.
     *
     * @param message message received from device.
     */
    private void handleMqttTxMessage(byte[] message) {
        MessageType messageType = new MessageType();
        if (!messageType.decode(message)) {
            return;
        }
        Log.i(TAG, "Handling Mqtt Message type : " + messageType.type);
        switch (messageType.type) {
            case MQTT_MSG_CONNECT:
                final Connect connect = new Connect();
                if (connect.decode(message)) {
                    connectToIoT(connect);
                }
                break;
            case MQTT_MSG_SUBSCRIBE:
                final Subscribe subscribe = new Subscribe();
                if (subscribe.decode(message)) {
                    Log.d(TAG, subscribe.toString());
                    subscribeToIoT(subscribe);
                /*
                  Currently, because the IoT part of aws mobile sdk for Android
                  does not provide suback callback when subscribe is successful,
                  we create a fake suback message and send to device as a workaround.
                  Wait for 0.5 sec so that the subscribe is complete. Potential bug:
                  Message is received from the subscribed topic before suback
                  is sent to device.
                 */
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendSubAck(subscribe);
                        }
                    }, 500);
                }
                break;
            case MQTT_MSG_UNSUBSCRIBE:
                final Unsubscribe unsubscribe = new Unsubscribe();
                if (unsubscribe.decode(message)) {
                    unsubscribeToIoT(unsubscribe);
                /*
                  TODO: add unsuback support in Aws Mobile sdk
                 */
                    sendUnsubAck(unsubscribe);
                }
                break;
            case MQTT_MSG_PUBLISH:
                final Publish publish = new Publish();
                if (publish.decode(message)) {
                    mMessageId = publish.getMsgID();
                    publishToIoT(publish);
                }
                break;
            case MQTT_MSG_DISCONNECT:
                disconnectFromIot();
                break;
            case MQTT_MSG_PUBACK:
                /*
                 AWS Iot SDK currently sends pub ack back to cloud without waiting
                 for pub ack from device.
                 */
                final Puback puback = new Puback();
                if (puback.decode(message)) {
                    Log.w(TAG, "Received mqtt pub ack from device. MsgID: " + puback.msgID);
                }
                break;
            case MQTT_MSG_PINGREQ:
                PingResp pingResp = new PingResp();
                byte[] pingRespBytes = pingResp.encode();
                sendDataToDevice(UUID_MQTT_PROXY_SERVICE, UUID_MQTT_PROXY_RX, UUID_MQTT_PROXY_RXLARGE, pingRespBytes);
                break;
            default:
                Log.e(TAG, "Unknown mqtt message type: " + messageType.type);
        }
    }

    private void handleNwTxMessage(byte[] message) {
        MessageType messageType = new MessageType();
        if (!messageType.decode(message)) {
            return;
        }
        Log.i(TAG, "Handling Network Message type : " + messageType.type);
        switch (messageType.type) {
            case LIST_NETWORK_RESP:
                ListNetworkResp listNetworkResp = new ListNetworkResp();
                if (listNetworkResp.decode(message) && mNetworkConfigCallback != null) {
                    Log.d(TAG, listNetworkResp.toString());
                    mNetworkConfigCallback.onListNetworkResponse(listNetworkResp);
                }
                break;
            case SAVE_NETWORK_RESP:
                SaveNetworkResp saveNetworkResp = new SaveNetworkResp();
                if (saveNetworkResp.decode(message) && mNetworkConfigCallback != null) {
                    mNetworkConfigCallback.onSaveNetworkResponse(saveNetworkResp);
                }
                break;
            case EDIT_NETWORK_RESP:
                EditNetworkResp editNetworkResp = new EditNetworkResp();
                if (editNetworkResp.decode(message) && mNetworkConfigCallback != null) {
                    mNetworkConfigCallback.onEditNetworkResponse(editNetworkResp);
                }
                break;
            case DELETE_NETWORK_RESP:
                DeleteNetworkResp deleteNetworkResp = new DeleteNetworkResp();
                if (deleteNetworkResp.decode(message) && mNetworkConfigCallback != null) {
                    mNetworkConfigCallback.onDeleteNetworkResponse(deleteNetworkResp);
                }
                break;
            default:
                Log.e(TAG, "Unknown network message type: " + messageType.type);
        }
    }

    private void connectToIoT(final Connect connect) {
        if (mMqttConnectionState == AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connected) {
            Log.w(TAG, "Already connected to IOT, sending connack to device again.");
            sendConnAck();
            return;
        }
        if (mMqttConnectionState != AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Disconnected) {
            Log.w(TAG, "Previous connection is active, please retry or disconnect mqtt first.");
            return;
        }
        mIotMqttManager = new AWSIotMqttManager(connect.clientID, connect.brokerEndpoint);

        Map<String, String> userMetaData = new HashMap<>();
        userMetaData.put("AFRSDK", "Android");
        //userMetaData.put("AFRSDKVersion", AMAZONFREERTOS_SDK_VERSION);
        userMetaData.put("AFRLibVersion", mAmazonFreeRTOSLibVersion);
        userMetaData.put("Platform", mAmazonFreeRTOSDeviceType);
        userMetaData.put("AFRDevID", mAmazonFreeRTOSDeviceId);
        mIotMqttManager.updateUserMetaData(userMetaData);

        AWSIotMqttClientStatusCallback mqttClientStatusCallback = new AWSIotMqttClientStatusCallback() {
            @Override
            public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                Log.i(TAG, "mqtt connection status changed to: " + status);
                switch (status) {
                    case Connected:
                        mMqttConnectionState = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connected;
                        //sending connack
                        if (isBLEConnected() && mBluetoothGatt != null) {
                            sendConnAck();
                        } else {
                            Log.e(TAG, "Cannot send CONNACK because BLE connection is: " + mBleConnectionState);
                        }
                        break;
                    case Connecting:
                    case Reconnecting:
                        mMqttConnectionState = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connecting;
                        break;
                    case ConnectionLost:
                        mMqttConnectionState = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Disconnected;
                        break;
                    default:
                        Log.e(TAG, "Unknown mqtt connection state: " + status);
                }
            }
        };

        if (mKeystore != null) {
            Log.i(TAG, "Connecting to IoT using KeyStore: " + connect.brokerEndpoint);
            mIotMqttManager.connect(mKeystore, mqttClientStatusCallback);
        } else {
            Log.i(TAG, "Connecting to IoT using AWS credential: " + connect.brokerEndpoint);
            mIotMqttManager.connect(mAWSCredential, mqttClientStatusCallback);
        }
    }

    private void disconnectFromIot() {
        if (mIotMqttManager != null) {
            try {
                mIotMqttManager.disconnect();
                mMqttConnectionState = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Disconnected;
            } catch (Exception e) {
                Log.e(TAG, "Mqtt disconnect error: ", e);
            }
        }
    }

    private void subscribeToIoT(final Subscribe subscribe) {
        if (mMqttConnectionState != AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connected) {
            Log.e(TAG, "Cannot subscribe because mqtt state is not connected.");
            return;
        }

        for (int i = 0; i < subscribe.topics.size(); i++) {
            try {
                String topic = subscribe.topics.get(i);
                Log.i(TAG, "Subscribing to IoT on topic : " + topic);
                final int QoS = subscribe.qoSs.get(i);
                AWSIotMqttQos qos = (QoS == 0 ? AWSIotMqttQos.QOS0 : AWSIotMqttQos.QOS1);
                mIotMqttManager.subscribeToTopic(topic, qos, new AWSIotMqttNewMessageCallback() {
                    @Override
                    public void onMessageArrived(final String topic, final byte[] data) {
                        String message = new String(data, StandardCharsets.UTF_8);
                        Log.i(TAG, " Message arrived on topic: " + topic);
                        Log.v(TAG, "   Message: " + message);
                        Publish publish = new Publish(
                                MQTT_MSG_PUBLISH,
                                topic,
                                mMessageId,
                                QoS,
                                data
                        );
                        publishToDevice(publish);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Subscription error.", e);
            }
        }
    }

    private void unsubscribeToIoT(final Unsubscribe unsubscribe) {
        if (mMqttConnectionState != AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connected) {
            Log.e(TAG, "Cannot unsubscribe because mqtt state is not connected.");
            return;
        }

        for (int i = 0; i < unsubscribe.topics.size(); i++) {
            try {
                String topic = unsubscribe.topics.get(i);
                Log.i(TAG, "UnSubscribing to IoT on topic : " + topic);
                mIotMqttManager.unsubscribeTopic(topic);
            } catch (Exception e) {
                Log.e(TAG, "Unsubscribe error.", e);
            }
        }
    }

    private void publishToIoT(final Publish publish) {
        if (mMqttConnectionState != AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connected) {
            Log.e(TAG, "Cannot publish message to IoT because mqtt connection state is not connected.");
            return;
        }
        AWSIotMqttMessageDeliveryCallback deliveryCallback = new AWSIotMqttMessageDeliveryCallback() {
            @Override
            public void statusChanged(MessageDeliveryStatus messageDeliveryStatus, Object o) {
                Log.d(TAG, "Publish msg delivery status: " + messageDeliveryStatus.toString());
                if (messageDeliveryStatus == MessageDeliveryStatus.Success && publish.getQos() == 1) {
                    sendPubAck(publish);
                }
            }
        };
        try {
            String topic = publish.getTopic();
            byte[] data = publish.getPayload();
            Log.i(TAG, "Sending mqtt message to IoT on topic: " + topic
                    + " message: " + new String(data)
                    + " MsgID: " + publish.getMsgID());
            mIotMqttManager.publishData(data, topic, AWSIotMqttQos.values()[publish.getQos()],
                    deliveryCallback, null);
        } catch (Exception e) {
            Log.e(TAG, "Publish error.", e);
        }
    }

    private void sendConnAck() {
        Connack connack = new Connack();
        connack.type = MQTT_MSG_CONNACK;
        connack.status = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connected.ordinal();
        byte[] connackBytes = connack.encode();
        sendDataToDevice(UUID_MQTT_PROXY_SERVICE, UUID_MQTT_PROXY_RX, UUID_MQTT_PROXY_RXLARGE, connackBytes);
    }

    private boolean isBLEConnected() {
        return mBleConnectionState == BleConnectionState.BLE_CONNECTED ||
                mBleConnectionState == BleConnectionState.BLE_INITIALIZED ||
                mBleConnectionState == BleConnectionState.BLE_INITIALIZING;
    }

    private void sendSubAck(final Subscribe subscribe) {
        if (!isBLEConnected() && mBluetoothGatt != null) {
            Log.e(TAG, "Cannot send SUB ACK to BLE device because BLE connection state" +
                    " is not connected");
            return;
        }
        Log.i(TAG, "Sending SUB ACK back to device.");
        Suback suback = new Suback();
        suback.type = MQTT_MSG_SUBACK;
        suback.msgID = subscribe.msgID;
        suback.status = subscribe.qoSs.get(0);
        byte[] subackBytes = suback.encode();
        sendDataToDevice(UUID_MQTT_PROXY_SERVICE, UUID_MQTT_PROXY_RX, UUID_MQTT_PROXY_RXLARGE, subackBytes);
    }

    private void sendUnsubAck(final Unsubscribe unsubscribe) {
        if (!isBLEConnected() && mBluetoothGatt != null) {
            Log.e(TAG, "Cannot send Unsub ACK to BLE device because BLE connection state" +
                    " is not connected");
            return;
        }
        Log.i(TAG, "Sending Unsub ACK back to device.");
        Unsuback unsuback = new Unsuback();
        unsuback.type = MQTT_MSG_UNSUBACK;
        unsuback.msgID = unsubscribe.msgID;
        byte[] unsubackBytes = unsuback.encode();
        sendDataToDevice(UUID_MQTT_PROXY_SERVICE, UUID_MQTT_PROXY_RX, UUID_MQTT_PROXY_RXLARGE, unsubackBytes);
    }

    private void sendPubAck(final Publish publish) {
        if (!isBLEConnected() && mBluetoothGatt != null) {
            Log.e(TAG, "Cannot send PUB ACK to BLE device because BLE connection state" +
                    " is not connected");
            return;
        }
        Log.i(TAG, "Sending PUB ACK back to device. MsgID: " + publish.getMsgID());
        Puback puback = new Puback();
        puback.type = MQTT_MSG_PUBACK;
        puback.msgID = publish.getMsgID();
        byte[] pubackBytes = puback.encode();
        sendDataToDevice(UUID_MQTT_PROXY_SERVICE, UUID_MQTT_PROXY_RX, UUID_MQTT_PROXY_RXLARGE, pubackBytes);
    }

    private void publishToDevice(final Publish publish) {
        if (!isBLEConnected() && mBluetoothGatt != null) {
            Log.e(TAG, "Cannot deliver mqtt message to BLE device because BLE connection state" +
                    " is not connected");
            return;
        }
        Log.d(TAG, "Sending received mqtt message back to device, topic: " + publish.getTopic()
                + " payload bytes: " + bytesToHexString(publish.getPayload())
                + " MsgID: " + publish.getMsgID());
        byte[] publishBytes = publish.encode();
        sendDataToDevice(UUID_MQTT_PROXY_SERVICE, UUID_MQTT_PROXY_RX, UUID_MQTT_PROXY_RXLARGE, publishBytes);
    }

    private void discoverServices() {
        if (isBLEConnected() && mBluetoothGatt != null) {
            sendBleCommand(new BleCommand(DISCOVER_SERVICES));
        } else {
            Log.w(TAG, "Bluetooth connection state is not connected.");
        }
    }

    private void setMtu(int mtu) {
        if (isBLEConnected() && mBluetoothGatt != null) {
            Log.i(TAG, "Setting mtu to: " + mtu);
            sendBleCommand(new BleCommand(REQUEST_MTU, mtu));
        } else {
            Log.w(TAG, "Bluetooth connection state is not connected.");
        }
    }

    private boolean getMtu() {
        if (isBLEConnected() && mBluetoothGatt != null) {
            Log.d(TAG, "Getting current MTU.");
            sendBleCommand(new BleCommand(READ_CHARACTERISTIC, UUID_DEVICE_MTU, UUID_DEVICE_INFORMATION_SERVICE));
            return true;
        } else {
            Log.w(TAG, "Bluetooth is not connected.");
            return false;
        }
    }

    private boolean getBrokerEndpoint() {
        if (isBLEConnected() && mBluetoothGatt != null) {
            Log.d(TAG, "Getting broker endpoint.");
            sendBleCommand(new BleCommand(READ_CHARACTERISTIC, UUID_IOT_ENDPOINT, UUID_DEVICE_INFORMATION_SERVICE));
            return true;
        } else {
            Log.w(TAG, "Bluetooth is not connected.");
            return false;
        }
    }

    private boolean getDeviceVersion() {
        if (isBLEConnected() && mBluetoothGatt != null) {
            Log.d(TAG, "Getting ble software version on device.");
            sendBleCommand(new BleCommand(READ_CHARACTERISTIC, UUID_DEVICE_VERSION, UUID_DEVICE_INFORMATION_SERVICE));
            return true;
        } else {
            Log.w(TAG, "Bluetooth is not connected.");
            return false;
        }
    }

    private boolean getDeviceType() {
        if (isBLEConnected() && mBluetoothGatt != null) {
            Log.d(TAG, "Getting device type...");
            sendBleCommand(new BleCommand(READ_CHARACTERISTIC, UUID_DEVICE_PLATFORM, UUID_DEVICE_INFORMATION_SERVICE));
            return true;
        } else {
            Log.w(TAG, "Bluetooth is not connected.");
            return false;
        }
    }

    private boolean getDeviceId() {
        if (isBLEConnected() && mBluetoothGatt != null) {
            Log.d(TAG, "Getting device cert id...");
            sendBleCommand(new BleCommand(READ_CHARACTERISTIC, UUID_DEVICE_ID, UUID_DEVICE_INFORMATION_SERVICE));
            return true;
        } else {
            Log.w(TAG, "Bluetooth is not connected.");
            return false;
        }
    }

    private void sendDataToDevice(final String service, final String rx, final String rxlarge, byte[] data) {
        if (data != null) {
            if (data.length < mMaxPayloadLen) {
                sendBleCommand(new BleCommand(WRITE_CHARACTERISTIC, rx, service, data));
            } else {
                mTotalPackets = data.length / mMaxPayloadLen + 1;
                Log.i(TAG, "This message is larger than max payload size: " + mMaxPayloadLen
                        + ". Breaking down to " + mTotalPackets + " packets.");
                mPacketCount = 0; //reset packet count
                while (mMaxPayloadLen * mPacketCount <= data.length) {
                    byte[] packet = Arrays.copyOfRange(data, mMaxPayloadLen * mPacketCount,
                            Math.min(data.length, mMaxPayloadLen * mPacketCount + mMaxPayloadLen));
                    mPacketCount++;
                    Log.d(TAG, "Packet #" + mPacketCount + ": " + bytesToHexString(packet));
                    sendBleCommand(new BleCommand(WRITE_CHARACTERISTIC, rxlarge, service, packet));
                }
            }
        }
    }

    private void sendBleCommand(final BleCommand command) {
        if (UUID_MQTT_PROXY_SERVICE.equals(command.getServiceUuid())) {
            mMqttQueue.add(command);
        } else {
            mNetworkQueue.add(command);
        }
        processBleCommandQueue();
    }

    private void processBleCommandQueue() {
        try {
            mutex.acquire();
            if (mBleOperationInProgress) {
                Log.d(TAG, "Ble operation is in progress. mqtt queue: " + mMqttQueue.size()
                        + " network queue: " + mNetworkQueue.size());
            } else {
                if (mMqttQueue.peek() == null && mNetworkQueue.peek() == null) {
                    Log.d(TAG, "There's no ble command in the queue.");
                    mBleOperationInProgress = false;
                } else {
                    mBleOperationInProgress = true;
                    BleCommand bleCommand;
                    if (mNetworkQueue.peek() != null && mMqttQueue.peek() != null) {
                        if (rr) {
                            bleCommand = mMqttQueue.poll();
                        } else {
                            bleCommand = mNetworkQueue.poll();
                        }
                        rr = !rr;
                    } else if (mNetworkQueue.peek() != null) {
                        bleCommand = mNetworkQueue.poll();
                    } else {
                        bleCommand = mMqttQueue.poll();
                    }
                    Log.d(TAG, "Processing BLE command: " + bleCommand.getType()
                            + " remaining mqtt queue " + mMqttQueue.size()
                            + ", network queue " + mNetworkQueue.size());
                    boolean commandSent = false;
                    switch (bleCommand.getType()) {
                        case WRITE_DESCRIPTOR:
                            if (writeDescriptor(bleCommand.getServiceUuid(), bleCommand.getCharacteristicUuid())) {
                                commandSent = true;
                            }
                            break;
                        case WRITE_CHARACTERISTIC:
                            if (writeCharacteristic(bleCommand.getServiceUuid(), bleCommand.getCharacteristicUuid(),
                                    bleCommand.getData())) {
                                commandSent = true;
                            }
                            break;
                        case READ_CHARACTERISTIC:
                            if (readCharacteristic(bleCommand.getServiceUuid(), bleCommand.getCharacteristicUuid())) {
                                commandSent = true;
                            }
                            break;
                        case DISCOVER_SERVICES:
                            if (mBluetoothGatt.discoverServices()) {
                                commandSent = true;
                            } else {
                                Log.e(TAG, "Failed to discover services!");
                            }
                            break;
                        case REQUEST_MTU:
                            if (mBluetoothGatt.requestMtu(ByteBuffer.wrap(bleCommand.getData()).getInt())) {
                                commandSent = true;
                            } else {
                                Log.e(TAG, "Failed to set MTU.");
                            }
                            break;
                        default:
                            Log.w(TAG, "Unknown Ble command, cannot process.");
                    }
                    if (commandSent) {
                        mHandler.postDelayed(resetOperationInProgress, BLE_COMMAND_TIMEOUT);
                    } else {
                        mHandler.post(resetOperationInProgress);
                    }
                }
            }
            mutex.release();
        } catch (InterruptedException e) {
            Log.e(TAG, "Mutex error", e);
        }
    }

    private Runnable resetOperationInProgress = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "Ble command failed to be sent OR timeout after " + BLE_COMMAND_TIMEOUT + "ms");
            // If current ble command timed out, process the next ble command.
            if (mBluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDING) {
                processNextBleCommand();
            }
        }
    };

    private void processNextBleCommand() {
        mHandler.removeCallbacks(resetOperationInProgress);
        mBleOperationInProgress = false;
        processBleCommandQueue();
    }

    private boolean writeDescriptor(final String serviceUuid, final String characteristicUuid) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceUuid, characteristicUuid);
        if (characteristic != null) {
            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    convertFromInteger(0x2902));
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                return true;
            } else {
                Log.w(TAG, "There's no such descriptor on characteristic: " + characteristicUuid);
            }
        }
        return false;
    }

    private boolean writeCharacteristic(final String serviceUuid, final String characteristicUuid, final byte[] value) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceUuid, characteristicUuid);
        if (characteristic != null) {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            Log.d(TAG, "<-<-<- Writing to characteristic: " + uuidToName.get(characteristicUuid)
                    + "  with data: " + bytesToHexString(value));
            mValueWritten = value;
            characteristic.setValue(value);
            if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
                mRWinProgress = false;
                Log.e(TAG, "Failed to write characteristic.");
            } else {
                mRWinProgress = true;
                return true;
            }
        }
        return false;
    }

    private BluetoothGattCharacteristic getCharacteristic(final String serviceUuid,
                                                          final String characteristicUuid) {
        BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
        if (service == null) {
            Log.w(TAG, "There's no such service found with uuid: " + serviceUuid);
            return null;
        }
        BluetoothGattCharacteristic characteristic =
                service.getCharacteristic(UUID.fromString(characteristicUuid));
        if (characteristic == null) {
            Log.w(TAG, "There's no such characteristic with uuid: " + characteristicUuid);
            return null;
        }
        return characteristic;
    }

    private boolean readCharacteristic(final String serviceUuid, final String characteristicUuid) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceUuid, characteristicUuid);
        if (characteristic != null) {
            Log.d(TAG, "<-<-<- Reading from characteristic: " + uuidToName.get(characteristicUuid));
            if (!mBluetoothGatt.readCharacteristic(characteristic)) {
                mRWinProgress = false;
                Log.e(TAG, "Failed to read characteristic.");
            } else {
                mRWinProgress = true;
                return true;
            }
        }
        return false;
    }

    private static void describeGattServices(List<BluetoothGattService> gattServices) {
        for (BluetoothGattService service : gattServices) {
            Log.d(TAG, "GattService: " + service.getUuid());
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                Log.d(TAG, " |-characteristics: " +
                        (uuidToName.containsKey(characteristic.getUuid().toString()) ?
                                uuidToName.get(characteristic.getUuid().toString()) : characteristic.getUuid()));
            }
        }
    }

    private static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

    private static String bytesToHexString(byte[] bytes) {

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        Formatter formatter = new Formatter(sb);
        for (int i = 0; i < bytes.length; i++) {
            formatter.format("%02x", bytes[i]);
            if (!VDBG && i > 10) {
                break;
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AmazonFreeRTOSDevice aDevice = (AmazonFreeRTOSDevice) obj;
        return Objects.equals(aDevice.mBluetoothDevice.getAddress(), mBluetoothDevice.getAddress());
    }
}
