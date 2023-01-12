package com.alibaba.csp.sentinel.cluster.flow;

import com.alibaba.csp.sentinel.cluster.request.data.DynamicFlowRequestData;
import com.alibaba.csp.sentinel.cluster.response.DynamicTokenResult;

/**
 * @author chenyicheng1
 * @version 1.0
 * @since 2023/1/3
 */
public interface DynamicTokenService {

    /**
     * 动态获取token 基于flowId、lastMaxCount、lastQpsCount计算
     *
     * @param requestData
     * @return
     */
    public DynamicTokenResult requestToken(DynamicFlowRequestData requestData);
}
