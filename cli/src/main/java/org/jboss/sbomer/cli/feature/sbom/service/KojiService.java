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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.redhat.red.build.koji.model.xmlrpc.KojiRpmInfo;
import org.apache.commons.collections4.MultiValuedMap;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.build.finder.core.BuildConfig;
import org.jboss.pnc.build.finder.core.BuildFinder;
import org.jboss.pnc.build.finder.core.BuildFinderListener;
import org.jboss.pnc.build.finder.core.BuildSystem;
import org.jboss.pnc.build.finder.core.BuildSystemInteger;
import org.jboss.pnc.build.finder.core.ChecksumType;
import org.jboss.pnc.build.finder.core.DistributionAnalyzer;
import org.jboss.pnc.build.finder.core.DistributionAnalyzerListener;
import org.jboss.pnc.build.finder.core.LocalFile;
import org.jboss.pnc.build.finder.koji.ClientSession;
import org.jboss.pnc.build.finder.koji.KojiBuild;
import org.jboss.pnc.dto.Artifact;
import org.jboss.sbomer.cli.feature.sbom.utils.buildfinder.FinderStatus;

import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiIdOrName;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * A service to interact with the Koji (Brew) build system.
 */
@Slf4j
@ApplicationScoped
public class KojiService {

    private static final Long MAX_BREW_WAIT_5_MIN = 5 * 60 * 1000L;

    @Inject
    ManagedExecutor executor;

    @Inject
    BuildConfig config;

    @Inject
    ClientSession kojiSession;

    /**
     * Executes analysis of the provided archives identified by URLs, which must be downloadable using HTTP(S). The
     * operation is executed synchronously, but the analysis itself runs several executors in parallel.
     *
     * @param url The URL to retrieve
     * @param distributionAnalyzerListener A listener for events from DistributionAnalyzer
     * @param buildFinderListener A listener for events from Buildfinder
     *
     * @return Results of the analysis if the whole operation was successful.
     * @throws Throwable Thrown in case of any errors during the analysis
     */
    public List<KojiBuild> find(
            String url,
            DistributionAnalyzerListener distributionAnalyzerListener,
            BuildFinderListener buildFinderListener) throws Throwable {

        Future<List<KojiBuild>> finderTask = executor.submit(() -> {
            try {
                return find(URI.create(url).normalize().toURL(), distributionAnalyzerListener, buildFinderListener);
            } catch (KojiClientException | MalformedURLException e) {
                throw new ExecutionException(e);
            }
        });

        try {
            return awaitResults(finderTask);
        } catch (InterruptedException | ExecutionException e) { // NOSONAR We are rethrowing it.
            log.debug("Analysis failed due to {}", e.getMessage(), e);
            throw e.getCause();
        }
    }

    /**
     * @param url url to analyze
     * @param distributionAnalyzerListener A listener for events from DistributionAnalyzer
     * @param buildFinderListener A listener for events from Build Finder
     *
     * @return the list of only the builds built in Brew
     * @throws KojiClientException Thrown in case of exceptions with Koji communication
     */
    private List<KojiBuild> find(
            URL url,
            DistributionAnalyzerListener distributionAnalyzerListener,
            BuildFinderListener buildFinderListener) throws KojiClientException {

        List<String> files = Collections.singletonList(url.toExternalForm());

        log.info("Starting analysis for '{}' with config {}", url.toExternalForm(), config);

        DistributionAnalyzer analyzer = new DistributionAnalyzer(files, config, null);
        analyzer.setListener(distributionAnalyzerListener);

        Map<ChecksumType, MultiValuedMap<String, LocalFile>> checksums;
        try {
            checksums = analyzer.call();
        } catch (IOException e) {
            throw new RuntimeException("Failed to analyze checksums", e);
        }

        BuildFinder buildFinder = new BuildFinder(kojiSession, config, analyzer);
        buildFinder.setListener(buildFinderListener);

        Map<BuildSystemInteger, KojiBuild> builds = buildFinder.call();
        int size = builds.size();
        int numBuilds = size >= 1 ? size - 1 : 0;

        log.debug("Got {} checksum types and {} builds", checksums.size(), numBuilds);

        List<KojiBuild> brewBuilds = builds.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getBuildSystem().equals(BuildSystem.koji))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        log.info("Finished analysis for '{}'", url.toExternalForm());

        return brewBuilds;
    }

    private List<KojiBuild> awaitResults(Future<List<KojiBuild>> finderTask)
            throws InterruptedException, ExecutionException {

        int retry = 1;
        while (!finderTask.isDone() && (retry * 500L) < MAX_BREW_WAIT_5_MIN) {
            try {
                retry++;
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.warn("Sleeping while awaiting results was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        if (finderTask.isDone()) {
            return finderTask.get();
        }
        return Collections.emptyList();
    }

    public BuildConfig getConfig() {
        return config;
    }

    public KojiBuild findBuild(Artifact artifact) {

        if (artifact.getPublicUrl() == null) {
            return null;
        }

        try {
            FinderStatus status = new FinderStatus();
            log.trace("Searching for artifact '{}' in Brew...", artifact.getPublicUrl());
            List<KojiBuild> brewBuilds = find(artifact.getPublicUrl(), status, status);
            if (brewBuilds.size() == 1) {
                log.trace(
                        "Found Brew build with id {} of artifact: '{}'",
                        brewBuilds.get(0).getId(),
                        artifact.getPublicUrl());
                return brewBuilds.get(0);
            } else if (brewBuilds.size() > 1) {
                String brewBuildIds = brewBuilds.stream().map(KojiBuild::getId).collect(Collectors.joining(", "));
                log.warn(
                        "Multiple builds (with ids: {}) where found in Brew of the artifact '{}', picking the first one!",
                        brewBuildIds,
                        artifact.getPublicUrl());
                return brewBuilds.get(0);
            }
        } catch (Throwable e) {
            log.error("Lookup in Brew failed due to {}", e.getMessage() == null ? e.toString() : e.getMessage(), e);
        }
        return null;
    }

    public KojiBuildInfo findBuildByRPM(String nvra) throws KojiClientException {
        if (nvra == null) {
            return null;
        }

        log.debug("Finding Brew build for RPM '{}'...", nvra);

        List<KojiRpmInfo> rpm = kojiSession.getRPM(List.of(new KojiIdOrName(nvra)));

        if (rpm.isEmpty()) {
            log.debug("No results found");
            return null;
        }
        KojiRpmInfo rpmInfo = rpm.get(0);
        // It could happen that the returned RPM info is null.
        if (rpmInfo == null) {
            log.debug("No results found");
            return null;
        }

        if (rpm.size() > 1) {
            log.warn(
                    "Multiple RPMs {} found in Brew, this should not happen. Did Kojiji have a breaking change"
                            + " update? Selected the first one {}",
                    nvra,
                    rpmInfo.getId());
        }

        if (rpmInfo.getBuildId() == null) {
            log.debug("RPM {} does not have assigned build", rpmInfo.getId());
            return null;
        }

        KojiBuildInfo buildInfo = kojiSession.getBuild(rpmInfo.getBuildId());

        log.debug("Found build: '{}'...", buildInfo.getId());
        return buildInfo;
    }

    public KojiBuildInfo findBuild(int id) throws KojiClientException {
        log.debug("Retrieving Brew build with id '{}'...", id);

        KojiBuildInfo build = kojiSession.getBuild(id);

        if (build == null) {
            log.warn("Build with id {} not found", id);
            return null;
        }

        return build;
    }

    public KojiBuildInfo findBuild(String nvr) throws KojiClientException {
        if (nvr == null) {
            return null;
        }

        log.debug("Finding Brew build for NVR '{}'...", nvr);

        List<KojiBuildInfo> builds = kojiSession.getBuild(List.of(KojiIdOrName.getFor(nvr)));

        if (builds.isEmpty()) {
            log.debug("No results found");
            return null;
        }

        if (builds.size() == 1) {
            KojiBuildInfo buildInfo = builds.get(0);

            // It could happen that the returned build info is null.
            if (buildInfo == null) {
                log.debug("No results found");
                return null;
            }

            log.debug("Found a single build: '{}'...", buildInfo.getId());
            return buildInfo;
        }

        log.warn("Found more than one build for NVR '{}', this is unexpected, returning nothing!", nvr);
        log.debug(builds.toString());

        return null;
    }
}
