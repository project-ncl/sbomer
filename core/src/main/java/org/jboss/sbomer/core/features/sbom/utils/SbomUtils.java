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
package org.jboss.sbomer.core.features.sbom.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_NAME;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VARIANT;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VERSION;
import static org.jboss.sbomer.core.features.sbom.Constants.PUBLISHER;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOMER_GIT_URL;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOMER_LICENSE_ID;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOMER_NAME;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOMER_WEBSITE;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_BREW_BUILD_ID;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_PNC_ARTIFACT_ID;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_PNC_BUILD_ID;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_PNC_OPERATION_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Commit;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Scope;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.component.evidence.Identity;
import org.cyclonedx.model.component.evidence.Identity.Field;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.Pedigree;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Tool;
import org.cyclonedx.model.metadata.ToolInformation;
import org.cyclonedx.parsers.JsonParser;
import org.jboss.pnc.common.Strings;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.DeliverableAnalyzerOperation;
import org.jboss.pnc.enums.BuildType;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.RedHatProductProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;

public class SbomUtils {

    private SbomUtils() {
        // This is a utility class
    }

    private static final Logger log = LoggerFactory.getLogger(SbomUtils.class);
    private static Pattern gitProtocolPattern = Pattern.compile("git@(.+):(.+)", Pattern.CASE_INSENSITIVE);

    public static Version schemaVersion() {
        return Version.VERSION_16;
    }

    public static Bom createBom() {
        try {
            BomJsonGenerator generator = BomGeneratorFactory.createJson(schemaVersion(), new Bom());
            return new JsonParser().parse(generator.toJsonString().getBytes());
        } catch (ParseException | GeneratorException e) {
            log.error("Unable to create a new Bom", e);
            return null;
        }
    }

    public static Component createComponent(Component component) {
        return createComponent(
                component.getGroup(),
                component.getName(),
                component.getVersion(),
                component.getDescription(),
                component.getPurl(),
                component.getType());
    }

    public static Component createComponent(
            String group,
            String name,
            String version,
            String description,
            String purl,
            Type type) {

        Component component = new Component();

        if (!Strings.isEmpty(group)) {
            component.setGroup(group);
        }
        if (!Strings.isEmpty(name)) {
            component.setName(name);
        }
        if (!Strings.isEmpty(version)) {
            component.setVersion(version);
        }
        if (!Strings.isEmpty(purl)) {
            component.setBomRef(purl);
            component.setPurl(purl);
        }
        if (type != null) {
            component.setType(type);
        }
        if (!Strings.isEmpty(description)) {
            component.setDescription(description);
        }
        return component;
    }

    private static void setCoordinates(Component component, String identifier, BuildType buildType) {

        switch (buildType) {
            case NPM:
                String[] gv = identifier.split(":");
                if (gv.length >= 1) {
                    component.setGroup(gv[0]);
                    component.setVersion(gv[1]);
                }
                break;
            case MVN:
            case GRADLE:
            case SBT:
            default:
                String[] gaecv = identifier.split(":");

                if (gaecv.length >= 3) {
                    component.setGroup(gaecv[0]);
                    component.setName(gaecv[1]);
                    component.setVersion(gaecv[3]);
                }
        }
    }

    public static Component setPncBuildMetadata(Component component, Build pncBuild, String pncApiUrl) {
        if (pncBuild == null) {
            return component;
        }

        addExternalReference(
                component,
                ExternalReference.Type.BUILD_SYSTEM,
                "https://" + pncApiUrl + "/pnc-rest/v2/builds/" + pncBuild.getId().toString(),
                SBOM_RED_HAT_PNC_BUILD_ID);

        addExternalReference(
                component,
                ExternalReference.Type.BUILD_META,
                pncBuild.getEnvironment().getSystemImageRepositoryUrl() + "/"
                        + pncBuild.getEnvironment().getSystemImageId(),
                SBOM_RED_HAT_ENVIRONMENT_IMAGE);

        if (!hasExternalReference(component, ExternalReference.Type.VCS)) {
            addExternalReference(
                    component,
                    ExternalReference.Type.VCS,
                    pncBuild.getScmRepository().getExternalUrl(),
                    "");
        }

        addPedigreeCommit(component, pncBuild.getScmUrl() + "#" + pncBuild.getScmTag(), pncBuild.getScmRevision());

        // If the SCM repository is not internal and a commitID was computed, add the pedigree.
        if (!Strings.isEmpty(pncBuild.getScmRepository().getExternalUrl())
                && pncBuild.getScmBuildConfigRevisionInternal() != null && !pncBuild.getScmBuildConfigRevisionInternal()
                && pncBuild.getScmBuildConfigRevision() != null) {

            addPedigreeCommit(
                    component,
                    pncBuild.getScmRepository().getExternalUrl() + "#"
                            + pncBuild.getBuildConfigRevision().getScmRevision(),
                    pncBuild.getScmBuildConfigRevision());
        }

        return component;
    }

    public static Component setArtifactMetadata(Component component, Artifact artifact, String pncApiUrl) {
        if (artifact == null) {
            return component;
        }

        addExternalReference(
                component,
                ExternalReference.Type.BUILD_SYSTEM,
                "https://" + pncApiUrl + "/pnc-rest/v2/artifacts/" + artifact.getId().toString(),
                SBOM_RED_HAT_PNC_ARTIFACT_ID);
        return component;
    }

    public static Component setPncOperationMetadata(
            Component component,
            DeliverableAnalyzerOperation operation,
            String pncApiUrl) {
        if (operation != null) {

            addExternalReference(
                    component,
                    ExternalReference.Type.BUILD_SYSTEM,
                    "https://" + pncApiUrl + "/pnc-rest/v2/operations/deliverable-analyzer/"
                            + operation.getId().toString(),
                    SBOM_RED_HAT_PNC_OPERATION_ID);
        }

        return component;
    }

    public static Component setBrewBuildMetadata(
            Component component,
            String brewBuildId,
            Optional<String> source,
            String kojiApiUrl) {
        if (brewBuildId != null) {

            addExternalReference(
                    component,
                    ExternalReference.Type.BUILD_SYSTEM,
                    kojiApiUrl + "/buildinfo?buildID=" + brewBuildId,
                    SBOM_RED_HAT_BREW_BUILD_ID);

            if (source.isPresent()) {
                String scmSource = source.get();

                if (!hasExternalReference(component, ExternalReference.Type.VCS)) {
                    addExternalReference(component, ExternalReference.Type.VCS, scmSource, "");
                }

                int hashIndex = scmSource.lastIndexOf('#');
                if (hashIndex != -1) {
                    String commit = scmSource.substring(hashIndex + 1);
                    addPedigreeCommit(component, scmSource, commit);
                }
            }
        }
        return component;
    }

    public static void setProductMetadata(Component component, OperationConfig config) {
        Optional.ofNullable(config.getProduct())
                .map(ProductConfig::getProcessors)
                .ifPresent(
                        processors -> processors.stream()
                                .filter(RedHatProductProcessorConfig.class::isInstance)
                                .map(RedHatProductProcessorConfig.class::cast)
                                .forEach(processorConfig -> {
                                    addPropertyIfMissing(
                                            component,
                                            PROPERTY_ERRATA_PRODUCT_NAME,
                                            processorConfig.getErrata().getProductName());
                                    addPropertyIfMissing(
                                            component,
                                            PROPERTY_ERRATA_PRODUCT_VERSION,
                                            processorConfig.getErrata().getProductVersion());
                                    addPropertyIfMissing(
                                            component,
                                            PROPERTY_ERRATA_PRODUCT_VARIANT,
                                            processorConfig.getErrata().getProductVariant());
                                }));
    }

    public static Component createComponent(Artifact artifact, Scope scope, Type type, BuildType buildType) {

        Component component = new Component();
        if (buildType != null) {
            setCoordinates(component, artifact.getIdentifier(), buildType);
        } else {
            component.setName(artifact.getFilename());
        }
        component.setScope(scope);
        component.setType(type);
        component.setPurl(artifact.getPurl());
        component.setBomRef(artifact.getPurl());

        List<Hash> hashes = new ArrayList<>();
        hashes.add(new Hash(Algorithm.MD5, artifact.getMd5()));
        hashes.add(new Hash(Algorithm.SHA1, artifact.getSha1()));
        hashes.add(new Hash(Algorithm.SHA_256, artifact.getSha256()));
        component.setHashes(hashes);

        if ((component.getVersion() != null && RhVersionPattern.isRhVersion(component.getVersion()))
                || (component.getPurl() != null && RhVersionPattern.isRhPurl(component.getPurl()))) {
            SbomUtils.setPublisher(component);
            SbomUtils.setSupplier(component);
            SbomUtils.addMrrc(component);
        }
        return component;
    }

    public static Metadata createDefaultSbomerMetadata(Component component, String version) {
        Metadata metadata = new Metadata();
        metadata.setComponent(component);
        metadata.setTimestamp(Date.from(Instant.now()));

        LicenseChoice licenseChoice = new LicenseChoice();
        License license = new License();
        license.setId(SBOMER_LICENSE_ID);
        licenseChoice.setLicenses(List.of(license));
        metadata.setLicenseChoice(licenseChoice);

        Property vcs = new Property();
        vcs.setName(ExternalReference.Type.VCS.name());
        vcs.setValue(SBOMER_GIT_URL);
        Property website = new Property();
        website.setName(ExternalReference.Type.WEBSITE.name());
        website.setValue(SBOMER_WEBSITE);
        metadata.setProperties(List.of(vcs, website));

        // Set legacy tool info
        if (Version.VERSION_14.equals(schemaVersion())) {
            metadata.setTools(List.of(createTool(version)));
        } else if (Version.VERSION_14.getVersion() < schemaVersion().getVersion()) {
            metadata.setToolChoice(createToolInformation(version));
        }

        return metadata;
    }

    public static List<ParseException> validate(JsonNode jsonNode) throws IOException {
        return new JsonParser().validate(
                jsonNode.isTextual() ? jsonNode.textValue().getBytes() : jsonNode.toString().getBytes(),
                schemaVersion());
    }

    public static Tool createTool(String version) {
        Tool tool = new Tool();
        tool.setName(SBOMER_NAME);
        tool.setVendor(PUBLISHER);
        if (version != null) {
            tool.setVersion(version);
        }

        return tool;
    }

    /**
     * <p>
     * For a given {@link Bom} update all references for a given purl within the manifest from {@code oldPurl} to
     * {@code newPurl}.
     * </p>
     *
     * <p>
     * This includes traversing through components as well as dependencies.
     * </p>
     *
     * <p>
     * In some cases this will lead to duplicate components and dependencies. This process ensures that there are no
     * duplicates as well.
     * </p>
     *
     * @param bom
     * @param oldPurl
     * @param newPurl
     */
    public static void updatePurl(Bom bom, String oldPurl, String newPurl) {
        // Update main component's purl
        updatePurl(bom.getMetadata().getComponent(), oldPurl, newPurl);

        // If we have any components (we really should!) then update these purls as well
        if (bom.getComponents() != null) {
            // Update all components' purls (if needed)
            bom.getComponents().forEach(c -> updatePurl(c, oldPurl, newPurl));
        }
    }

    /**
     * Updates the purl for the given component if it matches the old purl.
     *
     * @param component
     * @param oldPurl
     * @param newPurl
     * @return
     */
    public static boolean updatePurl(Component component, String oldPurl, String newPurl) {
        if (component.getPurl().equals(oldPurl)) {
            component.setPurl(newPurl);

            return true;
        }

        return false;
    }

    public static ToolInformation createToolInformation(String version) {
        ToolInformation information = new ToolInformation();
        Component toolComponent = new Component();
        toolComponent.setName(SBOMER_NAME);
        toolComponent.setType(Type.APPLICATION);
        toolComponent.setAuthor(PUBLISHER);
        if (version != null) {
            toolComponent.setVersion(version);
        }
        information.setComponents(List.of(toolComponent));
        return information;
    }

    public static Dependency createDependency(String ref) {
        return new Dependency(ref);
    }

    public static boolean hasProperty(Component component, String property) {
        return component.getProperties() != null
                && component.getProperties().stream().anyMatch(c -> c.getName().equalsIgnoreCase(property));
    }

    public static boolean hasHash(Component component, Algorithm algorithm) {
        return getHash(component, algorithm).isPresent();
    }

    public static Optional<String> getHash(Component component, Algorithm algorithm) {
        List<Hash> hashes = null;

        if (component.getHashes() != null) {
            hashes = component.getHashes();
        } else {
            if (component.getExternalReferences() == null) {
                return Optional.empty();
            }

            Optional<ExternalReference> buildMetaRefOpt = component.getExternalReferences()
                    .stream()
                    .filter(r -> r.getType().equals(ExternalReference.Type.BUILD_META))
                    .findFirst();

            if (buildMetaRefOpt.isEmpty()) {
                return Optional.empty();
            }

            hashes = buildMetaRefOpt.get().getHashes();
        }

        return hashes.stream()
                .filter(h -> h.getAlgorithm().equalsIgnoreCase(algorithm.getSpec()))
                .map(h -> h.getValue())
                .findFirst();
    }

    public static void addHashIfMissing(Component component, String hash, Algorithm algorithm) {

        List<Hash> hashes = new ArrayList<Hash>();
        if (component.getHashes() != null) {
            hashes.addAll(component.getHashes());
        }
        // If there isn't already the same algorithm present (do not override), add it
        if (!hashes.stream()
                .filter(h -> h.getAlgorithm().equalsIgnoreCase(algorithm.getSpec()))
                .findAny()
                .isPresent()) {
            hashes.add(new Hash(algorithm.getSpec(), hash));
            component.setHashes(hashes);
        }
    }

    public static void addHashIfMissing(Component component, String hash, String algorithmSpec) {
        addHashIfMissing(component, hash, Hash.Algorithm.fromSpec(algorithmSpec));
    }

    public static void addProperty(Component component, String key, String value) {
        log.debug("addProperty {}: {}", key, value);
        List<Property> properties = new ArrayList<Property>();
        if (component.getProperties() != null) {
            properties.addAll(component.getProperties());
        }
        Property property = new Property();
        property.setName(key);
        property.setValue(value != null ? value : "");
        properties.add(property);
        component.setProperties(properties);
    }

    /**
     * <p>
     * Adds a property of a given name in case it's not already there.
     * </p>
     *
     * @param component The {@link Component} to add the property to.
     * @param property The name of the property.
     * @param value The value of the property.
     */
    public static void addPropertyIfMissing(Component component, String property, String value) {
        if (!hasProperty(component, property)) {
            log.info("Adding {} property with value: {}", property, value);
            addProperty(component, property, value);
        } else {
            log.debug(
                    "Property {} already exist, value: {}",
                    property,
                    findPropertyWithNameInComponent(property, component).get().getValue()); // NOSONAR It's checked
                                                                                            // above
        }
    }

    public static void removeProperty(Component component, String name) {
        if (component.getProperties() != null) {
            Optional<Property> property = component.getProperties()
                    .stream()
                    .filter(p -> p.getName().equalsIgnoreCase(name))
                    .findFirst();
            if (property.isPresent()) {
                component.getProperties().remove(property.get());
            }
        }
    }

    public static Optional<Component> findComponentWithPurl(String purl, Bom bom) {
        return bom.getComponents().stream().filter(c -> c.getPurl().equals(purl)).findFirst();
    }

    public static Optional<Property> findPropertyWithNameInComponent(String propertyName, Component component) {
        if (component == null) {
            return Optional.empty();
        }

        if (component.getProperties() == null || component.getProperties().isEmpty()) {
            return Optional.empty();
        }

        return component.getProperties().stream().filter(c -> c.getName().equals(propertyName)).findFirst();
    }

    public static boolean hasExternalReference(Component c, ExternalReference.Type type) {
        return !getExternalReferences(c, type).isEmpty();
    }

    public static boolean hasExternalReference(Component c, ExternalReference.Type type, String comment) {
        return !getExternalReferences(c, type, comment).isEmpty();
    }

    public static List<ExternalReference> getExternalReferences(Component c, ExternalReference.Type type) {
        List<ExternalReference> filteredExternalReferences = Optional.ofNullable(c.getExternalReferences())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(ref -> ref.getType().equals(type))
                .toList();

        return filteredExternalReferences;
    }

    public static List<ExternalReference> getExternalReferences(
            Component c,
            ExternalReference.Type type,
            String comment) {
        List<ExternalReference> filteredExternalReferences = Optional.ofNullable(c.getExternalReferences())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(ref -> ref.getType().equals(type))
                .filter(ref -> Objects.equals(ref.getComment(), comment))
                .toList();

        return filteredExternalReferences;
    }

    public static void addExternalReference(Component c, ExternalReference.Type type, String url, String comment) {
        if (!Strings.isEmpty(url)) {
            List<ExternalReference> externalRefs = new ArrayList<>();
            if (c.getExternalReferences() != null) {
                externalRefs.addAll(c.getExternalReferences());
            }

            ExternalReference reference = Optional.ofNullable(externalRefs)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .filter(ref -> ref.getType().equals(type))
                    .filter(ref -> Objects.equals(ref.getComment(), comment))
                    .findFirst()
                    .orElse(null);

            if (reference == null) {
                reference = new ExternalReference();
                reference.setType(type);
                externalRefs.add(reference);
            }

            reference.setUrl(url);
            reference.setComment(comment);

            c.setExternalReferences(externalRefs);
        }
    }

    public static void addPedigreeCommit(Component c, String url, String uid) {
        if (!Strings.isEmpty(url)) {

            Matcher matcher = gitProtocolPattern.matcher(url);

            if (matcher.find()) {
                log.debug(
                        "Found URL to be added as pedigree commit with the 'git@' protocol: '{}', trying to convert it into 'https://'",
                        url);

                url = "https://" + matcher.group(1) + "/" + matcher.group(2);

                log.debug("Converted into: '{}'", url);

            }

            Pedigree pedigree = c.getPedigree() == null ? new Pedigree() : c.getPedigree();
            List<Commit> commits = new ArrayList<>();
            if (pedigree.getCommits() != null) {
                commits.addAll(pedigree.getCommits());
            }

            Commit newCommit = new Commit();
            newCommit.setUid(uid);
            newCommit.setUrl(url);
            commits.add(newCommit);
            pedigree.setCommits(commits);

            c.setPedigree(pedigree);
        }
    }

    public static void setPublisher(Component c) {
        c.setPublisher(Constants.PUBLISHER);
    }

    public static void setSupplier(Component c) {
        OrganizationalEntity org = new OrganizationalEntity();
        org.setName(Constants.SUPPLIER_NAME);
        org.setUrls(Arrays.asList(new String[] { Constants.SUPPLIER_URL }));
        c.setSupplier(org);
    }

    public static void addMrrc(Component c) {
        List<ExternalReference> externalRefs = new ArrayList<>();
        if (c.getExternalReferences() != null) {
            externalRefs.addAll(c.getExternalReferences());
        }
        ExternalReference dist = null;
        for (ExternalReference r : externalRefs) {
            if (r.getType().equals(ExternalReference.Type.DISTRIBUTION)) {
                dist = r;
                break;
            }
        }
        if (dist == null) {
            dist = new ExternalReference();
            dist.setType(ExternalReference.Type.DISTRIBUTION);
            externalRefs.add(dist);
        }
        dist.setUrl(Constants.MRRC_URL);
        c.setExternalReferences(externalRefs);
    }

    /**
     * Converts the given CycloneDX {@link Bom} into a {@link JsonNode} object.
     *
     * @param bom The CycloneDX {@link Bom} to convert
     * @return {@link JsonNode} representation of the {@link Bom}.
     */
    public static JsonNode toJsonNode(Bom bom) {
        BomJsonGenerator generator = BomGeneratorFactory.createJson(SbomUtils.schemaVersion(), bom);
        return generator.toJsonNode();
    }

    /**
     * Converts the given CycloneDX {@link Bom} into a JSON {@link String} object.
     *
     * @param bom The CycloneDX {@link Bom} to convert
     * @return {@link String} representation of the {@link Bom}.
     * @throws GeneratorException
     */
    public static String toJson(Bom bom) throws GeneratorException {
        BomJsonGenerator generator = BomGeneratorFactory.createJson(SbomUtils.schemaVersion(), bom);
        return generator.toJsonString();
    }

    /**
     * Converts the {@link JsonNode} into a CycloneDX {@link Bom} object.
     *
     * @param jsonNode The {@link JsonNode} to convert.
     * @return The converted {@link Bom} or <code>null</code> in case of troubles in converting it.
     */
    public static Bom fromJsonNode(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }

        try {
            final String content = jsonNode.isTextual() ? jsonNode.textValue() : jsonNode.toString();
            return new JsonParser().parse(content.getBytes(UTF_8));
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static String[] computeNVRFromContainerManifest(JsonNode jsonNode) {
        Bom bom = fromJsonNode(jsonNode);
        if (bom == null || bom.getComponents() == null || bom.getComponents().isEmpty()) {
            return null;
        }

        Component mainComponent = bom.getComponents().get(0);
        Property n = findPropertyWithNameInComponent("sbomer:image:labels:com.redhat.component", mainComponent)
                .orElse(null);
        Property v = findPropertyWithNameInComponent("sbomer:image:labels:version", mainComponent).orElse(null);
        Property r = findPropertyWithNameInComponent("sbomer:image:labels:release", mainComponent).orElse(null);

        if (n != null && v != null && r != null) {
            return new String[] { n.getValue(), v.getValue(), r.getValue() };
        }

        return null;
    }

    public static void setEvidenceIdentities(Component c, Set<String> concludedValues, Field field) {
        List<Identity> identities = concludedValues.stream().map(concludedValue -> {
            Identity identity = new Identity();
            identity.setField(field);
            identity.setConcludedValue(concludedValue);
            return identity;
        }).toList();

        Evidence evidence = new Evidence();
        evidence.setIdentities(identities);
        c.setEvidence(evidence);
    }

    public static Bom fromPath(Path path) {
        try {
            return new JsonParser().parse(path.toFile());
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static void toPath(Bom bom, Path path) {
        try {
            Files.writeString(path, SbomUtils.toJson(bom));
        } catch (IOException | GeneratorException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static Bom fromString(String bomStr) {
        try {
            return new JsonParser().parse(bomStr.getBytes(UTF_8));
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts the {@link JsonNode} into a runtime Config {@link Config} object.
     *
     * @param jsonNode The {@link JsonNode} to convert.
     * @return The converted {@link Config} or <code>null</code> in case of troubles in converting it.
     */
    public static PncBuildConfig fromJsonBuildConfig(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }

        try {
            return ObjectMapperProvider.json()
                    .readValue(
                            jsonNode.isTextual() ? jsonNode.textValue().getBytes() : jsonNode.toString().getBytes(),
                            PncBuildConfig.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts the {@link JsonNode} into a runtime {@link OperationConfig} object.
     *
     * @param jsonNode The {@link JsonNode} to convert.
     * @return The converted {@link OperationConfig} or <code>null</code> in case of troubles in converting it.
     */
    public static OperationConfig fromJsonOperationConfig(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }

        try {
            return ObjectMapperProvider.json()
                    .readValue(
                            jsonNode.isTextual() ? jsonNode.textValue().getBytes() : jsonNode.toString().getBytes(),
                            OperationConfig.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts the given config {@link Config} into a {@link JsonNode} object.
     *
     * @param config The config {@link Config} to convert
     * @return {@link JsonNode} representation of the {@link Config}.
     */
    public static JsonNode toJsonNode(Config config) {

        try {
            String configuration = ObjectMapperProvider.json()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(config);
            return ObjectMapperProvider.json().readTree(configuration);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts the given config {@link OperationConfig} into a {@link JsonNode} object.
     *
     * @param operationConfig The config {@link OperationConfig} to convert
     * @return {@link JsonNode} representation of the {@link OperationConfig}.
     */
    public static JsonNode toJsonNode(OperationConfig operationConfig) {

        try {
            String configuration = ObjectMapperProvider.json()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(operationConfig);
            return ObjectMapperProvider.json().readTree(configuration);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts the given string into a {@link JsonNode} object.
     *
     * @param jsonString The string to convert
     * @return {@link JsonNode} representation of the string.
     */
    public static JsonNode toJsonNode(String jsonString) {

        try {
            return ObjectMapperProvider.json().readTree(jsonString);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts the given environment config {@link Map} into a {@link JsonNode} object.
     *
     * @param envConfig The environment config {@link Map} to convert
     * @return {@link JsonNode} representation of the {@link Map}.
     */
    public static JsonNode toJsonNode(Map<String, String> envConfig) {

        try {
            String configuration = ObjectMapperProvider.json()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(envConfig);
            return ObjectMapperProvider.json().readTree(configuration);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Given a raw {@link JsonNode}, converts it to a CycloneDX {@link Bom} object, and removes any Errata information
     * from the root component properties.
     *
     * @param jsonNode The {@link JsonNode} to convert.
     */
    public static JsonNode removeErrataProperties(JsonNode jsonNode) {
        Bom bom = fromJsonNode(jsonNode);
        removeErrataProperties(bom);
        return toJsonNode(bom);
    }

    /**
     * Removes any Errata information from the provided CycloneDX {@link Bom} object.
     *
     * @param bom The {@link Bom} containing the root component to be cleaned up from its Errata properties.
     */
    public static void removeErrataProperties(Bom bom) {
        if (bom != null && bom.getMetadata() != null && bom.getMetadata().getComponent() != null) {
            removeProperty(bom.getMetadata().getComponent(), Constants.PROPERTY_ERRATA_PRODUCT_NAME);
            removeProperty(bom.getMetadata().getComponent(), Constants.PROPERTY_ERRATA_PRODUCT_VERSION);
            removeProperty(bom.getMetadata().getComponent(), Constants.PROPERTY_ERRATA_PRODUCT_VARIANT);
        }
    }

    /**
     * Creates a purl of OCI type for the image
     *
     * @param imageName the image name (can contain registry and repository)
     * @param imageDigest the image digest (sha)
     */
    public static String createContainerImageOCIPurl(String imageName, String imageDigest) {
        // Extract last fragment from the imageName (in case of nested repositories or registry)
        String repositoryName = Optional.ofNullable(imageName).map(repo -> {
            int lastSlashIndex = repo.lastIndexOf('/');
            return lastSlashIndex != -1 ? repo.substring(lastSlashIndex + 1) : repo;
        }).orElseThrow(() -> new IllegalArgumentException("Repository name is null"));

        if (imageDigest == null || imageDigest.isEmpty()) {
            throw new IllegalArgumentException("Image digest is null or empty");
        }

        try {
            return PackageURLBuilder.aPackageURL()
                    .withType("oci")
                    .withName(repositoryName)
                    .withVersion(imageDigest)
                    .build()
                    .toString();
        } catch (MalformedPackageURLException | IllegalArgumentException e) {
            log.warn(
                    "Error while creating summary PURL for imageName {} and imageDigest {}",
                    imageName,
                    imageDigest,
                    e);
            return null;
        }
    }

    /**
     * Creates a purl of OCI type for the image
     *
     * @param imageName the image fullname
     */
    public static String createContainerImageOCIPurl(String imageFullname) {
        if (imageFullname == null || imageFullname.isEmpty()) {
            throw new IllegalArgumentException("Image full name is null or empty");
        }

        String[] imageTokens = imageFullname.split("@");
        if (imageTokens == null || imageTokens.length != 2) {
            throw new IllegalArgumentException("Image full name has wrong format");
        }
        return createContainerImageOCIPurl(imageTokens[0], imageTokens[1]);
    }

    public static void addMissingSerialNumber(Bom bom) {
        if (bom.getSerialNumber() == null || bom.getSerialNumber().isEmpty()) {
            log.debug("Setting 'serialNumber' for manifest with purl '{}'", bom.getMetadata().getComponent().getPurl());

            try {
                String jsonContent = SbomUtils.toJson(bom);
                bom.setSerialNumber("urn:uuid:" + UUID.nameUUIDFromBytes(jsonContent.getBytes(UTF_8)).toString());
            } catch (GeneratorException e) {
                log.warn("Could not generate serialNumber out of the manifest content, setting random UUID", e);
                bom.setSerialNumber(UUID.randomUUID().toString());
            }
        }
    }
}
