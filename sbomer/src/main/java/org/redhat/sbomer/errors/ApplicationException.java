package org.redhat.sbomer.errors;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.helpers.MessageFormatter;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApplicationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Object[] params;

    private String formattedMessage;

    public ApplicationException(String msg, Object... params) {
        super(msg, MessageFormatter.getThrowableCandidate(params));
        this.params = params;
    }

    public Error toError() {
        return new Error(this.getMessage());
    }

    @Override
    public synchronized String getMessage() {
        if (formattedMessage == null) {
            formattedMessage = MessageFormatter.arrayFormat(super.getMessage(), params).getMessage();
        }
        return formattedMessage;
    }
}
