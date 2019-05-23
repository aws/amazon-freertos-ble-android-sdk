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

package software.amazon.freertos.amazonfreertossdk.networkconfig;

import android.util.Log;

import java.io.ByteArrayOutputStream;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

import static software.amazon.freertos.amazonfreertossdk.AmazonFreeRTOSConstants.SAVE_NETWORK_REQ;

/**
 * Save network request.
 */
public class SaveNetworkReq {
    /**
     * SSID of the network to be saved.
     */
    public String ssid;
    /**
     * BSSID of the network to be saved.
     */
    public byte[] bssid;
    /**
     * Password of the network to be saved.
     */
    public String psk;
    /**
     * Network security type.
     */
    public int security;
    /**
     * Current index of the network to be saved.
     */
    public int index;

    private static final String TAG = "SaveNetworkRequest";
    private static final String INDEX_KEY = "g";
    private static final String SSID_KEY = "r";
    private static final String BSSID_KEY = "b";
    private static final String PSK_KEY = "m";
    private static final String SECURITY_KEY = "q";
    private static final String TYPE_KEY = "w";

    public byte[] encode() {
        byte[] SaveNetworkRequestBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put(TYPE_KEY, SAVE_NETWORK_REQ)
                    .put(INDEX_KEY, index)
                    .put(SSID_KEY, ssid)
                    .put(BSSID_KEY, bssid)
                    .put(PSK_KEY, psk)
                    .put(SECURITY_KEY, security)
                    .end()
                    .build());
            SaveNetworkRequestBytes = baos.toByteArray();
        } catch (CborException e) {
            Log.e(TAG, "Failed to encode.", e);
        }
        return SaveNetworkRequestBytes;
    }
}
