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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;

/**
 * This class represents the MQTT PUBACK message.
 */
public class Puback {
    private static final String TAG = "MqttPuback";

    private static final String TYPE_KEY = "w";
    private static final String MSGID_KEY = "i";
    /**
     * MQTT message type.
     */
    public int type;
    /**
     * MQTT message ID.
     */
    public int msgID;

    public byte[] encode() {
        byte[] pubackBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(new CborBuilder()
                .addMap()
                .put(TYPE_KEY, type)
                .put(MSGID_KEY, msgID)
                .end()
                .build());
            pubackBytes = baos.toByteArray();
        } catch (CborException e) {
            Log.e(TAG, "Failed to encode.", e);
        }
        return pubackBytes;
    }

    public boolean decode(byte[] cborEncodedBytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(cborEncodedBytes);
        try {
            List<DataItem> dataItems = new CborDecoder(bais).decode();
            // process data item
            Map map = (Map) dataItems.get(0);
            DataItem dataItem = map.get(new UnicodeString(TYPE_KEY));
            type = ((UnsignedInteger) dataItem).getValue().intValue();
            dataItem = map.get(new UnicodeString(MSGID_KEY));
            msgID = ((Number) dataItem).getValue().intValue();
            return true;
        } catch (CborException e) {
            Log.e(TAG,"Failed to decode.", e);
            return false;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }
}
