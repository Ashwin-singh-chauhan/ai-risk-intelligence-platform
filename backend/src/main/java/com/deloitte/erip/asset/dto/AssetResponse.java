package com.deloitte.erip.asset.dto;

import com.deloitte.erip.asset.AssetEnvironment;
import com.deloitte.erip.asset.AssetType;
import com.deloitte.erip.asset.Criticality;
import com.deloitte.erip.asset.Exposure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        String assetTag,
        String name,
        AssetType assetType,
        String businessUnit,
        String owner,
        Criticality criticality,
        Exposure exposure,
        AssetEnvironment environment,
        String ipAddress,
        String hostname,
        List<String> tags,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
