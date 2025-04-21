package org.jboss.sbomer.service.test;

import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class PyxisWireMock implements QuarkusTestResourceLifecycleManager {

    public static WireMockServer wireMockServer;

    // https://github.com/geoand/quarkus-test-demo/blob/main/src/test/java/org/acme/getting/started/country/WiremockCountries.java
    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
        wireMockServer.start();

        /*
         * stubFor( get( urlEqualTo(
         * "v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-9999?include=data.repositories.registry&include=data.repositories.repository&include=data.repositories.tags&include=data.repositories.published"
         * )) .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(""" { "data":[] } """)));
         */

        return Collections.singletonMap("quarkus.rest-client.pyxis.url", wireMockServer.baseUrl());
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }

    /*
     * https://catalog.redhat.com/api/containers/docs/endpoints/RESTGetImagesByNVR.html
     */
    /*
     * @Test void nonExistantRepo() { Assertions.assertNotNull(wiremock);
     * wiremock.register(get(urlEqualTo("v1/images/nvr/")).willReturn(aResponce().withStatus(200).withBody(emptyPayload)
     * ));
     *
     * }
     *
     * @Test void slowPublishRepo() {
     *
     * }
     */

}
