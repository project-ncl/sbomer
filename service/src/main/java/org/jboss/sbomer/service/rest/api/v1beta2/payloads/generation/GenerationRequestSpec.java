package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * A single generation request.
 * </p>
 *
 * <p>
 * It covers a request to manifest a single deliverable which is identified by the {@code target} parameter. The
 * optional {@code config} parameter allows for customization of the generation process.
 * </p>
 *
 * @param target Information about the deliverable to manifest.
 * @param config Optional configuration for the generator that will take care of manifesting.
 */
public record GenerationRequestSpec(@NotNull @Valid TargetSpec target, @Valid ConfigSpec config) {
}