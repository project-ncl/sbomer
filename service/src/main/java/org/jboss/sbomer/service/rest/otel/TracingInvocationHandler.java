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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jboss.sbomer.core.features.sbom.utils.OtelHelper;
import org.slf4j.MDC;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TracingInvocationHandler<T> implements InvocationHandler {

    private final T delegate;
    private final Class<T> clientClass;

    public TracingInvocationHandler(T delegate, Class<T> clientClass) {
        this.delegate = delegate;
        this.clientClass = clientClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Invoke the method directly on the delegate declared on the Object clas, like toString, equals, hashCode
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(delegate, args);
        }
        // Invoke the method directly on the delegate if there is no "Traced" annotation
        if (!method.isAnnotationPresent(Traced.class)) {
            return method.invoke(delegate, args);
        }

        String spanName = extractSpanName(method);
        Map<String, String> spanAttributes = extractAttributes(method, args);

        try {
            log.debug(
                    "Calling invoke on {} for span {} with attributes",
                    clientClass.getSimpleName(),
                    spanName,
                    spanAttributes);
            // Wrap the call inside a new Span for better traceability
            return OtelHelper.withSpan(clientClass, "." + spanName, spanAttributes, MDC.getCopyOfContextMap(), () -> {

                try {

                    return method.invoke(delegate, args);

                } catch (InvocationTargetException | IllegalAccessException e) {
                    Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;

                    Span.current().recordException(cause);
                    Span.current().setStatus(StatusCode.ERROR, cause.getMessage());

                    throw new RuntimeException(cause);
                }
            });
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception) {
                throw cause;
            }
            throw ex;
        }
    }

    private Map<String, String> extractAttributes(Method method, Object[] args) {
        Map<String, String> attributes = new HashMap<>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                attributes.put("param" + i, String.valueOf(args[i]));
            }
        }
        return attributes;
    }

    private String extractSpanName(Method method) {
        return method.isAnnotationPresent(SpanName.class) ? method.getAnnotation(SpanName.class).value()
                : clientClass.getSimpleName() + "." + method.getName();

    }
}
