# Amazon FreeRTOS BLE Mobile SDK for Android

## Introduction

Using the Android SDK for Amazon FreeRTOS Bluetooth Devices, you can create mobile applications that do the following:

- Scan for and connect to nearby BLE devices running Amazon FreeRTOS

- Act as a proxy for transmitting MQTT messages between a device running Amazon FreeRTOS and the AWS IoT cloud

## System requirements

- Android 6.0 (API level 23) or higher

- Bluetooth 4.2 or higher

- Android Studio

## Setting Up the SDK

**To install the Android SDK for Amazon FreeRTOS Bluetooth Devices**

1. Import [amazonfreertossdk](amazonfreertossdk) into your app project in Android Studio.

2. In your app's `gradle` file, add the following dependencies:

```
dependencies {
    implementation project(":amazonfreertossdk")
}
```

3. In your app's `AndroidManifest.xml` file, add following permissions:

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

## Contents

All main APIs are defined in [AmazonFreeRTOSManager.java](amazonfreertossdk/src/main/java/com/amazon/aws/amazonfreertossdk/AmazonFreeRTOSManager.java). These functions include:

### BLE Helper Functions

The SDK includes some functions that help you perform BLE operations with Amazon FreeRTOS devices:

    ```
    startScanDevices
    stopScanDevices
    connectToDevice
    disconnectFromDevice
    ```

Once the connection to the device is established, you get an AmazonFreeRTOSDevice object, and you can
use this object to do WiFi provisioning or Mqtt proxy.

### WiFi Provisioning Service

Provision the WiFi credential on the Amazon FreeRTOS device through the app. It provides 4 functions:

```
ListNetwork
SaveNetwork
EditNetwork
DeleteNetwork
````

### MQTT Proxy Service 

The MQTT proxy service controls the MQTT proxy. It allows the device to send and receive MQTT messages
from the AWS IoT cloud through the phone, when this feature is enabled.


You can find the documentation for these functions in [documentation](documentation).


## Demo Application

The SDK includes a demo application that demonstrates some of the main features of the SDK. You can find the demo in [app](app).

## License

This library is licensed under the Apache 2.0 License. 
