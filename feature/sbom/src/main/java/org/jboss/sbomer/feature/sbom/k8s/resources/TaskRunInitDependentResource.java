package org.jboss.sbomer.feature.sbom.k8s.resources;

import java.util.Map;

import org.jboss.sbomer.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.feature.sbom.k8s.model.SbomGenerationPhase;

import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.pipeline.v1beta1.StepBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDNoGCKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import lombok.extern.slf4j.Slf4j;

@KubernetesDependent(resourceDiscriminator = InitResourceDiscriminator.class)
@Slf4j
public class TaskRunInitDependentResource extends CRUDNoGCKubernetesDependentResource<TaskRun, GenerationRequest> {

	TaskRunInitDependentResource() {
		super(TaskRun.class);
	}

	public TaskRunInitDependentResource(Class<TaskRun> resourceType) {
		super(TaskRun.class);
	}

	/**
	 * <p>
	 * Method that creates a {@link TaskRun} related to the {@link GenerationRequest} in order to perform the
	 * initialization.
	 * </p>
	 * 
	 * <p>
	 * This is done just one right after the {@link GenerationRequest} is created within the system.
	 * </p>
	 */
	@Override
	protected TaskRun desired(GenerationRequest generationRequest, Context<GenerationRequest> context) {
		log.debug(
				"Preparing dependent resource for the '{}' phase related to '{}'",
				SbomGenerationPhase.INIT,
				generationRequest.getMetadata().getName());

		Map<String, String> labels = Labels.defaultLabelsToMap();

		labels.put(Labels.LABEL_BUILD_ID, generationRequest.getBuildId());
		labels.put(Labels.LABEL_PHASE, SbomGenerationPhase.INIT.name().toLowerCase());

		return new TaskRunBuilder().withNewMetadata()
				.withNamespace("default") // TODO!
				.withLabels(labels)
				.withName(generationRequest.dependentResourceName(SbomGenerationPhase.INIT))
				.withOwnerReferences(
						new OwnerReferenceBuilder().withKind(generationRequest.getKind())
								.withName(generationRequest.getMetadata().getName())
								.withApiVersion(generationRequest.getApiVersion())
								.withUid(generationRequest.getMetadata().getUid())
								.build())
				.endMetadata()
				.withNewSpec()
				.withNewTaskSpec()
				.withSteps(
						new StepBuilder().withName("hello")
								.withImage("alpine")
								.withScript("#!/bin/sh\necho \"Hello World\"")
								.build())
				.endTaskSpec()
				.endSpec()
				.build();

	}

}
