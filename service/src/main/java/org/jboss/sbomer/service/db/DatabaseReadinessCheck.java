package org.jboss.sbomer.service.db;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class DatabaseReadinessCheck implements HealthCheck {

    @Inject
    SbomGenerationRequestRepository repository;

    @Override
    public HealthCheckResponse call() {
        try {
            repository.getEntityManager().createNativeQuery("SELECT 1").getSingleResult();
            return HealthCheckResponse.up("Database connection is ready");
        } catch (Exception e) {
            return HealthCheckResponse.down("Database connection is not ready");
        }
    }
}
