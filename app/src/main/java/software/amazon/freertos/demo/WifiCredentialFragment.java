package software.amazon.freertos.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class WifiCredentialFragment extends DialogFragment {
    public static final String EXTRA_WIFI_PW = "com.amazon.freertos.wifipassword";
    public static final String EXTRA_WIFI_BSSID = "com.amazon.freertos.wifibssid";
    private static final String SSID = "ssid";
    private static final String BSSID = "bssid";
    private String mBssid;

    public static WifiCredentialFragment newInstance(String ssid, String bssid) {
        Bundle args = new Bundle();
        args.putString(SSID, ssid);
        args.putString(BSSID, bssid);
        WifiCredentialFragment fragment = new WifiCredentialFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String ssid = getArguments().getString(SSID);
        mBssid = getArguments().getString(BSSID);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.wifi_credential_dialog, null);
        TextView ssidTextView = (TextView) view.findViewById(R.id.ssid_name);
        final EditText pwEditText = (EditText) view.findViewById(R.id.wifi_pw);
        ssidTextView.setText(ssid);
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.wifi_credential_dialog_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendResult(Activity.RESULT_OK, pwEditText.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private void sendResult(int resultCode, String result) {
        if (getTargetFragment() == null) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(EXTRA_WIFI_PW, result);
        intent.putExtra(EXTRA_WIFI_BSSID, mBssid);
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }
}
