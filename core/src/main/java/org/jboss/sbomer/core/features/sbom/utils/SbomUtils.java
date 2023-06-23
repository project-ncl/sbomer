/**
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Commit;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.Pedigree;
import org.cyclonedx.model.Property;
import org.cyclonedx.parsers.JsonParser;
import org.jboss.pnc.common.Strings;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SbomUtils {

    private static final Logger log = LoggerFactory.getLogger(SbomUtils.class);

    public static Version schemaVersion() {
        return Version.VERSION_14;
    }

    public static boolean hasProperty(Component component, String property) {
        return component.getProperties() != null
                && component.getProperties().stream().filter(c -> c.getName().equalsIgnoreCase(property)).count() > 0;
    }

    public static boolean hasHash(Component component, Algorithm algorithm) {
        return component.getHashes() != null && component.getHashes()
                .stream()
                .filter(h -> h.getAlgorithm().equalsIgnoreCase(algorithm.getSpec()))
                .count() > 0;
    }

    public static Optional<String> getHash(Component component, Algorithm algorithm) {
        if (component.getHashes() == null) {
            return Optional.empty();
        }
        return component.getHashes()
                .stream()
                .filter(h -> h.getAlgorithm().equalsIgnoreCase(algorithm.getSpec()))
                .map(h -> h.getValue())
                .findFirst();
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

    public static Optional<Component> findComponentWithPurl(String purl, Bom bom) {
        return bom.getComponents().stream().filter(c -> c.getPurl().equals(purl)).findFirst();
    }

    public static Optional<Property> findPropertyWithNameInComponent(String propertyName, Component component) {
        return component.getProperties().stream().filter(c -> c.getName().equals(propertyName)).findFirst();
    }

    public static boolean hasExternalReference(Component c, ExternalReference.Type type) {
        return getExternalReferences(c, type).size() > 0;
    }

    public static List<ExternalReference> getExternalReferences(Component c, ExternalReference.Type type) {
        List<ExternalReference> filteredExternalReferences = Optional.ofNullable(c.getExternalReferences())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(ref -> ref.getType().equals(type))
                .toList();

        return filteredExternalReferences;
    }

    public static void addExternalReference(Component c, ExternalReference.Type type, String url, String comment) {
        if (!Strings.isEmpty(url)) {
            List<ExternalReference> externalRefs = new ArrayList<>();
            if (c.getExternalReferences() != null) {
                externalRefs.addAll(c.getExternalReferences());
            }
            ExternalReference reference = null;
            for (ExternalReference r : externalRefs) {
                if (r.getType().equals(type)) {
                    reference = r;
                    break;
                }
            }
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
            return new JsonParser()
                    .parse(jsonNode.isTextual() ? jsonNode.textValue().getBytes() : jsonNode.toString().getBytes());
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return null;
        }
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
            new ObjectMapper().writeValue(path.toFile(), SbomUtils.toJsonNode(bom));

        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static Bom fromString(String bomStr) {
        try {
            return new JsonParser().parse(bomStr.getBytes(Charset.defaultCharset()));
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
    public static Config fromJsonConfig(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }

        try {
            byte[] content = jsonNode.isTextual() ? jsonNode.textValue().getBytes() : jsonNode.toString().getBytes();
            return ObjectMapperProvider.json().readValue(content, Config.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts the given config {@link Config} into a {@link JsonNode} object.
     *
     * @param bom The config {@link Config} to convert
     * @return {@link JsonNode} representation of the {@link Bom}.
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
}
