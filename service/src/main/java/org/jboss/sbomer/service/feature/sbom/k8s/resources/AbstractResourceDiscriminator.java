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
package org.jboss.sbomer.service.feature.sbom.k8s.resources;

import static org.jboss.sbomer.service.feature.sbom.k8s.reconciler.BuildController.EVENT_SOURCE_NAME;

import java.util.Optional;

import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public abstract class AbstractResourceDiscriminator implements ResourceDiscriminator<TaskRun, GenerationRequest> {
    /**
     * The phase of the SBOM generation, could be: init, envconfig or generate.
     *
     * @return Name of the phase
     */
    protected abstract SbomGenerationPhase getPhase();

    @Override
    public Optional<TaskRun> distinguish(
            Class<TaskRun> resource,
            GenerationRequest primary,
            Context<GenerationRequest> context) {

        InformerEventSource<TaskRun, GenerationRequest> eventSource = (InformerEventSource<TaskRun, GenerationRequest>) context
                .eventSourceRetriever()
                .getResourceEventSourceFor(TaskRun.class, EVENT_SOURCE_NAME);

        return eventSource
                .get(new ResourceID(primary.dependentResourceName(getPhase()), primary.getMetadata().getNamespace()));

    }
}
