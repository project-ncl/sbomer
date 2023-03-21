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
package org.redhat.sbomer.generator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.pnc.dto.Build;
import org.redhat.sbomer.errors.ApplicationException;
import org.redhat.sbomer.service.PNCService;
import org.redhat.sbomer.utils.enums.Processors;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ArrayOrString;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBindingBuilder;

@CycloneDX
@ApplicationScoped
public class TektonCycloneDXSbomGenerator implements SbomGenerator {

    @Inject
    PNCService pncService;

    @Inject
    TektonClient tektonClient;

    @Override
    public void generate(String buildId, Processors processor) throws ApplicationException {
        Build build = pncService.getBuild(buildId);
        String jdkVersion = "";
        if (build.getEnvironment().getAttributes() != null
                && build.getEnvironment().getAttributes().containsKey("JDK")) {
            jdkVersion = build.getEnvironment().getAttributes().get("JDK");
        }

        String mavenVersion = "";
        if (build.getEnvironment().getAttributes() != null
                && build.getEnvironment().getAttributes().containsKey("MAVEN")) {
            mavenVersion = build.getEnvironment().getAttributes().get("MAVEN");
        }

        PipelineRun pipelineRun = new PipelineRunBuilder().withNewMetadata()
                .withGenerateName("sbom-")
                .endMetadata()
                .withNewSpec()
                .withNewPipelineRef()
                .withName("sbom-generator-cyclonedx")
                .endPipelineRef()
                .withNewPodTemplate()
                .withSecurityContext(
                        new PodSecurityContextBuilder().withFsGroup(65532l)
                                .withRunAsNonRoot()
                                .withRunAsUser(65532l)
                                .build())
                .endPodTemplate()
                // TODO: make the below "additional-cyclonedx-args" and "cyclonedx-version" configurable
                .withParams(
                        new Param("git-url", new ArrayOrString(build.getScmUrl())),
                        new Param("git-rev", new ArrayOrString(build.getScmRevision())),
                        new Param("build-id", new ArrayOrString(build.getId())),
                        new Param("build-env-jdk", new ArrayOrString(jdkVersion)),
                        new Param("build-env-mvn", new ArrayOrString(mavenVersion)),
                        new Param(
                                "additional-cyclonedx-args",
                                new ArrayOrString("--batch-mode --no-transfer-progress --quiet")),
                        new Param("cyclonedx-version", new ArrayOrString("2.7.5")))
                .withWorkspaces(
                        new WorkspaceBindingBuilder().withName("data")
                                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSource("sbomer-data", false))
                                .build())

                .endSpec()
                .build();

        tektonClient.v1beta1().pipelineRuns().resource(pipelineRun).createOrReplace();
    }

}
