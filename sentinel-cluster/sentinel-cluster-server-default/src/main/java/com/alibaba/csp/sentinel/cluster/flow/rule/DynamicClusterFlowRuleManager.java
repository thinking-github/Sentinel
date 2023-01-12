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
package com.alibaba.csp.sentinel.cluster.flow.rule;

import com.alibaba.csp.sentinel.cluster.flow.statistic.ClusterMetricStatistics;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.CurrentConcurrencyManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.metric.ClusterMetric;
import com.alibaba.csp.sentinel.cluster.server.connection.ConnectionManager;
import com.alibaba.csp.sentinel.cluster.server.util.ClusterRuleUtil;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.property.DynamicSentinelProperty;
import com.alibaba.csp.sentinel.property.SentinelProperty;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleUtil;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.csp.sentinel.util.function.Function;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支持单个限流规则更新
 * Manager for cluster flow rules.
 */
public final class DynamicClusterFlowRuleManager {

    /**
     * The default cluster flow rule property supplier that creates a new dynamic property
     * for a specific namespace to do rule management manually.
     */
    public static final Function<String, SentinelProperty<List<FlowRule>>> DEFAULT_PROPERTY_SUPPLIER =
            new Function<String, SentinelProperty<List<FlowRule>>>() {
                @Override
                public SentinelProperty<List<FlowRule>> apply(String namespace) {
                    return new DynamicSentinelProperty<>();
                }
            };

    /**
     * (flowId, clusterRule)
     */
    private static final Map<Long, FlowRule> FLOW_RULES = new ConcurrentHashMap<>();
    /**
     * (namespace, [flowId...])
     */
    private static final Map<String, Set<Long>> NAMESPACE_FLOW_ID_MAP = new ConcurrentHashMap<>();
    /**
     * <p>This map (flowId, namespace) is used for getting connected count
     * when checking a specific rule in {@code ruleId}:</p>
     *
     * <pre>
     * ruleId -> namespace -> connection group -> connected count
     * </pre>
     */
    private static final Map<Long, String> FLOW_NAMESPACE_MAP = new ConcurrentHashMap<>();

    /**
     * (namespace, property-listener wrapper)
     */
    private static final Map<String, NamespaceFlowProperty<FlowRule>> PROPERTY_MAP = new ConcurrentHashMap<>();
    /**
     * Cluster flow rule property supplier for a specific namespace.
     */
    private static volatile Function<String, SentinelProperty<List<FlowRule>>> propertySupplier
            = DEFAULT_PROPERTY_SUPPLIER;

    private static final Object UPDATE_LOCK = new Object();
    
    /**
     * Get flow rule by rule ID.
     *
     * @param id rule ID
     * @return flow rule
     */
    public static FlowRule getFlowRuleById(Long id) {
        if (!ClusterRuleUtil.validId(id)) {
            return null;
        }
        return FLOW_RULES.get(id);
    }

    public static Set<Long> getFlowIdSet(String namespace) {
        if (StringUtil.isEmpty(namespace)) {
            return new HashSet<>();
        }
        Set<Long> set = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (set == null) {
            return new HashSet<>();
        }
        return new HashSet<>(set);
    }

    public static List<FlowRule> getAllFlowRules() {
        return new ArrayList<>(FLOW_RULES.values());
    }

    /**
     * Get all cluster flow rules within a specific namespace.
     *
     * @param namespace valid namespace
     * @return cluster flow rules within the provided namespace
     */
    public static List<FlowRule> getFlowRules(String namespace) {
        if (StringUtil.isEmpty(namespace)) {
            return new ArrayList<>();
        }
        List<FlowRule> rules = new ArrayList<>();
        Set<Long> flowIdSet = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (flowIdSet == null || flowIdSet.isEmpty()) {
            return rules;
        }
        for (Long flowId : flowIdSet) {
            FlowRule rule = FLOW_RULES.get(flowId);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    /**
     * Load flow rules for a specific namespace. The former rules of the namespace will be replaced.
     *
     * @param namespace a valid namespace
     * @param rules     rule list
     */
    public static void loadRules(String namespace, List<FlowRule> rules) {
        AssertUtil.notEmpty(namespace, "namespace cannot be empty");
        NamespaceFlowProperty<FlowRule> property = PROPERTY_MAP.get(namespace);
        if (property != null) {
            property.getProperty().updateValue(rules);
        }
    }

    private static void resetNamespaceFlowIdMapFor(/*@Valid*/ String namespace) {
        NAMESPACE_FLOW_ID_MAP.put(namespace, new HashSet<Long>());
    }

    /**
     * Clear all rules of the provided namespace and reset map.
     *
     * @param namespace valid namespace
     */
    private static void clearAndResetRulesFor(/*@Valid*/ String namespace) {
        Set<Long> flowIdSet = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (flowIdSet != null && !flowIdSet.isEmpty()) {
            for (Long flowId : flowIdSet) {
                FLOW_RULES.remove(flowId);
                FLOW_NAMESPACE_MAP.remove(flowId);
                if (CurrentConcurrencyManager.containsFlowId(flowId)) {
                    CurrentConcurrencyManager.remove(flowId);
                }
            }
            flowIdSet.clear();
        } else {
            resetNamespaceFlowIdMapFor(namespace);
        }
    }

    /**
     * Get connected count for associated namespace of given {@code flowId}.
     *
     * @param flowId unique flow ID
     * @return connected count
     */
    public static int getConnectedCount(long flowId) {
        if (flowId <= 0) {
            return 0;
        }
        String namespace = FLOW_NAMESPACE_MAP.get(flowId);
        if (namespace == null) {
            return 0;
        }
        return ConnectionManager.getConnectedCount(namespace);
    }

    public static String getNamespace(long flowId) {
        return FLOW_NAMESPACE_MAP.get(flowId);
    }

    /**
     * 添加限流规则
     *
     * @param flowRule
     * @param namespace
     */
    public static void addClusterFlowRule(FlowRule flowRule, /*@Valid*/ String namespace) {
        if (flowRule == null) {
            return;
        }

        FlowRule rule = flowRule;
        if (!rule.isClusterMode()) {
            return;
        }
        if (!FlowRuleUtil.isValidRule(rule)) {
            RecordLog.warn(
                    "[ClusterFlowRuleManager] Ignoring invalid flow rule when loading new flow rules: " + rule);
            return;
        }
        if (StringUtil.isBlank(rule.getLimitApp())) {
            rule.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
        }

        // Flow id should not be null after filtered.
        ClusterFlowConfig clusterConfig = rule.getClusterConfig();
        Long flowId = clusterConfig.getFlowId();
        if (flowId == null) {
            return;
        }
        FLOW_RULES.put(flowId, rule);
        FLOW_NAMESPACE_MAP.put(flowId, namespace);
        Set<Long> flowIdSet = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (flowIdSet == null) {
            flowIdSet = new HashSet<>();
            NAMESPACE_FLOW_ID_MAP.put(namespace, flowIdSet);
        }

        flowIdSet.add(flowId);
        if (!CurrentConcurrencyManager.containsFlowId(flowId)) {
            CurrentConcurrencyManager.put(flowId, 0);
        }

        // Prepare cluster metric from valid flow ID.
        ClusterMetricStatistics.putMetricIfAbsent(flowId,
                new ClusterMetric(clusterConfig.getSampleCount(), clusterConfig.getWindowIntervalMs()));

    }

    /**
     * 删除限流规则
     * @param flowId
     * @param namespace
     */
    public static void removeClusterFlowRule(long flowId, /*@Valid*/ String namespace) {
        // Cleanup unused cluster metrics.
        FLOW_RULES.remove(flowId);
        FLOW_NAMESPACE_MAP.remove(flowId);
        ClusterMetricStatistics.removeMetric(flowId);
        if (CurrentConcurrencyManager.containsFlowId(flowId)) {
            CurrentConcurrencyManager.remove(flowId);
        }
        Set<Long> oldIdSet = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (oldIdSet != null && !oldIdSet.isEmpty()) {
            oldIdSet.remove(flowId);
        }
    }

    /**
     * 基于限流规则,查询 namespace 空间列表
     * @return
     */
    public static Set<String> getNamespaces() {
        Set<String> namespaces = NAMESPACE_FLOW_ID_MAP.keySet();
        return namespaces;
    }
    

    private DynamicClusterFlowRuleManager() {
    }
}
