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
package org.jboss.sbomer.service.feature;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.getunleash.FeatureToggle;
import io.getunleash.Unleash;
import io.getunleash.event.UnleashSubscriber;
import io.getunleash.repository.FeatureToggleResponse;
import io.quarkus.arc.Unremovable;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * An entrypoint for all feature flags supported in SBOMer.
 */
@ApplicationScoped
@Unremovable
@Getter
@Slf4j
public class FeatureFlags implements UnleashSubscriber {
    public static final String EVENT_NAME = "feature-flag-state-change";
    public static final String TOGGLE_DRY_RUN = "dry-run";
    public static final String TOGGLE_S3_STORAGE = "s3-storage";

    /**
     * A map holding all toggle values we are interested in. This is used for logging purposes. We are retrieving the
     * state of the toggle via {@link Unleash} object instead.
     */
    private final Map<String, Boolean> toggleValues = new HashMap<>();

    @Inject
    Unleash unleash;

    @Inject
    EventBus bus;

    /** 
     * Returns {@code true} in case the dry-run mode is enabled.
     *
     * @return {@code true} if dry-run is enabled, {@code false} otherwise
     */
    public boolean isDryRun() {
        return unleash.isEnabled(TOGGLE_DRY_RUN, false);
    }

    /**
     * Returns {@code true} if storing logs in S3 bucket is enabled.
     *
     * @return {@code true} if s3 support for logs is enabled, {@code false} otherwise
     */
    public boolean s3Storage() {
        return unleash.isEnabled(TOGGLE_S3_STORAGE, false);
    }

    private void updateToggles(final FeatureToggleResponse toggleResponse) {
        for (String toggleName : Set.of(TOGGLE_DRY_RUN, TOGGLE_S3_STORAGE)) {
            FeatureToggle toggle = toggleResponse.getToggleCollection().getToggle(toggleName);

            if (toggle != null) {
                Boolean previousValue = toggleValues.put(toggleName, toggle.isEnabled());

                if (previousValue == null || previousValue != toggle.isEnabled()) {
                    log.info("Feature toggle {} was just {}", toggleName, toggle.isEnabled() ? "enabled" : "disabled");
                    bus.publish(EVENT_NAME, Map.of(toggleName, toggle.isEnabled()));
                }
            } else {
                log.debug("Feature toggle {} was disabled", toggleName);
                toggleValues.remove(toggleName);
            }
        }

        try {
            log.debug("Current feature toggles: {}", ObjectMapperProvider.json().writeValueAsString(toggleValues));
        } catch (JsonProcessingException e) {
            // Ignored
        }
    }

    /**
     * A callback which will be called when feature flags will be retreieved.
     */
    @Override
    public void togglesFetched(FeatureToggleResponse toggleResponse) {
        switch (toggleResponse.getStatus()) {
            case CHANGED:
                log.debug("Feature flags change detected, procesing...");
                updateToggles(toggleResponse);
                break;
            case NOT_CHANGED:
                log.trace("No changes detected to feature flags");
                break;
            default:
                break;
        }
    }
}
