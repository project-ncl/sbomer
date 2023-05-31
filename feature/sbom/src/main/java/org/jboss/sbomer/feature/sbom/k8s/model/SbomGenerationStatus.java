package org.jboss.sbomer.feature.sbom.k8s.model;

public enum SbomGenerationStatus {
	NEW, INITIALIZING, INITIALIZED, GENERATING, FAILED, FINISHED;

	public static SbomGenerationStatus fromName(String phase) {
		return SbomGenerationStatus.valueOf(phase.toUpperCase());
	}

	public String toName() {
		return this.name().toLowerCase();
	}

	public boolean isOlderThan(SbomGenerationStatus desiredStatus) {
		if (desiredStatus == null) {
			return false;
		}

		return desiredStatus.ordinal() > this.ordinal();
	}

	public boolean isFinal() {
		if (this.equals(FAILED) || this.equals(FINISHED)) {
			return true;
		}

		return false;

	}
}
