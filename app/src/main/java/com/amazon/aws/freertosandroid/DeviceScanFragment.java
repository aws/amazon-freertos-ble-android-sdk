package com.amazon.aws.freertosandroid;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;

import com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSConstants;
import com.amazon.aws.amazonfreertossdk.AmazonFreeRTOSManager;
import com.amazon.aws.amazonfreertossdk.BleConnectionStatusCallback;
import com.amazon.aws.amazonfreertossdk.BleScanResultCallback;
import com.amazonaws.mobile.client.AWSMobileClient;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanFragment extends Fragment {
    private static final String TAG = "DeviceScanFragment";
    private RecyclerView mBleDeviceRecyclerView;
    private BleDeviceAdapter mBleDeviceAdapter;
    List<BleDevice> mBleDevices = new ArrayList<>();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private Button scanButton;

    private AmazonFreeRTOSManager mAmazonFreeRTOSManager;

    private class BleDeviceHolder extends RecyclerView.ViewHolder {
        private TextView mBleDeviceNameTextView;
        private TextView mBleDeviceMacTextView;
        private Switch mBleDeviceSwitch;
        private TextView mMenuTextView;

        private BleDevice mBleDevice;

        public BleDeviceHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_device, parent, false));
            mBleDeviceNameTextView = (TextView) itemView.findViewById(R.id.device_name);
            mBleDeviceMacTextView = (TextView) itemView.findViewById(R.id.device_mac);
            mBleDeviceSwitch = (Switch) itemView.findViewById(R.id.connect_switch);
            mMenuTextView = (TextView) itemView.findViewById(R.id.menu_option);
        }

        public void bind(BleDevice bleDevice) {
            mBleDevice = bleDevice;
            mBleDeviceNameTextView.setText(mBleDevice.getName());
            mBleDeviceMacTextView.setText(mBleDevice.getMacAddr());
            mBleDeviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                    Log.i(TAG, "connect switch isChecked: " + (isChecked ? "ON":"OFF"));
                    if (isChecked) {
                        mAmazonFreeRTOSManager.connectToDevice(mBleDevice.getBluetoothDevice(),
                                connectionStatusCallback);
                    } else {
                        mAmazonFreeRTOSManager.close();
                        resetUI();
                    }
                }
            });

            mMenuTextView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    Log.i(TAG, "Click menu.");
                    PopupMenu popup = new PopupMenu(getContext(), mMenuTextView);
                    popup.inflate(R.menu.options_menu);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.wifi_provisioning_menu_id:
                                    Intent intentToStartWifiProvision
                                            = WifiProvisionActivity.newIntent(getActivity());
                                    startActivity(intentToStartWifiProvision);
                                    return true;
                                case R.id.mqtt_proxy_menu_id:
                                    Intent intentToStartAuthenticator
                                            = AuthenticatorActivity.newIntent(getActivity());
                                    startActivity(intentToStartAuthenticator);
                                    return true;
                            }
                            return false;
                        }
                    });
                    popup.show();
                }
            });

            resetUI();
        }

        final BleConnectionStatusCallback connectionStatusCallback = new BleConnectionStatusCallback() {
            @Override
            public void onBleConnectionStatusChanged(AmazonFreeRTOSConstants.BleConnectionState connectionStatus) {
                Log.i(TAG, "BLE connection status changed to: " + connectionStatus);
                if (connectionStatus == AmazonFreeRTOSConstants.BleConnectionState.BLE_CONNECTED) {
                    mAmazonFreeRTOSManager.setMtu(DemoConstants.MTU);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMenuTextView.setEnabled(true);
                            mMenuTextView.setTextColor(getResources().getColor(R.color.colorAccent, null));
                        }
                    });
                } else if (connectionStatus == AmazonFreeRTOSConstants.BleConnectionState.BLE_DISCONNECTED) {
                    resetUI();
                }
            }
        };

        private void resetUI() {
            mMenuTextView.setEnabled(false);
            mMenuTextView.setTextColor(Color.GRAY);
        }

    }

    private class BleDeviceAdapter extends RecyclerView.Adapter<BleDeviceHolder> {
        private List<BleDevice> mDeviceList;
        public BleDeviceAdapter(List<BleDevice> devices) {
            mDeviceList = devices;
        }

        @Override
        public BleDeviceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new BleDeviceHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(BleDeviceHolder holder, int position) {
            BleDevice device = mDeviceList.get(position);
            holder.bind(device);
        }

        @Override
        public int getItemCount() {
            return mDeviceList.size();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device_scan, container, false);

        //Enabling Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        // requesting user to grant permission.
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);

        //Getting AmazonFreeRTOSManager
        mAmazonFreeRTOSManager = AmazonFreeRTOSAgent.getAmazonFreeRTOSManager(getActivity());

        scanButton = (Button)view.findViewById(R.id.scanbutton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "scan button clicked.");
                mAmazonFreeRTOSManager.startScanBleDevices(new BleScanResultCallback() {
                    @Override
                    public void onBleScanResult(ScanResult result) {
                        BleDevice thisDevice = new BleDevice(result.getDevice().getName(),
                                result.getDevice().getAddress(), result.getDevice());
                        if (!mBleDevices.contains(thisDevice)) {
                            Log.d(TAG, "new ble device found. Mac: " + thisDevice.getMacAddr());
                            mBleDevices.add(thisDevice);
                            mBleDeviceAdapter.notifyDataSetChanged();
                        }
                    }
                }, 10000);
            }
        });

        //RecyclerView for displaying list of BLE devices.
        mBleDeviceRecyclerView = (RecyclerView) view.findViewById(R.id.device_recycler_view);
        mBleDeviceRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mBleDeviceAdapter = new BleDeviceAdapter(mBleDevices);
        mBleDeviceRecyclerView.setAdapter(mBleDeviceAdapter);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_device_scan, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                AWSMobileClient.getInstance().signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == getActivity().RESULT_OK) {
                Log.i(TAG, "successfully enabled bluetooth");
            } else {
                Log.w(TAG, "Failed to enable bluetooth");
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "ACCESS_FINE_LOCATION granted.");
                } else {
                    Log.w(TAG, "ACCESS_FINE_LOCATION denied");
                    scanButton.setEnabled(false);
                }
            }
        }
    }

}
