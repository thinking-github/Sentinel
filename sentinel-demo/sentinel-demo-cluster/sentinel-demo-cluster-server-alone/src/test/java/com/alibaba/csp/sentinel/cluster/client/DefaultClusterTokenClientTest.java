package com.alibaba.csp.sentinel.cluster.client;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenServerDescriptor;
import com.alibaba.csp.sentinel.cluster.client.codec.registry.RequestDataWriterRegistry;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.ha.DefaultLoadBalancingClusterTokenClient;
import com.alibaba.csp.sentinel.cluster.client.init.DefaultClusterClientInitFunc;
import com.alibaba.csp.sentinel.cluster.response.DynamicTokenResult;
import org.junit.Test;

/**
 * @author chenyicheng1
 * @version 1.0
 * @since 2022/11/28
 */
public class DefaultClusterTokenClientTest {


    @Test
    public void connTokenServer() throws Exception {

        System.setProperty("csp.sentinel.app.name", "appA");
        
        String appName = "dataServer-tokenServer";
        String host = "192.168.1.2";
        int port = 7001;

        // appName?discovery=springCloud
        DefaultLoadBalancingClusterTokenClient client = (DefaultLoadBalancingClusterTokenClient) TokenClientProvider.getClient();
        ClusterClientAssignConfig assignConfig = new ClusterClientAssignConfig(appName, port);
        client.onRemoteServerChange(assignConfig);

       
        

        NettyTransportClient nettyTransportClient = new NettyTransportClient(host, port);
        TokenServerDescriptor serverDescriptor = new TokenServerDescriptor(host, port);
        DefaultClusterTokenClient clusterTokenClient = new DefaultClusterTokenClient(nettyTransportClient,serverDescriptor);
        clusterTokenClient.start();
        
        //防止无法初始化,保护加载
        if (RequestDataWriterRegistry.getWriter(0) == null) {
            DefaultClusterClientInitFunc init = new DefaultClusterClientInitFunc();
            init.init();
        }
        


        Thread.sleep(2000);
        //client.requestDynamicToken(1L,100,100);
        //TokenResult tokenResult = clusterTokenClient.requestToken(1L, 1, true);
        for (int i = 0; i < 10; i++) {
            DynamicTokenResult dynamicTokenResult = clusterTokenClient.requestDynamicToken(1L,100,100);
            Thread.sleep(1000);
        }
        

        System.out.println(2);

    }
}
