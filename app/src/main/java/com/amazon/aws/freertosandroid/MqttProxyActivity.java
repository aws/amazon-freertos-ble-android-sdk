package com.amazon.aws.freertosandroid;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class MqttProxyActivity extends SingleFragmentActivity {
    public static final String EXTRA_DEVICE_MAC = "com.amazonaws.freertosandroid.device_bssid";

    @Override
    protected Fragment createFragment() {
        return MqttProxyFragment.newInstance();
    }

    public static Intent newIntent(Context packageContext, String macAddr) {
        Intent intent = new Intent(packageContext, MqttProxyActivity.class);
        intent.putExtra(EXTRA_DEVICE_MAC, macAddr);
        return intent;
    }
}