package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findBySiteId(String siteId);

    // ponytail: derived delete queries load+remove entities individually and
    // need an active transaction (unlike save()/deleteById()), or they throw
    // jakarta.persistence.TransactionRequiredException when rows exist.
    @Transactional
    void deleteBySiteId(String siteId);
}
