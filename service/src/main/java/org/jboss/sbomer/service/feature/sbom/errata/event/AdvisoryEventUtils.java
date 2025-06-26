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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.cyclonedx.model.Component;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1GenerationRecord;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataCDNRepoNormalized;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataCDNContentType;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.RepositoryCoordinates;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdvisoryEventUtils {
    private AdvisoryEventUtils() {
        throw new IllegalStateException("This is a utility class that should not be instantiated");
    }

    /*
     * In the case of mainline RHEL releases, if the ProductVersion contains a major.minor, augment the CPE, e.g., in
     * the case of ProductVersion 'RHEL-9.4.0.Z.EUS', change
     *
     * cpe:/a:redhat:enterprise_linux:9::highavailability -> cpe:/a:redhat:enterprise_linux:9.4::highavailability
     * cpe:/a:redhat:enterprise_linux:9::server -> cpe:/a:redhat:enterprise_linux:9.4::server
     */
    /**
     * In the case of mainline RHEL releases, if the ProductVersion contains a {major}.{minor} pattern, augment the
     * original CPEs with the {minor} value, if not present already.
     *
     * @param productVersion {@link ProductVersionEntry} to analyze
     * @param originalCPEs the set of original CPEs to augment with more granular {minor} value
     * @return The list of CPEs with additional more granular values
     */
    public static List<String> createGranularCPEs(ProductVersionEntry productVersion, Set<String> originalCPEs) {
        String majorMinor = captureMajorMinorVersion(productVersion);
        if (majorMinor == null) {
            return Collections.emptyList();
        }
        String[] majorMinorTokens = majorMinor.split("\\.");

        return originalCPEs.stream().map(originalCPE -> {
            if (originalCPE == null) {
                return null;
            }
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

    /**
     * Returns {@link Component.Type} for the specified generation based on its {@link GenerationRequestType}.
     *
     * @param generation the {@link V1Beta1GenerationRecord}.
     * @return The {@link Component.Type} value.
     */
    public static Component.Type getComponentTypeForGeneration(V1Beta1GenerationRecord generation) {
        GenerationRequestType generationRequestType = GenerationRequestType.fromName(generation.type());
        if (GenerationRequestType.CONTAINERIMAGE.equals(generationRequestType)) {
            return Component.Type.CONTAINER;
        }
        return Component.Type.LIBRARY;
    }

    /**
     * Returns {@link Component.Type} for the specified product short name.
     *
     * @param productShortName the Product short name
     * @return The {@link Component.Type} value.
     */
    public static Component.Type getComponentTypeForProduct(String productShortName) {
        // Products MUST use the type "operating-system" (in case of RHEL) or "framework" (for all other, non-OS
        // products).
        return productShortName.equals("RHEL") ? Component.Type.OPERATING_SYSTEM : Component.Type.FRAMEWORK;
    }

    /**
     * Returns the list of purls from the specific {@link JsonNode} of type "manifest" for text-only advisories
     *
     * @param manifestNode the specific {@link JsonNode} of type "manifest"
     * @return The list of purls as strings.
     */
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

    /**
     * Creates a set of purls from the given list of {@link RepositoryCoordinates} and with a specified version
     *
     * @param repositories the list of {@link RepositoryCoordinates} which contain registry, repository and tag values
     * @param version the version to set up on the purls
     * @param includeRepositoryQualifiers the flag which specifies whether the purls should contain the repository
     *        coordinates as qualifiers
     * @return The list of purls as strings.
     */
    public static Set<String> createPurls(
            List<RepositoryCoordinates> repositories,
            String version,
            boolean includeRepositoryQualifiers) {
        Set<String> set = repositories.stream()
                .map(repository -> createPurl(repository, version, includeRepositoryQualifiers))
                .filter(Objects::nonNull)
                .sorted(
                        Comparator.comparingInt(String::length)
                                .reversed() // longest first
                                .thenComparing(Comparator.naturalOrder()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(set);
    }

    /**
     * Creates a purl from the given {@link RepositoryCoordinates} and with a specified version
     *
     * @param repository the {@link RepositoryCoordinates} which contains registry, repository and tag values
     * @param version the version to set up on the purl
     * @param includeRepositoryQualifiers the flag which specifies whether the purl should contain the repository
     *        coordinates as qualifiers
     * @return The purl as string.
     */
    public static String createPurl(
            RepositoryCoordinates repository,
            String version,
            boolean includeRepositoryQualifiers) {

        PackageURLBuilder builder = PackageURLBuilder.aPackageURL()
                .withType("oci")
                .withName(repository.getRepositoryFragment())
                .withVersion(version);

        if (includeRepositoryQualifiers) {
            builder.withQualifier("repository_url", repository.getRegistry() + "/" + repository.getRepository())
                    .withQualifier("tag", repository.getTag());
        }
        try {
            return builder.build().toString();
        } catch (MalformedPackageURLException | IllegalArgumentException e) {
            log.warn("Error while creating summary PURL for repository {}", repository, e);
            return null;
        }
    }

    /**
     * Creates a set of purls from the starting purl and the list of {@link ErrataCDNRepoNormalized} this build is
     * published to
     *
     * @param purl the purl to modify
     * @param cdns the list of {@link ErrataCDNRepoNormalized} which contain the CDN information
     * @param manifestArches the list of arches found in the purl's manifest to use as a match with
     *        {@link ErrataCDNRepoNormalized}
     * @return The list of purls as strings.
     */
    public static Set<String> createPurls(
            String purl,
            Collection<ErrataCDNRepoNormalized> cdns,
            Set<String> manifestArches) {

        try {
            PackageURL componentPurl = new PackageURL(purl);

            if (componentPurl.getQualifiers() == null) {
                return Collections.emptySet();
            }

            String componentArch = componentPurl.getQualifiers().get("arch");

            if (componentArch == null) {
                return Collections.emptySet();
            }

            if (componentArch.equals("src")) {
                // Select the "-source-rpms" CDN repositories and include all the archs provided
                return cdns.stream()
                        .filter(
                                cdn -> cdn.getContentType().equals(ErrataCDNContentType.SOURCE)
                                        && manifestArches.stream().anyMatch(arch -> cdn.getArchName().equals(arch)))
                        .map(cdn -> rebuildPurl(purl, cdn))
                        .collect(Collectors.toSet());
            } else if (componentArch.equals("noarch")) {
                // Select the "-rpms" CDN repositories (excluding "-source-rpms" and "-debug-rpms") and include all the
                // archs provided
                return cdns.stream()
                        .filter(
                                cdn -> cdn.getContentType().equals(ErrataCDNContentType.BINARY)
                                        && manifestArches.stream().anyMatch(arch -> cdn.getArchName().equals(arch)))
                        .map(cdn -> rebuildPurl(purl, cdn))
                        .collect(Collectors.toSet());
            } else if (componentPurl.getName().endsWith("-debugsource")) {
                // Select the "-source-rpms" CDN repositories and include only the component arch
                return cdns.stream()
                        .filter(
                                cdn -> cdn.getContentType().equals(ErrataCDNContentType.SOURCE)
                                        && cdn.getArchName().equals(componentArch))
                        .map(cdn -> rebuildPurl(purl, cdn))
                        .collect(Collectors.toSet());
            } else if (componentPurl.getName().endsWith("-debuginfo")) {
                // Select the "-debug-rpms" CDN repositories and include only the component arch
                return cdns.stream()
                        .filter(
                                cdn -> cdn.getContentType().equals(ErrataCDNContentType.DEBUGINFO)
                                        && cdn.getArchName().equals(componentArch))
                        .map(cdn -> rebuildPurl(purl, cdn))
                        .collect(Collectors.toSet());
            } else {
                // Select the "-rpms" CDN repositories (excluding -source-rpms and -debug-rpms) and include only the
                // component arch
                return cdns.stream()
                        .filter(
                                cdn -> cdn.getContentType().equals(ErrataCDNContentType.BINARY)
                                        && cdn.getArchName().equals(componentArch))
                        .map(cdn -> rebuildPurl(purl, cdn))
                        .collect(Collectors.toSet());
            }
        } catch (MalformedPackageURLException | IllegalArgumentException e) {
            log.warn("Error while creating evidence PURLs for purl {} with CDNs{}", purl, cdns, e);
            return Collections.emptySet();
        }
    }

    /**
     * Given an input purl, creates a new purl with the same name, namespace, subpath, type, version and qualifiers.
     * Additionally, add the new qualifier "repository_id" with the values provided in the
     * {@link ErrataCDNRepoNormalized}. Finally, rebuilds the purl to make sure it is valid and qualifiers are properly
     * sorted.
     *
     * @param originalPurl the input purl string
     * @param cdn the {@link ErrataCDNRepoNormalized} which contains CDN information
     * @return The new validated purl as string.
     */
    public static String rebuildPurl(String originalPurl, ErrataCDNRepoNormalized cdn) {
        try {

            PackageURL purl = new PackageURL(originalPurl);
            PackageURLBuilder builder = purl.toBuilder();

            // Add the repository_id name
            builder.withQualifier("repository_id", cdn.getCdnName());

            return builder.build().toString();
        } catch (MalformedPackageURLException | IllegalArgumentException e) {
            log.warn("Error while creating summary PURL for cdn {}", cdn, e);
            return null;
        }
    }

    /**
     * Given an input purl, creates a new purl with the same name, namespace, subpath, type, version and qualifiers.
     * Additionally, add new qualifiers "tag" and "repository_url" with the values provided in the
     * {@link RepositoryCoordinates}. Finally, rebuilds the purl to make sure it is valid and qualifiers are properly
     * sorted.
     *
     * @param originalPurl the input purl string
     * @param repository the {@link RepositoryCoordinates} which contains registry, repository and tag values
     * @return The new validated purl as string.
     */
    public static String rebuildPurl(String originalPurl, RepositoryCoordinates repository) {
        try {

            PackageURL purl = new PackageURL(originalPurl);
            PackageURLBuilder builder = purl.toBuilder();

            // Override tag and set repository url
            builder.withQualifier("tag", repository.getTag())
                    .withQualifier("repository_url", repository.getRegistry() + "/" + repository.getRepository());

            return builder.build().toString();
        } catch (MalformedPackageURLException | IllegalArgumentException e) {
            log.warn("Error while creating summary PURL for repository {}", repository, e);
            return null;
        }
    }

    /**
     * Given an input purl, creates a set of new purls with the same name, namespace, subpath, type, version and
     * qualifiers. Additionally, add new qualifiers "tag" and "repository_url" with the values provided in the
     * {@link RepositoryCoordinates}. Finally, rebuilds the purls to make sure they are valid and qualifiers are
     * properly sorted.
     *
     * @param originalPurl the input purl string
     * @param repositories the list of {@link RepositoryCoordinates} which contain registry, repository and tag values
     * @return The new validated set of purls as string.
     */
    public static Set<String> rebuildPurls(String originalPurl, List<RepositoryCoordinates> repositories) {
        return repositories.stream().map(repo -> rebuildPurl(originalPurl, repo)).collect(Collectors.toSet());
    }

    /**
     * Given a list of {@link RepositoryCoordinates} find the preferred one (based on a score which gives the highest
     * value to (repository fragment + tag) the highest length).
     *
     * @param repositories the list of {@link RepositoryCoordinates} which contain registry, repository and tag values
     * @return The preferred {@link RepositoryCoordinates}.
     */
    public static Optional<RepositoryCoordinates> findPreferredRepo(List<RepositoryCoordinates> repositories) {
        return repositories.stream().max(Comparator.comparingInt(RepositoryCoordinates::getScore));
    }

    /**
     * Given a {@link ProductVersionEntry} finds any {major}.{minor} pattern (in case of RHEL product versions) and
     * returns the {minor} value
     *
     * @param productVersion {@link ProductVersionEntry} to analyze
     * @return The {minor} value found if any.
     */
    private static String captureMajorMinorVersion(ProductVersionEntry productVersion) {
        Pattern pattern = Pattern.compile("RHEL-(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(productVersion.getName());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

}
