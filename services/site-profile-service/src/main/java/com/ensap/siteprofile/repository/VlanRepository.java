package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.Vlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface VlanRepository extends JpaRepository<Vlan, String> {

    List<Vlan> findByNetworkProfileId(String networkProfileId);

    List<Vlan> findByNetworkProfileIdIn(List<String> networkProfileIds);

    // ponytail: see DeviceRepository#deleteBySiteId — derived delete queries
    // need an active transaction once there are rows to remove.
    @Transactional
    void deleteByNetworkProfileId(String networkProfileId);
}
