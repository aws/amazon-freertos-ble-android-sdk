package com.amazon.aws.amazonfreertossdk.networkconfig;

/**
 * Edit network request
 */
public class EditNetworkReq {
    /**
     * The index of the saved network to be edited.
     */
    public int index;
    /**
     * The new index of the saved network. Must be one of the existing indices of saved networks.
     */
    public int newIndex;
}
