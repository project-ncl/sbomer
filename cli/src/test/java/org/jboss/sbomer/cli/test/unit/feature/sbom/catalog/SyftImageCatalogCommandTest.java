package org.jboss.sbomer.cli.test.unit.feature.sbom.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.jboss.sbomer.cli.feature.sbom.command.catalog.SyftImageCatalogCommand;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Test;

public class SyftImageCatalogCommandTest {

    static class SyftImageCatalogCommandAlt extends SyftImageCatalogCommand {
        @Override
        public Bom doCatalog(String imageName, String imageDigest, List<Bom> boms) {
            return super.doCatalog(imageName, imageDigest, boms);
        }

        @Override
        public String getSBOMerVersion() {
            return "1.0.0-dev";
        }
    }

    @Test
    void createImageIndex() throws IOException, GeneratorException {

        Bom amd64 = SbomUtils.fromString(TestResources.asString("boms/catalog/amd64.json"));
        Bom arm64 = SbomUtils.fromString(TestResources.asString("boms/catalog/arm64.json"));
        Bom ppc64le = SbomUtils.fromString(TestResources.asString("boms/catalog/ppc64le.json"));
        Bom s390x = SbomUtils.fromString(TestResources.asString("boms/catalog/s390x.json"));
        List<Bom> boms = List.of(amd64, arm64, ppc64le, s390x);

        String imageName = "rh-osbs/ubi9-ubi-micro";
        String imageDigest = "sha256:1c8483e0fda0e990175eb9855a5f15e0910d2038dd397d9e2b357630f0321e6d";

        SyftImageCatalogCommandAlt cataloger = new SyftImageCatalogCommandAlt();
        Bom imageIndex = cataloger.doCatalog(imageName, imageDigest, boms);
        SbomUtils.validate(SbomUtils.toJsonNode(imageIndex));

        String purl = "pkg:oci/ubi9-ubi-micro@sha256%3A1c8483e0fda0e990175eb9855a5f15e0910d2038dd397d9e2b357630f0321e6d";

        assertEquals(1, imageIndex.getComponents().size());
        assertEquals(purl, imageIndex.getMetadata().getComponent().getPurl());
        assertEquals(purl, imageIndex.getComponents().get(0).getPurl());
        assertEquals(4, imageIndex.getComponents().get(0).getPedigree().getVariants().getComponents().size());

        Set<String> variantsBomRefs = imageIndex.getComponents()
                .get(0)
                .getPedigree()
                .getVariants()
                .getComponents()
                .stream()
                .map(Component::getBomRef)
                .collect(Collectors.toSet());

        Set<String> expectedVariantsBomRefs = Set.of(
                "ubi9-micro-container_amd64",
                "ubi9-micro-container_arm64",
                "ubi9-micro-container_ppc64le",
                "ubi9-micro-container_s390x");

        assertEquals(expectedVariantsBomRefs, variantsBomRefs);

        Set<String> variantsPurls = imageIndex.getComponents()
                .get(0)
                .getPedigree()
                .getVariants()
                .getComponents()
                .stream()
                .map(Component::getPurl)
                .collect(Collectors.toSet());

        // Convert expectedRefs to a Set for comparison
        Set<String> expectedVariantsPurls = Set.of(
                "pkg:oci/ubi-micro@sha256%3A213fd2a0116a76eaa274fee20c86eef4dfba9f311784e8fb7d7f5fc38b32f3ef?arch=amd64&os=linux&tag=9.4-6.1716471860",
                "pkg:oci/ubi-micro@sha256%3Ac72c705fe4e9de2e065a817be2fbf1b6406010610532243727fdc3042227c71b?arch=arm64&os=linux&tag=9.4-6.1716471860",
                "pkg:oci/ubi-micro@sha256%3A26f08722139c4da653b870272a192fac700960a3315baa1f79f83a4712a436d4?arch=ppc64le&os=linux&tag=9.4-6.1716471860",
                "pkg:oci/ubi-micro@sha256%3A2c9e70f4174747c6b53d253e879177c52731cc4bdc5fe9c6a2555412d849a952?arch=s390x&os=linux&tag=9.4-6.1716471860");

        assertEquals(expectedVariantsPurls, variantsPurls);

        System.out.println("\n" + SbomUtils.toJson(imageIndex));
    }

}
