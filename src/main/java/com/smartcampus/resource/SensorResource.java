package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public List<Sensor> getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(DataStore.getInstance().getSensors().values());
        if (type != null && !type.isEmpty()) {
            return sensors.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        }
        return sensors;
    }

    @POST
    public Response createSensor(Sensor sensor) {
        Room room = DataStore.getInstance().getRooms().get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Room with ID " + sensor.getRoomId() + " not found.");
        }

        DataStore.getInstance().getSensors().put(sensor.getId(), sensor);
        room.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    @GET
    @Path("/{sensorId}")
    public Sensor getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getInstance().getSensors().get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        return sensor;
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        // Verify sensor exists
        if (!DataStore.getInstance().getSensors().containsKey(sensorId)) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        return new SensorReadingResource(sensorId);
    }

    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updated) {
        if (updated == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(java.util.Map.of("error", "BAD_REQUEST", "message", "Sensor data is required."))
                    .build();
        }
        if (!DataStore.getInstance().getSensors().containsKey(sensorId)) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        DataStore.getInstance().getSensors().put(sensorId, updated);
        return Response.ok(updated).build();
    }

    @PATCH
    @Path("/{sensorId}")
    public Response patchSensor(@PathParam("sensorId") String sensorId, Sensor partialUpdate) {
        Sensor sensor = DataStore.getInstance().getSensors().get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        if (partialUpdate.getStatus() != null) {
            sensor.setStatus(partialUpdate.getStatus());
        }
        if (partialUpdate.getType() != null) {
            sensor.setType(partialUpdate.getType());
        }
        return Response.ok(sensor).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor s = DataStore.getInstance().getSensors().remove(sensorId);
        if (s == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        Room r = DataStore.getInstance().getRooms().get(s.getRoomId());
        if (r != null) {
            r.getSensorIds().remove(sensorId);
        }
        return Response.noContent().build();
    }
}
