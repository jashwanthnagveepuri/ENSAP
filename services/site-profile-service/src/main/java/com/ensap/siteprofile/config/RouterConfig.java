package com.ensap.siteprofile.config;

/**
 * Central place documenting the composed functional API surface of this
 * service (master spec §31/§32). Actual route beans are declared per
 * resource in {@code router/} (currently just {@link com.ensap.siteprofile.router.SiteRouter})
 * — Spring merges every {@code RouterFunction<ServerResponse>} bean
 * automatically, so this class carries no bean definitions of its own today.
 * If/when this service grows a second resource router, compose them here
 * explicitly (e.g. {@code siteRoutes.andOther(otherRoutes)}) instead of
 * relying on bean auto-merging, to keep routing declarative in one place
 * (master spec §32).
 */
public class RouterConfig {
}
