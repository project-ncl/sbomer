package org.jboss.sbomer.service.v1beta2.generator.rhrelease;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Manifest;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.GenerationRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationStatus;
import org.jboss.sbomer.service.v1beta2.controller.request.Request;
import org.jboss.sbomer.service.v1beta2.worker.AbstractWorker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@NoArgsConstructor
@Slf4j
public class RedHatReleaseGenerator extends AbstractWorker {

    @Inject
    public RedHatReleaseGenerator(ManagedExecutor managedExecutor) {
        super(managedExecutor);
    }

    @Override
    public String getType() {
        return "EVENT";
    }

    @Override
    @ActivateRequestContext
    public void handle(EventRecord e, GenerationRecord generationRecord) {
        Request request = Request.parse(generationRecord);

        // Fetch event identified by the provided ID
        Event event = Event.findById(request.target().identifier());
        // Event event = Event
        // .find(
        // "FROM Event e LEFT JOIN FETCH e.generations g LEFT JOIN FETCH g.manifests WHERE e.id = ?1",
        // e.id())
        // .firstResult();

        if (event == null) {
            throw new ApplicationException(
                    "Event with id '{}' could not be found, cannot process generation",
                    request.target().identifier());

        }

        List<Manifest> manifests = new ArrayList<>();

        // Iterate over each generation that was part of that particular event and create copies
        // updates
        for (Generation g : event.getGenerations()) {
            log.info("Processing generation '{}'", g.getId());

            for (Manifest m : g.getManifests()) {
                log.info("Processing manifest '{}'", m.getId());

                Manifest manifest = Manifest.builder().withSbom(m.getSbom()).build();
                manifests.add(manifest);
            }
        }

        // Apply qualifier transformations to all manifests
        manifests.forEach(m -> adjustQualifiers(m));

        // Create release manifest
        manifests.add(createReleaseManifest(manifests));

        // Save all manifests linked to the current generation
        save(manifests, generationRecord.id());
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    protected void save(List<Manifest> manifests, String generationId) {
        Generation generation = Generation.findById(generationId);

        if (generation == null) {
            throw new ApplicationException(
                    "Generation with id '{}' could not be found, cannot process generation",
                    generationId);
        }

        for (Manifest manifest : manifests) {
            manifest.setGeneration(generation);
            generation.getManifests().add(manifest);
        }

        generation.setStatus(GenerationStatus.FINISHED);
        generation.setReason("Successfully generated release manifests");
        generation.save();

        // TODO: send event
    }

    private void adjustQualifiers(Manifest manifest) {

    }

    private Manifest createReleaseManifest(List<Manifest> manifests) {
        return Manifest.builder().build();
    }
}
