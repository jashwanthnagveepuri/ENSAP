package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.Vlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VlanRepository extends JpaRepository<Vlan, String> {

    List<Vlan> findByNetworkProfileId(String networkProfileId);

    void deleteByNetworkProfileId(String networkProfileId);
}
