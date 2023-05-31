package org.jboss.sbomer.feature.sbom.k8s.model;

import io.fabric8.kubernetes.api.builder.VisitableBuilder;

public class GenerationRequestBuilder extends GenerationRequestFluentImpl<GenerationRequestBuilder>
		implements VisitableBuilder<GenerationRequest, GenerationRequestBuilder> {

	@Override
	public GenerationRequest build() {
		addToData(GenerationRequest.KEY_BUILD_ID, getBuildId());
		addToData(GenerationRequest.KEY_STATUS, getStatus().name());

		GenerationRequest buildable = new GenerationRequest(
				getApiVersion(),
				getBinaryData(),
				getData(),
				getImmutable(),
				getKind(),
				buildMetadata());

		buildable.setAdditionalProperties(getAdditionalProperties());

		return buildable;
	}
}