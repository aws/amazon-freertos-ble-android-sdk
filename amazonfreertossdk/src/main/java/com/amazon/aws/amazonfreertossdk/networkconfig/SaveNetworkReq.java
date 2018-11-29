package com.amazon.aws.amazonfreertossdk.networkconfig;

/**
 * Save network request.
 */
public class SaveNetworkReq {
    /**
     * SSID of the network to be saved.
     */
    public String ssid;
    /**
     * BSSID of the network to be saved.
     */
    public String bssid;
    /**
     * Password of the network to be saved.
     */
    public String psk;
    /**
     * Network security type.
     */
    public int security;
    /**
     * Current index of the network to be saved.
     */
    public int index;
}
