# Amazon FreeRTOS BLE Mobile SDK for Android: Demo Application

## Introduction

This demo application demonstrates some of the features of the Android SDK for Amazon FreeRTOS Bluetooth devices.

For information about configuring and using the demo application, see [Amazon FreeRTOS BLE Mobile SDK Demo Application](https://docs.aws.amazon.com/freertos/latest/userguide/ble-demo.html#ble-sdk-app) on docs.aws.amazon.com.

## Usage

After you configure the demo app, you can build and install the demo. To build the app from the command line, use the following command:

```
./gradlew installDebug
```

You can also build the app in Android Studio, after enabling "USB debugging" on your Android device.

**Note** 

The demo app only connects to the BLE device if its MAC address or device name matches the one defined as in _**BLE_DEVICE_MAC_ADDR**_ and _**BLE_DEVICE_NAME**_ .

[MainActivity](app/src/main/java/com/amazon/aws/freertosandroid/MainActivity.java) defines the following buttons and switches for the application:

**Scan**

Press this button to start scanning for nearby Amazon FreeRTOS devices. If the app finds a matching device, the **Connect** switch is enabled.

**Connect**

Toggle this switch to connect to the found device. If the connection is successful, **Discover**, **Set MTU**, **MQTT proxy**, and **Sign out** are enabled.

**Discover**

Press this button to discover all the services and characteristics supported on the found device.

**Set MTU**

Press this button to set the desired MTU between the device and the app. Note that the actual MTU value is limited by the maximum supported MTU value on the Amazon FreeRTOS device and the Android device.

**MQTT proxy**

Toggle this switch to enable/disable MQTT proxy. If enabled, the app behaves as a proxy for MQTT messages transmitted between the Amazon FreeRTOS device and AWS IoT.

**Sign out**

Press this button to sign out of the app. Users can sign in again on the Sign-in page.


## License

This library is licensed under the Apache 2.0 License. 
