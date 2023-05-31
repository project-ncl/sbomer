package org.jboss.sbomer.feature.sbom.k8s.model;

import io.fabric8.kubernetes.api.model.ConfigMapFluent;

public interface GenerationRequestFluent<A extends GenerationRequestFluent<A>> extends ConfigMapFluent<A> {

	public A withBuildId(String buildId);

	public A withStatus(SbomGenerationStatus status);

	public ConfigMapFluent.MetadataNested<A> withNewDefaultMetadata();

}
