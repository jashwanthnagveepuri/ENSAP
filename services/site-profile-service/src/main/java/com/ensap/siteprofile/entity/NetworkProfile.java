package com.ensap.siteprofile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Maps the {@code network_profile} table (docs/09-database-design.md).
 * VLANs and subnets are separate rows ({@link Vlan}, {@link Subnet})
 * keyed by {@code networkProfileId}.
 */
@Entity
@Table(name = "network_profile")
public class NetworkProfile {

    @Id
    private String id;

    @Column(name = "site_id", nullable = false)
    private String siteId;

    @Column(name = "profile_name", nullable = false)
    private String profileName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
