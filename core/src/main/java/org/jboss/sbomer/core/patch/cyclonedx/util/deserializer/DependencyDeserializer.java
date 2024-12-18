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
package org.jboss.sbomer.core.patch.cyclonedx.util.deserializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jboss.sbomer.core.patch.cyclonedx.model.Dependency;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;

/*
 * Modified by Andrea Vibelli to temporarily fix https://github.com/CycloneDX/cyclonedx-core-java/issues/565.
 */
public class DependencyDeserializer extends StdDeserializer<List<Dependency>> {
    public DependencyDeserializer() {
        this(null);
    }

    public DependencyDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public List<Dependency> deserialize(final JsonParser parser, final DeserializationContext context)
            throws IOException {
        Dependency[] dependencies = parser.readValueAs(Dependency[].class);
        if (parser instanceof FromXmlParser) {
            if (dependencies != null && dependencies.length > 0) {
                return dependencies[0].getDependencies();
            }
        } else {
            return Arrays.asList(dependencies.clone());
        }

        return null;
    }
}