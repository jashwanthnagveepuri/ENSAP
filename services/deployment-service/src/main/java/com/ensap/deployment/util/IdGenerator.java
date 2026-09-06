package com.ensap.deployment.util;

import java.util.UUID;

/**
 * Generates human-readable synthetic IDs (e.g. {@code DEP-A1B2C3D4}) —
 * matches the {@code SITE-001}-style convention in
 * docs/09-database-design.md without needing a DB sequence.
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String next(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
