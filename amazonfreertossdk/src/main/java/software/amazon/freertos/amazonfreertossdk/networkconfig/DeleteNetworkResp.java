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

import java.io.ByteArrayInputStream;
import java.util.List;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;

/**
 * Delete network response
 */
public class DeleteNetworkResp {
    /**
     * Status of the operation. 0 for success.
     */
    int status;
    public String toString() {
        return String.format("DeleteNetworkResponse ->\n status: %d", status);
    }

    private static final String TAG = "DeleteNetworkResponse";
    private static final String STATUS_KEY = "s";

    public boolean decode(byte[] cborEncodedBytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(cborEncodedBytes);
        try {
            List<DataItem> dataItems = new CborDecoder(bais).decode();
            // process data item
            Map map = (Map) dataItems.get(0);
            DataItem dataItem = map.get(new UnicodeString(STATUS_KEY));
            status = ((UnsignedInteger) dataItem).getValue().intValue();
            return true;
        } catch (CborException e) {
            Log.e(TAG,"Failed to decode.", e);
            return false;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }
}
