package org.jboss.sbomer.service.test.unit.nextgen.unit.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.api.Generator;
import org.jboss.sbomer.service.nextgen.core.dto.api.Target;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.GenerationStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.workflow.WorkflowProcessor;
import org.jboss.sbomer.service.nextgen.workflow.WorkflowSpec;
import org.jboss.sbomer.service.nextgen.workflow.actions.EventAction;
import org.jboss.sbomer.service.nextgen.workflow.conditions.ExpressionCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class WorkflowTest {

    private static final String CM_NAME = "sbomer-workflow-config";

    WorkflowProcessor workflow;

    KubernetesClient kubernetesClientMock;
    MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> mockedConfigMapOps;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void beforeEach() throws JsonMappingException, JsonProcessingException {
        mockedConfigMapOps = mock(MixedOperation.class, RETURNS_DEEP_STUBS);
        kubernetesClientMock = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);

        when(kubernetesClientMock.configMaps()).thenReturn(mockedConfigMapOps);

        workflow = new WorkflowProcessor(kubernetesClientMock);
    }

    private void withWorkflowDefinition(Supplier<String> supplier) {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName(CM_NAME)
                .endMetadata()
                .addToData(WorkflowProcessor.CONFIG_KEY, supplier.get())
                .build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);
    }

    @Test
    void testSimpleDefinition() throws Exception {
        withWorkflowDefinition(
                () -> """
                        workflows:
                            - triggers:
                                - type: event
                                  name: "generation.status.change"
                              conditions:
                                - type: expression
                                  if: {#if event.generation.status.toName() == 'FINISHED' && event.generation.request.generator.name == 'syft'}true{#else}false{/if}
                              actions:
                                - type: event
                                  name: "generation.request.adjust"
                                  payload:
                                      generation: "{event.generation.id}"
                        """);

        GenerationRecord generationRecord = new GenerationRecord(
                "G",
                Instant.now(),
                Instant.now(),
                null,
                JacksonUtils.toObjectNode(
                        new GenerationRequest(
                                new Generator("syft", "1.16.0", null),
                                new Target("CONTAINER_IMAGE", "quay.io/org/image1:tag"))),
                null,
                GenerationStatus.FINISHED,
                null,
                null);

        List<WorkflowSpec> workflows = workflow.eval(new GenerationStatusChangeEvent(generationRecord));

        assertEquals(1, workflows.size());

        WorkflowSpec workflowSpec = workflows.get(0);

        assertEquals(1, workflowSpec.conditions().size());
        assertTrue(workflowSpec.conditions().get(0) instanceof ExpressionCondition);

        ExpressionCondition condition = (ExpressionCondition) workflowSpec.conditions().get(0);

        // This is the crucial part.
        assertTrue(condition.isMet());

        assertEquals(1, workflowSpec.actions().size());

        EventAction action = (EventAction) workflowSpec.actions().get(0);

        assertEquals("G", action.payload().get("generation").asText());
    }

    @Test
    void testMultipleWorkflows() throws Exception {
        withWorkflowDefinition(
                () -> """
                        workflows:
                            - triggers:
                                - type: event
                                  name: "generation.status.change"
                              conditions:
                                - type: expression
                                  if: {#if event.generation.status.toName() == 'FINISHED' && event.generation.request.generator.name == 'syft'}true{#else}false{/if}
                              actions:
                                - type: event
                                  name: "generation.request.adjust"
                                  payload:
                                      generation: "{event.generation.id}"
                            - triggers:
                                - type: event
                                  name: "unknown"
                              conditions:
                                - type: expression
                                  if: {unsupported.name == 'syft'}
                              actions:
                                - type: event
                                  name: "generation.request.adjust"
                                  payload:
                                      test: true
                        """);

        GenerationRecord generationRecord = new GenerationRecord(
                "G",
                Instant.now(),
                Instant.now(),
                null,
                JacksonUtils.toObjectNode(
                        new GenerationRequest(
                                new Generator("syft", "1.16.0", null),
                                new Target("CONTAINER_IMAGE", "quay.io/org/image1:tag"))),
                null,
                GenerationStatus.FINISHED,
                null,
                null);

        List<WorkflowSpec> workflows = workflow.eval(new GenerationStatusChangeEvent(generationRecord));

        // Only one workflow was evaluated for this type of event
        assertEquals(1, workflows.size());
    }

    @Test
    void testNoWorkflowsToRun() throws Exception {
        withWorkflowDefinition(
                () -> """
                        workflows:
                            - triggers:
                                - type: event
                                  name: "other.status.change"
                              conditions:
                                - type: expression
                                  if: {#if nothing.generation.status.toName() == 'FINISHED' && nothing.generation.request.generator.name == 'syft'}true{#else}false{/if}
                              actions:
                                - type: event
                                  name: "generation.request.adjust"
                                  payload:
                                      generation: "{event.generation.id}"
                            - triggers:
                                - type: event
                                  name: "unknown"
                              conditions:
                                - type: expression
                                  if: {unsupported.name == 'syft'}
                              actions:
                                - type: event
                                  name: "generation.request.adjust"
                                  payload:
                                      test: true
                        """);

        GenerationRecord generationRecord = new GenerationRecord(
                "G",
                Instant.now(),
                Instant.now(),
                null,
                JacksonUtils.toObjectNode(
                        new GenerationRequest(
                                new Generator("syft", "1.16.0", null),
                                new Target("CONTAINER_IMAGE", "quay.io/org/image1:tag"))),
                null,
                GenerationStatus.FINISHED,
                null,
                null);

        List<WorkflowSpec> workflows = workflow.eval(new GenerationStatusChangeEvent(generationRecord));

        assertTrue(workflows.isEmpty());
    }
}
