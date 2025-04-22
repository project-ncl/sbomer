package org.jboss.sbomer.service.test.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;
import org.jboss.sbomer.service.leader.LeaderManager;
import org.jboss.sbomer.service.scheduler.GenerationRequestScheduler;
import org.jboss.sbomer.service.scheduler.GenerationSchedulerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.LockModeType;

public class GenerationRequestSchedulerTest {
    KubernetesClient kubernetesClient;

    GenerationRequestScheduler scheduler;

    LeaderManager leaderManager;

    SbomGenerationRequestRepository requestRepository;

    @BeforeEach
    void beforeEach() {
        GenerationSchedulerConfig schedulerConfig = mock(GenerationSchedulerConfig.class);

        when(schedulerConfig.maxConcurrentGenerations()).thenReturn(5);
        when(schedulerConfig.syncInterval()).thenReturn("10s");
        when(schedulerConfig.syncBatch()).thenReturn(5);

        this.leaderManager = mock(LeaderManager.class);

        requestRepository = mock(SbomGenerationRequestRepository.class);

        this.kubernetesClient = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);
        this.scheduler = spy(
                new GenerationRequestScheduler(requestRepository, kubernetesClient, schedulerConfig, leaderManager));

    }

    @Test
    void shouldDoNothingIfWeAreNotTheLeader() {
        when(leaderManager.isLeader()).thenReturn(false);

        scheduler.scheduleGenerations();

        verifyNoInteractions(kubernetesClient);
        verifyNoInteractions(requestRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotSyncIfThereIsNoCapacity() {
        when(leaderManager.isLeader()).thenReturn(true);

        FilterWatchListDeletable<ConfigMap, ConfigMapList, Resource<ConfigMap>> watchList = mock(
                FilterWatchListDeletable.class,
                RETURNS_DEEP_STUBS);

        ConfigMapList configMapList = mock(ConfigMapList.class, RETURNS_DEEP_STUBS);

        when(watchList.list()).thenReturn(configMapList);

        when(
                kubernetesClient.configMaps()
                        .withLabelSelector(
                                eq(
                                        "sbomer.jboss.org/type=generation-request,sbomer.jboss.org/status notin (FAILED, FINISHED)")))
                                                .thenReturn(watchList);

        // 10 already in progress
        when(watchList.list().getItems().size()).thenReturn(10);

        scheduler.scheduleGenerations();

        // Max concurrent generations is set to 5, so there is no capacity to take any new generations this time
        verifyNoInteractions(requestRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotCreateAnyNewGenerationsIfThereAreNoNewGenerationsInTheDatabase() {
        when(leaderManager.isLeader()).thenReturn(true);

        FilterWatchListDeletable<ConfigMap, ConfigMapList, Resource<ConfigMap>> watchList = mock(
                FilterWatchListDeletable.class,
                RETURNS_DEEP_STUBS);

        ConfigMapList configMapList = mock(ConfigMapList.class, RETURNS_DEEP_STUBS);

        when(watchList.list()).thenReturn(configMapList);
        MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps = mock(
                MixedOperation.class,
                RETURNS_DEEP_STUBS);

        when(kubernetesClient.configMaps()).thenReturn(configMaps);

        when(
                configMaps.withLabelSelector(
                        eq(
                                "sbomer.jboss.org/type=generation-request,sbomer.jboss.org/status notin (FAILED, FINISHED)")))
                                        .thenReturn(watchList);

        // 1 already in progress
        when(watchList.list().getItems().size()).thenReturn(1);

        PanacheQuery<SbomGenerationRequest> panacheQuery = mock(PanacheQuery.class, RETURNS_DEEP_STUBS);

        when(requestRepository.find(eq("status = ?1 ORDER BY creationTime ASC"), eq(SbomGenerationStatus.NEW)))
                .thenReturn(panacheQuery);

        // Nothing in the DB to sync
        when(panacheQuery.withLock(eq(LockModeType.PESSIMISTIC_WRITE)).page(eq(0), eq(5)).list())
                .thenReturn(Collections.emptyList());

        scheduler.scheduleGenerations();

        verify(requestRepository, times(1)).find(anyString(), any(SbomGenerationStatus.class));
        verify(configMaps, never()).resource(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSyncWithCluster() {
        when(leaderManager.isLeader()).thenReturn(true);

        var configMapList = mock(ConfigMapList.class);
        when(configMapList.getItems()).thenReturn(List.of(new ConfigMap())); // already 1 in progress

        var watchList = mock(FilterWatchListDeletable.class);
        when(watchList.list()).thenReturn(configMapList);

        var configMaps = mock(MixedOperation.class);
        when(kubernetesClient.configMaps()).thenReturn(configMaps);
        when(
                configMaps.withLabelSelector(
                        "sbomer.jboss.org/type=generation-request,sbomer.jboss.org/status notin (FAILED, FINISHED)"))
                                .thenReturn(watchList);

        PanacheQuery<SbomGenerationRequest> panacheQuery = mock(PanacheQuery.class, RETURNS_DEEP_STUBS);

        when(requestRepository.find(eq("status = ?1 ORDER BY creationTime ASC"), eq(SbomGenerationStatus.NEW)))
                .thenReturn(panacheQuery);

        SbomGenerationRequest req1 = new SbomGenerationRequest();
        SbomGenerationRequest req2 = new SbomGenerationRequest();

        when(panacheQuery.withLock(eq(LockModeType.PESSIMISTIC_WRITE)).page(eq(0), eq(5)).list())
                .thenReturn(List.of(req1, req2));

        doNothing().when(scheduler).schedule(any());

        scheduler.scheduleGenerations();

        assertEquals(SbomGenerationStatus.SCHEDULED, req1.getStatus());
        assertEquals(SbomGenerationStatus.SCHEDULED, req2.getStatus());

        verify(requestRepository, times(1)).find(anyString(), any(SbomGenerationStatus.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldSchedule() {
        var request = new SbomGenerationRequest();
        request.setId("GEN1");
        request.setType(GenerationRequestType.ANALYSIS);
        request.setIdentifier("ANALYSISID");

        when(kubernetesClient.configMaps().withName(anyString())).thenReturn(mock(Resource.class));

        ArgumentCaptor<GenerationRequest> cmCaptor = ArgumentCaptor.forClass(GenerationRequest.class);

        Resource<ConfigMap> cmResource = mock(Resource.class);
        when(cmResource.create()).thenReturn(new GenerationRequestBuilder(GenerationRequestType.ANALYSIS).build());

        when(kubernetesClient.configMaps().resource(cmCaptor.capture())).thenReturn(cmResource);

        scheduler.schedule(request);

        GenerationRequest cmRequest = cmCaptor.getValue();

        assertEquals(GenerationRequestType.ANALYSIS, cmRequest.getType());
        assertEquals("GEN1", cmRequest.getId());
        assertEquals("ANALYSISID", cmRequest.getIdentifier());
    }

}