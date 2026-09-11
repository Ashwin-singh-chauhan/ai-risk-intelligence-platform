package com.deloitte.erip.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceControlRepository extends JpaRepository<ComplianceControl, UUID> {
    List<ComplianceControl> findAllByFramework(ComplianceFramework framework);
}
