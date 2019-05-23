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

package software.amazon.freertos.amazonfreertossdk.deviceinfo;

/**
 * This class represents the broker endpoint object that is transferred between ble device and SDK.
 * When SDK sends a read characteristic command to the ble device, this class object is returned in
 * the response back to SDK.
 * The broker endpoint is the AWS IoT endpoint from AWS IOT Console.
 */
public class BrokerEndpoint {
    public String brokerEndpoint;
}
