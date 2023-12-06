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
package org.jboss.sbomer.service.feature.sbom.features.umb.producer;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.GenerationFinishedMessageBody;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Unremovable
@Slf4j
public class AmqpMessageProducer {

	@Inject
	@Channel("generation-finished")
	Emitter<GenerationFinishedMessageBody> emitter;

	public void notify(GenerationFinishedMessageBody msg) {
		log.info("About to send notification for finished SBOM generation using the AMQP producer");
		CompletionStage<Void> acked = emitter.send(msg);
		acked.toCompletableFuture().join();
	}
}
