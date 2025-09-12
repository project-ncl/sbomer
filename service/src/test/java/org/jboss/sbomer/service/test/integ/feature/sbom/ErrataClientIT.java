package org.jboss.sbomer.service.test.integ.feature.sbom;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata.Details;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.test.ErrataWireMock;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
@WithTestResource(ErrataWireMock.class)
@TestProfile(TestUmbProfile.class)
@Slf4j
public class ErrataClientIT {

    @Inject
    @RestClient
    ErrataClient ec;

    public static String BIG_ADVISORY_ID = "139014";

    @Test
    void errataStubsLoaded() {
        List<StubMapping> errataStubs = ErrataWireMock.wireMockServer.listAllStubMappings()
                .getMappings()
                .stream()
                .filter(s -> s.getRequest().getUrl().contains("/api/v1/erratum"))
                .collect(Collectors.toList());

        assertTrue(errataStubs.size() > 1, "There should be at least one stub for the erratum URI");
    }

    @Test
    void fetchesBuilds() {

        Errata e = ec.getErratum(BIG_ADVISORY_ID);
        Details details = e.getDetails().get();
        ErrataBuildList erratumBuildList = ec.getBuildsList(String.valueOf(details.getId()));

        List<org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem> allBuilds = erratumBuildList
                .getProductVersions()
                .values()
                .stream()
                .flatMap(
                        productVersionEntry -> productVersionEntry.getBuilds()
                                .stream()
                                .flatMap(build -> build.getBuildItems().values().stream()))
                .toList();

        assertEquals("Expected 501 builds attached to the erratum", 501, allBuilds.size());
    }

    @Test
    void fetchesProduct() {

        Errata e = ec.getErratum(BIG_ADVISORY_ID);
        assertEquals(
                "Product should be",
                "Ansible Automation Platform",
                e.getDetails().get().getProduct().getShortName());
    }

    @Test
    void hasDelay() {
        Instant startGetErratum = Instant.now();

        Errata e = ec.getErratum(BIG_ADVISORY_ID);

        Instant endGetErratum = Instant.now();
        Duration getErratumDuration = Duration.between(startGetErratum, endGetErratum);

        assertTrue(
                getErratumDuration.toMillis() >= 2000,
                "getErratum call should take at least 2000ms, but took " + getErratumDuration.toMillis() + "ms");

        Details details = e.getDetails().get();

        Instant startGetBuilds = Instant.now();

        ErrataBuildList erratumBuildList = ec.getBuildsList(String.valueOf(details.getId()));

        Instant endGetBuilds = Instant.now();
        Duration getBuildsDuration = Duration.between(startGetBuilds, endGetBuilds);

        assertTrue(
                getBuildsDuration.toMillis() >= 20000,
                "getBuildsList call should take at least 20000ms, but took " + getBuildsDuration.toMillis() + "ms");
    }
}
