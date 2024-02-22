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
package org.jboss.sbomer.cli.feature.sbom.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.Configuration.ConfigurationBuilder;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.DeliverableAnalyzerOperation;
import org.jboss.pnc.dto.DeliverableAnalyzerReport;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.GroupConfigurationRef;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.sbomer.cli.errors.pnc.GeneralPncException;
import org.jboss.pnc.dto.response.AnalyzedArtifact;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.RemoteResourceException;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.RemoteResourceNotFoundException;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.client.BuildClient;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.client.BuildConfigurationClient;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.client.DeliverableAnalyzerReportClient;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.client.GroupConfigurationClient;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.client.OperationClient;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.client.ProductMilestoneClient;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.client.ProductVersionClient;
import org.jboss.sbomer.core.errors.ApplicationException;

import io.quarkus.oidc.client.Tokens;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A service to interact with the PNC build system.
 */
@Slf4j
@ApplicationScoped
public class PncService {

    @ConfigProperty(name = "sbomer.pnc.host")
    @Getter
    String apiUrl;

    @ConfigProperty(name = "quarkus.oidc-client.client-enabled")
    boolean oidcClientEnabled;

    BuildClient buildClient;

    BuildConfigurationClient buildConfigurationClient;

    GroupConfigurationClient groupConfigurationClient;

    OperationClient operationClient;

    ProductMilestoneClient productMilestoneClient;

    ProductVersionClient productVersionClient;

    DeliverableAnalyzerReportClient deliverableAnalyzerReportClient;

    @PostConstruct
    void init() {
        buildClient = new BuildClient(getConfiguration());
        buildConfigurationClient = new BuildConfigurationClient(getConfiguration());
        groupConfigurationClient = new GroupConfigurationClient(getConfiguration());
        operationClient = new OperationClient(getConfiguration());
        productMilestoneClient = new ProductMilestoneClient(getConfiguration());
        productVersionClient = new ProductVersionClient(getConfiguration());
        deliverableAnalyzerReportClient = new DeliverableAnalyzerReportClient(getConfiguration());
    }

    @PreDestroy
    void cleanup() {
        buildClient.close();
        buildConfigurationClient.close();
        groupConfigurationClient.close();
        operationClient.close();
        productMilestoneClient.close();
        productVersionClient.close();
        deliverableAnalyzerReportClient.close();
    }

    /**
     * Setup basic configuration to be able to talk to PNC.
     *
     *
     * @return
     */
    public Configuration getConfiguration() {

        ConfigurationBuilder configurationBuilder = Configuration.builder().host(apiUrl).protocol("http");

        if (oidcClientEnabled) {
            Tokens serviceTokens = CDI.current().select(Tokens.class).get();
            configurationBuilder.bearerTokenSupplier(() -> serviceTokens.getAccessToken());
        }

        return configurationBuilder.build();
    }

    /**
     * <p>
     * Fetch information about the PNC {@link Build} identified by the particular {@code buildId}.
     * </p>
     *
     * <p>
     * In case the {@link Build} with provided identifier cannot be found {@code null} is returned.
     * </p>
     *
     * @param buildId Tbe {@link Build} identifier in PNC
     * @return The {@link Build} object or {@code null} in case the {@link Build} could not be found.
     */
    public Build getBuild(String buildId) {
        log.debug("Fetching Build from PNC with id '{}'", buildId);
        try {
            return buildClient.getSpecific(buildId);
        } catch (RemoteResourceNotFoundException ex) {
            log.warn("Build with id '{}' was not found in PNC", buildId);
            return null;
        } catch (RemoteResourceException ex) {
            throw new GeneralPncException("Build could not be retrieved because PNC responded with an error", ex);
        }
    }

    /**
     * <p>
     * Fetch information about the PNC {@link BuildConfiguration} identified by the particular {@code buildConfigId}.
     * </p>
     *
     * <p>
     * In case the {@link BuildConfiguration} with provided identifier cannot be found {@code null} is returned.
     * </p>
     *
     * @param buildConfigId The {@link BuildConfiguration} identifier in PNC
     * @return The {@link BuildConfiguration} object or {@code null} in case the {@link BuildConfiguration} could not be
     *         found.
     */
    public BuildConfiguration getBuildConfig(String buildConfigId) {
        log.debug("Fetching BuildConfiguration from PNC with id '{}'", buildConfigId);
        try {
            return buildConfigurationClient.getSpecific(buildConfigId);
        } catch (RemoteResourceNotFoundException ex) {
            log.warn("BuildConfig with id '{}' was not found in PNC", buildConfigId);
            return null;
        } catch (RemoteResourceException ex) {
            throw new GeneralPncException("BuildConfig could not be retrieved because PNC responded with an error", ex);
        }
    }

    /**
     * <p>
     * Fetch information about the PNC {@link GroupConfiguration} identified by the particular {@code groupConfigId}.
     * </p>
     *
     * <p>
     * In case the {@link GroupConfiguration} with provided identifier cannot be found {@code null} is returned.
     * </p>
     *
     * @param groupConfigId The {@link GroupConfiguration} identifier in PNC
     * @return The {@link GroupConfiguration} object or {@code null} in case the {@link GroupConfiguration} could not be
     *         found.
     */
    public GroupConfiguration getGroupConfig(String groupConfigId) {
        log.debug("Fetching GroupConfiguration from PNC with id '{}'", groupConfigId);
        try {
            return groupConfigurationClient.getSpecific(groupConfigId);
        } catch (RemoteResourceNotFoundException ex) {
            log.warn("GroupConfiguration with id '{}' was not found in PNC", groupConfigId);
            return null;
        } catch (RemoteResourceException ex) {
            throw new GeneralPncException(
                    "GroupConfiguration could not be retrieved because PNC responded with an error",
                    ex);
        }
    }

    /**
     * <p>
     * Fetch information about the PNC {@link DeliverableAnalyzerOperation} identified by the particular
     * {@code operationId}.
     * </p>
     *
     * <p>
     * In case the {@link DeliverableAnalyzerOperation} with provided identifier cannot be found {@code null} is
     * returned.
     * </p>
     *
     * @param operationId The {@link DeliverableAnalyzerOperation} identifier in PNC
     * @return The {@link DeliverableAnalyzerOperation} object or {@code null} in case the
     *         {@link DeliverableAnalyzerOperation} could not be found.
     */
    public DeliverableAnalyzerOperation getDeliverableAnalyzerOperation(String operationId) {
        log.debug("Fetching DeliverableAnalyzerOperation from PNC with id '{}'", operationId);
        try {
            return operationClient.getSpecificDeliverableAnalyzer(operationId);
        } catch (RemoteResourceNotFoundException ex) {
            log.warn("DeliverableAnalyzerOperation with id '{}' was not found in PNC", operationId);
            return null;
        } catch (RemoteResourceException ex) {
            throw new ApplicationException(
                    "DeliverableAnalyzerOperation could not be retrieved because PNC responded with an error",
                    ex);
        }
    }

    /**
     * <p>
     * Fetch information about the PNC {@link ProductVersion} identified by the particular {@code productVersionId}.
     * </p>
     *
     * <p>
     * In case the {@link ProductVersion} with provided identifier cannot be found {@code null} is returned.
     * </p>
     *
     * @param productVersionId The {@link ProductVersion} identifier in PNC
     * @return The {@link ProductVersion} object or {@code null} in case the {@link ProductVersion} could not be found.
     */
    public ProductVersion getProductVersion(String productVersionId) {
        log.debug("Fetching ProductVersion from PNC with id '{}'", productVersionId);
        try {
            return productVersionClient.getSpecific(productVersionId);
        } catch (RemoteResourceNotFoundException ex) {
            log.warn("ProductVersion with id '{}' was not found in PNC", productVersionId);
            return null;
        } catch (RemoteResourceException ex) {
            throw new ApplicationException(
                    "ProductVersion could not be retrieved because PNC responded with an error",
                    ex);
        }
    }

    /**
     * <p>
     * Fetch information about the PNC {@link ProductMilestone} identified by the particular {@code milestoneId}.
     * </p>
     *
     * <p>
     * In case the {@link ProductMilestone} with provided identifier cannot be found {@code null} is returned.
     * </p>
     *
     * @param milestoneId The {@link ProductMilestone} identifier in PNC
     * @return The {@link ProductMilestone} object or {@code null} in case the {@link ProductMilestone} could not be
     *         found.
     */
    public ProductMilestone getMilestone(String milestoneId) {
        log.debug("Fetching ProductMilestone from PNC with id '{}'", milestoneId);
        try {
            return productMilestoneClient.getSpecific(milestoneId);
        } catch (RemoteResourceNotFoundException ex) {
            log.warn("ProductMilestone with id '{}' was not found in PNC", milestoneId);
            return null;
        } catch (RemoteResourceException ex) {
            throw new ApplicationException(
                    "ProductMilestone could not be retrieved because PNC responded with an error",
                    ex);
        }
    }

    /**
     * <p>
     * Obtains the {@link ProductVersionRef} for a given PNC {@link Build} identifier.
     * <p>
     *
     * @param buildId The {@link Build} identifier to get the Product Version for.
     * @return A list with {@link ProductVersionRef} objects for the given Build or empty list in case it is not
     *         possible to obtain any product version.
     */
    public List<ProductVersionRef> getProductVersions(String buildId) {
        List<ProductVersionRef> productVersions = new ArrayList<>();

        log.debug("Fetching Product Version information from PNC for build '{}'", buildId);

        if (buildId == null) {
            log.warn("No PNC Build ID provided, interrupting processing");
            return Collections.emptyList();
        }

        Build build = getBuild(buildId);

        if (build == null) {
            log.warn("Build with ID '{}' could not be found in PNC, interrupting processing", buildId);
            return Collections.emptyList();
        }

        log.debug("Build: {}", build);

        BuildConfiguration buildConfig = getBuildConfig(build.getBuildConfigRevision().getId());

        if (buildConfig == null) {
            log.warn("BuildConfig related to Build '{}' could not be found in PNC, interrupting processing", buildId);
            return Collections.emptyList();
        }

        log.debug("BuildConfig: {}", buildConfig);

        ProductVersionRef productVersion = buildConfig.getProductVersion();

        // Product version was found, let's use it!
        if (productVersion != null) {
            return List.of(productVersion);
        }

        // So, the product version is not assigned to the build configuration. We need to check whether this build
        // config is part of a group config which has the product versions assigned.

        log.warn(
                "BuildConfig '{}' does not provide product version information, trying to obtain product version from Group Configurations",
                buildConfig.getId());

        Map<String, GroupConfigurationRef> groupConfigs = buildConfig.getGroupConfigs();

        // It looks that there are no group configs, nothing to do then!
        if (groupConfigs == null || groupConfigs.size() == 0) {
            log.warn(
                    "BuildConfig does not have any Group COnfiguration, unable to proceed without product version, interrupting processing");
            return Collections.emptyList();
        }

        // In case there are some group configs, let's iterate over these and find out whether these are related to
        // a product version
        groupConfigs.values().forEach(groupConfigRef -> {
            GroupConfiguration groupConfig = getGroupConfig(groupConfigRef.getId());

            if (groupConfig == null) {
                log.warn("GroupConfiguration '{}' could not be found, this is unexpected, but ignoring");
                return;
            }

            productVersions.add(groupConfig.getProductVersion());

        });

        log.debug("ProductVersions: {}", productVersions);

        return productVersions;
    }

    /**
     * Fetch information about the PNC {@link Artifact} identified by the particular purl and sha256 if available.
     *
     * @param sha256
     * @param purl
     * @return The {@link Artifact} object or {@code null} if it cannot be found.
     */
    public Artifact getArtifact(String buildId, String purl, Optional<String> sha256) {
        log.debug(
                "Fetching artifact from PNC for purl '{}' and sha256 '{}'",
                purl,
                sha256.isPresent() ? sha256.get() : null);

        try {
            // Fetch all artifacts via purl (always available)
            String artifactQuery = "purl==\"" + purl + "\"";
            // We need to make a small tweak to find the NPM purls because PNC does not like the % in the purl
            if (purl.startsWith("pkg:npm/%40redhat/")) {
                artifactQuery = "purl=like=\"" + purl.replace("pkg:npm/%40redhat/", "pkg:npm/?40redhat/") + "\"";
            }
            RemoteCollection<Artifact> artifacts = buildClient
                    .getDependencyArtifacts(buildId, Optional.empty(), Optional.of(artifactQuery));
            if (artifacts.size() == 0) {
                // If the artifact is not a dependency of the provided buildId, it might be a built artifact from the
                // buildId
                artifacts = buildClient.getBuiltArtifacts(buildId, Optional.empty(), Optional.of(artifactQuery));
            }

            if (artifacts.size() == 0) {
                log.debug("Artifact with purl '{}' was not found in PNC", purl);
                return null;
            } else if (artifacts.size() > 1) {
                // In case of multiple matches by purl, if no sha256 is provided, return error
                if (sha256.isEmpty()) {
                    throw new IllegalStateException(
                            "No sha256 was provided, and there should exist only one artifact with purl " + purl);
                }
                // In case of provided sha256, filter all artifacts
                List<Artifact> filteredArtifacts = artifacts.getAll()
                        .stream()
                        .peek(
                                a -> log.info(
                                        "Filtering the retrieved artifact having purl: '{}', id: {}, sha256: '{}' by the SBOM detected sha256: '{}'",
                                        a.getPurl(),
                                        a.getId(),
                                        a.getSha256(),
                                        sha256.get()))
                        .filter(a -> a.getSha256().equals(sha256.get()))
                        .peek(a -> log.info("Artifact with id: {} matched.", a.getId()))
                        .collect(Collectors.toList());

                if (filteredArtifacts.size() == 0) {
                    throw new IllegalStateException(
                            "No matching artifact found with purl " + purl + " and sha256 " + sha256.get());
                }
                // Return first one matching (to handle duplicates in PNC)
                log.info("Returning the first matching artifact with id: {}", filteredArtifacts.get(0).getId());
                return filteredArtifacts.get(0);
            }
            // Only one matching artifact was found, all good
            return artifacts.iterator().next();
        } catch (RemoteResourceNotFoundException ex) {
            throw new ApplicationException("Artifact with purl '{}' was not found in PNC", purl, ex);
        } catch (RemoteResourceException ex) {
            throw new ApplicationException(
                    "Artifact with purl '{}' could not be retrieved because PNC responded with an error",
                    purl,
                    ex);
        }
    }

    /**
     * Fetch all the {@link AnalyzedArtifact} which have been analyzed in a {@link DeliverableAnalyzerReport} .
     *
     * @param reportId
     * @return The {@link AnalyzedArtifact}s
     */
    public List<AnalyzedArtifact> getAllAnalyzedArtifacts(String reportId) {
        log.debug("Fetching analyzed artifacts from PNC for DeliverableAnalyzerReport '{}'", reportId);

        try {
            RemoteCollection<AnalyzedArtifact> analyzedArtifact = deliverableAnalyzerReportClient
                    .getAnalyzedArtifacts(reportId);
            return analyzedArtifact.getAll().stream().collect(Collectors.toList());
        } catch (RemoteResourceNotFoundException ex) {
            throw new ApplicationException(
                    "Analyzed Artifacts for the DeliverableAnalyzerReport '{}' were not found in PNC",
                    reportId,
                    ex);
        } catch (RemoteResourceException ex) {
            throw new ApplicationException(
                    "Analyzed Artifacts for the DeliverableAnalyzerReport '{}' could not be retrieved because PNC responded with an error",
                    reportId,
                    ex);
        }
    }

}
