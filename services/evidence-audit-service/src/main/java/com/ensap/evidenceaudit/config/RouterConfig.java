package com.ensap.evidenceaudit.config;

/**
 * Central place documenting the composed functional API surface of this
 * service (master spec §31/§32). Actual route beans are declared per
 * resource in {@code router/} ({@link com.ensap.evidenceaudit.router.EvidenceRouter},
 * {@link com.ensap.evidenceaudit.router.AuditRouter}) — Spring merges every
 * {@code RouterFunction<ServerResponse>} bean automatically, so this class
 * carries no bean definitions of its own today. Keep composing new resource
 * routers the same way rather than growing either router class beyond its
 * one resource (master spec §32).
 */
public class RouterConfig {
}
