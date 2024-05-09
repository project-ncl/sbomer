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
package org.jboss.sbomer.service.feature.sbom.service;

import java.util.List;

import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.Configuration.ConfigurationBuilder;
import org.jboss.pnc.client.ProductMilestoneClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.DeliverableAnalyzerOperation;
import org.jboss.pnc.dto.requests.DeliverablesAnalysisRequest;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.service.feature.sbom.config.SbomerConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class PncClientsWrapper {

    @Inject
    SbomerConfig sbomerConfig;

    ProductMilestoneClient productMilestoneClient;

    @PostConstruct
    void init() {
        productMilestoneClient = new ProductMilestoneClient(getConfiguration());
    }

    @PreDestroy
    void cleanup() {
        productMilestoneClient.close();
    }

    /**
     * Setup basic configuration to be able to talk to PNC.
     *
     *
     * @return
     */
    public Configuration getConfiguration() {
        ConfigurationBuilder configurationBuilder = Configuration.builder()
                .host(sbomerConfig.pnc().host())
                .protocol("http");
        return configurationBuilder.build();
    }

    /**
     * Triggers a new Deliverable Analysis operation in PNC for the specified milestone and urls.
     *
     * @param milestoneId The milestone identifier for the new deliverable analysis operation
     * @param deliverableUrls The list of deliverable URLs to be analyzed
     * @return The {@link DeliverableAnalyzerOperation} object identifying the new operation
     */
    public DeliverableAnalyzerOperation analyzeDeliverables(String milestoneId, List<String> deliverableUrls) {
        DeliverablesAnalysisRequest request = DeliverablesAnalysisRequest.builder()
                .deliverablesUrls(deliverableUrls)
                .runAsScratchAnalysis(false)
                .build();
        try {
            log.info(
                    "Triggering new deliverable analysis operation for the milestone {} and urls '{}'",
                    milestoneId,
                    deliverableUrls);
            return productMilestoneClient.analyzeDeliverables(milestoneId, request);
        } catch (RemoteResourceException ex) {
            throw new ClientException(
                    "A Deliverable Analysis Operation could not be started because PNC responded with an error",
                    ex);
        }
    }

}
