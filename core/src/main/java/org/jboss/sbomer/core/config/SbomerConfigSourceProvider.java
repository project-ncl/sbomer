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
package org.jboss.sbomer.core.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.source.yaml.YamlConfigSource;

class SbomerConfigSourceProvider extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {
    @Override
    public String[] getFileExtensions() {
        return new String[] { "yaml", "yml" };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new YamlConfigSource(url, ordinal);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
        return new ArrayList<>(loadConfigSources("META-INF/sbomer-config.yaml", 110, classLoader));
    }

}
