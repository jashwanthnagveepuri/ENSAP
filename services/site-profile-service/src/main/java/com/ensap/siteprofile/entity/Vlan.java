package com.ensap.siteprofile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the {@code vlan} table (docs/09-database-design.md).
 */
@Entity
@Table(name = "vlan")
public class Vlan {

    @Id
    private String id;

    @Column(name = "network_profile_id", nullable = false)
    private String networkProfileId;

    @Column(name = "vlan_tag", nullable = false)
    private Integer vlanTag;

    private String name;

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

    public Integer getVlanTag() {
        return vlanTag;
    }

    public void setVlanTag(Integer vlanTag) {
        this.vlanTag = vlanTag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
