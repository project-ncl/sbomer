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
package org.jboss.sbomer.service.feature.sbom.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with")
@JsonInclude(Include.NON_EMPTY)
public class Stats {

    /**
     * Statistics related to message PNC consumer. All stats since application boot time.
     */
    @Data
    @Builder(setterPrefix = "with")
    public static class PncConsumer {
        long received;
        long processed;
    }

    /**
     * Statistics related to message Errata consumer. All stats since application boot time.
     */
    @Data
    @Builder(setterPrefix = "with")
    public static class ErrataConsumer {
        long received;
        long processed;
    }

    /**
     * Statistics related to message producer. All stats since application boot time.
     */
    @Data
    @Builder(setterPrefix = "with")
    public static class Producer {
        long nacked;
        long acked;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class Messaging {
        PncConsumer pncConsumer;
        ErrataConsumer errataConsumer;
        Producer producer;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class Resources {
        SbomStats sboms;
        GenerationRequestStats generationRequests;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class SbomStats {
        long total;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class GenerationRequestStats {
        long total;
        long inProgress;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class Deployment {
        String type;
        String target;
        String zone;
    }

    Messaging messaging;
    Resources resources;
    Deployment deployment;
    long uptimeMillis;
    String uptime;
    String version;
    String release;
    String appEnv;
    String hostname;
}
