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
package org.jboss.sbomer.service.nextgen.workflow;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.nextgen.core.events.Event;
import org.jboss.sbomer.service.nextgen.core.utils.ConfigUtils;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.workflow.model.EventTrigger;
import org.jboss.sbomer.service.nextgen.workflow.model.WorkflowDefinition;
import org.jboss.sbomer.service.nextgen.workflow.model.WorkflowSpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.qute.CompletedStage;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.ValueResolver;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class WorkflowProcessor {

    public static final String CONFIG_KEY = "workflows-config.yaml";

    public static class JsonNodeResolver implements ValueResolver {
        @Override
        public boolean appliesTo(EvalContext context) {
            return context.getBase() instanceof JsonNode;
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            JsonNode node = (JsonNode) context.getBase();
            String name = context.getName();
            if (node.has(name)) {
                if (node.get(name).isTextual()) {
                    return CompletedStage.of(node.get(name).asText());
                } else if (node.get(name).isNumber()) {
                    return CompletedStage.of(node.get(name).asLong());
                } else if (node.get(name).isBoolean()) {
                    return CompletedStage.of(node.get(name).asBoolean());
                } else {
                    return CompletedStage.of(node.get(name));
                }
            }
            return null;
        }

    }

    KubernetesClient kubernetesClient;
    String sbomerReleaseName;

    Engine engine = Engine.builder()
            .addDefaults()
            .addValueResolver(new ReflectionValueResolver())
            .addValueResolver(new JsonNodeResolver())
            // .strictRendering(false)
            .build();

    String template;
    WorkflowDefinition config;

    @Inject
    public WorkflowProcessor(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        this.sbomerReleaseName = ConfigUtils.getRelease();
    }

    public String getTemplate() {
        if (template == null) {
            updateTemplate();
        }

        return template;
    }

    @Scheduled(every = "20s", delay = 10, delayUnit = TimeUnit.SECONDS, concurrentExecution = ConcurrentExecution.SKIP)
    public void updateTemplate() {
        this.template = getCmContent(cmName());
    }

    /**
     * Returns the content of the ConfigMap particular key as a {@code String}.
     *
     * @return CM content
     */
    private String getCmContent(String configMapName) {
        ConfigMap configMap = kubernetesClient.configMaps().withName(configMapName).get();

        if (configMap == null) {
            log.debug("Could not find '{}' ConfigMap", configMapName);
            return null;
        }

        if (configMap.getData() == null) {
            log.debug("'{}' ConfigMap content is empty", configMapName);
            return null;
        }

        if (configMap.getData().get(CONFIG_KEY) == null) {
            log.debug("'{}' ConfigMap does not contain the '{}' key", configMapName, CONFIG_KEY);
            return null;
        }

        return configMap.getData().get(CONFIG_KEY);
    }

    public List<WorkflowSpec> eval(Event event) {
        log.debug("Received event to evaluate");
        log.trace(event.toString());

        log.debug("Evaluating workflow definition with received event...");

        String evaluated;

        try {
            evaluated = engine.parse(getTemplate()).data(Map.of("event", event)).render();
        } catch (TemplateException e) {

            log.error("Unable to parse workflow definition", e);

            return Collections.emptyList();
        }

        log.debug("Definition successfully evaluated");
        log.trace(evaluated);

        WorkflowDefinition definition;

        try {
            definition = JacksonUtils
                    .parse(WorkflowDefinition.class, (ObjectNode) ObjectMapperProvider.yaml().readTree(evaluated));
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Unable to read evaluated workflow definition", e);
        }

        log.debug("Finding matching workflows for event...");

        // Filter workflows to match only these that use:
        //
        // * event as the trigger
        // * all conditions match
        List<WorkflowSpec> matchedWorkflows = definition.workflows()
                .stream()
                .filter(
                        w -> w.triggers().stream().anyMatch(t -> t instanceof EventTrigger)
                                && w.conditions().stream().allMatch(c -> c.isMet()))
                .toList();

        log.debug("Found {} matching workflow(s)", matchedWorkflows.size());

        return matchedWorkflows;
    }

    /**
     * Constructs the ConfigMap name for profiles based on the SBOMer release name.
     *
     * @return Name of the ConfigMap holding generation profiles.
     */
    private String cmName() {
        return sbomerReleaseName + "-workflow-config";
    }

}
