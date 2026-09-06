package com.ensap.events;

/**
 * Kafka topic names fixed by docs/11-event-catalog.md §"Topics (local,
 * single-broker KRaft)". One constant per topic so producers and consumers
 * never duplicate the literal string.
 */
public final class Topics {

    public static final String SITE_EVENTS = "ensap.site.events";
    public static final String DEPLOYMENT_EVENTS = "ensap.deployment.events";
    public static final String ROUTER_EVENTS = "ensap.router.events";
    public static final String SWITCH_EVENTS = "ensap.switch.events";
    public static final String WIRELESS_EVENTS = "ensap.wireless.events";
    public static final String FIREWALL_EVENTS = "ensap.firewall.events";
    public static final String TICKETING_EVENTS = "ensap.ticketing.events";

    private Topics() {
    }
}
