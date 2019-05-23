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

/**
 * This is a callback class to notify the app of device information, including mtu, broker endpoint
 * and device software version.
 */
public abstract class DeviceInfoCallback {

    /**
     * This callback is triggered when device sends the current mtu number in response to getMtu.
     * @param mtu the current mtu value negotiated between device and Android phone.
     */
    public void onObtainMtu(int mtu){}
    /**
     * This callback is triggered when device sends its MQTT broker endpoint in response to
     * getBrokerEndpoint.
     * @param endpoint The current MQTT broker endpoint set on the device.
     */
    public void onObtainBrokerEndpoint(String endpoint){}
    /**
     * This callback is triggered when device sends its current software version in response to
     * getDeviceVersion.
     * @param version The current device library version on the device.
     */
    public void onObtainDeviceSoftwareVersion(String version){}

    public void onError(AmazonFreeRTOSConstants.AmazonFreeRTOSError Error) {}
}
