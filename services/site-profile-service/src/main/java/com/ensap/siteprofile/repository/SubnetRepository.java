package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.Subnet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubnetRepository extends JpaRepository<Subnet, String> {

    List<Subnet> findByNetworkProfileId(String networkProfileId);

    void deleteByNetworkProfileId(String networkProfileId);
}
