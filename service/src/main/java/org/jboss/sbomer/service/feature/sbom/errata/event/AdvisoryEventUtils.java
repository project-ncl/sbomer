/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.feature.sbom.errata.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1GenerationRecord;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepositoryDetails;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURLBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdvisoryEventUtils {

    /*
     * In case of mainline RHEL releases if the ProductVersion contains a major.minor, augment the CPE. e.g. in case of
     * ProductVersion 'RHEL-9.4.0.Z.EUS', change
     *
     * cpe:/a:redhat:enterprise_linux:9::highavailability -> cpe:/a:redhat:enterprise_linux:9.4::highavailability
     * cpe:/a:redhat:enterprise_linux:9::server -> cpe:/a:redhat:enterprise_linux:9.4::server
     */
    public static List<String> createGranularCPEs(ProductVersionEntry productVersion, Set<String> originalCPEs) {
        String majorMinor = captureMajorMinorVersion(productVersion);
        if (majorMinor == null) {
            return Collections.emptyList();
        }
        String[] majorMinorTokens = majorMinor.split("\\.");

        return originalCPEs.stream().map(originalCPE -> {
            if (originalCPE.contains(":" + majorMinor + ":")) {
                // CPE looks augmented already
                return null;
            }
            if (originalCPE.contains(":" + majorMinorTokens[0] + ":")) {
                return originalCPE.replace(":" + majorMinorTokens[0] + ":", ":" + majorMinor + ":");
            }
            return null;
        }).filter(Objects::nonNull).toList();
    }

    public static void addMissingSerialNumber(Bom bom) {
        if (bom.getSerialNumber() == null || bom.getSerialNumber().isEmpty()) {
            log.debug("Setting 'serialNumber' for manifest with purl '{}'", bom.getMetadata().getComponent().getPurl());

            try {
                String jsonContent = SbomUtils.toJson(bom);
                bom.setSerialNumber("urn:uuid:" + UUID.nameUUIDFromBytes(jsonContent.getBytes(UTF_8)).toString());
            } catch (GeneratorException e) {
                log.warn("Could not generate serialNumber out of the manifest content, setting random UUID");
                bom.setSerialNumber(UUID.randomUUID().toString());
            }
        }
    }

    public static Component.Type getComponentTypeForGeneration(V1Beta1GenerationRecord generation) {
        GenerationRequestType generationRequestType = GenerationRequestType.fromName(generation.type());
        if (GenerationRequestType.CONTAINERIMAGE.equals(generationRequestType)) {
            return Component.Type.CONTAINER;
        }
        return Component.Type.LIBRARY;
    }

    public static Component.Type getComponentTypeForProduct(String productShortName) {
        // Products MUST use the type "operating-system" (in case of RHEL) or "framework" (for all other, non-OS
        // products).
        return productShortName.equals("RHEL") ? Component.Type.OPERATING_SYSTEM : Component.Type.FRAMEWORK;
    }

    public static Hash retrieveHashFromGeneration(V1Beta1GenerationRecord generation) {
        GenerationRequestType generationRequestType = GenerationRequestType.fromName(generation.type());
        if (GenerationRequestType.CONTAINERIMAGE.equals(generationRequestType)) {
            String[] checksumString = (generation.identifier().split("@")[1]).split(":");
            String alg = checksumString[0].replace("sha", "SHA-");
            String checksum = checksumString[1];
            return new Hash(Algorithm.fromSpec(alg), checksum);
        }
        throw new ApplicationException("**** NOT IMPLEMENTED ****");
    }

    public static Set<String> createPurls(
            List<PyxisRepositoryDetails.Repository> repositories,
            Hash hash,
            boolean summaryPurl) {
        return repositories.stream()
                .flatMap(repository -> createPurls(repository, hash, summaryPurl).stream())
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toSet());
    }

    public static List<String> extractPurlUrisFromManifestNode(JsonNode manifestNode) {
        return Optional.ofNullable(manifestNode.path("manifest").path("refs")).filter(JsonNode::isArray).map(refs -> {
            List<String> purlUris = new ArrayList<>();
            refs.forEach(ref -> {
                if ("purl".equals(ref.path("type").asText())) {
                    purlUris.add(ref.path("uri").asText());
                }
            });
            return purlUris;
        }).orElse(Collections.emptyList());
    }

    private static Set<String> createPurls(
            PyxisRepositoryDetails.Repository repository,
            Hash hash,
            boolean summaryPurl) {

        // Extract last fragment from the repository
        String repositoryName = Optional.ofNullable(repository.getRepository()).map(repo -> {
            int lastSlashIndex = repo.lastIndexOf('/');
            return lastSlashIndex != -1 ? repo.substring(lastSlashIndex + 1) : repo;
        }).orElseThrow(() -> new IllegalArgumentException("Repository name is null"));

        if (!summaryPurl) {
            return repository.getTags().stream().map(tag -> {
                try {
                    return PackageURLBuilder.aPackageURL()
                            .withType("oci")
                            .withName(repositoryName)
                            .withVersion(hash.getAlgorithm().replace("SHA-", "sha") + ":" + hash.getValue())
                            .withQualifier(
                                    "repository_url",
                                    repository.getRegistry() + "/" + repository.getRepository())
                            .withQualifier("tag", tag.getName())
                            .build()
                            .toString();
                } catch (MalformedPackageURLException e) {
                    log.warn("Error while creating PURL for tag {}", tag, e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        } else {
            try {
                return Set.of(
                        PackageURLBuilder.aPackageURL()
                                .withType("oci")
                                .withName(repositoryName)
                                .withVersion(hash.getAlgorithm().replace("SHA-", "sha") + ":" + hash.getValue())
                                .build()
                                .toString());
            } catch (MalformedPackageURLException | IllegalArgumentException e) {
                log.warn("Error while creating summary PURL for repository {}", repository, e);
                return null;
            }
        }
    }

    private static String captureMajorMinorVersion(ProductVersionEntry productVersion) {
        Pattern pattern = Pattern.compile("(?:RHEL-)(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(productVersion.getName());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

}
