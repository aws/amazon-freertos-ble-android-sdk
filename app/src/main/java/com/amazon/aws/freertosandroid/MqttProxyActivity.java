package com.amazon.aws.freertosandroid;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class MqttProxyActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return MqttProxyFragment.newInstance();
    }

    public static Intent newIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, MqttProxyActivity.class);
        return intent;
    }
}