package org.jboss.sbomer.service.v1beta2.controller.syft;

import java.util.List;

/**
 * Representation of configuration options related to Syft generator.
 */
public record SyftOptions(boolean includeRpms, List<String> paths, String timeout) {
}