package com.ensap.evidenceaudit.util;

import java.util.UUID;

/**
 * Generates human-readable synthetic IDs (e.g. {@code AUD-A1B2C3D4}) — same
 * convention as deployment-service's {@code IdGenerator}
 * (docs/09-database-design.md).
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String next(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
