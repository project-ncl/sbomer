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
package org.jboss.sbomer.cli.feature.sbom.utils.auth;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.oidc.client.Tokens;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Duration;

@ApplicationScoped
@LookupIfProperty(name = "quarkus.oidc-client.enabled", stringValue = "false")
@IfBuildProfile(anyOf = { "test", "dev" })
/**
 * To be able to start in test mode without authorization
 */
public class TokensAlternative {
    @Produces
    public Tokens produceToken() {
        return new Tokens(
                "access-token",
                Long.MAX_VALUE,
                Duration.ofNanos(Long.MAX_VALUE),
                "refresh-token",
                Long.MAX_VALUE,
                JsonObject.of());
    }
}