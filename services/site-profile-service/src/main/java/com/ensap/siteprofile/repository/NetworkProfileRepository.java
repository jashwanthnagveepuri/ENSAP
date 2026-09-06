package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.NetworkProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NetworkProfileRepository extends JpaRepository<NetworkProfile, String> {

    List<NetworkProfile> findBySiteId(String siteId);

    // ponytail: see DeviceRepository#deleteBySiteId — derived delete queries
    // need an active transaction once there are rows to remove.
    @Transactional
    void deleteBySiteId(String siteId);
}
