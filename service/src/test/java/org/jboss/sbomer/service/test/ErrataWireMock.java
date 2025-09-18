package org.jboss.sbomer.service.test;

import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

//import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ErrataWireMock implements QuarkusTestResourceLifecycleManager {

    public static WireMockServer wireMockServer;
    public static String mappingsDir = "mappings/errata";

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
        wireMockServer.start();
        return Collections.singletonMap("quarkus.rest-client.errata.url", wireMockServer.baseUrl());
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }
}
