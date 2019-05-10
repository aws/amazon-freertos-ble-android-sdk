# Amazon FreeRTOS BLE Mobile SDK for Android: Demo Application

## Introduction

This demo application demonstrates some of the features of the Android SDK for Amazon FreeRTOS Bluetooth devices.

For information about configuring and using the demo application, see [Amazon FreeRTOS BLE Mobile SDK Demo Application](https://docs.aws.amazon.com/freertos/latest/userguide/ble-demo.html#ble-sdk-app) on docs.aws.amazon.com.

## Usage

1. Replace the constants in DemoConstants.java and res/raw/awsconfiguration.json.
2. After you configure the demo app, you can build and install the demo. To build the app from the command line, use the following command:

    ```
    ./gradlew installDebug
    ```

    You can also build the app in Android Studio, after enabling "USB debugging" on your Android device.
  
3. Start the demo app. Click on the "Scan" button to scan for nearby BLE devices.
4. Toggle the switch to connect to one of them. "More..." menu will be enabled after connection is successful
5. Click on "More..." menu to do WiFi provisioning or to sign-in to use MQTT Proxy.

**Note** 


1. For WiFi provisioning, make sure you wait for the app to finish refreshing after each list/save/edit/delete operation.
2. For Mqtt Proxy, make sure you click on "Mqtt proxy" menu option after connection is established in order to login.
3. The current MTU size shows "N/A". This is a limitation of the current version of the app.

## License

This library is licensed under the Apache 2.0 License. 
