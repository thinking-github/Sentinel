/*
 * Copyright 1999-2022 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.cluster.client.ha.discovery;

import com.alibaba.csp.sentinel.cluster.TokenServerDescriptor;
import com.alibaba.csp.sentinel.cluster.client.ha.TokenServerDiscovery;
import com.alibaba.csp.sentinel.spi.Spi;

import java.util.Arrays;
import java.util.List;

/**
 * @author icodening
 * @date 2022.03.06
 */
@Spi(value = LocalSentinelTokenServerDiscovery.NAME)
public class LocalSentinelTokenServerDiscovery implements TokenServerDiscovery {

    public static final String NAME = "sentinel";

    private static final String CONSOLE_SERVER = "csp.sentinel.dashboard.server";

    private static final int OK_STATUS = 200;


    private int currentAddressIdx = 0;

    public LocalSentinelTokenServerDiscovery() {
    }


    @Override
    @SuppressWarnings("unchecked")
    public List<TokenServerDescriptor> getTokenServers(String appName) {
        String host = "192.168.1.2";
        int port = 7001;

        TokenServerDescriptor serverDescriptor = new TokenServerDescriptor(host, port);
        return Arrays.asList(serverDescriptor);
    }


}

