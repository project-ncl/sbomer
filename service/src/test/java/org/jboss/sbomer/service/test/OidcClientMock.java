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
package org.jboss.sbomer.service.test;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Mock
public class OidcClientMock implements OidcClient {
    @Override
    public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
        return Uni.createFrom()
                .item(
                        new Tokens(
                                "accessToken",
                                1L,
                                Duration.of(5, ChronoUnit.MINUTES),
                                "refreshToken",
                                1L,
                                null,
                                "client-id"));
    }

    @Override
    public Uni<Tokens> refreshTokens(String refreshToken) {
        return null;
    }

    @Override
    public Uni<Tokens> refreshTokens(String refreshToken, Map<String, String> additionalGrantParameters) {
        return null;
    }

    @Override
    public Uni<Boolean> revokeAccessToken(String accessToken) {
        return null;
    }

    @Override
    public Uni<Boolean> revokeAccessToken(String accessToken, Map<String, String> additionalParameters) {
        return null;
    }

    @Override
    public void close() {

    }
}
