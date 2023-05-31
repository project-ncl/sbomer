package org.jboss.sbomer.feature.sbom.k8s.reconciler.condition;

import org.jboss.sbomer.feature.sbom.k8s.model.GenerationRequest;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class NewRequestCondition implements Condition<TaskRun, GenerationRequest> {

	@Override
	public boolean isMet(GenerationRequest primary, TaskRun secondary, Context<GenerationRequest> context) {
		if (primary.getStatus() != null && !primary.getStatus().isFinal()) {
			return true;
		}

		return false;
	}

}
