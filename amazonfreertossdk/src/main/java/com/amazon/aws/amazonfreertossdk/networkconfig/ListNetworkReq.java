package com.amazon.aws.amazonfreertossdk.networkconfig;

/**
 * List network request
 */
public class ListNetworkReq {
    /**
     * Maximum total number of networks to return.
     */
    public int maxNetworks;
    /**
     * Time in seconds for BLE device to scan available networks.
     */
    public int timeout;
}
