/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.cluster.server.processor;


import com.alibaba.csp.sentinel.cluster.ClusterConstants;
import com.alibaba.csp.sentinel.cluster.annotation.RequestType;
import com.alibaba.csp.sentinel.cluster.flow.DynamicTokenService;
import com.alibaba.csp.sentinel.cluster.flow.DynamicTokenServiceImpl;
import com.alibaba.csp.sentinel.cluster.request.ClusterRequest;
import com.alibaba.csp.sentinel.cluster.request.data.DynamicFlowRequestData;
import com.alibaba.csp.sentinel.cluster.response.ClusterResponse;
import com.alibaba.csp.sentinel.cluster.response.DynamicTokenResult;
import com.alibaba.csp.sentinel.cluster.response.data.DynamicFlowTokenResponseData;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.spi.SpiLoader;

/**
 * 动态流控处理类
 *
 * @author cdliuhaibo
 */
@RequestType(ClusterConstants.MSG_TYPE_DYNAMIC_FLOW)
public class DynamicFlowRequestProcessor implements RequestProcessor<DynamicFlowRequestData, DynamicFlowTokenResponseData> {

    private DynamicTokenService dynamicTokenService;

    public DynamicFlowRequestProcessor() {

        DynamicTokenService resolvedTokenService = SpiLoader.of(DynamicTokenService.class).loadHighestPriorityInstance();
        //DynamicTokenService first = SpiLoader.of(DynamicTokenService.class).loadFirstInstance();
        this.dynamicTokenService = resolvedTokenService;

        if (dynamicTokenService == null) {
            dynamicTokenService = new DynamicTokenServiceImpl();
        }
        RecordLog.info("[FlowRequestProcessor] DynamicTokenService impl className:" + dynamicTokenService.getClass().getName());
    }

    @Override
    public ClusterResponse<DynamicFlowTokenResponseData> processRequest(ClusterRequest<DynamicFlowRequestData> request) {
        DynamicTokenResult result = dynamicTokenService.requestToken(request.getData());
        return toResponse(result, request);
    }

    private ClusterResponse<DynamicFlowTokenResponseData> toResponse(DynamicTokenResult result, ClusterRequest request) {
        return new ClusterResponse<>(request.getId(), request.getType(), result.getStatus(),
                new DynamicFlowTokenResponseData()
                        .setCount(result.getCount())
                        .setNodes(result.getNodes())
                        .setWaitInMs(result.getWaitInMs())
        );
    }
}
