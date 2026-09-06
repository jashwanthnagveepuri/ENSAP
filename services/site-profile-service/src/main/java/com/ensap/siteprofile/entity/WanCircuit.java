package com.ensap.siteprofile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the {@code wan_circuit} table (docs/09-database-design.md).
 */
@Entity
@Table(name = "wan_circuit")
public class WanCircuit {

    @Id
    private String id;

    @Column(name = "site_id", nullable = false)
    private String siteId;

    private String carrier;

    @Column(name = "circuit_id")
    private String circuitId;

    @Column(name = "bandwidth_mbps")
    private Integer bandwidthMbps;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getCircuitId() {
        return circuitId;
    }

    public void setCircuitId(String circuitId) {
        this.circuitId = circuitId;
    }

    public Integer getBandwidthMbps() {
        return bandwidthMbps;
    }

    public void setBandwidthMbps(Integer bandwidthMbps) {
        this.bandwidthMbps = bandwidthMbps;
    }
}
