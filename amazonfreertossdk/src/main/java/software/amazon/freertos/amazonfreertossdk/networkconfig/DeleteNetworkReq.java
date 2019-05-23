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

import static software.amazon.freertos.amazonfreertossdk.AmazonFreeRTOSConstants.DELETE_NETWORK_REQ;

/**
 * Delete network request.
 */
public class DeleteNetworkReq {
    /**
     * The index of the saved network to be deleted.
     */
    public int index;

    private static final String TAG = "DeleteNetworkRequest";
    private static final String INDEX_KEY = "g";
    private static final String TYPE_KEY = "w";

    public byte[] encode() {
        byte[] DeleteNetworkRequestBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put(TYPE_KEY, DELETE_NETWORK_REQ)
                    .put(INDEX_KEY, index)
                    .end()
                    .build());
            DeleteNetworkRequestBytes = baos.toByteArray();
        } catch (CborException e) {
            Log.e(TAG, "Failed to encode.", e);
        }
        return DeleteNetworkRequestBytes;
    }
}
