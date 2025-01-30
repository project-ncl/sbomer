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

import org.jboss.sbomer.core.config.DefaultGenerationConfig.DefaultGeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SbomerConfigProvider {

    @Getter
    final DefaultGenerationConfig defaultGenerationConfig;

    private static SbomerConfigProvider instance;

    public static SbomerConfigProvider getInstance() {
        if (instance == null) {
            instance = new SbomerConfigProvider();
        }
        return instance;
    }

    public SbomerConfigProvider() {
        SmallRyeConfig smallRyeConfig = new SmallRyeConfigBuilder().forClassLoader(this.getClass().getClassLoader())
                .addDefaultInterceptors()
                .addDefaultSources()
                .withSources(new SbomerConfigSourceProvider())
                .withMapping(DefaultGenerationConfig.class, "sbomer.generation")
                .withMapping(DefaultProcessingConfig.class, "sbomer.processing")
                .build();

        defaultGenerationConfig = smallRyeConfig.getConfigMapping(DefaultGenerationConfig.class);
    }

    /**
     * Adjusts the provided {@link Config} by providing default values for not provided elements in generators as well
     * as processors for each defined product.
     *
     * @param config The {@link Config} object to be adjusted.
     */
    public void adjust(PncBuildConfig config) {
        log.debug("Adjusting configuration...");

        config.getProducts().forEach(product -> {
            // Adjusting generator configuration. This is the only thing we can adjust,
            // because processor configuration is specific to the build and product release.
            adjustGenerator(product);

            if (!product.hasDefaultProcessor()) {
                // Adding default processor as the first one
                log.debug("No default processor specified, adding one");
                product.getProcessors().add(0, new DefaultProcessorConfig());
            }
        });
    }

    /**
     * Adjusts the provided {@link OperationConfig} by providing default values for not provided elements in generators.
     *
     * @param config The {@link OperationConfig} object to be adjusted.
     */
    public void adjust(OperationConfig config) {
        log.debug("Adjusting operation configuration...");

        // If we have not specified any products (for example when provided an empty config)
        if (config.getProduct() == null) {
            config.setProduct(ProductConfig.builder().build());
        }

        GeneratorConfig generatorConfig = config.getProduct().getGenerator();

        // Generator configuration was not provided, will use defaults
        if (generatorConfig == null) {
            log.debug("No generator provided, will use defaults: '{}'", GeneratorType.CYCLONEDX_OPERATION);
            generatorConfig = GeneratorConfig.builder().type(GeneratorType.CYCLONEDX_OPERATION).build();
            config.getProduct().setGenerator(generatorConfig);
        }
        // Nullify the args and version as they are not used anyway
        generatorConfig.setArgs(null);
        generatorConfig.setVersion(null);
    }

    private void adjustGenerator(ProductConfig product) {
        GeneratorConfig generatorConfig = product.getGenerator();

        GeneratorConfig defaultGeneratorConfig = defaultGeneratorConfig();

        // Generator configuration was not provided, will use defaults
        if (generatorConfig == null) {
            log.debug("No generator provided, will use defaults: '{}'", defaultGeneratorConfig);
            product.setGenerator(defaultGeneratorConfig);
        } else {

            if (generatorConfig.getVersion() == null) {
                String defaultVersion = defaultGenerationConfig.forGenerator(generatorConfig.getType())
                        .defaultVersion();

                log.debug("No generator version provided, will use default: '{}'", defaultVersion);
                generatorConfig.setVersion(defaultVersion);
            }

            if (generatorConfig.getArgs() == null) {
                String defaultArgs = defaultGenerationConfig.forGenerator(generatorConfig.getType()).defaultArgs();

                log.debug("No generator args provided, will use default: '{}'", defaultArgs);
                generatorConfig.setArgs(defaultArgs);
            }
        }
    }

    private GeneratorConfig defaultGeneratorConfig() {
        DefaultGeneratorConfig defaultGeneratorConfig = defaultGenerationConfig
                .forGenerator(defaultGenerationConfig.defaultGenerator());

        return GeneratorConfig.builder()
                .type(defaultGenerationConfig.defaultGenerator())
                .args(defaultGeneratorConfig.defaultArgs())
                .version(defaultGeneratorConfig.defaultVersion())
                .build();
    }
}
