package com.ensap.siteprofile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Site Profile Service — owns site/device/network-profile/WAN circuit/VLAN/subnet
 * data (docs/06-component-design.md). Phase 0: scaffolding only, see
 * config.RouterConfig for the (stubbed) HTTP API.
 */
@SpringBootApplication
public class SiteProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiteProfileServiceApplication.class, args);
    }
}
