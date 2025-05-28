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

import static org.jboss.sbomer.core.rest.faulttolerance.Constants.KOJI_DOWNLOAD_CLIENT_DELAY;
import static org.jboss.sbomer.core.rest.faulttolerance.Constants.KOJI_DOWNLOAD_CLIENT_MAX_RETRIES;

import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.sbomer.core.rest.faulttolerance.RetryLogger;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@RegisterRestClient(configKey = "koji-download")
@Path("/brewroot/packages")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public interface KojiDownloadClient {

    @GET
    @Path("/{name}/{version}/{release}/files/remote-sources/{remoteSourcesName}.tar.gz")
    @Retry(
            maxRetries = KOJI_DOWNLOAD_CLIENT_MAX_RETRIES,
            delay = KOJI_DOWNLOAD_CLIENT_DELAY,
            delayUnit = ChronoUnit.SECONDS)
    @ExponentialBackoff
    @BeforeRetry(RetryLogger.class)
    Response downloadSourcesFile(
            @PathParam("name") String name,
            @PathParam("version") String version,
            @PathParam("release") String release,
            @PathParam("remoteSourcesName") String remoteSourcesName);
}
