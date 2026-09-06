package com.ensap.mock.domain;

/**
 * The five infrastructure provider mocks this app serves via {@code
 * /api/v1/{providerType}/...} (master spec §22). One app, path-routed,
 * instead of five near-duplicate services — they are mocks, not the real
 * isolation boundary.
 */
public enum ProviderType {
    ROUTER,
    SWITCH,
    WIRELESS,
    FIREWALL,
    TICKETING;

    /** Case-insensitive lookup for path variables, or null if not a known provider. */
    public static ProviderType fromPath(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
