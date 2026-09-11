package com.deloitte.erip.asset;

import com.deloitte.erip.common.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "assets")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset extends BaseEntity {

    @Column(name = "asset_tag", nullable = false, unique = true)
    private String assetTag;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "business_unit", nullable = false)
    private String businessUnit;

    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Criticality criticality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Exposure exposure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetEnvironment environment;

    @Column(name = "ip_address")
    private String ipAddress;

    private String hostname;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private JsonNode tags = null;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
