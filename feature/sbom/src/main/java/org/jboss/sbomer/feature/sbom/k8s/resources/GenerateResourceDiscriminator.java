package org.jboss.sbomer.feature.sbom.k8s.resources;

import org.jboss.sbomer.feature.sbom.k8s.model.SbomGenerationPhase;

public class GenerateResourceDiscriminator extends AbstractResourceDiscriminator {

	@Override
	protected SbomGenerationPhase getPhase() {
		return SbomGenerationPhase.GENERATE;
	}

}
