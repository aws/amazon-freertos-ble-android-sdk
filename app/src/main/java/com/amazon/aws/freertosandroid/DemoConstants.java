package com.amazon.aws.freertosandroid;

import com.amazonaws.regions.Regions;

public class DemoConstants {
    /*
     * Replace with your AWS IoT policy name.
     */
    final static String AWS_IOT_POLICY_NAME = "Your AWS IoT policy name";
    /*
     * Replace with your AWS IoT region, eg: us-west-2.
     */
    final static String AWS_IOT_REGION = "us-west-2";
    /*
     * Replace with your Amazon Cognito Identity pool ID. Make sure this matches the pool id in
     * awsconfiguration.json file.
     */
    final static String COGNITO_POOL_ID = "us-west-2:xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
    /*
     * Replace with your Amazon Cognito region, eg: Regions.US_WEST_2.
     */
    final static Regions COGNITO_REGION = Regions.US_WEST_2;
    /*
     * Replace with the desired MTU value to set between the BLE device and the Android device.
     * Note: this is only required if you want to set MTU that is different than the original MTU
     * value on the BLE device. However, even after you set the MTU, the actual MTU value may be
     * smaller than what you set, because it is limited by the maximum MTU the devices can support.
     * Please refer to API documentation for AmazonFreeRTOSManager.class#setMtu.
     */
    static final int MTU = 512;
}
