# ![image](https://user-images.githubusercontent.com/56273942/202568467-0ee721bb-1424-4efd-88fc-31b4f2a59dc6.png) DEPRECATED

## Announcement:
As of July 20th, 2023, this repository is deprecated. The contents of this repository will remain available but we will no longer provide updates or accept new contributions and pull requests. We recommend you start [here](https://docs.amplify.aws/start/q/integration/android/) for creating interactions between an Android device and AWS. For Bluetooth Low Energy APIs we recommend you start [here](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview).

Since the initial release of this package, several new Android version have release brining with them enhanced security and usability. AWS Amplify has also [release dozens of later versions](https://github.com/aws-amplify/aws-sdk-android/releases) of the [AWS SDK](https://github.com/aws-amplify/aws-sdk-android) with many new features and bug fixes.

The amazon-freertos-ble-android-sdk is being deprecated to remove an example built on outdated code.

Have more questions? Post them in the [FreeRTOS forum](https://forums.freertos.org/).

# FreeRTOS BLE Mobile SDK for Android
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/software.amazon.freertos/amazonfreertossdk/badge.svg?cacheSeconds=10)](https://maven-badges.herokuapp.com/maven-central/software.amazon.freertos/amazonfreertossdk/)
## Introduction

Using the Android SDK for FreeRTOS Bluetooth Devices, you can create mobile applications that do the following:

- Scan for and connect to nearby BLE devices running FreeRTOS

- Perform WiFi provisioning of the FreeRTOS BLE devices after connecting to them ( Only supported for Espressif ESP32-DevKitC )

- Act as a proxy for transmitting MQTT messages between a device running FreeRTOS and the AWS IoT cloud

## System requirements

- Android 6.0 (API level 23) or higher

- Bluetooth 4.2 or higher

- Android Studio

## Setting Up the SDK

1. Set the SDK as a dependency for the application.

**Option 1**: install from maven
In your app's `build.gradle` file, add the following into dependencies block:
(replace x.y.z with [![Maven Central](https://maven-badges.herokuapp.com/maven-central/software.amazon.freertos/amazonfreertossdk/badge.svg?cacheSeconds=30)](https://maven-badges.herokuapp.com/maven-central/software.amazon.freertos/amazonfreertossdk/))
```
    implementation('software.amazon.freertos:amazonfreertossdk:x.y.z')
```

**Option 2**: Build the sdk locally.
In your app's `build.gradle` file, add the following into dependencies block:
```
    implementation project(':amazonfreertossdk')
```
In project's `settings.gradle` file, add ':amazonfreertossdk'
```
    include ':app', ':amazonfreertossdk'
```

2. In your app's `AndroidManifest.xml` file, add following permissions:

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

3. Turn on phone's **Location Services**.

In addition to adding location permissions, this is required by android to perform a successful BLE scan.

## Contents

### BLE Helper Functions

The SDK includes some functions that help you perform BLE operations with FreeRTOS devices:

```
    startScanDevices
    stopScanDevices
    connectToDevice
    disconnectFromDevice
```

Once the connection to the device is established, you get an AmazonFreeRTOSDevice object, and you can
use this object to do WiFi provisioning or Mqtt proxy.

### WiFi Provisioning Service

Provision the WiFi credential on the FreeRTOS device through the app. It provides 4 functions:

```
    ListNetwork
    SaveNetwork
    EditNetwork
    DeleteNetwork
````

### MQTT Proxy Service

The MQTT proxy service controls the MQTT proxy. It allows the device to send and receive MQTT messages
from the AWS IoT cloud through the phone, when this feature is enabled.


You can find the [API documentation](https://aws.github.io/amazon-freertos-ble-android-sdk/) for these functions in the docs directory of this repository and [on github pages](https://aws.github.io/amazon-freertos-ble-android-sdk/).


## Demo Application

The SDK includes a demo application that demonstrates some of the main features of the SDK. You can find the demo in [app](app).

## License

This library is licensed under the Apache 2.0 License.
