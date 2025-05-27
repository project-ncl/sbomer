package org.jboss.sbomer.service.worker;

public interface Worker {
    public static final String KEY_WORKER = "worker";

    String getType();

    public void resolve(String eventId, String identifier);
}
