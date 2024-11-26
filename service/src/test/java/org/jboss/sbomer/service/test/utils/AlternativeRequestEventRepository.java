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
package org.jboss.sbomer.service.test.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import jakarta.persistence.Query;

@Alternative
@Singleton
public class AlternativeRequestEventRepository extends RequestEventRepository {

    private static final String BASE_COUNT_QUERY = "SELECT * FROM request";

    @Override
    protected StringBuilder initCountRequestQuery() {
        return new StringBuilder(BASE_COUNT_QUERY);
    }

    @Override
    protected long executeCountQuery(String query, Map<String, Object> params) {
        Query q = getEntityManager().createNativeQuery(query, RequestEvent.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            q.setParameter(entry.getKey(), entry.getValue());
        }
        return q.getResultList().size();
    }

    @Override
    protected StringBuilder addEventCondition(StringBuilder query, String condition, String property, String operator) {
        query.append(" ")
                .append(condition)
                .append(" JSON_EXTRACT(event, '$.")
                .append(property)
                .append("') ")
                .append(operator)
                .append(" :")
                .append(property);
        return query;
    }

    @Override
    protected StringBuilder addConfigCondition(
            StringBuilder query,
            String condition,
            String property,
            String operator) {
        query.append(" ")
                .append(condition)
                .append(" JSON_EXTRACT(request_config, '$.")
                .append(property)
                .append("') ")
                .append(operator)
                .append(" :")
                .append(property);
        return query;
    }

    @Override
    protected Instant convertFromTimestamp(Object rawTimeObject) {
        OffsetDateTime offsetDateTime = (OffsetDateTime) rawTimeObject;
        return offsetDateTime.toInstant();
    }

}