package com.smartcampus.exception;

import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", "ROOM_NOT_EMPTY", "message", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
