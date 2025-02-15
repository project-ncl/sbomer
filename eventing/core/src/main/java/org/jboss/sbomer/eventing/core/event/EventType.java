package org.jboss.sbomer.eventing.core.event;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;

@Getter
public enum EventType {
    REQUEST_ZIP_V1ALPHA1("org.jboss.sbomer.generation.request.zip.v1alpha1"),
    GENERATION_STATUS_V1ALPHA1("org.jboss.sbomer.generation.status.v1alpha1");

    final String type;

    EventType(String type) {
        this.type = type;
    }

    public static Optional<EventType> get(String slug) {
        return Arrays.stream(EventType.values()).filter(impl -> impl.type.equals(slug)).findFirst();
    }
}
