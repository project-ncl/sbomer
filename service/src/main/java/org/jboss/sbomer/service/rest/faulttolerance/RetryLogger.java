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
package org.jboss.sbomer.service.rest.faulttolerance;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import io.smallrye.faulttolerance.api.BeforeRetryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class RetryLogger implements BeforeRetryHandler {
    public void handle(ExecutionContext context) {

        Throwable failure = context.getFailure();
        String method = context.getMethod().getDeclaringClass().getSimpleName() + "#" + context.getMethod().getName();
        String args = context.getParameters() != null ? java.util.Arrays.toString(context.getParameters()) : "[]";

        log.warn(
                "Retry attempt for method {} with args {} due to: {}: {}",
                method,
                args,
                failure.getClass().getSimpleName(),
                failure.getMessage());
    }
}
