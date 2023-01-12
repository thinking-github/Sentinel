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
package com.alibaba.csp.sentinel.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.DynamicClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.ClusterMetricStatistics;
import com.alibaba.csp.sentinel.cluster.flow.statistic.metric.ClusterMetric;
import com.alibaba.csp.sentinel.cluster.request.data.DynamicFlowRequestData;
import com.alibaba.csp.sentinel.cluster.response.DynamicTokenResult;
import com.alibaba.csp.sentinel.cluster.server.connection.ConnectionManager;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.fastjson.JSON;


/**
 * 动态Token服务
 *
 * @author cdliuhaibo
 */
public class DynamicTokenServiceImpl implements DynamicTokenService {

    @Override
    public DynamicTokenResult requestToken(DynamicFlowRequestData requestData) {

        // The rule should be valid.
        Long flowId = requestData.getFlowId();
        int maxCount = requestData.getMaxCount();
        int lastQps = requestData.getLastCount();
        //FlowRule rule = ClusterFlowRuleManager.getFlowRuleById(requestData.getFlowId());
        FlowRule rule = DynamicClusterFlowRuleManager.getFlowRuleById(flowId);
        if (rule == null) {
            return new DynamicTokenResult(TokenResultStatus.NO_RULE_EXISTS);
        }
        String namespace = DynamicClusterFlowRuleManager.getNamespace(flowId);
        int count = ConnectionManager.getConnectedCount(namespace);

        RecordLog.info("handle requestToken flowId:" + flowId + " namespace:" + namespace + " maxCount:" + maxCount + " lastCount:" + lastQps + " host count:" + count);

        return acquireClusterToken(rule, maxCount);
        //DynamicTokenResult result = MonitorUtil.monitor("dynamic.requestToken", () -> requestToken0(requestData));
        //return result;
    }


    public static DynamicTokenResult acquireClusterToken(FlowRule rule, int maxCount) {
        Long flowId = rule.getClusterConfig().getFlowId();

        ClusterMetric metric = ClusterMetricStatistics.getMetric(flowId);
        if (metric == null) {
            return new DynamicTokenResult(TokenResultStatus.FAIL);
        }

        // 集群限流总数
        double count = rule.getCount();
        // app集群机器总数
        int connectedCount = DynamicClusterFlowRuleManager.getConnectedCount(flowId);

        double localCount = count;
        if (count > 0 && connectedCount > 0) {
            localCount = count / connectedCount;
        }

        // 当前新值/上一次值,当应用节点增加、减少超过一半时告警记录提示
        if (maxCount > 0) {
            double ratio = localCount / maxCount;
            if (ratio <= 0.5 || ratio >= 2) {
                RecordLog.error("warning to handle requestToken[" + flowId + "]. The newMaxCount/lastMaxCount[{}/{}={}]", localCount, maxCount, ratio);
            }
        }

        return new DynamicTokenResult(TokenResultStatus.OK)
                .setCount((int) localCount)
                .setNodes(connectedCount)
                .setWaitInMs(0);
    }


    //对不同的code做对应的监控空
    private DynamicTokenResult requestToken0(DynamicFlowRequestData requestData) {
        DynamicTokenResult result = doRequestToken(requestData);
        // TODO: 2022/12/4  thinking
        return null;
        //return MonitorUtil.monitor("dynamic.requestToken." + codeDesc(result, requestData.getFlowId()), () -> result);
    }

    private String codeDesc(DynamicTokenResult result, Long ruleId) {
        int code = (result == null || result.getStatus() == null) ? -999 : result.getStatus();
        //报错了，打印日志
        if (code != 0 && code != 1) {
            RecordLog.info("Failed to handle requestToken[" + ruleId + "]. The response is " + JSON.toJSONString(result));
        }

        switch (code) {
            case -999:
                return "null";
            case TokenResultStatus.BAD_REQUEST:
                return "bad.request";
            case TokenResultStatus.TOO_MANY_REQUEST:
                return "many.request";
            case TokenResultStatus.FAIL:
                return "failed";
            case TokenResultStatus.OK:
                return "success";
            case TokenResultStatus.BLOCKED:
                return "blocked";
            case TokenResultStatus.SHOULD_WAIT:
                return "wait";
            case TokenResultStatus.NO_RULE_EXISTS:
                return "no.rule";
            case TokenResultStatus.NO_REF_RULE_EXISTS:
                return "no.ref.rule";
            case TokenResultStatus.NOT_AVAILABLE:
                return "not.available";
            default:
                return "exception";
        }
    }

    private DynamicTokenResult doRequestToken(DynamicFlowRequestData requestData) {
        if (notValidRequest(requestData.getFlowId())) {
            return badRequest();
        }
        // The rule should be valid.
        FlowRule rule = ClusterFlowRuleManager.getFlowRuleById(requestData.getFlowId());
        if (rule == null) {
            return new DynamicTokenResult(TokenResultStatus.NO_RULE_EXISTS);
        }
        // TODO: 2022/12/4  thinking
        return null;
        //return DynamicClusterFlowChecker.acquireClusterToken(rule, requestData);
    }


    private boolean notValidRequest(Long id) {
        return id == null || id <= 0;
    }

    private DynamicTokenResult badRequest() {
        return new DynamicTokenResult(TokenResultStatus.BAD_REQUEST);
    }
}
