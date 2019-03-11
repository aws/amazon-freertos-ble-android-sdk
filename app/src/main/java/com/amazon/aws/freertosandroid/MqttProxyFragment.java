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

import com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSManager;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.iot.AWSIotCertificateException;

public class MqttProxyFragment extends Fragment {
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    private static final boolean USING_KEYSTORE = false;

    private static final String TAG = "MqttProxyFragment";
    private Switch mqttProxySwitch;
    private AmazonFreeRTOSManager mAmazonFreeRTOSManager;
    public static MqttProxyFragment newInstance() {
        MqttProxyFragment fragment = new MqttProxyFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAmazonFreeRTOSManager = AmazonFreeRTOSAgent.getAmazonFreeRTOSManager(getActivity());
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
                if (isChecked) {
                    mAmazonFreeRTOSManager.enableMqttProxy(true);
                } else {
                    mAmazonFreeRTOSManager.enableMqttProxy(false);
                }
            }
        });
        mqttProxySwitch.setEnabled(false);

        if (USING_KEYSTORE) {
            Log.d(TAG, "Using KeyStore");
            try {
                mAmazonFreeRTOSManager.setKeyStore(CERTIFICATE_ID,
                        getResources().openRawResource(R.raw.replace_with_keystore), KEYSTORE_PASSWORD);
                mqttProxySwitch.setEnabled(true);
            } catch (AWSIotCertificateException e) {
                Log.e(TAG, "Failed to load KeyStore, cannot enable mqtt proxy.", e);
            }
        } else {
            AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance();
            Log.d(TAG, "Using aws credential");
            mAmazonFreeRTOSManager.setCredentialProvider(credentialsProvider);
            mqttProxySwitch.setEnabled(true);
        }
        return v;
    }

}
