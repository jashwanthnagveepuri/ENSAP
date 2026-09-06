# shared/common-observability

Reusable observability library for ENSAP WebFlux services: correlation-id propagation, the
Prometheus metric-naming convention, and the four Phase 8 dashboards (spec §24, docs/13-observability.md).

Not a Spring Boot application — a plain library jar, built once with its own `mvnw` and consumed
as a dependency by each service.

## What's in here

- `CorrelationIdWebFilter` — a `WebFilter` that reads (or generates) `X-Correlation-Id` and the
  sibling `X-Site-Id` / `X-Deployment-Id` / `X-Workflow-Instance-Id` / `X-Operation-Id` headers,
  echoes `X-Correlation-Id` on the response, and writes every present value into the Reactor
  `Context`.
- `ObservabilityAutoConfiguration` — a Spring Boot auto-configuration (no `@Import` needed) that
  registers the filter and bridges those Reactor `Context` values into SLF4J's MDC via Micrometer's
  context-propagation, so a plain `log.info(...)` anywhere in the reactive chain sees them.
- `MetricNames` — naming constants for the handful of business metrics spec §24 requires that no
  auto-instrumentation covers (deployment duration/result, provider failures, retries, manual
  interventions, active workflows).

`traceId` is deliberately **not** managed by this module — it comes from Micrometer Tracing
(`micrometer-tracing-bridge-otel`), which populates MDC's `traceId`/`spanId` on its own once a
service adds that dependency.

## Adopting this module in a service

1. Add the dependency (module isn't published to a repo yet — `mvn install` it locally, or use a
   `reactor`/multi-module build once one exists):

   ```xml
   <dependency>
     <groupId>com.ensap</groupId>
     <artifactId>common-observability</artifactId>
     <version>0.1.0-SNAPSHOT</version>
   </dependency>
   ```

2. Add the dependencies that make Spring Boot's *built-in* observability autoconfiguration kick
   in — this module deliberately doesn't bundle them (each service already picks its own actuator
   footprint):

   ```xml
   <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   <dependency>
     <groupId>io.micrometer</groupId>
     <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   <dependency>
     <groupId>io.micrometer</groupId>
     <artifactId>micrometer-tracing-bridge-otel</artifactId>
   </dependency>
   <dependency>
     <groupId>io.opentelemetry</groupId>
     <artifactId>opentelemetry-exporter-otlp</artifactId>
   </dependency>
   ```

3. Expose the endpoints and turn structured JSON logging on, in `application.yml`:

   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health, prometheus
   logging:
     structured:
       format:
         console: ecs   # or logstash/gelf — any format that emits MDC as structured fields
   ```

   That's it — no logging framework config to write. Spring Boot 3.4+'s structured logging support
   includes every MDC entry (including this module's correlation fields and Micrometer Tracing's
   `traceId`/`spanId`) as structured JSON fields automatically.

4. Nothing else to wire up: `ObservabilityAutoConfiguration` is picked up automatically (Spring
   Boot `AutoConfiguration.imports`), registering the correlation-id filter and MDC propagation as
   soon as the jar is on the classpath.

5. For the custom business metrics (deployment duration/result, provider failures, retries, manual
   interventions, active workflows), record them directly against the injected `MeterRegistry`
   using the names in `MetricNames` — see docs/13-observability.md for the full naming convention
   and tag guidance.

## What you get for free vs. what domain code must emit

Request rate/latency/error-rate (`http_server_requests_seconds`), Kafka consumer lag, and database
connection-pool usage are all emitted automatically by Spring Boot's own auto-instrumentation once
step 2's dependencies (plus `spring-kafka` / a HikariCP-backed datasource, where applicable) are on
the classpath — no code in this module or the consuming service is needed for those. Only the
domain-specific counters/timers/gauges in `MetricNames` require an explicit call from business code.

## Building

```bash
cd shared/common-observability
./mvnw test      # or: mvn test, if a working `mvn`/JAVA_HOME is already on PATH
```
