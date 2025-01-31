package org.jboss.sbomer.service.test.unit.feature.sbom.errata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataMessageHelper;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataPage;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataProduct;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataVariant;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataType;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.ErrataStatusChangeMessageBody;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

class ErrataMessageHelperTestCase {

    @Test
    void testErrataStatusChangeMessage() throws IOException {
        String rawErrataStatusMsg = TestResources.asString("errata/umb/errata_status_change.json");

        ErrataStatusChangeMessageBody msg = ErrataMessageHelper.fromStatusChangeMessage(rawErrataStatusMsg);
        assertEquals(139230L, msg.getErrataId());
        assertEquals(ErrataStatus.SHIPPED_LIVE, msg.getStatus());
        assertEquals(ErrataStatus.IN_PUSH, msg.getFrom());
        assertEquals(ErrataStatus.SHIPPED_LIVE, msg.getTo());
        assertEquals("RHSA-2041:XXXX-YY", msg.getFullAdvisory());
        assertEquals("MYPRODUCT", msg.getProduct());
        assertEquals("MYRELEASE", msg.getRelease());
        assertEquals("Important: security update", msg.getSynopsis());
        assertEquals(ErrataType.RHSA, msg.getType());

        assertEquals(ZonedDateTime.of(2041, 10, 1, 8, 3, 25, 0, ZoneOffset.UTC).toInstant(), msg.getWhen());
        assertEquals("john@doyle.com", msg.getWho());
        assertTrue(List.of(msg.getContentTypes()).contains("rpm"));
    }

    @Test
    void testErrataReleaseDTO() throws IOException {
        String releaseJsonString = TestResources.asString("errata/api/release.json");
        ErrataRelease release = ObjectMapperProvider.json().readValue(releaseJsonString, ErrataRelease.class);
        assertEquals(2227L, release.getData().getId());
        assertEquals("releases", release.getData().getType());
        assertEquals("RHEL-8-RHBQ-3.8", release.getData().getAttributes().getName());
        assertEquals("Red Hat build of Quarkus 3.8 on RHEL 8", release.getData().getAttributes().getDescription());
        assertEquals(153L, release.getData().getRelationships().getProduct().getId());
        assertEquals("RHBQ", release.getData().getRelationships().getProduct().getShortName());
        assertEquals(1, release.getData().getRelationships().getProductVersions().size());
        ErrataRelease.ErrataProductVersion productVersion = release.getData()
                .getRelationships()
                .getProductVersions()
                .get(0);
        assertEquals(2166L, productVersion.getId());
        assertEquals("RHEL-8-RHBQ-3.8", productVersion.getName());
    }

    @Test
    void testErrataProductDTO() throws IOException {
        String productJsonString = TestResources.asString("errata/api/product.json");
        ErrataProduct product = ObjectMapperProvider.json().readValue(productJsonString, ErrataProduct.class);
        assertEquals(153L, product.getData().getId());
        assertEquals("products", product.getData().getType());
        assertEquals("Red Hat build of Quarkus", product.getData().getAttributes().getName());
        assertEquals("RHBQ", product.getData().getAttributes().getShortName());
        assertEquals(
                "Quarkus is a Kubernetes-native Java framework tailored for JVM and native compilation, crafted from best-of-breed Java libraries and standards.",
                product.getData().getAttributes().getDescription());
        assertEquals(8, product.getData().getRelationships().getProductVersions().size());
        ErrataProduct.ErrataProductVersion productVersion = product.getData()
                .getRelationships()
                .getProductVersions()
                .get(0);
        assertEquals(1229L, productVersion.getId());
        assertEquals("RHBQ Text-Only", productVersion.getName());
    }

    @Test
    void testErrataVariantDTO() throws IOException {
        String variantJsonString = TestResources.asString("errata/api/variant.json");
        ErrataVariant variant = ObjectMapperProvider.json().readValue(variantJsonString, ErrataVariant.class);
        assertEquals(4603L, variant.getData().getId());
        assertEquals("variants", variant.getData().getType());
        assertEquals("8Base-RHBQ-3.8", variant.getData().getAttributes().getName());
        assertEquals("cpe:/a:rh:qk:3.8::el8", variant.getData().getAttributes().getCpe());
        assertEquals("Red Hat build of Quarkus 3.8", variant.getData().getAttributes().getDescription());
        assertEquals(153L, variant.getData().getAttributes().getRelationships().getProduct().getId());
        assertEquals(
                "Red Hat build of Quarkus",
                variant.getData().getAttributes().getRelationships().getProduct().getName());
        assertEquals("RHBQ", variant.getData().getAttributes().getRelationships().getProduct().getShortName());
        ErrataVariant.ErrataProductVersion productVersion = variant.getData()
                .getAttributes()
                .getRelationships()
                .getProductVersion();
        assertEquals(2166L, productVersion.getId());
        assertEquals("RHEL-8-RHBQ-3.8", productVersion.getName());
        assertEquals(87L, variant.getData().getAttributes().getRelationships().getRhelRelease().getId());
        assertEquals("RHEL-8", variant.getData().getAttributes().getRelationships().getRhelRelease().getName());
        assertEquals(2235L, variant.getData().getAttributes().getRelationships().getRhelVariant().getId());
        assertEquals("8Base", variant.getData().getAttributes().getRelationships().getRhelVariant().getName());
    }

    @Test
    void testErrataErratumDTO() throws IOException {
        String erratumJsonString = TestResources.asString("errata/api/erratum.json");
        Errata erratum = ObjectMapperProvider.json().readValue(erratumJsonString, Errata.class);
        Errata.Details details = erratum.getDetails().get();
        assertEquals(139230L, details.getId());
        assertEquals("RHBA-2041:7158-01", details.getFulladvisory());
        assertEquals("updated q/m container image", details.getSynopsis());
        assertEquals(ErrataStatus.NEW_FILES, details.getStatus());
        assertTrue(details.getBrew());
        assertEquals(2227L, details.getGroupId());
        assertEquals(ZonedDateTime.of(2041, 9, 24, 7, 4, 48, 0, ZoneOffset.UTC).toInstant(), details.getCreatedAt());
        assertEquals("RHBA-2041:139230-01", details.getOldAdvisory());
        assertFalse(details.getTextonly());
        assertTrue(details.getContentTypes().contains("docker"));
        assertEquals(153L, details.getProduct().getId());
        assertEquals("Red Hat build of Quarkus", details.getProduct().getName());
        assertEquals("RHBQ", details.getProduct().getShortName());

        assertEquals(ErrataType.RHBA, erratum.getOriginalType());

        Errata.Content content = erratum.getContent().getContent();
        assertEquals(136810L, content.getId());
        assertEquals("", content.getCve());
        assertEquals(
                "The q/m container image has been updated to address the following security advisory: RHSA-2041:6975 (see References)\n\nUsers of q/m container images are advised to upgrade to these updated images, which contain backported patches to correct these security issues, fix these bugs and add these enhancements. Users of these images are also encouraged to rebuild all container images that depend on these images.\n\nYou can find images updated by this advisory in Red Hat Container Catalog (see References).",
                content.getDescription());
        assertEquals(139230L, content.getErrataId());

        assertEquals(136810L, content.getId());
        assertEquals("", content.getProductVersionText());
        assertEquals("The container image provided by this update can be downloaded.", content.getSolution());
        assertNull(content.getTextOnlyCpe());
        assertEquals("Updated q/m container image is now available.", content.getTopic());
        assertEquals(ZonedDateTime.of(2041, 9, 26, 8, 23, 7, 0, ZoneOffset.UTC).toInstant(), content.getUpdatedAt());
        assertEquals(
                "{\r\n  \"manifest\": {\r\n    \"refs\": [\r\n      {\r\n        \"type\": \"purl\",\r\n        \"uri\": \"pkg:oci/q-m@sha256%3A?os=linux&arch=arm64&tag=13.12345\"\r\n      },\r\n      {\r\n        \"type\": \"purl\",\r\n        \"uri\": \"pkg:oci/q-m@sha256%3A6cf157?os=linux&arch=amd64&tag=13.12345\"\r\n      }\r\n    ]\r\n  }\r\n}",
                content.getNotes());

        Optional<JsonNode> notes = erratum.getNotesMapping();
        assertFalse(notes.isEmpty());
        JsonNode refs = notes.get().get("manifest").get("refs");
        JsonNode firstRef = refs.get(0);
        assertEquals("purl", firstRef.get("type").asText());
        assertEquals("pkg:oci/q-m@sha256%3A?os=linux&arch=arm64&tag=13.12345", firstRef.get("uri").asText());
    }

    @Test
    void testPagedErrataReleaseDTO() throws IOException {
        String pagedVariantsJson = TestResources.asString("errata/api/paged_variants.json");
        ErrataPage<ErrataVariant.VariantData> pagedVariants = ObjectMapperProvider.json()
                .readValue(pagedVariantsJson, new TypeReference<>() {
                });

        assertEquals(1L, pagedVariants.getPage().getPageNumber());
        assertEquals(1L, pagedVariants.getPage().getTotalPages());
        assertEquals(11L, pagedVariants.getPage().getTotalHits());
        assertEquals(11L, pagedVariants.getData().size());
        assertEquals(2990L, pagedVariants.getData().get(0).getId());
        assertEquals("6Server-RHBQ-TextOnly", pagedVariants.getData().get(0).getAttributes().getName());
        assertEquals("cpe:/a:redhat:quarkus:::el6", pagedVariants.getData().get(0).getAttributes().getCpe());
        assertEquals(
                "RHBQ Text-Only",
                pagedVariants.getData().get(0).getAttributes().getRelationships().getProductVersion().getName());
    }

    @Test
    void testErrataBuildListDTO() throws IOException {
        String errataBuildListJson = TestResources.asString("errata/api/build_list.json");
        ErrataBuildList buildList = ObjectMapperProvider.json().readValue(errataBuildListJson, ErrataBuildList.class);

        assertEquals(1, buildList.getProductVersions().size());
        Entry<String, ErrataBuildList.ProductVersionEntry> firstItem = buildList.getProductVersions()
                .entrySet()
                .iterator()
                .next();
        assertEquals("release-e2e-test-1.0-updates-rhel-9", firstItem.getKey());
        assertEquals("release-e2e-test-1.0-updates-rhel-9", firstItem.getValue().getName());
        assertEquals(1, firstItem.getValue().getBuilds().size());
        Entry<String, ErrataBuildList.BuildItem> firstBuild = firstItem.getValue()
                .getBuilds()
                .get(0)
                .getBuildItems()
                .entrySet()
                .iterator()
                .next();
        assertEquals("release-e2e-test-1.0.4928-1.el9", firstBuild.getKey());
        assertEquals(2405700, firstBuild.getValue().getId());
        assertEquals("release-e2e-test-1.0.4928-1.el9", firstBuild.getValue().getNvr());
    }

    @Test
    void testErrataMultipleBuildListDTO() throws IOException {
        String multipleErrataBuildListJson = TestResources.asString("errata/api/multiple_build_list.json");
        ErrataBuildList buildList = ObjectMapperProvider.json()
                .readValue(multipleErrataBuildListJson, ErrataBuildList.class);

        assertEquals(6, buildList.getProductVersions().size());
        assertTrue(
                buildList.getProductVersions()
                        .values()
                        .stream()
                        .map(ProductVersionEntry::getName)
                        .collect(Collectors.toList())
                        .containsAll(
                                List.of(
                                        "RHEL-8.6.0.Z.EUS",
                                        "RHEL-8.8.0.Z.EUS",
                                        "RHEL-9.0.0.Z.EUS",
                                        "RHEL-9.2.0.Z.EUS",
                                        "RHEL-9.3.0.Z.MAIN",
                                        "RHEL-8.9.0.Z.MAIN")));
        Optional<Entry<String, ProductVersionEntry>> firstValue = buildList.getProductVersions()
                .entrySet()
                .stream()
                .filter(pv -> pv.getValue().getName().equals("RHEL-8.6.0.Z.EUS"))
                .findAny();
        assertTrue(firstValue.isPresent());
        Optional<Entry<String, BuildItem>> firstBuildItem = firstValue.get()
                .getValue()
                .getBuilds()
                .stream()
                .flatMap(build -> build.getBuildItems().entrySet().stream())
                .filter(bi -> bi.getKey().equals("sudo-1.9.5p2-1.el8_6"))
                .findAny();
        assertTrue(firstBuildItem.isPresent());
        assertEquals("sudo-1.9.5p2-1.el8_6", firstBuildItem.get().getKey());
        assertEquals(2871088, firstBuildItem.get().getValue().getId());
        assertEquals("sudo-1.9.5p2-1.el8_6", firstBuildItem.get().getValue().getNvr());
        assertEquals(1, firstBuildItem.get().getValue().getVariantArch().size());
        assertTrue(firstBuildItem.get().getValue().getVariantArch().containsKey("BaseOS-8.6.0.Z.EUS"));

        Optional<Entry<String, ProductVersionEntry>> secondValue = buildList.getProductVersions()
                .entrySet()
                .stream()
                .filter(pv -> pv.getValue().getName().equals("RHEL-8.8.0.Z.EUS"))
                .findAny();
        assertTrue(secondValue.isPresent());
        Optional<Entry<String, BuildItem>> secondBuildItem = secondValue.get()
                .getValue()
                .getBuilds()
                .stream()
                .flatMap(build -> build.getBuildItems().entrySet().stream())
                .filter(bi -> bi.getKey().equals("sudo-1.9.5p2-1.el8_8"))
                .findAny();
        assertTrue(secondBuildItem.isPresent());
        assertEquals("sudo-1.9.5p2-1.el8_8", secondBuildItem.get().getKey());
        assertEquals(2870637, secondBuildItem.get().getValue().getId());
        assertEquals("sudo-1.9.5p2-1.el8_8", secondBuildItem.get().getValue().getNvr());
        assertEquals(1, secondBuildItem.get().getValue().getVariantArch().size());
        assertTrue(secondBuildItem.get().getValue().getVariantArch().containsKey("BaseOS-8.8.0.Z.EUS"));
    }

}
