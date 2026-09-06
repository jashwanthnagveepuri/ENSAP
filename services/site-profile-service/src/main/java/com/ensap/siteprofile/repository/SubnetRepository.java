package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.Subnet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SubnetRepository extends JpaRepository<Subnet, String> {

    List<Subnet> findByNetworkProfileId(String networkProfileId);

    List<Subnet> findByNetworkProfileIdIn(List<String> networkProfileIds);

    // ponytail: see DeviceRepository#deleteBySiteId — derived delete queries
    // need an active transaction once there are rows to remove.
    @Transactional
    void deleteByNetworkProfileId(String networkProfileId);
}
