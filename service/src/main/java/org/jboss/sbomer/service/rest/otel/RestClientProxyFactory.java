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
package org.jboss.sbomer.service.rest.otel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.pyxis.PyxisClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Dynamic proxy wrapper around RestClient. This will intercept all calls and propagate them to TracingInvocationHandler
 */
public class RestClientProxyFactory {

    @Produces
    @TracingRestClient
    @ApplicationScoped
    public ErrataClient tracingErrataClient(@RestClient ErrataClient delegate) {
        return createProxy(delegate, ErrataClient.class);
    }

    @Produces
    @TracingRestClient
    @ApplicationScoped
    public PyxisClient tracingPyxisClient(@RestClient PyxisClient delegate) {
        return createProxy(delegate, PyxisClient.class);
    }

    public <T> T createProxy(T delegate, Class<T> iface) {
        InvocationHandler handler = new TracingInvocationHandler<>(delegate, iface);
        // noinspection unchecked
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface }, handler);
    }
}
