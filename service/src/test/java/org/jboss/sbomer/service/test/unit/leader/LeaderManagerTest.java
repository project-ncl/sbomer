package org.jboss.sbomer.service.test.unit.leader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.jboss.sbomer.service.leader.LeaderManager;
import org.jboss.sbomer.service.leader.LeaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class LeaderManagerTest {
    LeaderManager leaderManager;
    KubernetesClient kubernetesClient;

    @BeforeEach
    void beforeEach() {
        LeaseConfig leaseConfig = mock(LeaseConfig.class);

        when(leaseConfig.leaseDuration()).thenReturn(30);
        when(leaseConfig.checkInterval()).thenReturn("10s");

        this.kubernetesClient = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);
        this.leaderManager = new LeaderManager(leaseConfig, kubernetesClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDoNothingIfWeAreNotTheLeaderAndLeaseIsValid() {
        var now = ZonedDateTime.now(ZoneId.of("UTC"));
        // Will expire in 10 seconds
        var futureRenewTime = ZonedDateTime.ofInstant(now.toInstant().plusSeconds(10), ZoneId.of("UTC"));

        var lease = mock(Lease.class, RETURNS_DEEP_STUBS);
        when(lease.getSpec().getRenewTime()).thenReturn(futureRenewTime);
        when(lease.getSpec().getHolderIdentity()).thenReturn("someone-else");

        var leaseResource = mock(Resource.class);
        when(leaseResource.get()).thenReturn(lease);

        var leases = mock(MixedOperation.class);
        when(leases.withName("sbomer-generation-scheduler")).thenReturn(leaseResource);

        when(kubernetesClient.leases()).thenReturn(leases);

        leaderManager.lease();

        assertFalse(leaderManager.isLeader());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBecomeLeaderIfThereAreNoLeases() {
        var leaseResource = mock(Resource.class);
        // No leases whatsoever
        when(leaseResource.get()).thenReturn(null);

        var leases = mock(MixedOperation.class);
        when(leases.resource(isA(Lease.class))).thenReturn(leaseResource);
        when(leases.withName("sbomer-generation-scheduler")).thenReturn(leaseResource);

        when(kubernetesClient.leases()).thenReturn(leases);

        leaderManager.lease();

        assertTrue(leaderManager.isLeader());
        verify(leases, times(1)).withName("sbomer-generation-scheduler");
        verify(leaseResource, times(1)).createOrReplace();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBecomeLeaderIfLeaseExpired() {
        var now = ZonedDateTime.now(ZoneId.of("UTC"));
        // Expired 10 sec ago
        var futureRenewTime = ZonedDateTime.ofInstant(now.toInstant().minusSeconds(10), ZoneId.of("UTC"));

        var lease = mock(Lease.class, RETURNS_DEEP_STUBS);
        when(lease.getSpec().getRenewTime()).thenReturn(futureRenewTime);
        when(lease.getSpec().getHolderIdentity()).thenReturn("someone-else");

        var leaseResource = mock(Resource.class);
        when(leaseResource.get()).thenReturn(lease);

        var leases = mock(MixedOperation.class);
        when(leases.resource(isA(Lease.class))).thenReturn(leaseResource);
        when(leases.withName("sbomer-generation-scheduler")).thenReturn(leaseResource);

        when(kubernetesClient.leases()).thenReturn(leases);

        leaderManager.lease();

        assertTrue(leaderManager.isLeader());
        verify(leases, times(1)).withName("sbomer-generation-scheduler");
        verify(leaseResource, times(1)).createOrReplace();
    }

}
