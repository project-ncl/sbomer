package org.jboss.sbomer.feature.sbom.k8s.model;

import org.jboss.sbomer.feature.sbom.k8s.resources.Labels;

import io.fabric8.kubernetes.api.model.ConfigMapFluent;
import io.fabric8.kubernetes.api.model.ConfigMapFluentImpl;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

@SuppressWarnings(value = "unchecked")
public class GenerationRequestFluentImpl<A extends GenerationRequestFluent<A>> extends ConfigMapFluentImpl<A>
		implements GenerationRequestFluent<A> {

	private String buildId;
	private SbomGenerationStatus status;

	@Override
	public ConfigMapFluent.MetadataNested<A> withNewDefaultMetadata() {

		return withNewMetadataLike(
				new ObjectMetaBuilder().withGenerateName("sbomer-sbom-request-")
						.withLabels(Labels.defaultLabelsToMap())
						.build());

	}

	@Override
	public A withBuildId(String buildId) {
		this.buildId = buildId;
		return (A) this;
	}

	public String getBuildId() {
		return buildId;
	}

	@Override
	public A withStatus(SbomGenerationStatus status) {
		this.status = status;
		return (A) this;
	}

	public SbomGenerationStatus getStatus() {
		return status;
	}

}