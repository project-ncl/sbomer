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
package org.jboss.sbomer.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Commit;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.Pedigree;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Hash.Algorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static void addHash(Component component, Algorithm algorithm, String value) {
        log.debug("addHash {}: {}", algorithm.getSpec(), value);
        List<Hash> hashes = new ArrayList<Hash>();
        if (component.getHashes() != null) {
            hashes.addAll(component.getHashes());
        }
        hashes.add(new Hash(algorithm, value));
        component.setHashes(hashes);
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
        return c.getExternalReferences() != null
                && c.getExternalReferences().stream().filter(ref -> ref.getType().equals(type)).count() > 0;
    }

    public static void addExternalReference(Component c, ExternalReference.Type type, String url, String comment) {
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
        }
        reference.setType(type);
        reference.setUrl(url);
        reference.setComment(comment);
        externalRefs.add(reference);
        c.setExternalReferences(externalRefs);
    }

    public static void addPedigreeCommit(Component c, String url, String uid) {
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
        c.setPublisher(Constants.PUBLISHER);
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

}
