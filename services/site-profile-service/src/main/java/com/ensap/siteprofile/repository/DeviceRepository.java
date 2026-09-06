package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findBySiteId(String siteId);

    void deleteBySiteId(String siteId);
}
