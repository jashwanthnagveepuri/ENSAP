package com.ensap.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ENSAP mock provider ecosystem — one configurable WebFlux app standing in
 * for the router, switch, wireless, firewall and ticketing provider APIs
 * (master spec §22), with runtime-controllable behavior via
 * {@code web.AdminRouter} for demonstrating resilience (master spec §21).
 */
@SpringBootApplication
public class ProviderMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderMockApplication.class, args);
    }
}
