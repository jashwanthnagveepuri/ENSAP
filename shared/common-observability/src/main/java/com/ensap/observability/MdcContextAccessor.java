package com.ensap.observability;

import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;

/**
 * Bridges one Reactor {@code Context} key into SLF4J's MDC, the same pattern Micrometer Tracing
 * uses internally for {@code traceId}/{@code spanId}. Registered with the global
 * {@link io.micrometer.context.ContextRegistry} so Reactor's automatic context propagation
 * (enabled by {@link ObservabilityAutoConfiguration}) restores the MDC value around every
 * operator, letting plain {@code log.info(...)} calls inside a reactive chain see it.
 */
final class MdcContextAccessor implements ThreadLocalAccessor<String> {

    private final String key;

    MdcContextAccessor(String key) {
        this.key = key;
    }

    @Override
    public Object key() {
        return key;
    }

    @Override
    public String getValue() {
        return MDC.get(key);
    }

    @Override
    public void setValue(String value) {
        MDC.put(key, value);
    }

    @Override
    public void setValue() {
        MDC.remove(key);
    }
}
