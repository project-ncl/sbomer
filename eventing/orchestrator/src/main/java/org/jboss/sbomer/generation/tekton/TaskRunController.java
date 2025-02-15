package org.jboss.sbomer.generation.tekton;

import java.util.Objects;

import org.jboss.logging.Logger;
import org.jboss.sbomer.generation.status.GenerationStatus;
import org.jboss.sbomer.generation.status.KafkaGenerationStatusProducer;

import io.fabric8.tekton.pipeline.v1.TaskRun;
import io.fabric8.tekton.pipeline.v1.TaskRunStatus;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.inject.Inject;

@ControllerConfiguration(
        labelSelector = "app.kubernetes.io/part-of=sbomer,app.kubernetes.io/component=sbom,app.kubernetes.io/managed-by=sbomer,sbomer.jboss.org/type=generation-request",
        namespaces = { Constants.WATCH_CURRENT_NAMESPACE })
public class TaskRunController implements Reconciler<TaskRun>, Cleaner<TaskRun> {
    private static final Logger log = Logger.getLogger(TaskRunController.class);

    @Inject
    KafkaGenerationStatusProducer producer;

    @Override
    public UpdateControl<TaskRun> reconcile(TaskRun resource, Context<TaskRun> context) throws Exception {
        log.infof("Reconciling on '%s'", resource.getMetadata().getName());

        TaskRunStatus status = resource.getStatus();

        if (status == null) {
            return UpdateControl.noUpdate();
        }

        // Temporarily, just show all conditions
        status.getConditions().forEach(c -> {
            log.debugf(
                    "Condition '%s', status: '%s', reason: '%s', message: '%s'",
                    c.getType(),
                    c.getStatus(),
                    c.getReason(),
                    c.getMessage());
        });

        // TODO: We are resending events when the controller starts
        if (Boolean.FALSE.equals(isFinished(resource))) {
            producer.send(GenerationStatus.GENERATING);
            return UpdateControl.noUpdate();
        }

        if (Boolean.TRUE.equals(isSuccessful(resource))) {
            producer.send(GenerationStatus.FINISHED);
        } else {
            producer.send(GenerationStatus.FAILED);
        }

        return UpdateControl.noUpdate();
    }

    /**
     * Checks whether given {@link TaskRun} has finished or not.
     *
     * @param taskRun The {@link TaskRun} to check
     * @return {@code true} if the {@link TaskRun} finished, {@code false} otherwise
     */
    protected boolean isFinished(TaskRun taskRun) {
        if (taskRun.getStatus() != null && taskRun.getStatus().getConditions() != null
                && !taskRun.getStatus().getConditions().isEmpty()
                && (Objects.equals(taskRun.getStatus().getConditions().get(0).getStatus(), "True")
                        || Objects.equals(taskRun.getStatus().getConditions().get(0).getStatus(), "False"))) {

            log.tracef("TaskRun '%s' finished", taskRun.getMetadata().getName());
            return true;
        }

        log.tracef("TaskRun '%s' still running", taskRun.getMetadata().getName());
        return false;
    }

    /**
     * Checks whether given {@link TaskRun} has finished successfully.
     *
     * @param taskRun The {@link TaskRun} to check
     * @return {@code true} if the {@link TaskRun} finished successfully, {@code false} otherwise or {@code null} in
     *         case it is still in progress.
     */
    protected Boolean isSuccessful(TaskRun taskRun) {
        if (!isFinished(taskRun)) {
            log.tracef("TaskRun '%s' still in progress", taskRun.getMetadata().getName());
            return null; // FIXME: This is not really binary, but trinary state
        }

        if (taskRun.getStatus() != null && taskRun.getStatus().getConditions() != null
                && !taskRun.getStatus().getConditions().isEmpty()
                && Objects.equals(taskRun.getStatus().getConditions().get(0).getStatus(), "True")) {
            log.tracef("TaskRun '%s' finished successfully", taskRun.getMetadata().getName());
            return true;
        }

        log.tracef("TaskRun '%s' failed", taskRun.getMetadata().getName());
        return false;
    }

    @Override
    public DeleteControl cleanup(TaskRun resource, Context<TaskRun> context) {
        log.debugf("TaskRun '%s' was removed from the system", resource.getMetadata().getName());
        return DeleteControl.defaultDelete();
    }
}
