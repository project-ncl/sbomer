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
package org.jboss.sbomer.service;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.transaction.Transactional;

import org.jboss.sbomer.config.ProcessingConfig;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.enums.SbomStatus;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.processor.ProcessingExecConfig;
import org.jboss.sbomer.tekton.AbstractTektonTaskRunner;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation responsible for processing SBOMs.
 *
 * @author Marek Goldmann
 */
@ApplicationScoped
@Slf4j
public class ProcessingService extends AbstractTektonTaskRunner {

    @Inject
    SbomRepository sbomRepository;

    @Inject
    SbomService sbomService;

    @Inject
    ProcessingConfig processingConfig;

    /**
     * Performs processing of the SBOM identified by the ID using selected processing configuration.
     *
     * @param sbomId SBOM identifier being a {@link Long}
     * @param ProcessingExecConfig Configuration for the execution of processing
     */
    @Transactional
    public Sbom process(Sbom sbom, ProcessingExecConfig execConfig) {
        if (!processingConfig.isEnabled()) {
            throw new ApplicationException(
                    "Processing is disabled in the configuration, skipping processing for SBOM '{}'",
                    sbom.getId());
        }

        if (sbom.getStatus() != SbomStatus.READY) {
            throw new ClientException(
                    "SBOM with id '{}' is not ready yet, current status: {}",
                    sbom.getId(),
                    sbom.getStatus());
        }

        // Default processing config needs to be established
        if (execConfig == null) {
            execConfig = processingConfig.defaultExecConfig();
        }

        Set<ProcessorImplementation> processors = execConfig.getProcessors()
                .stream()
                .map(p -> p.getProcessor())
                .collect(Collectors.toSet());

        log.debug("Preparing to process SBOM id '{}' with configured processors: {}", sbom.getId(), processors);

        // Create the child object
        Sbom child = sbom.giveBirth();

        child.setProcessors(processors);

        // Store the child in database
        child = sbomService.save(child);

        // Schedule processing
        var config = Json.createObjectBuilder().add("processors", execConfig.processorsCommand()).build();

        runTektonTask("sbomer-process", child.getId(), config);

        return child;
    }

    /**
     * Performs processing of the SBOM identified by the ID using default processing configuration.
     *
     * @param sbomId SBOM identifier being a {@link Long}
     */
    public Sbom process(Sbom sbom) {
        return process(sbom, null);
    }
}
