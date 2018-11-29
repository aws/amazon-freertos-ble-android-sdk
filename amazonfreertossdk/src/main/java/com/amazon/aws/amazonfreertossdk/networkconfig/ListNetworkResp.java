package com.amazon.aws.amazonfreertossdk.networkconfig;

import lombok.Getter;

/**
 * List network response
 */
@Getter
public class ListNetworkResp {
    /**
     * Status of the operation. 0 for success.
     */
    private int status;
    /**
     * SSID of the scanned network.
     */
    private String ssid;
    /**
     * BSSID of the scanned network.
     */
    private String bssid;
    /**
     * Network security type.
     */
    private int security;
    /**
     * Whether the network is hidden.
     */
    private Boolean hidden;
    /**
     * RSSI value of the scanned network.
     */
    private int rssi;
    /**
     * Whether BLE device is connected to this network.
     */
    private Boolean connected;
    /**
     * The index of this network. Index is used to indicate the connection preference of each saved
     * network. For non-saved networks, the index is negative.
     */
    private int index;

    public String toString() {
        return String.format("List network response -> Status: %d ssid: %s bssid: %s security: %d hidden: %s" +
                        " rssi: %d connected: %s index: %d", status, ssid, bssid, security,
                hidden ? "true":"false", rssi, connected ? "true":"false", index);
    }
}
