package com.smartcampus.exception;

import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SensorUnavailableMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", "SENSOR_UNAVAILABLE", "message", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
