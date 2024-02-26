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
package org.jboss.sbomer.cli.feature.sbom.service.pnc.client;

import java.util.Optional;

import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.dto.DeliverableAnalyzerReport;
import org.jboss.pnc.dto.response.AnalyzedArtifact;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.PageReader;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.RemoteResourceException;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.RemoteResourceNotFoundException;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.endpoint.DeliverableAnalyzerReportEndpoint;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

public class DeliverableAnalyzerReportClient extends ClientBase<DeliverableAnalyzerReportEndpoint> {

    public DeliverableAnalyzerReportClient(Configuration configuration) {
        super(configuration, DeliverableAnalyzerReportEndpoint.class);
    }

    public DeliverableAnalyzerReport getSpecific(String id) throws RemoteResourceException {
        try {
            return getEndpoint().getSpecific(id);
        } catch (NotFoundException e) {
            throw new RemoteResourceNotFoundException(e);
        } catch (NotAuthorizedException e) {
            if (configuration.getBearerTokenSupplier() != null) {
                try {
                    bearerAuthentication.setToken(configuration.getBearerTokenSupplier().get());
                    return getEndpoint().getSpecific(id);
                } catch (WebApplicationException wae) {
                    throw new RemoteResourceException(readErrorResponse(wae), wae);
                }
            } else {
                throw new RemoteResourceException(readErrorResponse(e), e);
            }
        } catch (WebApplicationException e) {
            throw new RemoteResourceException(readErrorResponse(e), e);
        }
    }

    public RemoteCollection<AnalyzedArtifact> getAnalyzedArtifacts(
            String id,
            Optional<String> sort,
            Optional<String> query) throws RemoteResourceException {
        try {
            PageReader pageLoader = new PageReader<>((pageParameters) -> {
                setSortAndQuery(pageParameters, sort, query);
                return getEndpoint().getAnalyzedArtifacts(id, pageParameters);
            }, getRemoteCollectionConfig());
            return pageLoader.getCollection();
        } catch (NotAuthorizedException e) {
            if (configuration.getBearerTokenSupplier() != null) {
                try {
                    bearerAuthentication.setToken(configuration.getBearerTokenSupplier().get());
                    PageReader pageLoader = new PageReader<>((pageParameters) -> {
                        setSortAndQuery(pageParameters, sort, query);
                        return getEndpoint().getAnalyzedArtifacts(id, pageParameters);
                    }, getRemoteCollectionConfig());
                    return pageLoader.getCollection();
                } catch (WebApplicationException wae) {
                    throw new RemoteResourceException(readErrorResponse(wae), wae);
                }
            } else {
                throw new RemoteResourceException(readErrorResponse(e), e);
            }
        } catch (WebApplicationException e) {
            throw new RemoteResourceException(readErrorResponse(e), e);
        }
    }

    public RemoteCollection<AnalyzedArtifact> getAnalyzedArtifacts(String id) throws RemoteResourceException {
        try {
            return getAnalyzedArtifacts(id, Optional.empty(), Optional.empty());
        } catch (NotAuthorizedException e) {
            if (configuration.getBearerTokenSupplier() != null) {
                try {
                    bearerAuthentication.setToken(configuration.getBearerTokenSupplier().get());
                    return getAnalyzedArtifacts(id, Optional.empty(), Optional.empty());
                } catch (WebApplicationException wae) {
                    throw new RemoteResourceException(readErrorResponse(wae), wae);
                }
            } else {
                throw new RemoteResourceException(readErrorResponse(e), e);
            }
        } catch (WebApplicationException e) {
            throw new RemoteResourceException(readErrorResponse(e), e);
        }
    }
}
