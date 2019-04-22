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

package com.amazon.aws.amazonfreertossdk;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.amazon.aws.amazonfreertossdk.deviceinfo.BrokerEndpoint;
import com.amazon.aws.amazonfreertossdk.deviceinfo.Mtu;
import com.amazon.aws.amazonfreertossdk.deviceinfo.Version;
import com.amazon.aws.amazonfreertossdk.mqttproxy.Connack;
import com.amazon.aws.amazonfreertossdk.mqttproxy.Connect;
import com.amazon.aws.amazonfreertossdk.mqttproxy.MqttProxyControl;
import com.amazon.aws.amazonfreertossdk.mqttproxy.MqttProxyMessage;
import com.amazon.aws.amazonfreertossdk.mqttproxy.Puback;
import com.amazon.aws.amazonfreertossdk.mqttproxy.Publish;
import com.amazon.aws.amazonfreertossdk.mqttproxy.Suback;
import com.amazon.aws.amazonfreertossdk.mqttproxy.Subscribe;
import com.amazon.aws.amazonfreertossdk.mqttproxy.Unsuback;
import com.amazon.aws.amazonfreertossdk.mqttproxy.Unsubscribe;
import com.amazon.aws.amazonfreertossdk.networkconfig.DeleteNetworkReq;
import com.amazon.aws.amazonfreertossdk.networkconfig.DeleteNetworkResp;
import com.amazon.aws.amazonfreertossdk.networkconfig.EditNetworkReq;
import com.amazon.aws.amazonfreertossdk.networkconfig.EditNetworkResp;
import com.amazon.aws.amazonfreertossdk.networkconfig.ListNetworkReq;
import com.amazon.aws.amazonfreertossdk.networkconfig.ListNetworkResp;
import com.amazon.aws.amazonfreertossdk.networkconfig.SaveNetworkReq;
import com.amazon.aws.amazonfreertossdk.networkconfig.SaveNetworkResp;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttMessageDeliveryCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.AMAZONFREERTOS_SDK_VERSION;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.BLE_COMMAND_TIMEOUT;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_CONNACK;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_CONNECT;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_DISCONNECT;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_PUBACK;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_PUBLISH;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_SUBACK;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_SUBSCRIBE;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_UNSUBACK;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_UNSUBSCRIBE;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_PROXY_CONTROL_OFF;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_PROXY_CONTROL_ON;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_DELETE_NETWORK_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_DEVICE_INFORMATION_SERVICE;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_DEVICE_MTU_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_DEVICE_VERSION_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_EDIT_NETWORK_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_IOT_ENDPOINT_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_LIST_NETWORK_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_MQTT_PROXY_CONTROL_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_MQTT_PROXY_RXLARGE_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_MQTT_PROXY_RX_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_MQTT_PROXY_SERVICE;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_MQTT_PROXY_TXLARGE_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_MQTT_PROXY_TX_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_NETWORK_SERVICE;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.UUID_SAVE_NETWORK_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.uuidToName;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.BleConnectionState;
import static com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants.MqttConnectionState;
import static com.amazon.aws.amazonfreertossdk.BleCommand.CommandType.DISCOVER_SERVICES;
import static com.amazon.aws.amazonfreertossdk.BleCommand.CommandType.READ_CHARACTERISTIC;
import static com.amazon.aws.amazonfreertossdk.BleCommand.CommandType.REQUEST_MTU;

public class AmazonFreeRTOSDevice {

    private static final String TAG = "AmazonFreeRTOSDevice";
    private Context mContext;

    @Getter
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private BleConnectionStatusCallback mBleConnectionStatusCallback;
    private NetworkConfigCallback mNetworkConfigCallback;
    private BleConnectionState mBleConnectionState = BleConnectionState.BLE_DISCONNECTED;
    private String mAmazonFreeRTOSLibVersion;
    private int mMtu = 0;

    private Queue<BleCommand> mBleCommandQueue = new LinkedList<>();
    private boolean mBleOperationInProgress = false;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private byte[] mValueWritten;
    private static Semaphore mutex = new Semaphore(1);

    //Buffer for receiving messages from device
    private ByteArrayOutputStream mTxLargeObject = new ByteArrayOutputStream();
    //Buffer for sending messages to device.
    private byte[] mRxLargeObject;
    private int mTotalPackets = 0;
    private int mPacketCount = 1;
    private int mMessageId = 0;
    private int mMaxPayloadLen = 0;

    private AWSIotMqttManager mIotMqttManager;
    private MqttConnectionState mMqttConnectionState = MqttConnectionState.MQTT_Disconnected;


    protected AmazonFreeRTOSDevice(@NonNull BluetoothDevice device, @NonNull Context context) {
        mContext = context;
        mBluetoothDevice = device;
    }

    /**
     * Connect to the BLE device, and notify the connection state via BleConnectionStatusCallback.
     * @param connectionStatusCallback The callback to notify app whether the BLE connection is
     *                                 successful. Must not be null.
     */
    protected void connect(@NonNull final BleConnectionStatusCallback connectionStatusCallback) {
        mBleConnectionStatusCallback = connectionStatusCallback;
        mHandlerThread = new HandlerThread("BleCommandHandler"); //TODO: unique thread name for each device?
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback, TRANSPORT_LE);
    }

    /**
     * Closing BLE connection, reset all variables, and disconnect from AWS IoT.
     */
    protected void disconnect() {
        // If ble connection is lost, clear any pending ble command.
        mBleCommandQueue.clear();
        mMessageId = 0;
        mMtu = 0;
        mTxLargeObject.reset();
        mRxLargeObject = null;
        mTotalPackets = 0;
        mPacketCount = 1;

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        // If ble connection is closed, there's no need to keep mqtt connection open.
        if (mMqttConnectionState != AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Disconnected) {
            disconnectFromIot();
        }
    }

    public void enableProxy() {
        enableMqttProxy(true);
    }

    public void disableProxy() {
        enableMqttProxy(false);
    }

    /**
     * Enable or disable MQTT proxy. It sends a BLE command to device to enable/disable MQTT proxy.
     * The request is sent asynchronously through BLE command. If enable is true, it enables MQTT
     * proxy. If enable is false, in addition to disable MQTT proxy, it also disconnects the MQTT
     * connection between the app and AWS IoT.
     * @param enable A boolean to indicate whether to enable or disable MQTT proxy.
     */
    private void enableMqttProxy(final boolean enable) {
        if (AmazonFreeRTOSManager.getClientKeyStore() == null && AmazonFreeRTOSManager.getCredentialProvider() == null) {
            Log.e(TAG, "Cannot enable/disable mqtt proxy because Iot credential is not set.");
            return;
        }
        Log.i(TAG, (enable ? "Enabling" : "Disabling") + " MQTT Proxy");

        MqttProxyControl mqttProxyControl = new MqttProxyControl();
        mqttProxyControl.proxyState = enable ? MQTT_PROXY_CONTROL_ON : MQTT_PROXY_CONTROL_OFF;
        byte[] mqttProxyControlBytes = mqttProxyControl.encode();
        if (mqttProxyControlBytes != null) {
            sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                    UUID_MQTT_PROXY_CONTROL_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE,
                    mqttProxyControlBytes));
        }
        if (!enable) {
            disconnectFromIot();
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
     * @param listNetworkReq The ListNetwork request
     * @param callback The callback which will be triggered once the BLE device sends a ListNetwork
     *                 response.
     */
    public void listNetworks(ListNetworkReq listNetworkReq, NetworkConfigCallback callback) {
        mNetworkConfigCallback = callback;
        byte[] listNetworkReqBytes = listNetworkReq.encode();
        if (listNetworkReqBytes != null) {
            sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                    UUID_LIST_NETWORK_CHARACTERISTIC, UUID_NETWORK_SERVICE, listNetworkReqBytes));
        }
    }

    /**
     * Sends a SaveNetworkReq command to the connected BLE device. The SaveNetworkReq contains the
     * network credential. A SaveNetworkResp will be sent by the BLE device and triggers the callback.
     * To get the updated order of all networks, call listNetworks again.
     * @param saveNetworkReq The SaveNetwork request.
     * @param callback The callback that is triggered once the BLE device sends a SaveNetwork response.
     */
    public void saveNetwork(SaveNetworkReq saveNetworkReq, NetworkConfigCallback callback) {
        mNetworkConfigCallback = callback;
        byte[] saveNetworkReqBytes = saveNetworkReq.encode();
        if (saveNetworkReqBytes != null) {
            sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                    UUID_SAVE_NETWORK_CHARACTERISTIC, UUID_NETWORK_SERVICE, saveNetworkReqBytes));
        }
    }

    /**
     * Sends an EditNetworkReq command to the connected BLE device. The EditNetwork request is used
     * to update the preference of a saved network. It contains the current index of the saved network
     * to be updated, and the desired new index of the save network to be updated to. Both the current
     * index and the new index must be one of those saved networks. Behavior is undefined if an index
     * of an unsaved network is provided in the EditNetworkReq.
     * To get the updated order of all networks, call listNetworks again.
     * @param editNetworkReq The EditNetwork request.
     * @param callback The callback that is triggered once the BLE device sends an EditNetwork response.
     */
    public void editNetwork(EditNetworkReq editNetworkReq, NetworkConfigCallback callback) {
        mNetworkConfigCallback = callback;
        byte[] editNetworkReqBytes = editNetworkReq.encode();
        if (editNetworkReqBytes != null) {
            sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                    UUID_EDIT_NETWORK_CHARACTERISTIC, UUID_NETWORK_SERVICE, editNetworkReqBytes));
        }
    }

    /**
     * Sends a DeleteNetworkReq command to the connected BLE device. The saved network with the index
     * specified in the delete network request will be deleted, making it a non-saved network again.
     * To get the updated order of all networks, call listNetworks again.
     * @param deleteNetworkReq The DeleteNetwork request.
     * @param callback The callback that is triggered once the BLE device sends a DeleteNetwork response.
     */
    public void deleteNetwork(DeleteNetworkReq deleteNetworkReq, NetworkConfigCallback callback) {
        mNetworkConfigCallback = callback;
        byte[] deleteNetworkReqBytes = deleteNetworkReq.encode();
        if (deleteNetworkReqBytes != null) {
            sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                    UUID_DELETE_NETWORK_CHARACTERISTIC, UUID_NETWORK_SERVICE, deleteNetworkReqBytes));
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
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        //intentAction = ACTION_GATT_CONNECTED;
                        mBleConnectionState = AmazonFreeRTOSConstants.BleConnectionState.BLE_CONNECTED;
                        //broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        discoverServices();
                        getDeviceVersion();
                        getMtu();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        //intentAction = ACTION_GATT_DISCONNECTED;
                        mBleConnectionState = AmazonFreeRTOSConstants.BleConnectionState.BLE_DISCONNECTED;
                        disconnect();
                        Log.i(TAG, "Disconnected from GATT server.");
                        mBleConnectionStatusCallback.onBleConnectionStatusChanged(mBleConnectionState);
                        //broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                        Log.i(TAG, "Discovered Ble gatt services successfully.");
                        describeGattServices(mBluetoothGatt.getServices());
                        sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_DESCRIPTOR,
                                UUID_MQTT_PROXY_TX_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE));
                        sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_DESCRIPTOR,
                                UUID_MQTT_PROXY_TXLARGE_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE));
                        sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_DESCRIPTOR,
                                UUID_LIST_NETWORK_CHARACTERISTIC, UUID_NETWORK_SERVICE));
                        sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_DESCRIPTOR,
                                UUID_SAVE_NETWORK_CHARACTERISTIC, UUID_NETWORK_SERVICE));
                        sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_DESCRIPTOR,
                                UUID_DELETE_NETWORK_CHARACTERISTIC, UUID_NETWORK_SERVICE));
                        sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_DESCRIPTOR,
                                UUID_EDIT_NETWORK_CHARACTERISTIC, UUID_NETWORK_SERVICE));
                    } else {
                        Log.e(TAG, "onServicesDiscovered received: " + status);
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

                    switch (characteristic.getUuid().toString()) {
                        case UUID_LIST_NETWORK_CHARACTERISTIC:
                            ListNetworkResp listNetworkResp = new ListNetworkResp();
                            if (listNetworkResp.decode(responseBytes) && mNetworkConfigCallback != null) {
                                Log.d(TAG, listNetworkResp.toString());
                                mNetworkConfigCallback.onListNetworkResponse(listNetworkResp);
                            }
                            break;
                        case UUID_SAVE_NETWORK_CHARACTERISTIC:
                            SaveNetworkResp saveNetworkResp = new SaveNetworkResp();
                            if (saveNetworkResp.decode(responseBytes) && mNetworkConfigCallback != null) {
                                mNetworkConfigCallback.onSaveNetworkResponse(saveNetworkResp);
                            }
                            break;
                        case UUID_EDIT_NETWORK_CHARACTERISTIC:
                            EditNetworkResp editNetworkResp = new EditNetworkResp();
                            if (editNetworkResp.decode(responseBytes) && mNetworkConfigCallback != null) {
                                mNetworkConfigCallback.onEditNetworkResponse(editNetworkResp);
                            }
                            break;
                        case UUID_DELETE_NETWORK_CHARACTERISTIC:
                            DeleteNetworkResp deleteNetworkResp = new DeleteNetworkResp();
                            if (deleteNetworkResp.decode(responseBytes) && mNetworkConfigCallback != null) {
                                mNetworkConfigCallback.onDeleteNetworkResponse(deleteNetworkResp);
                            }
                            break;
                        case UUID_MQTT_PROXY_CONTROL_CHARACTERISTIC:
                            Log.i(TAG, "MQTT proxy control characteristic "
                                    + characteristic.getStringValue(0));
                            break;
                        case UUID_MQTT_PROXY_TX_CHARACTERISTIC:
                            handleMqttTxMessage(responseBytes);
                            break;
                        case UUID_MQTT_PROXY_TXLARGE_CHARACTERISTIC:
                            try {
                                mTxLargeObject.write(responseBytes);
                                sendBleCommand(new BleCommand(READ_CHARACTERISTIC,
                                        UUID_MQTT_PROXY_TXLARGE_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE));
                            } catch (IOException e) {
                                Log.e(TAG, "Failed to concatenate byte array.", e);
                            }
                            break;
                        default:
                            Log.e(TAG, "Unknown characteristic " + characteristic.getUuid());
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
                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status)  {
                    Log.i(TAG, "onMTUChanged : " + mtu + " status: " + (status == 0 ? "Success" : status));
                    mMtu = mtu;
                    mMaxPayloadLen = mMtu - 3;
                    mMaxPayloadLen = mMaxPayloadLen > 0 ? mMaxPayloadLen : 0;
                    mBleConnectionStatusCallback.onBleConnectionStatusChanged(mBleConnectionState);
                    processNextBleCommand();
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Log.d(TAG, "->->-> onCharacteristicRead status: " + (status == 0 ? "Success" : status));
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        byte[] responseBytes = characteristic.getValue();
                        switch (characteristic.getUuid().toString()) {
                            case UUID_MQTT_PROXY_TXLARGE_CHARACTERISTIC:
                                try {
                                    mTxLargeObject.write(responseBytes);
                                    if (responseBytes.length < mMaxPayloadLen) {
                                        byte[] largeMessage = mTxLargeObject.toByteArray();
                                        Log.d(TAG, "Large object received from device successfully: "
                                                + bytesToHexString(largeMessage));
                                        handleMqttTxMessage(largeMessage);
                                        mTxLargeObject.reset();
                                    } else {
                                        sendBleCommand(new BleCommand(READ_CHARACTERISTIC,
                                                UUID_MQTT_PROXY_TXLARGE_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE));
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "Failed to concatenate byte array.", e);
                                }
                                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                                break;
                            case UUID_DEVICE_MTU_CHARACTERISTIC:
                                Mtu currentMtu = new Mtu();
                                currentMtu.mtu = new String(responseBytes);
                                Log.i(TAG, "Default MTU is set to: " + currentMtu.mtu);
                                try {
                                    mMtu = Integer.parseInt(currentMtu.mtu);
                                    setMtu(mMtu);
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Cannot parse default MTU value.");
                                }
                                break;
                            case UUID_IOT_ENDPOINT_CHARACTERISTIC:
                                BrokerEndpoint currentEndpoint = new BrokerEndpoint();
                                currentEndpoint.brokerEndpoint = new String(responseBytes);
                                Log.i(TAG, "Current broker endpoint is set to: "
                                        + currentEndpoint.brokerEndpoint);
                                break;
                            case UUID_DEVICE_VERSION_CHARACTERISTIC:
                                Version currentVersion = new Version();
                                currentVersion.version = new String(responseBytes);
                                mAmazonFreeRTOSLibVersion = currentVersion.version;
                                Log.i(TAG, "Ble software version on device is: " + currentVersion.version);
                                break;
                            default:
                                Log.w(TAG, "Unknown characteristic read. ");
                        }
                    }
                    processNextBleCommand();
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  int status) {
                    byte[] value = characteristic.getValue();
                    Log.d(TAG, "onCharacteristicWrite for: "
                            + uuidToName.get(characteristic.getUuid().toString())
                            + "; status: " + (status == 0 ? "Success" : status) + "; value: " + bytesToHexString(value));
                    if (Arrays.equals(mValueWritten, value)) {
                        processNextBleCommand();
                    } else {
                        Log.e(TAG, "values don't match!");
                    }
                }
            };


    /**
     * Handle MQTT related messages received from device.
     * @param message message received from device.
     */
    private void handleMqttTxMessage(byte[] message) {
        MqttProxyMessage mqttProxyMessage = new MqttProxyMessage();
        if (!mqttProxyMessage.decode(message)) {
            return;
        }
        Log.i(TAG, "Handling Mqtt Message type : " + mqttProxyMessage.type);
        switch (mqttProxyMessage.type) {
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
            default:
                Log.e(TAG, "Unknown mqtt message type: " + mqttProxyMessage.type);
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
        userMetaData.put("AmazonFreeRTOSSDK", "Android");
        userMetaData.put("AmazonFreeRTOSSDKVersion", AMAZONFREERTOS_SDK_VERSION);
        userMetaData.put("AmazonFreeRTOSLibVersion", mAmazonFreeRTOSLibVersion);
        mIotMqttManager.addUserMetaData(userMetaData);

        AWSIotMqttClientStatusCallback mqttClientStatusCallback = new AWSIotMqttClientStatusCallback() {
            @Override
            public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                Log.i(TAG, "mqtt connection status changed to: " + String.valueOf(status));
                switch (status) {
                    case Connected:
                        mMqttConnectionState = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connected;
                        //sending connack
                        if (mBleConnectionState == BleConnectionState.BLE_CONNECTED) {
                            sendConnAck();
                        } else {
                            Log.e(TAG, "Cannot send CONNACK because BLE connection is: " + mBleConnectionState);
                        }
                        break;
                    case Connecting:
                        mMqttConnectionState = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connecting;
                        break;
                    case ConnectionLost:
                        mMqttConnectionState = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Disconnected;
                        break;
                    case Reconnecting:
                        mMqttConnectionState = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connecting;
                        break;
                    default:
                        Log.e(TAG, "Unknown mqtt connection state: " + status);
                }
            }
        };

        if (AmazonFreeRTOSManager.getClientKeyStore() != null) {
            Log.i(TAG, "Connecting to IoT using KeyStore: " + connect.brokerEndpoint);
            mIotMqttManager.connect(AmazonFreeRTOSManager.getClientKeyStore(), mqttClientStatusCallback);
        } else {
            Log.i(TAG, "Connecting to IoT using AWS credential: " + connect.brokerEndpoint);
            mIotMqttManager.connect(AmazonFreeRTOSManager.getCredentialProvider(), mqttClientStatusCallback);
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
                        try {
                            String message = new String(data, "UTF-8");
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
                        } catch (UnsupportedEncodingException e) {
                            Log.e(TAG, "Message encoding error.", e);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Subscription error.", e);
            }
        }
    }

    private void sendConnAck() {
        Connack connack = new Connack();
        connack.type = MQTT_MSG_CONNACK;
        connack.status = AmazonFreeRTOSConstants.MqttConnectionState.MQTT_Connected.ordinal();
        byte[] connackBytes = connack.encode();
        if (connackBytes != null) {
            sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                    UUID_MQTT_PROXY_RX_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE,
                    connackBytes));
        }
    }

    private void sendSubAck(final Subscribe subscribe) {
        if (mBleConnectionState != BleConnectionState.BLE_CONNECTED) {
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
        if (subackBytes != null) {
            sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                    UUID_MQTT_PROXY_RX_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE, subackBytes));
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
            } catch(Exception e){
                Log.e(TAG, "Unsubscribe error.", e);
            }
        }
    }

    private void sendUnsubAck(final Unsubscribe unsubscribe) {
        if (mBleConnectionState != BleConnectionState.BLE_CONNECTED) {
            Log.e(TAG, "Cannot send Unsub ACK to BLE device because BLE connection state" +
                    " is not connected");
            return;
        }
        Log.i(TAG, "Sending Unsub ACK back to device.");
        Unsuback unsuback = new Unsuback();
        unsuback.type = MQTT_MSG_UNSUBACK;
        unsuback.msgID = unsubscribe.msgID;
        byte[] unsubackBytes = unsuback.encode();
        if (unsubackBytes != null) {
            sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                    UUID_MQTT_PROXY_RX_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE, unsubackBytes));
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

    private void sendPubAck(final Publish publish) {
        if (mBleConnectionState != BleConnectionState.BLE_CONNECTED) {
            Log.e(TAG, "Cannot send PUB ACK to BLE device because BLE connection state" +
                    " is not connected");
            return;
        }
        Log.i(TAG, "Sending PUB ACK back to device. MsgID: " + publish.getMsgID());
        Puback puback = new Puback();
        puback.type = MQTT_MSG_PUBACK;
        puback.msgID = publish.getMsgID();
        byte[] pubackBytes = puback.encode();
        if (pubackBytes != null) {
            sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                    UUID_MQTT_PROXY_RX_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE, pubackBytes));
        }
    }

    private void publishToDevice(final Publish publish) {
        if (mBleConnectionState != BleConnectionState.BLE_CONNECTED) {
            Log.e(TAG, "Cannot deliver mqtt message to BLE device because BLE connection state" +
                    " is not connected");
            return;
        }
        Log.d(TAG, "Sending received mqtt message back to device, topic: " + publish.getTopic()
                + " payload bytes: " + bytesToHexString(publish.getPayload())
                + " MsgID: " + publish.getMsgID());
        byte[] publishBytes = publish.encode();
        if (publishBytes != null) {
            if (publishBytes.length < mMaxPayloadLen) {
                sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                        UUID_MQTT_PROXY_RX_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE, publishBytes));
            } else {
                mTotalPackets = publishBytes.length / mMaxPayloadLen + 1;
                Log.i(TAG, "This message is larger than max payload size: " + mMaxPayloadLen
                        + ". Breaking down to " + mTotalPackets + " packets.");
                mPacketCount = 0; //reset packet count
                mRxLargeObject = Arrays.copyOf(publishBytes, publishBytes.length);
                while (mMaxPayloadLen * mPacketCount <= mRxLargeObject.length) {
                    byte[] packet = Arrays.copyOfRange(mRxLargeObject, mMaxPayloadLen * mPacketCount,
                            Math.min(mRxLargeObject.length, mMaxPayloadLen * mPacketCount + mMaxPayloadLen));
                    mPacketCount++;
                    Log.d(TAG, "Packet #" + mPacketCount + ": " + bytesToHexString(packet));
                    sendBleCommand(new BleCommand(BleCommand.CommandType.WRITE_CHARACTERISTIC,
                            UUID_MQTT_PROXY_RXLARGE_CHARACTERISTIC, UUID_MQTT_PROXY_SERVICE, packet));
                }
            }
        }
    }

    private void discoverServices() {
        if (mBleConnectionState == BleConnectionState.BLE_CONNECTED && mBluetoothGatt != null) {
            sendBleCommand(new BleCommand(DISCOVER_SERVICES));
        } else {
            Log.w(TAG, "Bluetooth connection state is not connected.");
        }
    }

    private void setMtu(int mtu) {
        if (mBleConnectionState == BleConnectionState.BLE_CONNECTED && mBluetoothGatt != null) {
            Log.i(TAG, "Setting mtu to: " + mtu);
            sendBleCommand(new BleCommand(REQUEST_MTU, mtu));
        } else {
            Log.w(TAG, "Bluetooth connection state is not connected.");
        }
    }

    private void getMtu() {
        if (mBleConnectionState == BleConnectionState.BLE_CONNECTED && mBluetoothGatt != null) {
            Log.d(TAG, "Getting current MTU.");
            sendBleCommand(new BleCommand(BleCommand.CommandType.READ_CHARACTERISTIC,
                    UUID_DEVICE_MTU_CHARACTERISTIC, UUID_DEVICE_INFORMATION_SERVICE));
        } else {
            Log.w(TAG, "Bluetooth connection state is not connected.");
        }
    }

    private void getBrokerEndpoint() {
        if (mBleConnectionState == BleConnectionState.BLE_CONNECTED && mBluetoothGatt != null) {
            Log.d(TAG, "Getting broker endpoint.");
            sendBleCommand(new BleCommand(BleCommand.CommandType.READ_CHARACTERISTIC,
                    UUID_IOT_ENDPOINT_CHARACTERISTIC, UUID_DEVICE_INFORMATION_SERVICE));
        } else {
            Log.w(TAG, "Bluetooth connection state is not connected.");
        }
    }

    private void getDeviceVersion() {
        if (mBleConnectionState == BleConnectionState.BLE_CONNECTED && mBluetoothGatt != null) {
            Log.d(TAG, "Getting ble software version on device.");
            sendBleCommand(new BleCommand(BleCommand.CommandType.READ_CHARACTERISTIC,
                    UUID_DEVICE_VERSION_CHARACTERISTIC, UUID_DEVICE_INFORMATION_SERVICE));
        } else {
            Log.w(TAG, "Bluetooth connection state is not connected.");
        }
    }

    private void sendBleCommand(final BleCommand command) {
        mBleCommandQueue.add(command);
        processBleCommandQueue();
    }

    private void processBleCommandQueue() {
        try {
            mutex.acquire();
            if (mBleOperationInProgress) {
                Log.d(TAG, "Ble operation is in progress. There are " + mBleCommandQueue.size()
                        + " Ble commands in the queue.");
            } else {
                BleCommand bleCommand = mBleCommandQueue.poll();
                if (bleCommand == null) {
                    Log.d(TAG, "There's no ble command in the queue.");
                    mBleOperationInProgress = false;
                } else {
                    mBleOperationInProgress = true;
                    Log.d(TAG, "Processing BLE command: " + bleCommand.getType()
                            + " queue size: " + mBleCommandQueue.size());
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
            Log.w(TAG, "Ble command failed to be sent OR timeout after " + BLE_COMMAND_TIMEOUT + "ms");
            // If current ble command timed out, process the next ble command.
            processNextBleCommand();
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
            if(!mBluetoothGatt.writeCharacteristic(characteristic)) {
                Log.e(TAG, "Failed to write characteristic.");
            } else {
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
                Log.e(TAG, "Failed to read characteristic.");
            } else {
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
                Log.d(TAG, " |-characteristics: " + characteristic.getUuid());
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
        for (int i =0; i< bytes.length; i++) {
            formatter.format("%02x", bytes[i]);
            if (i > 10) break;
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
