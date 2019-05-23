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

import android.bluetooth.le.ScanResult;

/**
 * This is a callback to notify app of BLE Scan results.
 */
public abstract class BleScanResultCallback {
    /**
     * This method is called when a nearby BLE device is found during scanning.
     * @param result BLE ScanResult
     */
    public void onBleScanResult(ScanResult result){}

    public void onBleScanFailed(int errorcode) {}
}
