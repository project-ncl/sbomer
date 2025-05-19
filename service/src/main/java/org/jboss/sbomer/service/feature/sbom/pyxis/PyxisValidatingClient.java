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
package org.jboss.sbomer.service.feature.sbom.pyxis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import org.eclipse.microprofile.faulttolerance.Retry;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepository;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepositoryDetails;
import org.jboss.sbomer.service.rest.faulttolerance.RetryLogger;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.FibonacciBackoff;

import jakarta.validation.ConstraintViolation;

import jakarta.validation.Validator;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import static org.jboss.sbomer.service.rest.faulttolerance.Constants.PYXIS_UNPUBLISHED_MAX_RETRIES;
import static org.jboss.sbomer.service.rest.faulttolerance.Constants.PYXIS_UNPUBLISHED_INITIAL_DELAY;
import static org.jboss.sbomer.service.rest.faulttolerance.Constants.PYXIS_UNPUBLISHED_MAX_DELAY;
import static org.jboss.sbomer.service.rest.faulttolerance.Constants.PYXIS_UNPUBLISHED_MAX_DURATION;

@ApplicationScoped
@Slf4j
public class PyxisValidatingClient {

    @RestClient
    PyxisClient p;

    @Inject
    Validator validator;

    @Retry(
            maxRetries = PYXIS_UNPUBLISHED_MAX_RETRIES,
            delay = PYXIS_UNPUBLISHED_INITIAL_DELAY,
            maxDuration = PYXIS_UNPUBLISHED_MAX_DURATION,
            retryOn = ConstraintViolationException.class)
    @BeforeRetry(RetryLogger.class)
    @FibonacciBackoff(maxDelay = PYXIS_UNPUBLISHED_MAX_DELAY)
    public PyxisRepositoryDetails getRepositoriesDetails(
            @PathParam("nvr") String nvr,
            @QueryParam("include") List<String> includes) {

        PyxisRepositoryDetails prd = p.getRepositoriesDetails(nvr, includes);
        log.debug("Pyxis response for NVR '{}': {}", nvr, prd);

        Set<ConstraintViolation<PyxisRepositoryDetails>> violations = validator.validate(prd);
        if (violations.isEmpty()) {
            return prd;
        }

        log.warn(
                "The deserialized Pyxis repository for nvr: {} and includes: {} violated the following constraints: {}",
                nvr,
                includes,
                violations.stream().map(v -> v.getMessage()).collect(Collectors.toList()));
        throw new ConstraintViolationException("Pyxis Repository constraint failure", violations);
    }

    public PyxisRepository getRepository(
            @PathParam("registry") String registry,
            @PathParam("repository") String repository,
            @QueryParam("include") List<String> includes) {
        return p.getRepository(registry, repository, includes);
    }
}
