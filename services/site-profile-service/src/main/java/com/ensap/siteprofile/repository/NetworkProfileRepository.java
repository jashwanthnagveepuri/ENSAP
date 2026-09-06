package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.NetworkProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NetworkProfileRepository extends JpaRepository<NetworkProfile, String> {

    List<NetworkProfile> findBySiteId(String siteId);

    void deleteBySiteId(String siteId);
}
