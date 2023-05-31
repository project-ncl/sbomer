package org.jboss.sbomer.feature.sbom.k8s.resources;

import static org.jboss.sbomer.feature.sbom.k8s.reconciler.GenerationRequestReconciler.EVENT_SOURCE_NAME;

import java.util.Optional;

import org.jboss.sbomer.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.feature.sbom.k8s.model.SbomGenerationPhase;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public abstract class AbstractResourceDiscriminator implements ResourceDiscriminator<TaskRun, GenerationRequest> {
	/**
	 * The phase of the SBOM generation, could be: init or generate.
	 * 
	 * @return Name of the phase
	 */
	protected abstract SbomGenerationPhase getPhase();

	@Override
	public Optional<TaskRun> distinguish(
			Class<TaskRun> resource,
			GenerationRequest primary,
			Context<GenerationRequest> context) {

		InformerEventSource<TaskRun, GenerationRequest> eventSource = (InformerEventSource<TaskRun, GenerationRequest>) context
				.eventSourceRetriever()
				.getResourceEventSourceFor(TaskRun.class, EVENT_SOURCE_NAME);

		return eventSource
				.get(new ResourceID(primary.dependentResourceName(getPhase()), primary.getMetadata().getNamespace()));

	}
}
