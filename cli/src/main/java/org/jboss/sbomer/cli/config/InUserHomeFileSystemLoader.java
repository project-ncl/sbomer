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
package org.jboss.sbomer.cli.config;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.config.yaml.runtime.ApplicationYamlConfigSourceLoader;

/**
 * Adds support for configuration file stored in the user home directory under the {@code $HOME/.sbomer/config.yaml}.
 * The configuration file extension could be {@code .yaml} or {@code .yml}.
 *
 */
public class InUserHomeFileSystemLoader extends ApplicationYamlConfigSourceLoader implements ConfigSourceProvider {

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
        List<ConfigSource> configSources = new ArrayList<>();
        configSources.addAll(
                loadConfigSources(
                        Paths.get(System.getProperty("user.home"), ".sbomer", "config.yaml").toUri().toString(),
                        275,
                        classLoader));
        configSources.addAll(
                loadConfigSources(
                        Paths.get(System.getProperty("user.home"), ".sbomer", "config.yml").toUri().toString(),
                        275,
                        classLoader));
        return configSources;
    }

}
