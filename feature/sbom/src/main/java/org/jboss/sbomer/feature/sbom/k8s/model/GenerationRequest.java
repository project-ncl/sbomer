package org.jboss.sbomer.feature.sbom.k8s.model;

import java.util.Map;

import org.jboss.sbomer.feature.sbom.k8s.resources.Labels;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * <p>
 * This is a convenience model class. It is basically a {@link ConfigMap}, just with additional features to make it work
 * in the SBOM generation context better.
 * </p>
 * 
 * <p>
 * Following labels are expect to be present on the {@link ConfigMap} resource in order for it to be used as
 * {@link GenerationRequest}.
 * <ul>
 * <li>{@code app.kubernetes.io/part-of=sbomer}</li>
 * <li>{@code app.kubernetes.io/component=sbom}</li>
 * <li>{@code app.kubernetes.io/managed-by=sbom}</li>
 * <li>{@code sbomer.jboss.org/generation-request}</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Additionally following labels can be added:
 * 
 * <ul>
 * <li>{@code sbomer.jboss.org/sbom-build-id} -- the identifier of the build for which the generation is triggered</li>
 * </ul>
 * </p>
 */
@Kind("ConfigMap")
@Version("v1")
@Group("")
public class GenerationRequest extends ConfigMap {

	public static final String KEY_BUILD_ID = "build-id";
	public static final String KEY_STATUS = "status";

	public GenerationRequest() {
		super();
	}

	public GenerationRequest(
			String apiVersion,
			Map<String, String> binaryData,
			Map<String, String> data,
			Boolean immutable,
			String kind,
			ObjectMeta metadata) {
		super(apiVersion, binaryData, data, immutable, kind, metadata);
	}

	@JsonIgnore
	public String getBuildId() {
		return getData().get(KEY_BUILD_ID);
	}

	public void setBuildId(String buildId) {
		getData().put(KEY_BUILD_ID, buildId);
	}

	public SbomGenerationStatus getStatus() {
		String statusStr = getData().get(KEY_STATUS);

		if (statusStr == null) {
			return null;
		}

		return SbomGenerationStatus.valueOf(statusStr);
	}

	public void setStatus(SbomGenerationStatus status) {
		getData().put(KEY_STATUS, status.name());
		getMetadata().getLabels().put(Labels.LABEL_PHASE, status.name());
	}

	@JsonIgnore
	public String dependentResourceName(SbomGenerationPhase phase) {
		return this.getMetadata().getName() + "-" + phase.ordinal() + "-" + phase.name().toLowerCase();
	}

}
