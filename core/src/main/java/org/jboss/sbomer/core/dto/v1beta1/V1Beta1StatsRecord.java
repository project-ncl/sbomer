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
package org.jboss.sbomer.core.dto.v1beta1;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "V1Beta1StatsRecord")
public record V1Beta1StatsRecord(
        V1Beta1StatsMessagingRecord messaging,
        V1Beta1StatsResourceRecord resources,
        V1Beta1StatsDeploymentRecord deployment,
        long uptimeMillis,
        String uptime,
        String version,
        String release,
        String appEnv,
        String hostname) {

    public record V1Beta1StatsResourceRecord(
            V1Beta1StatsResourceManifestsRecord manifests,
            V1Beta1StatsResourceGenerationsRecord generations) {
    }

    public record V1Beta1StatsResourceManifestsRecord(long total) {
    }

    public record V1Beta1StatsResourceGenerationsRecord(long total, long inProgress) {
    }

    public record V1Beta1StatsDeploymentRecord(String type, String target, String zone) {
    }

    public record V1Beta1StatsMessagingRecord(
            V1Beta1StatsMessagingPncConsumerRecord pncConsumer,
            V1Beta1StatsMessagingErrataConsumerRecord errataConsumer,
            V1Beta1StatsMessagingProducerRecord producer) {
    }

    public record V1Beta1StatsMessagingPncConsumerRecord(long received, long processed) {
    }

    public record V1Beta1StatsMessagingErrataConsumerRecord(long received, long processed) {
    }

    public record V1Beta1StatsMessagingProducerRecord(long nacked, long acked) {
    }
}
