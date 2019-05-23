/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.freertos.amazonfreertossdk;

import software.amazon.freertos.amazonfreertossdk.networkconfig.DeleteNetworkResp;
import software.amazon.freertos.amazonfreertossdk.networkconfig.EditNetworkResp;
import software.amazon.freertos.amazonfreertossdk.networkconfig.ListNetworkResp;
import software.amazon.freertos.amazonfreertossdk.networkconfig.SaveNetworkResp;

public abstract class NetworkConfigCallback {

    public void onListNetworkResponse(ListNetworkResp response){}

    public void onSaveNetworkResponse(SaveNetworkResp response){}

    public void onEditNetworkResponse(EditNetworkResp response){}

    public void onDeleteNetworkResponse(DeleteNetworkResp response){}
}
