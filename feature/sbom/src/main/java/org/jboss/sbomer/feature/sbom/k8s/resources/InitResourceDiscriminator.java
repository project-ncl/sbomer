package org.jboss.sbomer.feature.sbom.k8s.resources;

import org.jboss.sbomer.feature.sbom.k8s.model.SbomGenerationPhase;

public class InitResourceDiscriminator extends AbstractResourceDiscriminator {

	@Override
	protected SbomGenerationPhase getPhase() {
		return SbomGenerationPhase.INIT;
	}

}
