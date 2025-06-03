package org.jboss.sbomer.service.v1beta2.controller.request;

import com.fasterxml.jackson.databind.JsonNode;

public record Config(Resources resources, String format, JsonNode options) {
}