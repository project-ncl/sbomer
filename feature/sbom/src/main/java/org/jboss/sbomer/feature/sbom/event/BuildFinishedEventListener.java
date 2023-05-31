package org.jboss.sbomer.feature.sbom.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.sbomer.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.feature.sbom.k8s.model.SbomGenerationStatus;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class BuildFinishedEventListener {

	@Inject
	KubernetesClient kubernetesClient;

	public void init(@Observes StartupEvent ev) {
		// TODO how to make these querable? add more labels?
		GenerationRequest req = new GenerationRequestBuilder().withNewDefaultMetadata()
				.endMetadata()
				.withBuildId("AABBCC")
				.withStatus(SbomGenerationStatus.NEW)
				.build();

		System.out.println(req);

		// ConfigMap cm = kubernetesClient.configMaps().resource(req).create();

		// System.out.println(cm);
	}
}
