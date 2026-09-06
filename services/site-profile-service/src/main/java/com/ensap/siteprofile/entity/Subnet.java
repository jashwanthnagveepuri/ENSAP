package com.ensap.siteprofile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the {@code subnet} table (docs/09-database-design.md).
 */
@Entity
@Table(name = "subnet")
public class Subnet {

    @Id
    private String id;

    @Column(name = "network_profile_id", nullable = false)
    private String networkProfileId;

    @Column(nullable = false)
    private String cidr;

    private String purpose;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNetworkProfileId() {
        return networkProfileId;
    }

    public void setNetworkProfileId(String networkProfileId) {
        this.networkProfileId = networkProfileId;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
