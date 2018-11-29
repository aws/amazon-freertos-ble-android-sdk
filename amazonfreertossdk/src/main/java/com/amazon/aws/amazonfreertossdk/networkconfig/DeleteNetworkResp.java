package com.amazon.aws.amazonfreertossdk.networkconfig;

/**
 * Delete network response
 */
public class DeleteNetworkResp {
    /**
     * Status of the operation. 0 for success.
     */
    int status;
    public String toString() {
        return String.format("DeleteNetworkResponse ->\n status: %d", status);
    }
}
