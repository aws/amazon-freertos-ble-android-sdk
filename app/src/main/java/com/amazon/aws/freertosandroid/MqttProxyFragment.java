package com.amazon.aws.freertosandroid;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSDevice;
import com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSManager;

public class MqttProxyFragment extends Fragment {
    private static final String TAG = "MqttProxyFragment";
    private Switch mqttProxySwitch;
    private AmazonFreeRTOSManager mAmazonFreeRTOSManager;

    private String mDeviceMac;
    private AmazonFreeRTOSDevice mDevice;
    public static MqttProxyFragment newInstance() {
        MqttProxyFragment fragment = new MqttProxyFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDeviceMac = getActivity().getIntent().getStringExtra(MqttProxyActivity.EXTRA_DEVICE_MAC);
        mAmazonFreeRTOSManager = AmazonFreeRTOSAgent.getAmazonFreeRTOSManager(getActivity());
        mDevice = mAmazonFreeRTOSManager.getConnectedDevice(mDeviceMac);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_mqtt_proxy, container, false);

        mqttProxySwitch = (Switch) v.findViewById(R.id.proxyControlSwitch);
        mqttProxySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                Log.i(TAG, "mqtt proxy switch isChecked: " + (isChecked ? "ON":"OFF"));
                if (mDevice != null) {
                    if (isChecked) {
                        //mDevice.enableProxy();
                    } else {
                        //mDevice.disableProxy();
                    }
                } else {
                    Log.e(TAG, "There's no connected device: " + mDeviceMac);
                }
            }
        });
        mqttProxySwitch.setEnabled(true);
        return v;
    }

}
