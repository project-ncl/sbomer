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
package org.jboss.sbomer.cli.feature.sbom.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Client used to interact with the SCM (GitLab) to fetch the config file for a particular build.
 */
@ApplicationScoped
@RegisterRestClient(configKey = "gitlab")
@Path("/")
public interface GitLabClient {
    /**
     * Fetch content of the file at the provided {@code path} in the Gitlab {@code project} identified by the
     * {@code ref}.
     *
     * @param project The project name in format: [namespace]/[repo]
     * @param ref The reference, for example, a tag: {@code 1.0.0.Final}
     * @param path The path to the file
     * @return Content of the file.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/{project}/-/raw/{ref}/{path}")
    String fetchFile(
            @PathParam("project") String project,
            @PathParam("ref") String ref,
            @PathParam("path") String path);
}
