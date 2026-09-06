package com.ensap.siteprofile.exception;

/**
 * Thrown when a requested resource (site/device/network profile) doesn't
 * exist. Mapped to 404 by {@link ErrorMapper}.
 */
public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static NotFoundException site(String siteId) {
        return new NotFoundException("SITE_NOT_FOUND", "No site with id " + siteId);
    }

    public static NotFoundException device(String siteId, String deviceId) {
        return new NotFoundException("DEVICE_NOT_FOUND", "No device " + deviceId + " for site " + siteId);
    }

    public static NotFoundException networkProfile(String siteId, String networkProfileId) {
        return new NotFoundException("NETWORK_PROFILE_NOT_FOUND",
                "No network profile " + networkProfileId + " for site " + siteId);
    }

    public String getCode() {
        return code;
    }
}
