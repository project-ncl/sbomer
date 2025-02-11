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
package org.jboss.sbomer.cli.feature.sbom.processor;

import com.github.packageurl.PackageURL;
import lombok.extern.slf4j.Slf4j;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.BomReference;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.ExternalReference;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.enums.BuildType;
import org.jboss.sbomer.core.pnc.PncService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_PNC_BUILD_ID;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.createComponent;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.createDependency;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.getExternalReferences;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.setArtifactMetadata;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.setPncBuildMetadata;

/**
 * Class for working around problem with missing NPM dependencies in manifest. For every component produced by a non-NPM
 * PNC build, this class will look up that build's build time NPM dependencies. It will check if these dependencies are
 * already present in the manifest, and if not, it will add them as new components.
 */
@Slf4j
public class WorkaroundMissingNpmDependencies {

    private final PncService pncService;

    // Map Build ID -> list of the build's NPM dependencies
    // We will add these dependencies as new components
    private final Map<String, List<Artifact>> buildsWithNpmDependencies = new HashMap<>();
    // Map Build ID -> components that were built by the build
    // These components will get new dependency components added to them
    private final Map<String, List<Component>> componentsToAddNpmDependencies = new HashMap<>();
    // Map PNC artifact -> the artifact transformed as a new manifest component
    private final Map<Artifact, Component> newComponents = new HashMap<>();

    public WorkaroundMissingNpmDependencies(PncService pncService) {
        this.pncService = pncService;
    }

    public void analyzeBuild(Component component, Build build) {
        if (build.getBuildConfigRevision().getBuildType() == BuildType.NPM) {
            // Not processing pure NPM builds, cyclonedx-nodejs plugin handles them correctly
            return;
        }
        String buildId = build.getId();
        List<Artifact> npmDependencies = buildsWithNpmDependencies
                .computeIfAbsent(buildId, k -> new ArrayList<>(pncService.getNPMDependencies(k)));
        if (npmDependencies.isEmpty()) {
            // No NPM dependencies to add
            return;
        }
        List<Component> components = componentsToAddNpmDependencies.computeIfAbsent(buildId, k -> new ArrayList<>());
        components.add(component);
    }

    public void analyzeComponentsBuild(Component component) {
        List<ExternalReference> externalReferences = getExternalReferences(
                component,
                ExternalReference.Type.BUILD_SYSTEM,
                SBOM_RED_HAT_PNC_BUILD_ID);
        if (externalReferences.isEmpty()) {
            // Not a PNC build
            return;
        }
        if (externalReferences.size() > 1) {
            log.warn(
                    "Component {} has more than one {}/{} external reference",
                    component.getPurl(),
                    ExternalReference.Type.BUILD_SYSTEM,
                    SBOM_RED_HAT_PNC_BUILD_ID);
            return;
        }
        String buildID = parseBuildIdFromURL(externalReferences.get(0).getUrl());
        if (buildID == null) {
            log.warn("Could not parse PNC build ID from url {}", externalReferences.get(0).getUrl());
            return;
        }
        Build build = pncService.getBuild(buildID);
        if (build == null) {
            log.warn("Could not obtain PNC build for build id {}", buildID);
            return;
        }
        analyzeBuild(component, build);
    }

    public void addMissingDependencies(Bom bom) {
        filterOutAlreadyPresentDependencies(bom);
        generateNewComponents(bom);
        addDependencies(bom);
    }

    private void filterOutAlreadyPresentDependencies(Bom bom) {
        if (bom.getComponents() == null) {
            return;
        }
        Set<String> listedPurls = bom.getComponents()
                .stream()
                .map(DefaultProcessor::getPackageURL)
                .map(PackageURL::getCoordinates)
                .collect(Collectors.toSet());

        Iterator<Map.Entry<String, List<Artifact>>> it = buildsWithNpmDependencies.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, List<Artifact>> entry = it.next();
            List<Artifact> missingArtifacts = entry.getValue();
            missingArtifacts.removeIf(a -> listedPurls.contains(a.getPurl()));
            if (missingArtifacts.isEmpty()) {
                it.remove();
                componentsToAddNpmDependencies.remove(entry.getKey());
            }
        }
    }

    private void generateNewComponents(Bom bom) {
        Set<Artifact> artifacts = new HashSet<>();
        buildsWithNpmDependencies.values().forEach(artifacts::addAll);

        for (Artifact artifact : artifacts) {
            Component newComponent = createComponent(artifact, Component.Scope.REQUIRED, Component.Type.LIBRARY);
            setArtifactMetadata(newComponent, artifact, pncService.getApiUrl());
            setPncBuildMetadata(newComponent, artifact.getBuild(), pncService.getApiUrl());
            bom.addComponent(newComponent);
            newComponents.put(artifact, newComponent);
            log.debug("Created new component {} from NPM dependency.", newComponent.getPurl());
        }
    }

    private void addDependencies(Bom bom) {
        Map<String, Dependency> bomDependencies;
        if (bom.getDependencies() == null) {
            bomDependencies = new HashMap<>();
        } else {
            bomDependencies = bom.getDependencies().stream().collect(Collectors.toMap(BomReference::getRef, c -> c));
        }

        for (Map.Entry<String, List<Artifact>> e : buildsWithNpmDependencies.entrySet()) {
            String buildId = e.getKey();
            List<Artifact> npmDependencies = e.getValue();

            for (Component dependant : componentsToAddNpmDependencies.get(buildId)) {
                Dependency bomDependant = bomDependencies
                        .computeIfAbsent(dependant.getBomRef(), ref -> addNewDependency(bom, ref));

                for (Artifact npmDependency : npmDependencies) {
                    String bomRef = newComponents.get(npmDependency).getBomRef();
                    Dependency bomDependency = bomDependencies
                            .computeIfAbsent(bomRef, ref -> addNewDependency(bom, ref));
                    bomDependant.addDependency(bomDependency);
                }
            }
        }
    }

    public static Dependency addNewDependency(Bom bom, String bomRef) {
        Dependency dependency = createDependency(bomRef);
        bom.addDependency(dependency);
        return dependency;
    }

    private static String parseBuildIdFromURL(String buildURL) {
        int lastSlashIndex = buildURL.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            return buildURL.substring(lastSlashIndex + 1);
        } else {
            return null;
        }
    }
}
