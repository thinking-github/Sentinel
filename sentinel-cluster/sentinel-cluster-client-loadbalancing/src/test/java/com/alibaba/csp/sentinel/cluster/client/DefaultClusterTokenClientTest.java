package com.alibaba.csp.sentinel.cluster.client;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenServerDescriptor;
import com.alibaba.csp.sentinel.cluster.client.ha.TokenServerDiscovery;
import com.alibaba.csp.sentinel.spi.SpiLoader;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chenyicheng1
 * @version 1.0
 * @since 2022/11/28
 */
public class DefaultClusterTokenClientTest {


    @Test
    public void connTokenServer() throws Exception {

        TokenServerDiscovery tokenServerDiscovery = SpiLoader.of(TokenServerDiscovery.class).loadInstance("sentinel");


        List<TokenServerDescriptor> tokenServers = new ArrayList<>();
        TokenServerDescriptor server = new TokenServerDescriptor("192.168.0.1",7001);
        System.out.println("oldServer: " +  server.hashCode());
        tokenServers.add(server);

        System.out.println(tokenServers.hashCode());

        List<TokenServerDescriptor> newServers = new ArrayList<>();
        TokenServerDescriptor newServer = new TokenServerDescriptor("192.168.0.1",7001);
        System.out.println("newServer: " +  newServer.hashCode());
        newServers.add(newServer);
        System.out.println(newServers.hashCode());

    }
}
