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
package org.jboss.sbomer.tekton;

import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.jboss.sbomer.core.utils.Constants;
import org.jboss.sbomer.core.utils.MDCUtils;

import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ArrayOrString;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBindingBuilder;

public abstract class AbstractTektonTaskRunner {

    @Inject
    TektonClient tektonClient;

    /**
     * Runs specific {@link Task}.
     *
     * @param tektonTaskName Name of the {@link Task}.
     * @param id The PNC build identifier.
     * @param config Additional configuration passed as a {@link JsonObject}.
     * @return A {@link TaskRun} object representing the execution.
     */
    protected TaskRun runTektonTask(
            final String tektonTaskName,
            final Long id,
            final String buildId,
            final JsonObject config) {

        try {
            // make sure there is no context
            MDCUtils.removeContext();
            MDCUtils.addBuildContext(buildId);
            MDCUtils.addProcessContext(String.valueOf(id));

            TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                    .withGenerateName(toTaskRunNamePrefix(tektonTaskName, String.valueOf(id)))
                    .withLabels(
                            Map.of(
                                    Constants.TEKTON_LABEL_NAME_APP_PART_OF,
                                    Constants.TEKTON_LABEL_VALUE_APP_PART_OF,
                                    Constants.TEKTON_LABEL_SBOM_ID,
                                    String.valueOf(id),
                                    Constants.TEKTON_LABEL_SBOM_BUILD_ID,
                                    buildId))
                    .endMetadata()
                    .withNewSpec()
                    .withServiceAccountName(Constants.TEKTON_SERVICE_ACCOUNT_NAME)
                    .withNewTaskRef()
                    .withName(tektonTaskName)
                    .endTaskRef()
                    .withNewPodTemplate()
                    .withSecurityContext(
                            new PodSecurityContextBuilder().withFsGroup(65532l)
                                    .withRunAsNonRoot()
                                    .withRunAsUser(65532l)
                                    .build())
                    .endPodTemplate()
                    .withParams(
                            new Param("id", new ArrayOrString(String.valueOf(id))),
                            new Param("config", new ArrayOrString(config.toString())))
                    .withWorkspaces(
                            new WorkspaceBindingBuilder().withName("data")
                                    .withEmptyDir(new EmptyDirVolumeSource())
                                    .build())
                    .endSpec()
                    .build();

            return tektonClient.v1beta1().taskRuns().resource(taskRun).createOrReplace();
        } finally {
            MDCUtils.removeContext();
        }
    }

    /**
     * Based on the provided {@link Task} we generate a string that could be used as the prefix for the generated
     * {@link TaskRun} name based on the {@link Task}.
     *
     * @param tektonTaskName The Tekton {@link Task} name.
     * @return Prefix
     */
    private String toTaskRunNamePrefix(final String tektonTaskName, final String suffix) {
        var parts = tektonTaskName.split("-");

        String type = parts.length > 2 ? parts[2] : parts[1];

        return parts[0] + "-" + type + "-" + suffix.toLowerCase() + "-";
    }
}
