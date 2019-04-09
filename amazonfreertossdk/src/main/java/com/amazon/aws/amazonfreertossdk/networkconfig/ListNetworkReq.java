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

package com.amazon.aws.amazonfreertossdk.networkconfig;

import android.util.Log;

import java.io.ByteArrayOutputStream;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

/**
 * List network request
 */
public class ListNetworkReq {
    /**
     * Maximum total number of networks to return.
     */
    public int maxNetworks;
    /**
     * Time in seconds for BLE device to scan available networks.
     */
    public int timeout;

    private static final String TAG = "ListNetworkRequest";
    private static final String MAXNETWORKS_KEY = "h";
    private static final String TIMEOUT_KEY = "t";

    public byte[] encode() {
        byte[] ListNetworkRequestBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put(MAXNETWORKS_KEY, maxNetworks)
                    .put(TIMEOUT_KEY, timeout)
                    .end()
                    .build());
            ListNetworkRequestBytes = baos.toByteArray();
        } catch (CborException e) {
            Log.e(TAG, "Failed to encode.", e);
        }
        return ListNetworkRequestBytes;
    }
}
