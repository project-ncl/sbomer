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

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@WithRetry
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RetryInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        WithRetry retry = context.getMethod().getAnnotation(WithRetry.class);
        if (retry == null) {
            retry = context.getTarget().getClass().getAnnotation(WithRetry.class);
        }

        int maxRetries = retry.maxRetries();
        long delay = retry.delay();
        long maxDelay = retry.maxDelay();
        boolean exponential = retry.exponential();

        int attempt = 0;
        // Get method class and name
        String methodName = context.getMethod().getDeclaringClass().getSimpleName() + "#"
                + context.getMethod().getName();
        String args = Arrays.toString(context.getParameters());

        while (true) {
            try {
                Object result = context.proceed();
                log.debug("Successful call to {} with args {}", methodName, args);

                return result;
            } catch (Throwable t) {
                attempt++;

                log.warn(
                        "Retry {}/{} for {} with args {} due to {}: {}",
                        attempt,
                        maxRetries,
                        methodName,
                        args,
                        t.getClass().getSimpleName(),
                        t.getMessage());

                if (attempt > maxRetries) {
                    log.error("Max retries exceeded for {}. Throwing exception.", methodName, t);
                    throw new RuntimeException("Max retries exceeded for " + methodName, t);
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt(); // restore interrupt status
                    throw new RuntimeException("Retry interrupted", ignored);
                }

                if (exponential) {
                    delay = Math.min(delay * 2, maxDelay);
                }
            }
        }
    }
}