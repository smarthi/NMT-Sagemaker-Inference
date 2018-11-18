/*
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dws.berlin.util;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sagemakerruntime.AmazonSageMakerRuntime;
import com.amazonaws.services.sagemakerruntime.AmazonSageMakerRuntimeClient;
import com.amazonaws.services.sagemakerruntime.model.InvokeEndpointRequest;

/**
 *  Utility class for common AWS functions
 *  @author Suneel Marthi
 */
public class AwsUtil {

  /**
   * Invoke SageMaker Service Endpoint
   * @param serviceEndpoint - sagemaker service endpoint
   * @return {@code InvokeEndpointRequest}
   */
  public static InvokeEndpointRequest getInvokeEndpointRequest(String serviceEndpoint) {
    InvokeEndpointRequest invokeEndpointRequest = new InvokeEndpointRequest();
    invokeEndpointRequest.setEndpointName(serviceEndpoint);
    invokeEndpointRequest.setContentType("application/json");
    invokeEndpointRequest.setAccept(("application/json"));
    return invokeEndpointRequest;
  }

  /**
   * Return instance of {@code AmazonSageMakerRuntime}
   * @param regionName - AWS region name
   *
   */
  public static AmazonSageMakerRuntime getSageMakerClient(String regionName) {
    return AmazonSageMakerRuntimeClient.builder()
        .withRegion(Regions.fromName(regionName))
        .withCredentials(new ProfileCredentialsProvider())
        .build();
  }
}
