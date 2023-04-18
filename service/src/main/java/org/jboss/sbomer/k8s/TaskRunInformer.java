package org.jboss.sbomer.k8s;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.jboss.sbomer.core.enums.SbomStatus;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.service.SbomRepository;
import org.jboss.sbomer.tekton.AbstractTektonTaskRunner;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class TaskRunInformer {

	@Inject
	KubernetesClient kubernetesClient;

	@Inject
	SbomRepository sbomRepository;

	SharedInformerFactory factory;

	Map<String, SbomStatus> statusCache = new HashMap<>(0);

	/**
	 * A resync period sync. At this interval -- we will re-request information about all resources.
	 */
	private final long resyncPeriod = 60 * 1000L;

	@Startup
	public void onStart() {
		factory = kubernetesClient.informers();

		Map<String, String[]> labels = new HashMap<>(1);
		labels.put("app.kubernetes.io/name", new String[] { "sbomer" });

		SharedIndexInformer<TaskRun> informer = factory.sharedIndexInformerFor(TaskRun.class, resyncPeriod);

		informer.addEventHandler(new ResourceEventHandler<TaskRun>() {

			@Override
			public void onAdd(TaskRun taskRun) {
				handleTaskRunUpdate(taskRun);
			}

			@Override
			public void onUpdate(TaskRun oldTaskRun, TaskRun taskRun) {
				handleTaskRunUpdate(taskRun);
			}

			@Override
			public void onDelete(TaskRun taskRun, boolean deletedFinalStateUnknown) {

			}

		});

		factory.startAllRegisteredInformers();
	}

	@Transactional
	void handleTaskRunUpdate(TaskRun taskRun) {

		String sbomId = taskRun.getMetadata().getLabels().get(AbstractTektonTaskRunner.LABEL_SBOM_ID);

		if (sbomId == null) {
			log.warn("Found a Tekton TaskRun without the SBOM id label: {}, skipping", taskRun.getMetadata().getName());
			return;
		}

		// TaskRun does not have status yet, skipping
		if (taskRun.getStatus() == null) {
			return;
		}

		String taskRunStatus = Optional.ofNullable(taskRun.getStatus().getConditions())
				.map(Collection::stream)
				.orElseGet(Stream::empty)
				.findFirst()
				.orElse(null)
				.getStatus();

		SbomStatus status = null;

		switch (taskRunStatus) {
			case "Unknown":
				status = SbomStatus.IN_PROGRESS;
				break;
			case "True":
				status = SbomStatus.READY;
				break;
			case "False":
				status = SbomStatus.FAILED;
				break;
			default:
				break;
		}

		SbomStatus cachedStatus = statusCache.get(sbomId);

		if (cachedStatus == status) {
			// Nothing to do, the cached status is the same
		} else {
			Sbom sbom = sbomRepository.findById(Long.valueOf(sbomId));

			if (sbom == null) {
				log.warn("Could not find Sbom with id {}, skipping updating the status", sbomId);
				return;
			}

			// Update resource
			sbom.setStatus(status);
			// Update status
			statusCache.put(sbomId, status);
			// Save the resource
			sbomRepository.saveSbom(sbom);
		}
	}

	void onStop(@Observes ShutdownEvent ev) {
		factory.stopAllRegisteredInformers();
	}
}
