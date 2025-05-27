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
package org.jboss.sbomer.service.feature.sbom.model.v1beta2;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "generation_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class GenerationStatusHistory extends BaseStatusHistory {

    public GenerationStatusHistory(Generation generation, String status, String reason) {
        this.generation = generation;
        this.status = status;
        this.reason = reason;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_id", nullable = false, updatable = false)
    private Generation generation;

    @Transactional
    public GenerationStatusHistory save() {
        persistAndFlush();
        return this;
    }
}