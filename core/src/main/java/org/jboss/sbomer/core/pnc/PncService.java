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
package org.jboss.sbomer.core.pnc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.Configuration.ConfigurationBuilder;
import org.jboss.pnc.client.DeliverableAnalyzerReportClient;
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.client.OperationClient;
import org.jboss.pnc.client.ProductMilestoneClient;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.client.RemoteResourceNotFoundException;
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
import org.jboss.pnc.dto.requests.DeliverablesAnalysisRequest;
import org.jboss.pnc.dto.response.AnalyzedArtifact;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A service to interact with the PNC build system.
 */
@Slf4j
public class PncService {

    @Getter
    final String apiUrl;

    final ArtifactClient artifactClient;

    final BuildClient buildClient;

    final BuildConfigurationClient buildConfigurationClient;

    final GroupConfigurationClient groupConfigurationClient;

    final OperationClient operationClient;

    final ProductMilestoneClient productMilestoneClient;

    final ProductVersionClient productVersionClient;

    final DeliverableAnalyzerReportClient deliverableAnalyzerReportClient;

    public PncService(String apiUrl) {
        this.apiUrl = apiUrl;

        artifactClient = new ArtifactClient(getConfiguration());
        buildClient = new BuildClient(getConfiguration());
        buildConfigurationClient = new BuildConfigurationClient(getConfiguration());
        groupConfigurationClient = new GroupConfigurationClient(getConfiguration());
        operationClient = new OperationClient(getConfiguration());
        productMilestoneClient = new ProductMilestoneClient(getConfiguration());
        productVersionClient = new ProductVersionClient(getConfiguration());
        deliverableAnalyzerReportClient = new DeliverableAnalyzerReportClient(getConfiguration());
    }

    public void close() {
        artifactClient.close();
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
            throw new ClientException("Build could not be retrieved because PNC responded with an error", ex);
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
            throw new ClientException("BuildConfig could not be retrieved because PNC responded with an error", ex);
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
            throw new ClientException(
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
            throw new ClientException(
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
            throw new ClientException("ProductVersion could not be retrieved because PNC responded with an error", ex);
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
            throw new ClientException(
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
        if (groupConfigs == null || groupConfigs.isEmpty()) {
            log.warn(
                    "BuildConfig does not have any Group COnfiguration, unable to proceed without product version, interrupting processing");
            return Collections.emptyList();
        }

        // In case there are some group configs, let's iterate over these and find out whether these are related to
        // a product version
        groupConfigs.values().forEach(groupConfigRef -> {
            GroupConfiguration groupConfig = getGroupConfig(groupConfigRef.getId());

            if (groupConfig == null) {
                log.warn(
                        "GroupConfiguration '{}' could not be found, this is unexpected, but ignoring",
                        groupConfigRef.getId());
                return;
            }

            productVersions.add(groupConfig.getProductVersion());

        });

        log.debug("ProductVersions: {}", productVersions);

        return productVersions;
    }

    public Artifact getArtifact(String purl, Optional<String> sha256, Optional<String> sha1, Optional<String> md5) {
        if (purl == null && sha256.orElse(null) == null && sha1.orElse(null) == null && md5.orElse(null) == null) {
            log.debug("No meaningful values provided for searching an artifact, returning nothing");

            return null;
        }

        List<String> query = new ArrayList<>();

        // Purl was provided, so let's use it, hashes will be used to filter out results, if there are multiple
        // artifacts returned
        if (purl != null) {
            // We need to make a small tweak to find the NPM purls because PNC does not like the % in the purl
            if (purl.startsWith("pkg:npm/%40redhat/")) {
                query.add("purl=like=\"" + purl.replace("pkg:npm/%40redhat/", "pkg:npm/?40redhat/") + "\"");
            } else {
                query.add("purl==\"" + purl + "\"");
            }
        }

        sha256.ifPresent(s -> query.add("sha256==" + s));

        sha1.ifPresent(s -> query.add("sha1==" + s));

        md5.ifPresent(s -> query.add("md5==" + s));

        // If query was not provided and no hashes are provided either
        if (query.isEmpty()) {
            log.debug("No query provided and not hashes available either, impossible to query for artifact");

            return null;
        }

        String rsql = String.join(",", query);

        log.debug("Using following rsql query to search for an artifact: '{}'", rsql);

        RemoteCollection<Artifact> remoteArtifacts;

        try {
            remoteArtifacts = artifactClient.getAll(null, null, null, Optional.empty(), Optional.of(rsql));

        } catch (RemoteResourceException ex) {
            throw new ClientException("Querying artifact failed, PNC responded with an error, query: '{}'", rsql, ex);
        }

        if (remoteArtifacts.size() == 0) {
            log.debug("No artifact found, returning nothing");
            return null;
        }

        if (remoteArtifacts.size() == 1) {
            log.debug("Single artifact found, returning it!");
            return remoteArtifacts.iterator().next();
        }

        // First try to select artifacts with an associated build
        Collection<Artifact> allArtifacts = remoteArtifacts.getAll();
        Optional<Artifact> artifact = allArtifacts.stream().filter(a -> a.getBuild() != null).findFirst();
        if (artifact.isPresent()) {
            log.debug("Found {} results, returning the artifact associated with a build", remoteArtifacts.size());
            return artifact.get();
        }
        // If no artifact has a build, return the newest one
        log.debug("Found {} results, returning newest one", remoteArtifacts.size());
        return allArtifacts.stream().skip(allArtifacts.size() - 1L).findFirst().get();
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
            throw new ClientException(
                    "Analyzed Artifacts for the DeliverableAnalyzerReport '{}' could not be retrieved because PNC responded with an error",
                    reportId,
                    ex);
        }
    }

    /**
     * Triggers a new Deliverable Analysis operation in PNC for the specified milestone and urls.
     *
     * @param milestoneId The milestone identifier for the new deliverable analysis operation
     * @param deliverableUrls The list of deliverable URLs to be analyzed
     * @return The {@link DeliverableAnalyzerOperation} object identifying the new operation
     */
    public DeliverableAnalyzerOperation analyzeDeliverables(String milestoneId, List<String> deliverableUrls) {
        DeliverablesAnalysisRequest request = DeliverablesAnalysisRequest.builder()
                .deliverablesUrls(deliverableUrls)
                .runAsScratchAnalysis(false)
                .build();
        try {
            log.info(
                    "Triggering new deliverable analysis operation for the milestone {} and urls '{}'",
                    milestoneId,
                    deliverableUrls);
            return productMilestoneClient.analyzeDeliverables(milestoneId, request);
        } catch (RemoteResourceException ex) {
            throw new ClientException(
                    "A Deliverable Analysis Operation could not be started because PNC responded with an error",
                    ex);
        }
    }

    /**
     * <p>
     * Fetch NPM dependencies of a PNC {@link Build} identified by the particular {@code buildId}.
     * </p>
     *
     * @param buildID Tbe {@link Build} identifier in PNC
     * @return The collection of {@link Artifact} objects or {@code null} in case the {@link Build} could not be found.
     */
    public Collection<Artifact> getNPMDependencies(String buildID) {
        log.debug("Fetching NPM Dependencies from PNC build with id '{}'", buildID);
        try {
            return buildClient.getDependencyArtifacts(buildID, Optional.empty(), Optional.of("purl=LIKE=pkg:npm*"))
                    .getAll();
        } catch (RemoteResourceException ex) {
            throw new ClientException("Dependencies could not be retrieved because PNC responded with an error", ex);
        }
    }
}
