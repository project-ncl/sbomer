/**
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
package org.jboss.sbomer.cli.feature.sbom.provider;

import java.net.URL;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.pnc.build.finder.core.BuildConfig;
import org.jboss.pnc.build.finder.koji.ClientSession;
import org.jboss.pnc.build.finder.koji.KojiClientSession;

import com.redhat.red.build.koji.KojiClientException;

import io.quarkus.arc.DefaultBean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KojiProvider {

    @Inject
    BuildConfig config;

    @Produces
    @DefaultBean
    public ClientSession createSession() throws KojiClientException {
        log.debug("Using default Koji ClientSession");

        URL kojiHubURL = config.getKojiHubURL();
        if (kojiHubURL == null) {
            throw new KojiClientException("Koji hub URL is not set");
        }
        log.info("Initializing Koji client session with URL {}", kojiHubURL);
        return new KojiClientSession(kojiHubURL);
    }

    public void close(@Disposes KojiClientSession session) {
        session.close();
    }

}