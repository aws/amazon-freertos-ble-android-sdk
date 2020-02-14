package software.amazon.freertos.demo;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WifiInfo {
    private static String[] NETWORK_TYPES = {"Open", "WEP", "WPA", "WPA2", "WPA2-Enterprise", "Other"};
    private String ssid;
    private byte[] bssid;
    private int rssi;
    private int networkType;
    private int index;
    private boolean connected;

    public String getNetworkTypeName() {
        return NETWORK_TYPES[networkType];
    }

    public WifiInfo(String ssid, byte[] bssid, int rssi, int networkType, int index, boolean connected) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.networkType = networkType;
        this.index = index;
        this.connected = connected;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WifiInfo wifiInfo = (WifiInfo) obj;
        return Objects.equals(wifiInfo.getSsid(), ssid);
    }
}
