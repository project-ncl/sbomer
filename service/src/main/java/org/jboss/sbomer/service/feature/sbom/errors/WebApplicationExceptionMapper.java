package org.jboss.sbomer.service.feature.sbom.errors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class WebApplicationExceptionMapper extends AbstractExceptionMapper<WebApplicationException> {
    @Override
    Status getStatus(WebApplicationException ex) {
        return ex.getResponse().getStatusInfo().toEnum();
    }

    @Override
    Response hook(ResponseBuilder responseBuilder, WebApplicationException ex) {
        log.warn("Web application error occured, unable to process request", ex);
        return responseBuilder.build();
    }

    @Override
    String errorMessage(WebApplicationException ex) {
        return formattedString("Unable to process request: {}", ex.getMessage());
    }

}
