package software.amazon.freertos.demo;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class WifiProvisionActivity extends SingleFragmentActivity {
    public static final String EXTRA_DEVICE_MAC = "com.amazonaws.freertosandroid.device_mac";
    @Override
    protected Fragment createFragment() {
        return new WifiProvisionFragment();
    }

    public static Intent newIntent(Context packageContext, String macAddr) {
        Intent intent = new Intent(packageContext, WifiProvisionActivity.class);
        intent.putExtra(EXTRA_DEVICE_MAC, macAddr);
        return intent;
    }
}
