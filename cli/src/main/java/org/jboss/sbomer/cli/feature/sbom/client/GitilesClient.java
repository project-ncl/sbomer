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

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Client used to interact with the SCM (Gerrit) to fetch the config file for particular build.
 */
@ApplicationScoped
@RegisterRestClient(configKey = "gerrit")
// @RegisterProvider(ClientExceptionMapper.class)
@Path("/gerrit/plugins/gitiles")
public interface GitilesClient {
    /**
     * Fetch content of the file at the provided {@code path} in the Gerrit {@code project} identified by the
     * {@code ref}.
     *
     * @param project The project name in format: [namespace]/[repo]
     * @param ref The reference, for example a tag: {@code refs/tags/1.0.0.Final}
     * @param path The path to the file
     * @return A Base64 encoded content on the file.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/{project}/+/{ref}/{path}?format=TEXT")
    String fetchFile(
            @PathParam("project") String project,
            @PathParam("ref") String ref,
            @PathParam("path") String path);

}
