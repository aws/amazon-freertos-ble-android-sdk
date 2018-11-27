# Amazon FreeRTOS BLE Mobile SDK for Android

## License

This library is licensed under the Apache 2.0 License. 

## Introduction

This SDK is used to communicate with the Amazon FreeRTOS Bluetooth Devices. It has the following functionality:

1. Scan and connect to a nearby BLE device running with Amazon FreeRTOS.
2. Act as a proxy for transmitting MQTT messages between device running with Amazon FreeRTOS and AWS IoT.

## System requirement

Android 8.0 (API level 26) or higher.

## Setting up

To connect to AWS IoT via MQTT, Cognito setup is required. Amazon Cognito provides authentication, authorization, and user management for your web and mobile apps. It allows the end user of the app to access the your AWS services such AWS IoT. (https://docs.aws.amazon.com/iot/latest/developerguide/protocols.html#mqtt-ws).

**Step 1 - Create AWS IoT Policy**

AWS IoT policies are used to authorize your device to perform AWS IoT operations, such as subscribing or publishing to MQTT topics.
If your are using the AWS Console, a step by step guide can be found here (https://docs.aws.amazon.com/iot/latest/developerguide/create-iot-policy.html).

> When adding the statements, switch to advanced mode, and paste in the sample policy JSON.
> Because we are using Cognito and not a device certificate, we don't need to attach the policy to device certificate, instead, we attach it to a Cognito identity using the AttachPrincipalPolicy API.

If your using the AWS API or SDK, please use the CreatePolicy API (https://docs.aws.amazon.com/iot/latest/apireference/API_CreatePolicy.html).

> policyDocument would be the sample policy JSON.

```
{
    "Version": "2012-10-17",
    "Statement": [
    {
        "Effect": "Allow",
        "Action": [
        "iot:Connect",
        "iot:Publish",
        "iot:Subscribe",
        "iot:Receive",
        "iot:GetThingShadow",
        "iot:UpdateThingShadow",
        "iot:DeleteThingShadow"
        ],
        "Resource": [
        "arn:aws:iot:us-east-1:123456789012:topicfilter/userid/deviceid/*"
        ]
    }
    ]
}
```
**Step 2 - Create Federated Identity Pool**

Create an identity pool that can be attached to the IoT Policy, Create an authenticated role (or unauthenticated role if needed, step 3 of the guide) and add the policies below. A step by step guide can be found here (https://docs.aws.amazon.com/cognito/latest/developerguide/getting-started-with-identity-pools.html).

**Step 3 - Create Cognito IAM Role Permissions Policies**

IAM > Roles > Cognito auth (or unauthenticated if supported) role > Permissions > Permissions policies. We need to additionally allow AttachPrincipalPolicy so that we can attach the Cognito Identity to the AWS IoT Policy.


```
{
    "Version": "2012-10-17",
    "Statement": [
    {
        "Effect": "Allow",
        "Action": [
        "iot:AttachPrincipalPolicy",
        "iot:Connect",
        "iot:Publish",
        "iot:Subscribe",
        "iot:Receive",
        "iot:GetThingShadow",
        "iot:UpdateThingShadow",
        "iot:DeleteThingShadow"
        ],
        "Resource": [
        "*"
        ]
    }
    ]
}
```

## SDK
The API documentation can be found under _**documentation**_ folder.

The SDK provides following functionality:
- **BLE helper**: These are ble helper methods for you to perform ble operations with the Amazon FreeRTOS Devices.
```
startScanBleDevices(final BleScanResultCallback scanResultCallback)
stopScanBleDevices()
connectToDevice(final BluetoothDevice bluetoothDevice, final BleConnectionStatusCallback connectionStatusCallback)
discoverServices()
close()
```
- **Device info service**: This service provides basic device related info.
```
setMtu(int mtu)
getMtu(DeviceInfoCallback callback)
getBrokerEndpoint(DeviceInfoCallback callback)
getDeviceVersion(DeviceInfoCallback callback)
```
- **MQTT proxy service**: This service provides control for the MQTT proxy.
```
enableMqttProxy(final boolean enable)
disconnectFromIot()
```

## Usage

Import _**amazonfreertossdk**_ into your app project in Android studio.
In your app gradle file, add following into dependencies:

```
dependencies {
    implementation project(":amazonfreertossdk")
}
```

In your app AndroidManifest.xml file, add following permissions:

```
<uses-permission android:name="android.permission.BLUETOOTH"/>
    <!-- initiate device discovery and manipulate bluetooth settings -->
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <!-- allow scan BLE -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <!-- AWS Mobile SDK -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```


## Sample App
There is a sample app under the folder _**app**_ to show how to use the Amazon FreeRTOS BLE Mobile SDK for Android. The sample app demostrates how to connect to a BLE enabled AmazonFreeRTOS device and act as a proxy for MQTT messages between the device and AWS IoT.

The sample app consists of two activities. _**AuthenticatorActivity**_ is a sign-in page, which shows an example of how to authenticate app users. _**MainActivity**_ shows how to scan for AmazonFreeRTOS device and enable the MQTT proxy.

Before building the app,
1. In AuthenticatorActivity.java, replace the following variables: AWS_IOT_POLICY_NAME, AWS_IOT_REGION, COGNITO_POOL_ID, COGNITO_REGION.
2. In MainActivity.java, replace the following variables: BLE_DEVICE_MAC_ADDR, BLE_DEVICE_NAME, MTU.
3. In res/raw/awsconfiguration.json, replace your Cognito Identity Pool Id and User Pool Id.

You may either build and install the sample using command line or using Android Studio after enabling "USB debugging" on your Android device. To use command line:
```
./gradlew installDebug
```

The sample app only connects to the BLE device if its MAC address or device name matches the one defined as in _**BLE_DEVICE_MAC_ADDR**_ and _**BLE_DEVICE_NAME**_ .

In the MainActivity, there are 5 steps, each represented by either a button or switch.

**[Step 1] Scan**: Click this button to start scanning for nearby AmazonFreeRTOS devices. If it finds a matching device, the [Step 2] switch should be enabled. The length of the scan period is defined in the Amazon FreeRTOS BLE Mobile SDK for Android.

**[Step 2] Connect**: Toggle this switch to connect to the found device. If the connection is successful, the buttons and switches for the remaining steps should be enabled.

**[Step 3] Discover**: Click this button to discover all the services and characteristics supported on the found device.

**[Step 4] Set MTU**: Click this button to set the desired MTU between the device and the app. Note that the actual MTU value is limited by the maximum supported MTU value on the AmazonFreeRTOS device and the Android device.

**[Step 5] Mqtt proxy**: Toggle this switch to enable/disable MQTT proxy. If enabled, the app should now behave as a proxy for MQTT messages transmitted between the AmazonFreeRTOS device and AWS IoT.

**Sign out**: Click this button to sign out of the app. User will then need to sign in again on the Sign-in page.
