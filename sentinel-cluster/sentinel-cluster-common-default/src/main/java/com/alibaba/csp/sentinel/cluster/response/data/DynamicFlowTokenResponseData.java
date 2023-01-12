package com.alibaba.csp.sentinel.cluster.response.data;

/**
 * 动态限流响应数据
 *
 * @author cdliuhaibo
 * @date 2022/3/17
 */
public class DynamicFlowTokenResponseData {
    /**
     * 最新的流控数据
     */
    private int count;

    /**
     * 某一个app客户端机器节点数
     */
    private int nodes;

    /**
     * 等待多久之后再次请求(单位毫秒)
     */
    private int waitInMs;

    public int getCount() {
        return count;
    }

    public DynamicFlowTokenResponseData setCount(int count) {
        this.count = count;
        return this;
    }

    public int getNodes() {
        return nodes;
    }

    public DynamicFlowTokenResponseData setNodes(int nodes) {
        this.nodes = nodes;
        return this;
    }

    public int getWaitInMs() {
        return waitInMs;
    }

    public DynamicFlowTokenResponseData setWaitInMs(int waitInMs) {
        this.waitInMs = waitInMs;
        return this;
    }
}
