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
package org.jboss.sbomer.service.test.feature.sbom.deprecated.generator;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
// @WithKubernetesTestServer
public class TestTektonCycloneDxSbomGenerator {
    // @Any
    // @Inject
    // Instance<SbomGenerator> generators;

    // SbomGenerator generator;

    // @InjectMock(convertScopes = true)
    // TektonClient tektonClient;

    // ObjectMapper mapper;

    // @BeforeEach
    // void init() {
    // generator = generators.select(TektonCycloneDXSbomGenerator.class).get();
    // mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    // }

    // @Test
    // void testDefaultCycloneDxArguments() {
    // var v1beta1 = Mockito.mock(V1beta1APIGroupDSL.class);
    // var taskRuns = Mockito.mock(MixedOperation.class);
    // var resource = Mockito.mock(Resource.class);

    // Mockito.when(taskRuns.resource(any())).thenReturn(resource);
    // Mockito.when(v1beta1.taskRuns()).thenReturn(taskRuns);
    // Mockito.when(tektonClient.v1beta1()).thenReturn(v1beta1);

    // generator.generate(12345l, "AABBCCDD");

    // Mockito.verify(tektonClient.v1beta1().taskRuns(), Mockito.times(1)).resource(argThat(taskRun -> {
    // assertNotNull(taskRun);
    // assertEquals("sbomer-cyclonedx-12345-", taskRun.getMetadata().getGenerateName());
    // assertEquals("sbomer-generate-cyclonedx", taskRun.getSpec().getTaskRef().getName());
    // assertEquals("sbomer-sa", taskRun.getSpec().getServiceAccountName());
    // assertEquals("12345", taskRun.getSpec().getParams().get(0).getValue().getStringVal());

    // try {
    // JsonNode config = mapper.readTree(taskRun.getSpec().getParams().get(1).getValue().getStringVal());
    // String version = config.get("version").asText().toString();
    // String additionalArgs = config.get("additional-args").asText().toString();
    // assertEquals("2.7.8", version);
    // assertEquals("--batch-mode", additionalArgs);
    // } catch (JsonProcessingException e) {
    // fail("Should not have thrown a parse error when processing the config object");
    // }

    // assertEquals("sbomer", taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_NAME_APP_PART_OF));
    // assertEquals("12345", taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_SBOM_ID));
    // assertEquals("AABBCCDD", taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_SBOM_BUILD_ID));
    // assertEquals(1, taskRun.getSpec().getWorkspaces().size());
    // assertEquals("data", taskRun.getSpec().getWorkspaces().get(0).getName());
    // return true;
    // }));
    // }
}
