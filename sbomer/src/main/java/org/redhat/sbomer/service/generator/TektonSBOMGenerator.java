package org.redhat.sbomer.service.generator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.pnc.dto.Build;
import org.redhat.sbomer.errors.ApplicationException;
import org.redhat.sbomer.service.PNCService;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ArrayOrString;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBindingBuilder;

@ApplicationScoped
public class TektonSBOMGenerator implements SBOMGenerator {

  @Inject
  PNCService pncService;

  @Inject
  TektonClient tektonClient;

  @Override
  public void generate(String buildId) throws ApplicationException {
    Build build = pncService.getBuild(buildId);

    PipelineRun pipelineRun = new PipelineRunBuilder()
        .withNewMetadata()
        .withGenerateName("sbom-")
        .endMetadata()
        .withNewSpec()
        .withNewPipelineRef().withName("sbom-generator").endPipelineRef()
        .withNewPodTemplate()
        .withSecurityContext(
            new PodSecurityContextBuilder().withFsGroup(65532l).withRunAsNonRoot().withRunAsUser(65532l).build())
        .endPodTemplate()
        .withParams(
            new Param("git-url", new ArrayOrString(build.getScmUrl())),
            new Param("git-rev", new ArrayOrString(build.getScmRevision())),
            new Param("build-id", new ArrayOrString(build.getId())),
            new Param("additional-domino-args", new ArrayOrString("--include-non-managed")))
        .withWorkspaces(
            new WorkspaceBindingBuilder().withName("data")
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSource("sbomer-data", false)).build())

        .endSpec()
        .build();

    tektonClient.v1beta1().pipelineRuns().resource(pipelineRun).createOrReplace();
  }

}
