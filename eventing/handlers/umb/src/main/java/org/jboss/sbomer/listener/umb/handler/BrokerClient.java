package org.jboss.sbomer.listener.umb.handler;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.cloudevents.CloudEvent;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "broker-client")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BrokerClient {

    @POST
    @Path("/default/example-broker")
    public CompletionStage<Void> publish(CloudEvent event);
}
