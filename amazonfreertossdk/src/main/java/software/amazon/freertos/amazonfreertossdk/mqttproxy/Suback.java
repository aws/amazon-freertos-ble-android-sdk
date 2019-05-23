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

package software.amazon.freertos.amazonfreertossdk.mqttproxy;

import android.util.Log;

import java.io.ByteArrayOutputStream;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

/**
 * This class represents the MQTT SUBACK message.
 */
public class Suback {
    private static final String TAG = "MqttSuback";
    private static final String TYPE_KEY = "w";
    private static final String MSGID_KEY = "i";
    private static final String STATUS_KEY = "s";
    /**
     * MQTT message type.
     */
    public int type;
    /**
     * MQTT message ID.
     */
    public int msgID;
    /**
     * MQTT SUBACK status. This is set to the QOS number in the corresponding MQTT SUBSCRIBE
     * message, to which this SUBACK is acknowledging.
     */
    public int status;

    public byte[] encode() {
        byte[] subackBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(new CborBuilder()
                .addMap()
                .put(TYPE_KEY, type)
                .put(MSGID_KEY, msgID)
                .put(STATUS_KEY, status)
                .end()
                .build());
            subackBytes = baos.toByteArray();
        } catch (CborException e) {
            Log.e(TAG, "Failed to encode.", e);
        }
        return subackBytes;
    }
}
