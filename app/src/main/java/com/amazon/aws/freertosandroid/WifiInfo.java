package com.amazon.aws.freertosandroid;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WifiInfo {
    private static String[] NETWORK_TYPES = {"Open", "WEP", "WPA", "WPA2", "Other"};
    private String ssid;
    private String bssid;
    private int rssi;
    private int networkType;
    private int index;
    private boolean connected;

    public String getNetworkTypeName() {
        return NETWORK_TYPES[networkType];
    }

    public WifiInfo(String ssid, String bssid, int rssi, int networkType, int index, boolean connected) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.networkType = networkType;
        this.index = index;
        this.connected = connected;
    }
}
