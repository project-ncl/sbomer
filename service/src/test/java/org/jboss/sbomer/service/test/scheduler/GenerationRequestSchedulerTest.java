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
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

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

        EntityManager em = mock(EntityManager.class);
        when(requestRepository.getEntityManager()).thenReturn(em);

        Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        when(
                em.createNativeQuery(
                        "SELECT * FROM sbom_generation_request WHERE status = 'NEW' ORDER BY creation_time ASC FOR UPDATE SKIP LOCKED LIMIT 5",
                        SbomGenerationRequest.class))
                .thenReturn(query);

        scheduler.scheduleGenerations();

        verify(em, times(1)).createNativeQuery(anyString(), any(Class.class));
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

        SbomGenerationRequest req1 = new SbomGenerationRequest();
        SbomGenerationRequest req2 = new SbomGenerationRequest();

        EntityManager em = mock(EntityManager.class);
        when(requestRepository.getEntityManager()).thenReturn(em);

        Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(List.of(req1, req2));

        when(
                em.createNativeQuery(
                        "SELECT * FROM sbom_generation_request WHERE status = 'NEW' ORDER BY creation_time ASC FOR UPDATE SKIP LOCKED LIMIT 5",
                        SbomGenerationRequest.class))
                .thenReturn(query);

        doNothing().when(scheduler).schedule(any());

        scheduler.scheduleGenerations();

        assertEquals(SbomGenerationStatus.SCHEDULED, req1.getStatus());
        assertEquals(SbomGenerationStatus.SCHEDULED, req2.getStatus());

        verify(em, times(1)).createNativeQuery(anyString(), any(Class.class));
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
        when(cmResource.serverSideApply())
                .thenReturn(new GenerationRequestBuilder(GenerationRequestType.ANALYSIS).build());

        when(kubernetesClient.configMaps().resource(cmCaptor.capture())).thenReturn(cmResource);

        scheduler.schedule(request);

        GenerationRequest cmRequest = cmCaptor.getValue();

        assertEquals(GenerationRequestType.ANALYSIS, cmRequest.getType());
        assertEquals("GEN1", cmRequest.getId());
        assertEquals("ANALYSISID", cmRequest.getIdentifier());
    }

}