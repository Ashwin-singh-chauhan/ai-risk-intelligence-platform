package com.deloitte.erip.compliance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplianceAssessmentRepository extends JpaRepository<ComplianceAssessment, UUID> {

    List<ComplianceAssessment> findAllByBusinessUnitIgnoreCase(String businessUnit);

    Optional<ComplianceAssessment> findFirstByControlIdAndBusinessUnitIgnoreCaseOrderByAssessedAtDesc(UUID controlId, String businessUnit);

    @Query("""
            SELECT a FROM ComplianceAssessment a WHERE a.control.framework = :framework
            AND (:businessUnit IS NULL OR LOWER(a.businessUnit) = LOWER(:businessUnit))
            """)
    List<ComplianceAssessment> findAllByFramework(@Param("framework") ComplianceFramework framework, @Param("businessUnit") String businessUnit);

    long countByStatus(ComplianceStatus status);
}
