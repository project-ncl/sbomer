package org.jboss.sbomer.service.resolver;

public interface Resolver {
    public static final String KEY_RESOLVER = "resolver";
    public static final String KEY_IDENTIFIER = "identifier";

    String getType();

    public void resolve(String eventId, String identifier);

    // public void handleAsync(String eventId, String identifier);

}
