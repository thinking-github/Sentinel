package com.alibaba.csp.sentinel.cluster;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;

/**
 * @author chenyicheng1
 * @version 1.0
 * @since 2022/12/14
 */
public class ApiFlowRule extends FlowRule {

    /**
     * 限流规则所属空间 [租户、APP] 扩展使用
     */
    private String namespace;

    /**
     * 是否开启限流
     */
    private boolean open;

    /**
     * 当前API 单机限流最大值
     */
    private int lastCount;


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }


    public int getLastCount() {
        return lastCount;
    }

    public void setLastCount(int lastCount) {
        this.lastCount = lastCount;
    }
}
