package com.amazon.aws.freertosandroid;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class WifiProvisionActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new WifiProvisionFragment();
    }

    public static Intent newIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, WifiProvisionActivity.class);
        return intent;
    }
}
