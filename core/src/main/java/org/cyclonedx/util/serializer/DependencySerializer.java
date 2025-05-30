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
/*
 * This file is part of CycloneDX Core (Java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) OWASP Foundation. All Rights Reserved.
 */
package org.cyclonedx.util.serializer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.CycloneDxSchema;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.DependencyList;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/*
 * Modified by Andrea Vibelli to temporarily fix https://github.com/CycloneDX/cyclonedx-core-java/issues/565.
 */
public class DependencySerializer extends StdSerializer<DependencyList> implements ContextualSerializer {
    private static final String REF = "ref";

    private boolean useNamespace;

    private final String parentTagName;

    public DependencySerializer(final boolean useNamespace, String parentTagName) {
        super(DependencyList.class, false);
        this.useNamespace = useNamespace;
        this.parentTagName = parentTagName;
    }

    public DependencySerializer() {
        this(false, null);
    }

    public DependencySerializer(Class<DependencyList> t, String parentTagName) {
        super(t);
        this.parentTagName = parentTagName;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        return new DependencySerializer(useNamespace, property.getName());
    }

    @Override
    public void serialize(
            final DependencyList dependencies,
            final JsonGenerator generator,
            final SerializerProvider provider) throws IOException {
        try {
            if (generator instanceof ToXmlGenerator toxmlgenerator) {
                writeXMLDependenciesWithGenerator(toxmlgenerator, dependencies);
            } else {
                writeJSONDependenciesWithGenerator(generator, dependencies);
            }
        } catch (XMLStreamException | IOException ex) {
            throw new IOException(ex);
        }
    }

    private void writeJSONDependenciesWithGenerator(final JsonGenerator generator, final List<Dependency> dependencies)
            throws IOException {
        if (dependencies != null) {
            generator.writeStartArray();
            if (!dependencies.isEmpty()) {
                for (Dependency dependency : dependencies) {
                    generator.writeStartObject();
                    generator.writeStringField(REF, dependency.getRef());
                    generator.writeArrayFieldStart("dependsOn");
                    if (CollectionUtils.isNotEmpty(dependency.getDependencies())) {
                        for (Dependency subDependency : dependency.getDependencies()) {
                            generator.writeString(subDependency.getRef());
                        }
                    }
                    generator.writeEndArray();
                    generator.writeArrayFieldStart("provides");
                    if (CollectionUtils.isNotEmpty(dependency.getProvides())) {
                        for (Dependency subDependency : dependency.getProvides()) {
                            generator.writeString(subDependency.getRef());
                        }
                    }
                    generator.writeEndArray();
                    generator.writeEndObject();
                }
            }
            generator.writeEndArray();
        }
    }

    private void writeXMLDependenciesWithGenerator(
            final ToXmlGenerator toXmlGenerator,
            final List<Dependency> dependencies) throws IOException, XMLStreamException {
        if (SbomUtils.isNotEmpty(dependencies)) {
            processNamespace(toXmlGenerator, parentTagName);
            toXmlGenerator.writeStartArray();

            for (Dependency dependency : dependencies) {
                writeXMLDependency(dependency, toXmlGenerator);
            }

            toXmlGenerator.writeEndArray();
            toXmlGenerator.writeEndObject();
        }
    }

    private void writeXMLDependency(final Dependency dependency, final ToXmlGenerator generator)
            throws IOException, XMLStreamException {
        processNamespace(generator, "dependency");

        if (CollectionUtils.isNotEmpty(dependency.getDependencies())) {
            generator.writeStartArray();
        }

        generator.setNextIsAttribute(true);
        generator.setNextName(new QName(REF));
        generator.writeString(dependency.getRef());
        generator.setNextIsAttribute(false);

        if (CollectionUtils.isNotEmpty(dependency.getDependencies())) {
            for (Dependency subDependency : dependency.getDependencies()) {
                // You got Shay'd
                writeXMLDependency(subDependency, generator);
            }
        }

        if (CollectionUtils.isNotEmpty(dependency.getProvides())) {
            for (Dependency subDependency : dependency.getProvides()) {
                // You got Shay'd
                writeXMLDependency(subDependency, generator);
            }
        }

        if (CollectionUtils.isNotEmpty(dependency.getDependencies())) {
            generator.writeEndArray();
        }

        generator.writeEndObject();
    }

    private void processNamespace(final ToXmlGenerator toXmlGenerator, final String dependencies)
            throws XMLStreamException, IOException {
        QName qName;

        String dependenciesNamespace = StringUtils.isBlank(dependencies) ? "dependencies" : dependencies;

        if (useNamespace) {
            qName = new QName(CycloneDxSchema.NS_DEPENDENCY_GRAPH_10, dependenciesNamespace, "dg");
            toXmlGenerator.getStaxWriter().setPrefix(qName.getPrefix(), qName.getNamespaceURI());
        } else {
            qName = new QName(dependenciesNamespace);
        }

        toXmlGenerator.setNextName(qName);
        toXmlGenerator.writeStartObject();
        toXmlGenerator.writeFieldName(qName.getLocalPart());
    }
}
