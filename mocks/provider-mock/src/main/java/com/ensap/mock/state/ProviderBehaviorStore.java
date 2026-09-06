package com.ensap.mock.state;

import com.ensap.mock.domain.ProviderBehavior;
import com.ensap.mock.domain.ProviderType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-local state for all five provider mocks: current behavior config
 * per provider (admin-writable, request-readable) and a rolling one-minute
 * request-timestamp window per provider for the rate-limiting knob. A mock
 * has no durability requirement (master spec §22) so in-memory is the
 * correct amount of storage — restarting the container resets every
 * provider back to {@link ProviderBehavior#defaults()}.
 */
@Component
public class ProviderBehaviorStore {

    private final Map<ProviderType, AtomicReference<ProviderBehavior>> behaviors = new EnumMap<>(ProviderType.class);
    private final Map<ProviderType, ConcurrentLinkedDeque<Instant>> requestWindows = new EnumMap<>(ProviderType.class);

    public ProviderBehaviorStore() {
        for (ProviderType type : ProviderType.values()) {
            behaviors.put(type, new AtomicReference<>(ProviderBehavior.defaults()));
            requestWindows.put(type, new ConcurrentLinkedDeque<>());
        }
    }

    public ProviderBehavior get(ProviderType type) {
        return behaviors.get(type).get();
    }

    public void set(ProviderType type, ProviderBehavior behavior) {
        behaviors.get(type).set(behavior);
    }

    public void reset(ProviderType type) {
        behaviors.get(type).set(ProviderBehavior.defaults());
    }

    public Map<ProviderType, ProviderBehavior> snapshotAll() {
        Map<ProviderType, ProviderBehavior> snapshot = new EnumMap<>(ProviderType.class);
        behaviors.forEach((type, ref) -> snapshot.put(type, ref.get()));
        return snapshot;
    }

    /**
     * Records a request now and returns true if it exceeds the given
     * per-minute limit (0 = unlimited). Prunes timestamps older than 60s on
     * every call so the deque never grows unbounded.
     */
    public boolean isRateLimited(ProviderType type, int limitPerMinute) {
        if (limitPerMinute <= 0) {
            return false;
        }
        ConcurrentLinkedDeque<Instant> window = requestWindows.get(type);
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(60);
        window.removeIf(ts -> ts.isBefore(cutoff));
        window.addLast(now);
        return window.size() > limitPerMinute;
    }
}
