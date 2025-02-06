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
package org.jboss.sbomer.cli.feature.sbom.adjuster;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.PurlSanitizer;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link Adjuster} for the {@link GeneratorType#IMAGE_SYFT} type.
 *
 * @author Marek Goldmann
 * @see SyftImageConfig
 */
@Slf4j
public class SyftImageAdjuster extends AbstractAdjuster {
    /**
     * <p>
     * For non-RPM content, a list of paths within the container image for which when a component is found it will be
     * retained in the generated manifest. In case the found component is not on a path in this list it will be removed
     * from the manifest.
     * </p>
     * ffi
     *
     * <p>
     * If this is {@code null} or an empty list is provided -- all components will be retained in the manifest.
     * </p>
     */
    List<String> paths = new ArrayList<>();
    /**
     * A flag to determine whether RPM packages should be retained in the generated manifests (value set to
     * {@code true}) or removed (value set to {@code false}).
     */
    boolean includeRpms = true;

    final Path workDir;

    /**
     * <p>
     * List of supported property name prefixes.
     * </p>
     *
     * @see SyftImageAdjuster#adjustProperties(List)
     */
    private static final List<String> ALLOWED_PROPERTY_PREFIXES = List.of(
            "sbomer:package:language",
            "sbomer:package:type",
            "sbomer:location:0:path",
            "sbomer:metadata:virtualPath",
            "sbomer:image:labels");

    public SyftImageAdjuster(Path workDir) {
        this.workDir = workDir;
    }

    public SyftImageAdjuster(Path workDir, List<String> paths, boolean includeRpms) {
        this.workDir = workDir;
        this.paths = paths;
        this.includeRpms = includeRpms;
    }

    /**
     * Checks whether given path is on or under paths specified by the {@link SyftImageAdjuster#paths} list.
     *
     * @param path
     * @return
     */
    private boolean isOnPath(String path) {
        // In case we haven't provided paths to filter, add all found artifacts.
        if (paths == null || paths.isEmpty()) {
            return true;
        }

        return paths.stream().anyMatch(path::startsWith);
    }

    @Override
    public Bom adjust(Bom bom) {
        log.debug(
                "Starting adjustment of the manifest, parameters: configuration paths: [{}], includeRpms: [{}]",
                paths,
                includeRpms);

        // Remove components from manifest according to 'paths' and 'includeRpms' parameters
        log.debug("Filtering out all components that do not meet requirements...");

        adjustEmptyComponents(bom);
        filterComponents(bom.getComponents());
        adjustProperties(bom);
        adjustNameAndPurl(bom, workDir);

        cleanupComponents(bom);

        adjustComponents(bom);

        // Populate dependencies section with components
        adjustDependencies(bom);

        // Adjust the publisher name
        adjustPublisher(bom);

        return bom;
    }

    /**
     * If the bom components are null initialize an empty list
     *
     * @param bom
     */
    private void adjustEmptyComponents(Bom bom) {
        if (bom.getComponents() == null) {
            bom.setComponents(new ArrayList<>());
        }
    }

    /**
     * Removes all components from the component tree that do not meet requirements: as defined by
     * {@link SyftImageAdjuster#includeRpms} and {@link SyftImageAdjuster#paths}.
     *
     * @param components
     * @see SyftImageAdjuster#includeRpms
     * @see SyftImageAdjuster#paths
     */
    private void filterComponents(List<Component> components) {
        if (components == null) {
            return;
        }

        components.removeIf(c -> {
            if (c.getPurl() == null) {
                log.debug(
                        "Component (of type '{}', cpe: '{}') does not have purl assigned, marked for removal",
                        c.getType(),
                        c.getCpe());
                return true;
            }

            if (!SbomUtils.hasValidOrSanitizablePurl(c)) {
                log.debug("Component has a purl ({}) which cannot be made valid!", c.getPurl());
                return true;
            }

            log.debug("Handling component '{}'", c.getPurl());

            // Handle RPMs
            if (c.getPurl().startsWith("pkg:" + PackageURL.StandardTypes.RPM)) {
                // Remove all components that are RPMs if the includeRpms is not set to true
                log.debug("Component is of type RPM, to be removed: {} (includeRpms: {})", c.getPurl(), includeRpms);
                return !includeRpms;
            } else {
                // Handle everything else

                // If paths are not specified, include everything
                if (paths == null || paths.isEmpty()) {
                    log.debug("No paths provided, component won't be removed");
                    return false;
                }

                // Remove all components that are not on the paths we are interested in
                boolean onPath = c.getProperties()
                        .stream()
                        .filter(p -> p.getName().equals("syft:location:0:path") && isOnPath(p.getValue()))
                        .findAny()
                        .isEmpty();

                log.debug("Component on path: {}", onPath);

                return onPath;
            }
        });

        // Go deep
        components.forEach(c -> filterComponents(c.getComponents()));
    }

    /**
     * <p>
     * Ensures that the main component is present in the {@link Bom#getComponents()} list.
     * </p>
     *
     * <p>
     * At the same time it cleans up the main component available in the {@link Metadata#getComponent()}.
     * </p>
     *
     * @param bom
     */
    private void adjustComponents(Bom bom) {
        Component mainComponent = bom.getMetadata().getComponent();

        // Create a new component out of the current main component which will replace it.
        Component metadataComponent = new Component();
        metadataComponent.setType(mainComponent.getType());
        metadataComponent.setName(mainComponent.getName());
        metadataComponent.setPurl(mainComponent.getPurl());

        // Set main component
        bom.getMetadata().setComponent(metadataComponent);

        // Set the main component
        bom.getComponents().add(0, mainComponent);
    }

    /**
     * <p>
     * Adjust properties in the manifest. This includes a few steps.
     * </p>
     *
     * <p>
     * If there are any properties in the metadata section, move these to the main component's properties.
     * </p>
     *
     * <p>
     * Adjusts any properties in the main component as well as for each component found in the component list
     * (recursively). See {@link SyftImageAdjuster#adjustProperties(List)}.
     * </p>
     *
     * @param bom The manifest to adjust the properties of.
     * @see SyftImageAdjuster#adjustProperties(List)
     */
    private void adjustProperties(Bom bom) {
        log.info("Adjusting manifest properties...");

        Component mainComponent = bom.getMetadata().getComponent();

        // Initialize properties for main component, if not done so yet
        if (mainComponent.getProperties() == null) {
            mainComponent.setProperties(new ArrayList<>());
        }

        // If there are properties in the metadata field of the manifest, move these into the main component's
        // properties.
        if (bom.getMetadata().getProperties() != null) {
            log.debug(
                    "Moving '{}' properties from metadata to main component",
                    bom.getMetadata().getProperties().size());

            mainComponent.getProperties().addAll(bom.getMetadata().getProperties());
            bom.getMetadata().setProperties(null);
        }

        // Adjust main component's properties...
        adjustProperties(bom.getMetadata().getComponent().getProperties());
        // ...and any other properties found in the component tree.
        bom.getComponents().forEach(c -> adjustProperties(c.getProperties()));

        log.info("Properties adjusted!");
    }

    /**
     * <p>
     * Adjust publisher name for Red Hat components.
     * </p>
     *
     * <p>
     * If the publisher is set to "Red Hat, Inc.", update it to "Red Hat" for consistency
     * </p>
     *
     * <p>
     * Adjusts any values in the main component as well as for each component found in the component list (recursively).
     * See {@link SyftImageAdjuster#adjustPublisher(Bom)}.
     * </p>
     *
     * @param bom The manifest to adjust the properties of.
     * @see SyftImageAdjuster#adjustPublisher(Bom)
     */
    private void adjustPublisher(Bom bom) {
        log.info("Adjusting manifest publisher...");

        if (bom == null) {
            return;
        }

        // Adjust the publisher for the main component
        Component mainComponent = bom.getMetadata() != null ? bom.getMetadata().getComponent() : null;
        adjustComponentPublisher(mainComponent);

        // Adjust the publisher for all components in the BOM
        if (bom.getComponents() != null) {
            bom.getComponents().forEach(this::adjustComponentPublisher);
        }
    }

    private void adjustComponentPublisher(Component component) {
        if (component == null) {
            return;
        }

        String currentPublisher = component.getPublisher();
        if ("Red Hat, Inc.".equals(currentPublisher)) {
            component.setPublisher(Constants.PUBLISHER);
        }
    }

    /**
     * Based on the metadata we got the output of Skopeo ({@code skopeo.json} file), adjust main component's purl and
     * name.
     *
     * @param bom
     * @param workDir
     */
    private void adjustNameAndPurl(Bom bom, Path workDir) {
        String tag;
        ContainerImageInspectOutput inspectData;
        final Component mainComponent = bom.getMetadata().getComponent();

        try {
            inspectData = ObjectMapperProvider.json()
                    .readValue(
                            Path.of(workDir.toAbsolutePath().toString(), "skopeo.json").toFile(),
                            ContainerImageInspectOutput.class);

        } catch (IOException e) {
            throw new ApplicationException("Could not read 'skopeo inpect' output", e);
        }

        // 7.4.17
        Optional<Property> versionOpt = SbomUtils
                .findPropertyWithNameInComponent("sbomer:image:labels:version", mainComponent);

        if (versionOpt.isEmpty()) {
            throw new ApplicationException(
                    "The 'version' label was not found within the container image, cannot proceed");
        }

        // 5.1717585311
        Optional<Property> releaseOpt = SbomUtils
                .findPropertyWithNameInComponent("sbomer:image:labels:release", mainComponent);

        if (releaseOpt.isEmpty()) {
            throw new ApplicationException(
                    "The 'release' label was not found within the container image, cannot proceed");
        }

        tag = versionOpt.get().getValue() + "-" + releaseOpt.get().getValue();

        String name = inspectData.labels.get("name");
        String[] componentNameParts = mainComponent.getName().split("/");

        if (name == null) {
            if (componentNameParts.length > 1) {
                name = mainComponent.getName().substring(mainComponent.getName().indexOf("/"));
            }
        }

        // FIXME: This can be null here
        String[] nameParts = name.split(("/"));

        TreeMap<String, String> qualifiers = new TreeMap<>();
        qualifiers.put("os", inspectData.getOs());
        qualifiers.put("arch", inspectData.getArchitecture());
        qualifiers.put("tag", tag);

        PackageURL purl;

        try {
            purl = new PackageURL(
                    "oci",
                    null,
                    nameParts[nameParts.length - 1],
                    inspectData.getDigest(),
                    qualifiers,
                    null);
        } catch (MalformedPackageURLException e) {
            throw new ApplicationException("Cannot generate purl for container image", e);
        }

        log.debug("Generated purl: '{}'", purl);

        mainComponent.setPurl(purl);
        mainComponent.setName(name);
    }

    /**
     * <p>
     * Populates the {@link Bom#getDependencies()} with the information we about components have.
     * </p>
     *
     * <p>
     * A main element will be added representing the container image itself with a {@code dependsOn} array populated
     * with the list of all components found in the image.
     * </p>
     *
     * <p>
     * Each component is added to the dependencies section as well with {@code dependsOn} being an emoty array.
     * </p>
     *
     * @param bom
     */
    private void adjustDependencies(Bom bom) {
        List<Dependency> dependencies = new ArrayList<>();

        populateDependencies(dependencies, bom.getComponents());

        // The image itself is the first element
        Dependency productDependency = dependencies.get(0);

        // If there are more dependencies (besides the main image), add all of them
        // as a product dependency
        if (dependencies.size() > 1) {
            for (Component component : bom.getComponents().subList(1, dependencies.size())) {
                productDependency.addDependency(SbomUtils.createDependency(component.getBomRef()));
            }
        }

        bom.setDependencies(dependencies);
    }

    /**
     * <p>
     * Adds a new {@link Dependency} to a list for each {@link Component} in the list.
     * </p>
     *
     * <p>
     * CAse where a component has nested components is handled as well.
     * </p>
     */
    private void populateDependencies(List<Dependency> dependencies, List<Component> components) {
        if (components == null) {
            return;
        }

        components.forEach(component -> {
            dependencies.add(SbomUtils.createDependency(component.getBomRef()));
            populateDependencies(dependencies, component.getComponents());
        });

    }

    /**
     * <p>
     * Updates property names to our rules.
     * </p>
     *
     * <p>
     * Removes any properties which names do not start with allowed prefixes,
     * {@link SyftImageAdjuster#ALLOWED_PROPERTY_PREFIXES}.
     * </p>
     *
     * @param properties
     * @see SyftImageAdjuster#ALLOWED_PROPERTY_PREFIXES
     */
    private void adjustProperties(List<Property> properties) {
        if (properties == null) {
            return;
        }

        // Adjust property names
        properties.forEach(p -> {
            String newName = p.getName().replace("syft:", "sbomer:");

            // log.debug("Adjusting property name from '{}' to '{}'", p.getName(), newName);

            p.setName(newName);
        });

        // Remove properties we don't care about
        properties.removeIf(prop -> {
            boolean supportedProp = ALLOWED_PROPERTY_PREFIXES.stream()
                    .anyMatch(prefix -> prop.getName().startsWith(prefix));

            // if (!supportedProp) {
            // log.debug(
            // "Property '{}' with value '{}' is not on the supported properties list, removing...",
            // prop.getName(),
            // prop.getValue());
            // }

            return !supportedProp;
        });

        properties.stream()
                .filter(
                        property -> "sbomer:image:labels:vendor".equals(property.getName())
                                || "sbomer:image:labels:maintainer".equals(property.getName()))
                .forEach(property -> {
                    if ("Red Hat, Inc.".equals(property.getValue())) {
                        property.setValue(Constants.SUPPLIER_NAME);
                    }
                });

    }

    @Override
    protected void cleanupComponent(Component component) {
        // log.debug("Cleaning up component '{}'", component.getPurl());

        // Remove CPE, we don't use it now
        component.setCpe(null);

        if (component.getProperties() != null) {
            // Adjust purl for Java components
            Property propType = SbomUtils.findPropertyWithNameInComponent("sbomer:package:type", component)
                    .orElse(null);

            if (component.getPurl() != null && propType != null) {
                switch (propType.getValue()) {
                    case "java-archive":
                        // log.debug(
                        // "Adjusting purl for the '{}' Java component by adding '?type=jar' suffix...",
                        // component.getPurl());
                        component.setPurl(component.getPurl() + "?type=jar");
                        break;

                    case "rpm":
                        // log.debug(
                        // "Adjusting purl for the '{}' RPM component by removing all qualifiers besides 'arch' and
                        // 'epoch'...",
                        // component.getPurl());

                        cleanupPurl(component);
                        break;

                    default:
                        break;
                }
            }
        }

        cleanupExternalReferences(component.getExternalReferences());
        log.debug("Component '{}' adjusted", component.getPurl());
    }

    private void cleanupPurl(Component component) {
        try {
            String cleanedUpPurl = doCleanupPurl(component.getPurl());
            component.setPurl(cleanedUpPurl);
        } catch (MalformedPackageURLException e) {
            String sanitizedPurl = PurlSanitizer.sanitizePurl(component.getPurl());
            log.debug("Sanitized purl {} to {}, cleaning up one more time", component.getPurl(), sanitizedPurl);
            component.setPurl(sanitizedPurl);

            try {
                String cleanedUpPurl = doCleanupPurl(component.getPurl());
                component.setPurl(cleanedUpPurl);
            } catch (MalformedPackageURLException e1) {
                log.warn("Could not clean up purl '{}'", component.getPurl(), e);
            }
        }
    }

    private String doCleanupPurl(String purl) throws MalformedPackageURLException {
        PackageURL packageURL = new PackageURL(purl);

        TreeMap<String, String> qualifiers = new TreeMap<>(packageURL.getQualifiers());

        // If we removed any qualifiers, we need to rebuild the purl
        if (qualifiers.entrySet().removeIf(q -> !q.getKey().equals("arch") && !q.getKey().equals("epoch"))) {

            // log.debug("Updating purl to: '{}'", updatedPurl);
            return new PackageURL(
                    packageURL.getType(),
                    packageURL.getNamespace(),
                    packageURL.getName(),
                    packageURL.getVersion(),
                    qualifiers,
                    packageURL.getSubpath()).canonicalize();
        }
        return packageURL.toString();
    }

}
