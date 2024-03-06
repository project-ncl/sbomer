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

import java.io.Closeable;
import java.util.Optional;

import org.jboss.pnc.api.dto.ErrorResponse;
import org.jboss.pnc.client.ApacheHttpClient43EngineWithRetry;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteCollectionConfig;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.BasicAuthentication;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.BearerAuthentication;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.MdcToHeadersFilter;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.PageParameters;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.RequestLoggingFilter;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.ResteasyJackson2ProviderWithDateISO8601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 * @author Jakub Bartecek
 */
public abstract class ClientBase<T> implements Closeable {

    private Logger logger = LoggerFactory.getLogger(ClientBase.class);

    protected final String BASE_PATH = "/pnc-rest";

    protected final String BASE_REST_PATH = BASE_PATH + "/v2";

    protected final Client client;

    protected final WebTarget target;

    protected T proxy;

    protected Configuration configuration;

    protected Class<T> iface;

    protected BearerAuthentication bearerAuthentication;

    protected ClientBase(Configuration configuration, Class<T> iface) {
        this.iface = iface;

        ApacheHttpClient43EngineWithRetry engine = new ApacheHttpClient43EngineWithRetry();
        // allow redirects for NCL-3766
        engine.setFollowRedirects(true);

        this.configuration = configuration;

        ResteasyClientBuilder clientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
        client = clientBuilder.httpEngine(engine).build();
        client.register(ResteasyJackson2ProviderWithDateISO8601.class);
        client.register(new MdcToHeadersFilter(configuration.getMdcToHeadersMappings()));
        client.register(RequestLoggingFilter.class);
        target = client.target(
                configuration.getProtocol() + "://" + configuration.getHost()
                        + (configuration.getPort() == null ? "" : ":" + configuration.getPort()) + BASE_REST_PATH);
        Configuration.BasicAuth basicAuth = configuration.getBasicAuth();

        if (basicAuth != null) {
            target.register(new BasicAuthentication(basicAuth.getUsername(), basicAuth.getPassword()));
        } else {
            if (configuration.getBearerTokenSupplier() != null) {
                bearerAuthentication = new BearerAuthentication(configuration.getBearerTokenSupplier().get());
                target.register(bearerAuthentication);
            } else {
                String bearerToken = configuration.getBearerToken();
                if (bearerToken != null && !bearerToken.equals("")) {
                    bearerAuthentication = new BearerAuthentication(bearerToken);
                    target.register(bearerAuthentication);
                }
            }
        }

        proxy = ProxyBuilder.builder(iface, target).build();
    }

    protected T getEndpoint() {
        return proxy;
    }

    RemoteCollectionConfig getRemoteCollectionConfig() {
        int pageSize = configuration.getPageSize();
        if (pageSize < 1) {
            pageSize = 100;
        }

        return RemoteCollectionConfig.builder().pageSize(pageSize).build();
    }

    protected void setSortAndQuery(PageParameters pageParameters, Optional<String> sort, Optional<String> q) {
        sort.ifPresent(pageParameters::setSort);
        q.ifPresent(pageParameters::setQ);
    }

    protected ErrorResponse readErrorResponse(WebApplicationException ex) {
        Response response = ex.getResponse();
        if (response.hasEntity()) {
            try {
                return response.readEntity(ErrorResponse.class);
            } catch (ProcessingException | IllegalStateException e) {
                logger.debug("Can't map response to ErrorResponse.", e);
            } catch (RuntimeException e) {
                logger.warn("Unexpected exception when trying to read ErrorResponse.", e);
            }
        }
        return null;
    }

    @Override
    public void close() {
        client.close();
    }
}
