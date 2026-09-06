package com.ensap.siteprofile.config;

/**
 * Central place documenting the composed functional API surface of this
 * service (master spec §31/§32). Actual route beans are declared per
 * resource in {@code router/} ({@link com.ensap.siteprofile.router.SiteRouter},
 * {@link com.ensap.siteprofile.router.DeviceRouter}, {@link com.ensap.siteprofile.router.NetworkProfileRouter})
 * — Spring merges every {@code RouterFunction<ServerResponse>} bean
 * automatically, so this class carries no bean definitions of its own.
 */
public class RouterConfig {
}
