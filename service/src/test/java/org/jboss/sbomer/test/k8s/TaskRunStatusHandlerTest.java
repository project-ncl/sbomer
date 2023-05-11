/**
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
package org.jboss.sbomer.test.k8s;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import javax.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.jboss.logmanager.LogContext;
import org.jboss.sbomer.core.enums.SbomStatus;
import org.jboss.sbomer.core.utils.Constants;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.service.ProcessingService;
import org.jboss.sbomer.service.SbomRepository;
import org.jboss.sbomer.test.utils.InMemoryLogHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.fabric8.knative.internal.pkg.apis.ConditionBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunStatusBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@QuarkusTest
@WithKubernetesTestServer
public class TaskRunStatusHandlerTest {
    InMemoryLogHandler logHandler;

    @Inject
    TaskRunStatusHandlerUnderTesting statusHandler;

    @InjectMock
    SbomRepository sbomRepository;

    @InjectMock
    ProcessingService processingService;

    @BeforeEach
    void beforeEach() {
        logHandler = new InMemoryLogHandler();
        logHandler.setLevel(Level.ALL);
        LogContext.getLogContext().getLogger("").addHandler(logHandler);
        LogContext.getLogContext().getLogger("").setLevel(Level.ALL);

        statusHandler.getStatusCache().clear();
    }

    @AfterEach
    void deregisterHandler() {
        logHandler.getRecords().clear();
        LogContext.getLogContext().getLogger("").removeHandler(logHandler);
    }

    @Test
    void testTaskRunWithoutStatus() {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("somename")
                .withLabels(
                        Map.of(
                                Constants.TEKTON_LABEL_NAME_APP_PART_OF,
                                Constants.TEKTON_LABEL_VALUE_APP_PART_OF,
                                Constants.TEKTON_LABEL_SBOM_ID,
                                "123456"))
                .endMetadata()
                .build();
        assertFalse(statusHandler.isUpdateable(taskRun));
        assertThat(
                logHandler.getMessages(),
                CoreMatchers.hasItems("Found Tekton TaskRun without status ready: 'somename', skipping"));
    }

    @Test
    void testTaskRunWithEmptyStatus() {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("somename")
                .withLabels(
                        Map.of(
                                Constants.TEKTON_LABEL_NAME_APP_PART_OF,
                                Constants.TEKTON_LABEL_VALUE_APP_PART_OF,
                                Constants.TEKTON_LABEL_SBOM_ID,
                                "123456"))
                .endMetadata()
                .withStatus(new TaskRunStatusBuilder().build())
                .build();
        assertFalse(statusHandler.isUpdateable(taskRun));
        assertThat(
                logHandler.getMessages(),
                CoreMatchers.hasItems("Found Tekton TaskRun without status ready: 'somename', skipping"));
    }

    @Test
    void testTaskRunWithEmptyStatusConditions() {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("somename")
                .withLabels(
                        Map.of(
                                Constants.TEKTON_LABEL_NAME_APP_PART_OF,
                                Constants.TEKTON_LABEL_VALUE_APP_PART_OF,
                                Constants.TEKTON_LABEL_SBOM_ID,
                                "123456"))
                .endMetadata()
                .withStatus(new TaskRunStatusBuilder().withConditions(Collections.emptyList()).build())
                .build();
        assertFalse(statusHandler.isUpdateable(taskRun));
        assertThat(
                logHandler.getMessages(),
                CoreMatchers.hasItems("Found Tekton TaskRun without status ready: 'somename', skipping"));
    }

    @Test
    void testTaskRunWithValidContent() {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("somename")
                .withLabels(
                        Map.of(
                                Constants.TEKTON_LABEL_NAME_APP_PART_OF,
                                Constants.TEKTON_LABEL_VALUE_APP_PART_OF,
                                Constants.TEKTON_LABEL_SBOM_ID,
                                "123456"))
                .endMetadata()
                .withStatus(
                        new TaskRunStatusBuilder().withConditions(new ConditionBuilder().withStatus("True").build())
                                .build())
                .build();
        assertTrue(statusHandler.isUpdateable(taskRun));
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("TaskRun 'somename' is valid for processing"));
    }

    @Test
    void testStatusSuccess() {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("somename")
                .endMetadata()
                .withStatus(
                        new TaskRunStatusBuilder().withConditions(new ConditionBuilder().withStatus("True").build())
                                .build())
                .build();

        SbomStatus status = statusHandler.toStatus(statusHandler.findLastCondition(taskRun).get().getStatus());

        assertEquals(status, SbomStatus.READY);
    }

    @Test
    void testStatusInProgress() {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("somename")
                .endMetadata()
                .withStatus(
                        new TaskRunStatusBuilder().withConditions(new ConditionBuilder().withStatus("Unknown").build())
                                .build())
                .build();

        SbomStatus status = statusHandler.toStatus(statusHandler.findLastCondition(taskRun).get().getStatus());

        assertEquals(status, SbomStatus.IN_PROGRESS);
    }

    @Test
    void testStatusInFAilure() {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("somename")
                .endMetadata()
                .withStatus(
                        new TaskRunStatusBuilder().withConditions(new ConditionBuilder().withStatus("False").build())
                                .build())
                .build();

        SbomStatus status = statusHandler.toStatus(statusHandler.findLastCondition(taskRun).get().getStatus());

        assertEquals(status, SbomStatus.FAILED);
    }

    @Test
    void testStatusUknown() {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("somename")
                .endMetadata()
                .withStatus(
                        new TaskRunStatusBuilder()
                                .withConditions(new ConditionBuilder().withStatus("Ohlalalala").build())
                                .build())
                .build();

        SbomStatus status = statusHandler.toStatus(statusHandler.findLastCondition(taskRun).get().getStatus());

        assertNull(status);
        assertThat(
                logHandler.getMessages(),
                CoreMatchers.hasItems("Received unknown status from TaskRun: 'Ohlalalala'"));
    }

    @Test
    void testUpdateStatusWithEmptyCache() {
        Sbom sbom = new Sbom();

        assertTrue(statusHandler.getStatusCache().isEmpty());
        assertEquals(sbom.getStatus(), SbomStatus.NEW);

        Mockito.when(sbomRepository.findById(123456l)).thenReturn(sbom);
        Mockito.when(sbomRepository.saveSbom(sbom)).thenReturn(sbom);

        statusHandler.updateStatus("123456", SbomStatus.FAILED, null);

        Mockito.verify(sbomRepository, times(1)).findById(123456l);
        Mockito.verify(sbomRepository, times(1)).saveSbom(sbom);

        assertEquals(1, statusHandler.getStatusCache().size());
        assertEquals(SbomStatus.FAILED, statusHandler.getStatusCache().get("123456"));
        assertEquals(SbomStatus.FAILED, sbom.getStatus());
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("Updated Sbom id '123456' with status: 'FAILED'"));
    }

    @Test
    void testUpdateStatusWithSubsequentUpdates() {
        Sbom sbom = new Sbom();

        assertTrue(statusHandler.getStatusCache().isEmpty());
        assertEquals(sbom.getStatus(), SbomStatus.NEW);

        Mockito.when(sbomRepository.findById(123456l)).thenReturn(sbom);
        Mockito.when(sbomRepository.saveSbom(sbom)).thenReturn(sbom);

        // For more updates received with the same status we fetch and update the resource only once!
        statusHandler.updateStatus("123456", SbomStatus.IN_PROGRESS, null);
        statusHandler.updateStatus("123456", SbomStatus.IN_PROGRESS, null);
        statusHandler.updateStatus("123456", SbomStatus.IN_PROGRESS, null);
        statusHandler.updateStatus("123456", SbomStatus.IN_PROGRESS, null);
        statusHandler.updateStatus("123456", SbomStatus.READY, null);
        statusHandler.updateStatus("123456", SbomStatus.READY, null);

        Mockito.verify(processingService, times(1)).process(sbom);
        Mockito.verify(sbomRepository, times(2)).findById(123456l);
        Mockito.verify(sbomRepository, times(2)).saveSbom(sbom);

        assertEquals(1, statusHandler.getStatusCache().size());
        assertEquals(SbomStatus.READY, statusHandler.getStatusCache().get("123456"));
        assertEquals(SbomStatus.READY, sbom.getStatus());
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("Updated Sbom id '123456' with status: 'READY'"));
    }

    @Test
    void testTaskRunWithCompletedDeletion() {
        Sbom sbom = new Sbom();

        assertTrue(statusHandler.getStatusCache().isEmpty());
        assertEquals(sbom.getStatus(), SbomStatus.NEW);

        Mockito.when(sbomRepository.findById(13131313l)).thenReturn(sbom);
        Mockito.when(sbomRepository.saveSbom(sbom)).thenReturn(sbom);

        TaskRun inProgressTaskRun = new TaskRunBuilder().withNewMetadata()
                .withName("someothername")
                .withLabels(
                        Map.of(
                                Constants.TEKTON_LABEL_NAME_APP_PART_OF,
                                Constants.TEKTON_LABEL_VALUE_APP_PART_OF,
                                Constants.TEKTON_LABEL_SBOM_ID,
                                "13131313"))
                .endMetadata()
                .withStatus(
                        new TaskRunStatusBuilder().withConditions(new ConditionBuilder().withStatus("Unknown").build())
                                .build())
                .build();

        statusHandler.handleTaskRunUpdate(inProgressTaskRun);

        Mockito.verify(sbomRepository, times(1)).findById(13131313l);
        Mockito.verify(sbomRepository, times(1)).saveSbom(sbom);

        assertEquals(1, statusHandler.getStatusCache().size());
        assertEquals(SbomStatus.IN_PROGRESS, statusHandler.getStatusCache().get("13131313"));
        assertEquals(SbomStatus.IN_PROGRESS, sbom.getStatus());
        assertThat(
                logHandler.getMessages(),
                CoreMatchers.hasItems("Updated Sbom id '13131313' with status: 'IN_PROGRESS'"));

        TaskRun readyTaskRun = new TaskRunBuilder(inProgressTaskRun).editStatus()
                .withCompletionTime(String.valueOf(LocalDateTime.now()))
                .withConditions(
                        new ConditionBuilder().withStatus("True")
                                .withLastTransitionTime(String.valueOf(LocalDateTime.now()))
                                .build())
                .endStatus()
                .build();

        statusHandler.handleTaskRunUpdate(readyTaskRun);

        Mockito.verify(processingService, times(1)).process(sbom);
        Mockito.verify(sbomRepository, times(2)).findById(13131313l);
        Mockito.verify(sbomRepository, times(2)).saveSbom(sbom);

        assertEquals(1, statusHandler.getStatusCache().size());
        assertEquals(SbomStatus.READY, statusHandler.getStatusCache().get("13131313"));
        assertEquals(SbomStatus.READY, sbom.getStatus());
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("Updated Sbom id '13131313' with status: 'READY'"));

        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("TaskRun 'someothername' completed successfully."));
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("Deleting taskRun 'someothername'..."));
    }

    @Test
    void testTaskRunWithRetries() {
        Sbom sbom = new Sbom();

        assertTrue(statusHandler.getStatusCache().isEmpty());
        assertEquals(sbom.getStatus(), SbomStatus.NEW);

        Mockito.when(sbomRepository.findById(17171717l)).thenReturn(sbom);
        Mockito.when(sbomRepository.saveSbom(sbom)).thenReturn(sbom);

        TaskRun inProgressTaskRun = new TaskRunBuilder().withNewMetadata()
                .withName("somefunnyname")
                .withLabels(
                        Map.of(
                                Constants.TEKTON_LABEL_NAME_APP_PART_OF,
                                Constants.TEKTON_LABEL_VALUE_APP_PART_OF,
                                Constants.TEKTON_LABEL_SBOM_ID,
                                "17171717"))
                .endMetadata()
                .withStatus(
                        new TaskRunStatusBuilder().withConditions(new ConditionBuilder().withStatus("Unknown").build())
                                .build())
                .build();

        statusHandler.handleTaskRunUpdate(inProgressTaskRun);

        assertEquals(1, statusHandler.getStatusCache().size());
        assertEquals(SbomStatus.IN_PROGRESS, statusHandler.getStatusCache().get("17171717"));
        assertEquals(SbomStatus.IN_PROGRESS, sbom.getStatus());
        assertThat(
                logHandler.getMessages(),
                CoreMatchers.hasItems("Updated Sbom id '17171717' with status: 'IN_PROGRESS'"));

        TaskRun failedTaskRun = new TaskRunBuilder(inProgressTaskRun).editStatus()
                .withCompletionTime(String.valueOf(LocalDateTime.now()))
                .withConditions(
                        new ConditionBuilder().withStatus("False")
                                .withLastTransitionTime(String.valueOf(LocalDateTime.now()))
                                .build())
                .endStatus()
                .build();

        statusHandler.handleTaskRunUpdate(failedTaskRun);

        assertEquals(1, statusHandler.getStatusCache().size());
        assertEquals(SbomStatus.FAILED, statusHandler.getStatusCache().get("17171717"));
        assertEquals(SbomStatus.FAILED, sbom.getStatus());
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("Updated Sbom id '17171717' with status: 'FAILED'"));

        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("TaskRun 'somefunnyname' completed with failure."));
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("Retrying failed taskRun 'somefunnyname'..."));
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("Deleting taskRun 'somefunnyname'..."));
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("Creating new taskRun 'somefunnyname-retry-1'..."));
        assertThat(logHandler.getMessages(), CoreMatchers.hasItems("Updated Sbom id '17171717' with status: 'FAILED'"));

    }
}
