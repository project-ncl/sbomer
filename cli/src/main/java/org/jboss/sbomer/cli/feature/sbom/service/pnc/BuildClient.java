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
package org.jboss.sbomer.cli.feature.sbom.service.pnc;

import java.util.Optional;

import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

public class BuildClient extends ClientBase<BuildEndpoint> {
    public BuildClient(Configuration configuration) {
        super(configuration, BuildEndpoint.class);
    }

    public Build getSpecific(String id) throws RemoteResourceException {
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

    public RemoteCollection<Artifact> getBuiltArtifacts(String id, Optional<String> sort, Optional<String> query)
            throws RemoteResourceException {
        try {
            PageReader pageLoader = new PageReader<>((pageParameters) -> {
                setSortAndQuery(pageParameters, sort, query);
                return getEndpoint().getBuiltArtifacts(id, pageParameters);
            }, getRemoteCollectionConfig());
            return pageLoader.getCollection();
        } catch (NotAuthorizedException e) {
            if (configuration.getBearerTokenSupplier() != null) {
                try {
                    bearerAuthentication.setToken(configuration.getBearerTokenSupplier().get());
                    PageReader pageLoader = new PageReader<>((pageParameters) -> {
                        setSortAndQuery(pageParameters, sort, query);
                        return getEndpoint().getBuiltArtifacts(id, pageParameters);
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

    public RemoteCollection<Artifact> getBuiltArtifacts(String id) throws RemoteResourceException {
        try {
            return getBuiltArtifacts(id, Optional.empty(), Optional.empty());
        } catch (NotAuthorizedException e) {
            if (configuration.getBearerTokenSupplier() != null) {
                try {
                    bearerAuthentication.setToken(configuration.getBearerTokenSupplier().get());
                    return getBuiltArtifacts(id, Optional.empty(), Optional.empty());
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

    public RemoteCollection<Artifact> getDependencyArtifacts(String id, Optional<String> sort, Optional<String> query)
            throws RemoteResourceException {
        try {
            PageReader pageLoader = new PageReader<>((pageParameters) -> {
                setSortAndQuery(pageParameters, sort, query);
                return getEndpoint().getDependencyArtifacts(id, pageParameters);
            }, getRemoteCollectionConfig());
            return pageLoader.getCollection();
        } catch (NotAuthorizedException e) {
            if (configuration.getBearerTokenSupplier() != null) {
                try {
                    bearerAuthentication.setToken(configuration.getBearerTokenSupplier().get());
                    PageReader pageLoader = new PageReader<>((pageParameters) -> {
                        setSortAndQuery(pageParameters, sort, query);
                        return getEndpoint().getDependencyArtifacts(id, pageParameters);
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

    public RemoteCollection<Artifact> getDependencyArtifacts(String id) throws RemoteResourceException {
        try {
            return getDependencyArtifacts(id, Optional.empty(), Optional.empty());
        } catch (NotAuthorizedException e) {
            if (configuration.getBearerTokenSupplier() != null) {
                try {
                    bearerAuthentication.setToken(configuration.getBearerTokenSupplier().get());
                    return getDependencyArtifacts(id, Optional.empty(), Optional.empty());
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
