package com.alibaba.csp.sentinel.cluster.response;

import com.alibaba.csp.sentinel.cluster.TokenResult;

/**
 * 动态请求结果
 *
 * @author cdliuhaibo
 * @date 2022/3/17
 * @see com.alibaba.csp.sentinel.cluster.TokenResultStatus#OK
 * @see com.alibaba.csp.sentinel.cluster.TokenResultStatus#FAIL
 */
public class DynamicTokenResult extends TokenResult {
    /**
     * 最新的流控数据
     */
    private int count;

    /**
     *  某一个app客户端机器节点数
     */
    private int nodes;

    /**
     * 等待多久之后再次请求(单位毫秒) 【使用父类属性】
     */
    //private int waitInMs;
    
    /**
     * 调用服务端ip,使用客户端端负载客户端填充即可
     */
    private String serverIp;
    
    public int getCount() {
        return count;
    }

    public DynamicTokenResult setCount(int count) {
        this.count = count;
        return this;
    }

    public DynamicTokenResult setWaitInMs(int waitInMs) {
        super.setWaitInMs(waitInMs);
        return this;
    }

    public int getNodes() {
        return nodes;
    }

    public DynamicTokenResult setNodes(int nodes) {
        this.nodes = nodes;
        return this;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public DynamicTokenResult(Integer status) {
        super(status);
    }
    
    public DynamicTokenResult(Integer status, String serverIp) {
        super(status);
        this.serverIp = serverIp;
    }
}
