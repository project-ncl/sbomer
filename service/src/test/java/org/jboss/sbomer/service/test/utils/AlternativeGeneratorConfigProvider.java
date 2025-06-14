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
package org.jboss.sbomer.service.test.utils;

import java.io.IOException;

import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.nextgen.service.config.GeneratorConfigProvider;
import org.jboss.sbomer.service.nextgen.service.config.mapping.GeneratorsConfig;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Alternative
@Priority(1)
@Singleton
@Slf4j
public class AlternativeGeneratorConfigProvider extends GeneratorConfigProvider {

    @Inject
    public AlternativeGeneratorConfigProvider(KubernetesClient kubernetesClient) {
        super(kubernetesClient);
    }

    @Override
    public void updateConfig() {
        log.info("AlternativeGeneratorConfigProvider is providing test config");

        try {
            this.config = ObjectMapperProvider.yaml()
                    .readValue(TestResources.asString("generator/syft-only.yaml"), GeneratorsConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
