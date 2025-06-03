package org.jboss.sbomer.service.v1beta2.controller.request;

public record Resources(ResourceSpec requests, ResourceSpec limits) {
}