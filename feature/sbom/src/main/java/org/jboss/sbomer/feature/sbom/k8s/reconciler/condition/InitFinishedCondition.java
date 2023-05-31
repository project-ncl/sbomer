package org.jboss.sbomer.feature.sbom.k8s.reconciler.condition;

import java.util.Objects;

import org.jboss.sbomer.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.feature.sbom.k8s.model.SbomGenerationStatus;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class InitFinishedCondition implements Condition<TaskRun, GenerationRequest> {

	@Override
	public boolean isMet(GenerationRequest primary, TaskRun secondary, Context<GenerationRequest> context) {
		if (Objects.equals(primary.getStatus(), SbomGenerationStatus.INITIALIZED)) {
			return true;
		}

		return false;
	}

}
