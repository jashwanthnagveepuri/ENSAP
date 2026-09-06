package com.ensap.siteprofile.util;

import java.util.UUID;

/**
 * Generates human-readable synthetic IDs (e.g. {@code SITE-A1B2C3D4}) when a
 * caller doesn't supply one — matches the {@code SITE-001}-style convention
 * in docs/09-database-design.md without needing a DB sequence.
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String next(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
