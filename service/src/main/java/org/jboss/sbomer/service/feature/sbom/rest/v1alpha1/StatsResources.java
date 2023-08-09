/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.feature.sbom.rest.v1alpha1;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.service.feature.sbom.model.Stats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.GenerationRequestStats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Resources;
import org.jboss.sbomer.service.feature.sbom.model.Stats.SbomStats;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

@Path("/api/v1alpha1/stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Stats", description = "SBOMer information and statistics")
@PermitAll
public class StatsResources {

	@Inject
	SbomService sbomService;

	@ConfigProperty(name = "buildNumber", defaultValue = "dev")
	String version;

	@GET
	@Operation(summary = "Get service runtime information", description = "Service information and statistics.")
	@APIResponses({ @APIResponse(
			responseCode = "200",
			description = "Available runtime information.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
	public Response stats() {
		long uptimeMilis = getUptimeMilis();

		Stats stats = Stats.builder()
				.withVersion(version)
				.withUptime(toUptime(uptimeMilis))
				.withUptimeMilis(uptimeMilis)
				.withResources(resources())
				.build();

		return Response.status(Status.OK).entity(stats).build();
	}

	private long getUptimeMilis() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}

	private Resources resources() {
		return Resources.builder().withSboms(sbomStats()).withGenerationRequests(generationRequestStats()).build();
	}

	private SbomStats sbomStats() {
		return SbomStats.builder().withTotal(sbomService.countSboms()).build();
	}

	private GenerationRequestStats generationRequestStats() {
		return GenerationRequestStats.builder()
				.withTotal(sbomService.countSbomGenerationRequests())
				.withInProgress(sbomService.countInProgressSbomGenerationRequests())
				.build();
	}

	private String toUptime(long miliseconds) {
		return Duration.ofMillis(miliseconds)
				.toString()
				.substring(2)
				.replaceAll("(\\d[HMS])(?!$)", "$1 ")
				.toLowerCase();
	}
}
