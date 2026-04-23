# Smart Campus — Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey) and deployed on **Apache Tomcat / TomEE** for managing rooms and sensors across a university campus.

## Project Structure

```text
Smart-Campus/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/smartcampus/
│   │   │   ├── AppConfig.java              # JAX-RS Application configuration
│   │   │   ├── model/                      # Data models (POJOs)
│   │   │   │   ├── Room.java
│   │   │   │   ├── Sensor.java
│   │   │   │   └── SensorReading.java
│   │   │   ├── store/                      # In-memory singleton DataStore
│   │   │   │   └── DataStore.java
│   │   │   ├── resource/                   # REST Endpoints
│   │   │   │   ├── DiscoveryResource.java
│   │   │   │   ├── RoomResource.java
│   │   │   │   ├── SensorResource.java
│   │   │   │   └── SensorReadingResource.java
│   │   │   ├── exception/                  # Error handling & mappers
│   │   │   │   ├── GlobalExceptionMapper.java
│   │   │   │   ├── RoomNotEmptyMapper.java
│   │   │   │   └── ...
│   │   │   └── filter/                     # Logging & Request filters
│   │   │       └── LoggingFilter.java
│   │   └── webapp/                         # Web configuration
│   │       ├── WEB-INF/web.xml             # Jersey Servlet mapping
│   │       └── META-INF/context.xml        # Tomcat context path
└── README.md
```

## How to build and run

**Requirements:** Java 11+, Maven 3.6+, Apache Tomcat 9.0+

1. **Clone and Build:**
   ```bash
   git clone https://github.com/Chandeepa22/CSA-Smart-Campus
   cd Smart-Campus
   mvn clean package
   ```

2. **Deploy:**
   - Copy the generated `target/smart-campus.war` file.
   - Paste it into your Tomcat `webapps/` directory.
   - Start your Tomcat server.

3. **Access:**
   The server will be available at: `http://localhost:8080/smart-campus/api/v1`

---

## Sample curl commands

```bash
# 1. Discovery
curl http://localhost:8080/smart-campus/api/v1

# 2. Create a room
curl -X POST http://localhost:8080/smart-campus/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Study Room","capacity":30}'

# 3. Create a sensor
curl -X POST http://localhost:8080/smart-campus/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","roomId":"LIB-301"}'

# 4. Filter sensors by type
curl "http://localhost:8080/smart-campus/api/v1/sensors?type=CO2"

# 5. Post a sensor reading
curl -X POST http://localhost:8080/smart-campus/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":520.5}'

# 6. Try deleting a room that has sensors (expect 409)
curl -X DELETE http://localhost:8080/smart-campus/api/v1/rooms/LIB-301
```

---

## Report — Question answers

### Part 1.1 — JAX-RS resource lifecycle

By default, JAX-RS creates a **new instance** of every resource class for each incoming HTTP request (request-scoped lifecycle). This means you cannot store state as instance variables in resource classes — a field like `private Map<String, Room> rooms` would be reset on every request and all data would be lost.

To solve this, a **singleton `DataStore`** class holds all in-memory data using `ConcurrentHashMap`, which is thread-safe. This ensures all requests share the same data and concurrent writes from different threads do not corrupt it.

### Part 1.2 — HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) means that API responses include links pointing to related resources and available actions. For example, a room response might include a link to its sensors. This benefits client developers because they can discover the API structure dynamically at runtime, rather than relying on static external documentation that may become out of date.

### Part 2.1 — IDs vs full objects in lists

Returning only IDs uses less network bandwidth but forces the client to make additional requests for each item (the N+1 problem). Returning full objects increases payload size but reduces round-trips. For large collections, a middle ground — returning a summary object with key fields — is common practice.

### Part 2.2 — DELETE idempotency

DELETE is idempotent: calling it multiple times produces the same server state as calling it once. In this implementation, the first DELETE removes the room. Subsequent calls return `404 Not Found`, but the server state remains identical — the room is still gone. This satisfies idempotency because the *outcome* (room does not exist) is consistent across repeated calls.

### Part 3.1 — `@Consumes` mismatch

If a client sends `text/plain` or `application/xml` when the endpoint is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS automatically returns `415 Unsupported Media Type` and never invokes the resource method. This protects the endpoint from receiving data it cannot deserialize.

### Part 3.2 — Query params vs path params for filtering

Query parameters like `?type=CO2` are designed for optional, non-hierarchical filtering and searching of collections. Path parameters like `/sensors/type/CO2` imply that `type/CO2` is a distinct resource — which is semantically incorrect. Query params can be combined freely (e.g. `?type=CO2&status=ACTIVE`), whereas path-based filtering becomes unwieldy with multiple filters and breaks REST's resource hierarchy model.

### Part 4.1 — Sub-resource locator benefits

The sub-resource locator pattern delegates handling of nested paths to dedicated classes. Instead of a single massive `SensorResource` handling every path from `/sensors` to `/sensors/{id}/readings/{rid}`, each class handles one concern. This improves readability, testability, and maintainability. Adding a new nested resource only requires creating a new class and adding one locator method — existing classes are untouched.

### Part 5.1 — Why 422 over 404 for missing `roomId`

`404 Not Found` implies the requested URL does not exist. In this scenario the URL (`POST /sensors`) is perfectly valid. The problem is that a referenced entity inside the request body (`roomId`) points to something that does not exist. HTTP 422 Unprocessable Entity is semantically accurate: the request is syntactically correct JSON, but the server cannot process it because of a semantic validation failure in the payload.

### Part 5.2 — Stack trace security risks

Exposing Java stack traces reveals: internal package and class names (helping attackers map the codebase), library names and versions (enabling known CVE exploitation), server configuration paths, and logic flow details that assist in crafting targeted injection or traversal attacks. The global `ExceptionMapper<Throwable>` ensures all unexpected errors return a generic message with no internal details.

### Part 5.3 — Filters vs manual logging

Inserting `Logger.info()` in every resource method violates the DRY principle and scatters cross-cutting concerns throughout the codebase. JAX-RS filters are applied automatically to every request and response via the `@Provider` annotation. This means logging logic lives in one place, is consistent, and can be updated without touching any resource class.