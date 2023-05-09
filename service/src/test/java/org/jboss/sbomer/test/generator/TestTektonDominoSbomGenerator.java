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
package org.jboss.sbomer.test.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.sbomer.core.utils.Constants;
import org.jboss.sbomer.generator.SbomGenerator;
import org.jboss.sbomer.tekton.generator.TektonDominoSbomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.client.dsl.V1beta1APIGroupDSL;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@QuarkusTest
@WithKubernetesTestServer
public class TestTektonDominoSbomGenerator {
    @Any
    @Inject
    Instance<SbomGenerator> generators;

    SbomGenerator generator;

    @InjectMock(convertScopes = true)
    TektonClient tektonClient;

    ObjectMapper mapper;

    @BeforeEach
    void init() {
        generator = generators.select(TektonDominoSbomGenerator.class).get();
        mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    @Test
    void testDefaultDominoArguments() {
        var v1beta1 = Mockito.mock(V1beta1APIGroupDSL.class);
        var taskRuns = Mockito.mock(MixedOperation.class);
        var resource = Mockito.mock(Resource.class);

        Mockito.when(taskRuns.resource(any())).thenReturn(resource);
        Mockito.when(v1beta1.taskRuns()).thenReturn(taskRuns);
        Mockito.when(tektonClient.v1beta1()).thenReturn(v1beta1);

        generator.generate(123456l);

        Mockito.verify(tektonClient.v1beta1().taskRuns(), Mockito.times(1)).resource(argThat(taskRun -> {
            assertNotNull(taskRun);
            assertEquals("sbomer-domino-123456-", taskRun.getMetadata().getGenerateName());
            assertEquals("sbomer-generate-domino", taskRun.getSpec().getTaskRef().getName());
            assertEquals("sbomer-sa", taskRun.getSpec().getServiceAccountName());
            assertEquals("123456", taskRun.getSpec().getParams().get(0).getValue().getStringVal());
            try {
                JsonNode config = mapper.readTree(taskRun.getSpec().getParams().get(1).getValue().getStringVal());
                String version = config.get("version").asText().toString();
                String additionalArgs = config.get("additional-args").asText().toString();
                assertEquals("0.0.88", version);
                assertEquals("--include-non-managed --warn-on-missing-scm", additionalArgs);
            } catch (JsonProcessingException e) {
                fail("Should not have thrown a parse error when processing the config object");
            }

            assertEquals("sbomer", taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_NAME_APP_PART_OF));
            assertEquals("123456", taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_SBOM_ID));
            assertEquals(1, taskRun.getSpec().getWorkspaces().size());
            assertEquals("data", taskRun.getSpec().getWorkspaces().get(0).getName());
            return true;
        }));
    }
}
