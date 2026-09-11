package com.deloitte.erip.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Page<Asset> findAllByBusinessUnitIgnoreCase(String businessUnit, Pageable pageable);

    Page<Asset> findAllByCriticality(Criticality criticality, Pageable pageable);

    @Query("""
            SELECT a FROM Asset a WHERE
            (:businessUnit IS NULL OR LOWER(a.businessUnit) = LOWER(:businessUnit)) AND
            (:criticality IS NULL OR a.criticality = :criticality) AND
            (:assetType IS NULL OR a.assetType = :assetType) AND
            (:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.assetTag) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Asset> search(
            @Param("businessUnit") String businessUnit,
            @Param("criticality") Criticality criticality,
            @Param("assetType") AssetType assetType,
            @Param("search") String search,
            Pageable pageable);

    List<Asset> findAllByBusinessUnitIgnoreCase(String businessUnit);

    boolean existsByAssetTagIgnoreCase(String assetTag);

    java.util.Optional<Asset> findFirstByIpAddressOrHostname(String ipAddress, String hostname);

    long countByCriticality(Criticality criticality);

    @Query("SELECT DISTINCT a.businessUnit FROM Asset a ORDER BY a.businessUnit")
    List<String> findDistinctBusinessUnits();
}
