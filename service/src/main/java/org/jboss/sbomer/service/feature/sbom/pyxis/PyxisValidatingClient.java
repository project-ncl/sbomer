package org.jboss.sbomer.service.feature.sbom.pyxis;

import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.Set;

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
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import static org.jboss.sbomer.service.rest.faulttolerance.Costants.PYXIS_UNPUBLISHED_MAX_RETRIES;
import static org.jboss.sbomer.service.rest.faulttolerance.Costants.PYXIS_UNPUBLISHED_INITIAL_DELAY;
import org.jboss.sbomer.service.rest.faulttolerance.RetryLogger;

@ApplicationScoped
public class PyxisValidatingClient {

    @RestClient
    PyxisService p;

    @Inject
    Validator validator;

    @Retry(
        maxRetries = PYXIS_UNPUBLISHED_MAX_RETRIES,
        durationUnit = ChronoUnit.MINUTES,
        delay = PYXIS_UNPUBLISHED_INITIAL_DELAY,
        delayUnit = ChronoUnit.MINUTES,
        maxDuration = (PYXIS_UNPUBLISHED_INITIAL_DELAY * (PYXIS_UNPUBLISHED_MAX_RETRIES + 1)),
        retryOn = ConstraintViolationException.class)
    @BeforeRetry(RetryLogger.class)
    @FibonacciBackoff(maxDelay = (PYXIS_UNPUBLISHED_INITIAL_DELAY * PYXIS_UNPUBLISHED_MAX_RETRIES), 
    maxDelayUnit = ChronoUnit.MINUTES)
    public PyxisRepositoryDetails getRepositoriesDetails(
            @PathParam("nvr") String nvr,
            @QueryParam("include") List<String> includes) {
        PyxisRepositoryDetails prd = p.getRepositoriesDetails(nvr, includes);
        Set<ConstraintViolation<PyxisRepositoryDetails>> violations = validator.validate(prd);
        if (violations.isEmpty()) {
            return prd;
        } else {
            throw new ConstraintViolationException("Pyxis Repository constraint failure", violations);
        }
    }

    @Retry(
        maxRetries = PYXIS_UNPUBLISHED_MAX_RETRIES,
        durationUnit = ChronoUnit.MINUTES,
        delay = PYXIS_UNPUBLISHED_INITIAL_DELAY,
        delayUnit = ChronoUnit.MINUTES,
        maxDuration = (PYXIS_UNPUBLISHED_INITIAL_DELAY * (PYXIS_UNPUBLISHED_MAX_RETRIES + 1)),
        retryOn = ConstraintViolationException.class)
    @BeforeRetry(RetryLogger.class)
    @FibonacciBackoff(maxDelay = (PYXIS_UNPUBLISHED_INITIAL_DELAY * PYXIS_UNPUBLISHED_MAX_RETRIES), 
    maxDelayUnit = ChronoUnit.MINUTES)
    public PyxisRepository getRepository(
            @PathParam("registry") String registry,
            @PathParam("repository") String repository,
            @QueryParam("include") List<String> includes) {
        PyxisRepository pr = p.getRepository(registry, repository, includes);
        Set<ConstraintViolation<PyxisRepository>> violations = validator.validate(pr);
        if (violations.isEmpty()) {
            return pr;
        } else {
            throw new ConstraintViolationException("Pyxis Repository constraint failure", violations);
        }
    }
}
