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

import java.nio.ByteBuffer;

import lombok.Getter;

/**
 * This class defines the BLE command that is sent from SDK to device.
 */
@Getter
public class BleCommand {
    enum CommandType {
        WRITE_DESCRIPTOR,
        WRITE_CHARACTERISTIC,
        READ_CHARACTERISTIC,
        DISCOVER_SERVICES,
        REQUEST_MTU,
        NOTIFICATION
    }

    /**
     * The type of the BLE command.
     */
    private CommandType type;

    /**
     * The characteristic uuid of the BLE command.
     */
    private String characteristicUuid;

    /**
     * The service uuid of the BLE command.
     */
    private String serviceUuid;

    /**
     * The data to be sent with the BLE command.
     */
    private byte[] data;

    /**
     * Construct a BLE command with data.
     * @param t the BLE command type.
     * @param cUuid the characteristic uuid.
     * @param sUuid the service uuid.
     * @param d the data to be sent with the BLE command.
     */
    public BleCommand(CommandType t, String cUuid, String sUuid, byte[] d) {
        type = t;
        characteristicUuid = cUuid;
        serviceUuid = sUuid;
        data = d;
    }

    /**
     * Construct a BLE command without any data.
     * @param t the BLE command type.
     * @param cUuid the characteristic uuid.
     * @param sUuid the service uuid.
     */
    public BleCommand(CommandType t, String cUuid, String sUuid) {
        this(t, cUuid, sUuid, null);
    }

    public BleCommand(CommandType t) {
        this(t, null, null);
    }

    public BleCommand(CommandType t, int mtu) {
        this(t, null, null, ByteBuffer.allocate(4).putInt(mtu).array());
    }
}
