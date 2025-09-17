package org.jboss.sbomer.service.test.integ.feature.sbom;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata.Details;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.test.ErrataWireMock;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    // Timing sensitive
    public static String BIG_ADVISORY_ID = "139014";
    // Not really timing sensitive
    public static String TEXT_ONLY_ADVISORY_ID = "107794";

    // Reuse the statics as we use them elsewhere
    private static Stream<Arguments> advisoryIdProvider() {
        return Stream.of(
                Arguments.of(BIG_ADVISORY_ID, "Ansible Automation Platform"),
                Arguments.of(TEXT_ONLY_ADVISORY_ID, "JBEAP"));
    }

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

    // Verify we have loaded the right wiremocks
    @ParameterizedTest
    @MethodSource("advisoryIdProvider")
    void fetchesProduct(String advisoryId, String expectedProductName) {

        Errata e = ec.getErratum(advisoryId);
        assertEquals("Product should be", expectedProductName, e.getDetails().get().getProduct().getShortName());
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
