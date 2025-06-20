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
package org.jboss.sbomer.service.nextgen.service.rest;

import java.util.List;

import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.utils.PaginationParameters;

public class RestUtils {
    /**
     * Prepares a {@link Page} object with the result of the search.
     *
     * @param content The content to populate the page with.
     * @param parameters Query parameters passed to the search.
     * @return A {@link Page} element with content.
     */
    public static <X> Page<X> toPage(List<X> content, PaginationParameters parameters, Long count) {
        int totalPages = 0;

        if (count == 0) {
            totalPages = 1; // a single page of zero results
        } else {
            totalPages = (int) Math.ceil((double) count / (double) parameters.getPageSize());
        }

        return new Page<>(parameters.getPageIndex(), parameters.getPageSize(), totalPages, count, content);
    }
}
