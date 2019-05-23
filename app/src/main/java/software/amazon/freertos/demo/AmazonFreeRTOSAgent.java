package software.amazon.freertos.demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import software.amazon.freertos.amazonfreertossdk.AmazonFreeRTOSManager;

/**
 * A wrapper class to get the instance of AmazonFreeRTOSManager.
 */
public class AmazonFreeRTOSAgent {
    private static AmazonFreeRTOSManager sAmazonFreeRTOSManager;
    private static BluetoothAdapter sBluetoothAdapter;
    private static final String TAG = "AmazonFreeRTOSAgent";

    /**
     * Return the instance of AmazonFreeRTOSManager. Initialize AmazonFreeRTOSManager if this is
     * called for the first time.
     * Bluetooth must be enabled before calling this method to get AmazonFreeRTOSManager.
     * @param context
     * @return The instance of AmazonFreeRTOSManager.
     */
    public static AmazonFreeRTOSManager getAmazonFreeRTOSManager(Context context) {
        if (sAmazonFreeRTOSManager == null) {
            BluetoothManager bluetoothManager
                    = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            sBluetoothAdapter = bluetoothManager.getAdapter();
            sAmazonFreeRTOSManager = new AmazonFreeRTOSManager(context, sBluetoothAdapter);
        }
        return sAmazonFreeRTOSManager;
    }
}
