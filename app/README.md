# FreeRTOS BLE Mobile SDK for Android: Demo Application

## Introduction

This demo application demonstrates some of the features of the Android SDK for FreeRTOS Bluetooth devices.

For information about configuring and using the demo application, see [FreeRTOS BLE Mobile SDK Demo Application](https://docs.aws.amazon.com/freertos/latest/userguide/ble-demo.html#ble-sdk-app).

## Usage

If you don't need the mqtt proxy feature, set boolean `mqttOn` to `false` in `DeviceScanFragment.java`, then skip to step 2.
1. Replace the constants in `DemoConstants.java` and `res/raw/awsconfiguration.json`.
2. After you configure the demo app, you can build and install the demo. To build the app from the command line, use the following command:

    ```
    ./gradlew installDebug
    ```

    You can also build the app in Android Studio, after enabling "USB debugging" on your Android device.
  
3. Start the demo app. Register account and sign-in if sign-in page appears.
4. Click on the "Scan" button to scan for nearby BLE devices.
5. Toggle the switch to connect to one of them. "More..." menu will be enabled after connection is successful
6. Click on the "More..." menu for WiFi provisioning.

**Note** 


1. When WiFi provisioning, make sure that you wait for the app to finish refreshing after each network list/save/edit/delete operation.
2. The current MTU size shows "N/A". This is a limitation of the current version of the app.

## License

This library is licensed under the Apache 2.0 License. 
