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

import java.util.HashMap;
import java.util.Map;

/**
 * This class defines some constants used in the SDK.
 */
public class AmazonFreeRTOSConstants {

    /**
     * Network security types.
     */
    public static final int NETWORK_SECURITY_TYPE_OPEN = 0;
    public static final int NETWORK_SECURITY_TYPE_WEP = 1;
    public static final int NETWORK_SECURITY_TYPE_WPA = 2;
    public static final int NETWORK_SECURITY_TYPE_WPA2 = 3;
    public static final int NETWORK_SECURITY_TYPE_NOT_SUPPORTED = 4;

    /**
     * MQTT proxy state.
     */
    public static final int MQTT_PROXY_CONTROL_OFF = 0;
    public static final int MQTT_PROXY_CONTROL_ON = 1;

    /**
     * message type.
     */
    public static final int MQTT_MSG_CONNECT = 1;
    public static final int MQTT_MSG_CONNACK = 2;
    public static final int MQTT_MSG_PUBLISH = 3;
    public static final int MQTT_MSG_PUBACK = 4;
    public static final int MQTT_MSG_PUBREC = 5;
    public static final int MQTT_MSG_PUBREL = 6;
    public static final int MQTT_MSG_PUBCOMP = 7;
    public static final int MQTT_MSG_SUBSCRIBE = 8;
    public static final int MQTT_MSG_SUBACK = 9;
    public static final int MQTT_MSG_UNSUBSCRIBE = 10;
    public static final int MQTT_MSG_UNSUBACK = 11;
    public static final int MQTT_MSG_PINGREQ = 12;
    public static final int MQTT_MSG_PINGRESP = 13;
    public static final int MQTT_MSG_DISCONNECT = 14;

    public static final int LIST_NETWORK_REQ = 1;
    public static final int LIST_NETWORK_RESP = 2;
    public static final int SAVE_NETWORK_REQ = 3;
    public static final int SAVE_NETWORK_RESP = 4;
    public static final int EDIT_NETWORK_REQ = 5;
    public static final int EDIT_NETWORK_RESP = 6;
    public static final int DELETE_NETWORK_REQ = 7;
    public static final int DELETE_NETWORK_RESP = 8;

    /**
     * Bluetooth connection state. This is matching with BluetoothProfile in the Android SDK.
     */
    public enum BleConnectionState {
        BLE_DISCONNECTED,  // = 0
        BLE_CONNECTING,    // = 1
        BLE_CONNECTED,     // = 2
        BLE_DISCONNECTING, // = 3
        BLE_INITIALIZED,   // = 4
        BLE_INITIALIZING   // = 5
    }

    public enum AmazonFreeRTOSError {
        BLE_DISCONNECTED_ERROR
    }

    /**
     * The MQTT connection state.
     * Do not change the order of this enum. This is a contract between device library and our sdk.
     */
    public enum MqttConnectionState {
        MQTT_Unknown,
        MQTT_Connecting,
        MQTT_Connected,
        MQTT_Disconnected,
        MQTT_ConnectionRefused,
        MQTT_ConnectionError,
        MQTT_ProtocolError
    }

    /**
     * This defines how much time the SDK scans for nearby BLE devices.
     */
    public static final long SCAN_PERIOD = 20000; //ms

    /**
     * After sending BLE commands to device, the SDK will wait for this amount of time, after which
     * it will time out and continue to process the next BLE command.
     */
    public static final int BLE_COMMAND_TIMEOUT = 3000; //ms

    public static final String UUID_AmazonFreeRTOS = "8a7f1168-48af-4efb-83b5-e679f932ff00";

    public static final String UUID_NETWORK_SERVICE = "a9d7166a-d72e-40a9-a002-48044cc30100";
    public static final String UUID_NETWORK_CONTROL = "a9d7166a-d72e-40a9-a002-48044cc30101";
    public static final String UUID_NETWORK_TX = "a9d7166a-d72e-40a9-a002-48044cc30102";
    public static final String UUID_NETWORK_RX = "a9d7166a-d72e-40a9-a002-48044cc30103";
    public static final String UUID_NETWORK_TXLARGE = "a9d7166a-d72e-40a9-a002-48044cc30104";
    public static final String UUID_NETWORK_RXLARGE = "a9d7166a-d72e-40a9-a002-48044cc30105";

    public static final String UUID_MQTT_PROXY_SERVICE = "a9d7166a-d72e-40a9-a002-48044cc30000";
    public static final String UUID_MQTT_PROXY_CONTROL = "a9d7166a-d72e-40a9-a002-48044cc30001";
    public static final String UUID_MQTT_PROXY_TX = "a9d7166a-d72e-40a9-a002-48044cc30002";
    public static final String UUID_MQTT_PROXY_RX = "a9d7166a-d72e-40a9-a002-48044cc30003";
    public static final String UUID_MQTT_PROXY_TXLARGE = "a9d7166a-d72e-40a9-a002-48044cc30004";
    public static final String UUID_MQTT_PROXY_RXLARGE = "a9d7166a-d72e-40a9-a002-48044cc30005";

    public static final String UUID_DEVICE_INFORMATION_SERVICE = "8a7f1168-48af-4efb-83b5-e679f932ff00";
    public static final String UUID_DEVICE_VERSION = "8a7f1168-48af-4efb-83b5-e679f932ff01";
    public static final String UUID_IOT_ENDPOINT = "8a7f1168-48af-4efb-83b5-e679f932ff02";
    public static final String UUID_DEVICE_MTU = "8a7f1168-48af-4efb-83b5-e679f932ff03";
    public static final String UUID_DEVICE_PLATFORM = "8a7f1168-48af-4efb-83b5-e679f932ff04";
    public static final String UUID_DEVICE_ID = "8a7f1168-48af-4efb-83b5-e679f932ff05";

    public static final Map<String, String> uuidToName = new HashMap<String, String>() {
        {
            put(UUID_NETWORK_CONTROL, "NETWORK_CONTROL");
            put(UUID_NETWORK_TX, "NETWORK_TX");
            put(UUID_NETWORK_RX, "NETWORK_RX");
            put(UUID_NETWORK_TXLARGE, "NETWORK_TXLARGE");
            put(UUID_NETWORK_RXLARGE, "NETWORK_RXLARGE");
            put(UUID_MQTT_PROXY_CONTROL, "MQTT_CONTROL");
            put(UUID_MQTT_PROXY_TX, "MQTT_TX");
            put(UUID_MQTT_PROXY_TXLARGE, "MQTT_TXLARGE");
            put(UUID_MQTT_PROXY_RX, "MQTT_RX");
            put(UUID_MQTT_PROXY_RXLARGE, "MQTT_RXLARGE");
            put(UUID_DEVICE_VERSION, "DEVICE_VERSION");
            put(UUID_IOT_ENDPOINT, "IOT_ENDPOINT");
            put(UUID_DEVICE_MTU, "DEVICE_MTU");
            put(UUID_DEVICE_PLATFORM, "DEVICE_PLATFORM");
            put(UUID_DEVICE_ID, "DEVICE_ID");
        }
    };

}
