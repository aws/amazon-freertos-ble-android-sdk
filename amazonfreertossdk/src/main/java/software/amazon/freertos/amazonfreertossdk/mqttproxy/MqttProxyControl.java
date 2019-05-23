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
 * This class represents the MQTT proxy state. SDK sends this object to device to switch on/off
 * MQTT proxy.
 */
public class MqttProxyControl {
    private static final String TAG = "MqttProxyControl";
    private static final String PROXYSTATE_KEY = "l";
    /**
     * The state of MQTT proxy.
     */
    public int proxyState;

    public byte[] encode() {
        byte[] mqttProxyControlBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put(PROXYSTATE_KEY, proxyState)
                    .end()
                    .build());
            mqttProxyControlBytes = baos.toByteArray();
        } catch (CborException e) {
            Log.e(TAG, "Failed to encode.", e);
        }
        return mqttProxyControlBytes;
    }
}
