package com.amazon.aws.amazonfreertossdk.networkconfig;

/**
 * Edit network response
 */
public class EditNetworkResp {
    /**
     * Status of the operation. 0 for success.
     */
    int status;
    public String toString() {
        return String.format("EditNetworkResponse ->\n status: %d", status);
    }
}
