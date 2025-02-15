package org.jboss.sbomer.eventing.core.event.requests;

import org.jboss.sbomer.eventing.core.event.requests.zip.GenerationRequestImageV1Alpha1Event;
import org.jboss.sbomer.eventing.core.event.requests.zip.GenerationRequestZipV1Alpha1Event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({ @JsonSubTypes.Type(value = GenerationRequestZipV1Alpha1Event.class),
        @JsonSubTypes.Type(value = GenerationRequestImageV1Alpha1Event.class) })
@RegisterForReflection
public interface GenerationRequestEvent {

}
