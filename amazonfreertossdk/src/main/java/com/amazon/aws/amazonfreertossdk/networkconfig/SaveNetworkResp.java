package com.amazon.aws.amazonfreertossdk.networkconfig;

/**
 * Save network response
 */
public class SaveNetworkResp {
    /**
     * Status of the operation. 0 for success.
     */
    int status;
    public String toString() {
        return String.format("SaveNetworkResponse ->\n status: %d", status);
    }
}
