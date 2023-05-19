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

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;

import org.jboss.pnc.rest.api.parameters.PaginationParameters;
import org.jboss.sbomer.config.GenerationConfig;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.enums.SbomStatus;
import org.jboss.sbomer.core.enums.SbomType;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.generator.Generator.GeneratorLiteral;
import org.jboss.sbomer.generator.SbomGenerator;
import org.jboss.sbomer.model.Sbom;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation responsible for generating SBOMs.
 *
 * @author Marek Goldmann
 */
@ApplicationScoped
@Slf4j
public class GenerationService {

    @Inject
    SbomRepository sbomRepository;

    @Inject
    SbomService sbomService;

    @Any
    @Inject
    Instance<SbomGenerator> generators;

    @Inject
    GenerationConfig generationConfig;

    /**
     * Runs the generation of SBOM using the selected implementation of the {@link SbomGenerator}.
     *
     * Generation is done in an asynchronous way. The returned object is a handle which will let the client to retrieve
     * the SBOM content once the generation is finished.
     *
     * @param buildId The PNC build ID for which the generation should be performed.
     * @param generator The selected generator implementation, see {@link GeneratorImplementation}
     * @param generatorVersion Use specific version of the generator, if {@code null} default version will be used.
     * @param generatorArgs Pass provided arguments to the generator, if {@code null} defaults will be used.
     * @return Returns a {@link Sbom} object that will represent the generated CycloneDX sbom.
     */
    @Transactional
    public Sbom generate(
            String buildId,
            GeneratorImplementation generator,
            String generatorVersion,
            String generatorArgs) {
        if (!generationConfig.isEnabled()) {
            throw new ApplicationException(
                    "Generation is disabled in the configuration, skipping generation for PNC Build '{}'",
                    buildId);
        }

        // Retrieve any base sbom for this build
        String rsqlQuery = "buildId=eq=" + buildId + ";generator=isnull=false;processors=isnull=true";
        List<Sbom> sboms = sbomRepository.searchByQuery(0, 1, rsqlQuery);
        if (sboms.size() > 0) {
            Sbom sbom = sboms.get(0);
            if (sbom.getStatus() == SbomStatus.READY) {
                if (sbom.getGenerator() == generator) {
                    log.info(
                            "An Sbom has been already generated for build: {} with generator: {}",
                            buildId,
                            sbom.getGenerator());
                } else {
                    log.info(
                            "An Sbom has been already generated for build: {} but with a different generator: {}",
                            buildId,
                            sbom.getGenerator());
                }
            } else if (sbom.getStatus() == SbomStatus.IN_PROGRESS || sbom.getStatus() == SbomStatus.NEW) {
                log.info(
                        "An Sbom is already being generated for build: {} with generator: {}",
                        buildId,
                        sbom.getGenerator());
            } else if (sbom.getStatus() == SbomStatus.FAILED) {
                log.info(
                        "An Sbom generation was already attempted for build: {} with generator: {} but FAILED with error: {}",
                        buildId,
                        sbom.getGenerator(),
                        sbom.getStatusMessage());
            }
            return sbom;
        }

        Sbom sbom = new Sbom();
        sbom.setType(SbomType.BUILD_TIME); // TODO Is it always the case?
        sbom.setBuildId(buildId);
        sbom.setGenerator(generator);

        // Store it in database
        sbom = sbomService.save(sbom);

        // Schedule the generation
        generators.select(GeneratorLiteral.of(generator)).get().generate(sbom.getId(), generatorVersion, generatorArgs);

        return sbom;
    }

    /**
     * Runs the generation of SBOM using the selected implementation of the {@link SbomGenerator}.
     *
     * Generation is done in an asynchronous way. The returned object is a handle which will let the client to retrieve
     * the SBOM content once the generation is finished.
     *
     * @param buildId The PNC build ID for which the generation should be performed.
     * @param generator The selected generator implementation, see {@link GeneratorImplementation}
     * @return Returns a {@link Sbom} object that will represent the generated CycloneDX sbom.
     */
    public Sbom generate(String buildId, GeneratorImplementation generator) {
        return generate(buildId, generator, null, null);
    }

    /**
     * Runs the generation of SBOM using the selected implementation of the {@link SbomGenerator}.
     *
     * Generation is done in an asynchronous way. The returned object is a handle which will let the client to retrieve
     * the SBOM content once the generation is finished.
     *
     * <p>
     * Uses default generator.
     * </p>
     *
     * @param buildId The PNC build ID for which the generation should be performed.
     * @return Returns a {@link Sbom} object that will represent the generated CycloneDX sbom.
     */
    public Sbom generate(String buildId) {
        return generate(buildId, generationConfig.defaultGenerator(), null, null);
    }
}
