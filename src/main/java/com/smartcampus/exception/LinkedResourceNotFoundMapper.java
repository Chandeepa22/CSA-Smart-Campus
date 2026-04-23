package com.smartcampus.exception;

import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        return Response.status(422) // Unprocessable Entity
                .entity(Map.of("error", "LINKED_RESOURCE_NOT_FOUND", "message", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
