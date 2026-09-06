package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.WanCircuit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WanCircuitRepository extends JpaRepository<WanCircuit, String> {

    List<WanCircuit> findBySiteId(String siteId);

    void deleteBySiteId(String siteId);
}
