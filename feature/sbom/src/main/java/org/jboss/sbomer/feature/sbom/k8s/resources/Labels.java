package org.jboss.sbomer.feature.sbom.k8s.resources;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class Labels {
	public static final String LABEL_PHASE = "sbomer.jboss.org/phase";
	public static final String LABEL_BUILD_ID = "sbomer.jboss.org/build-id";
	public static final String LABEL_SELECTOR = "app.kubernetes.io/part-of=sbomer,app.kubernetes.io/component=sbom,app.kubernetes.io/managed-by=sbom,sbomer.jboss.org/type=generation-request";

	public static Map<String, String> defaultLabelsToMap() {
		return Arrays.asList(LABEL_SELECTOR.split(","))
				.stream()
				.map(l -> l.split("="))
				.collect(Collectors.toMap(splitLabel -> splitLabel[0], splitLabel -> splitLabel[1]));
	}
}
