package com.deloitte.erip.asset.dto;

import com.deloitte.erip.asset.AssetEnvironment;
import com.deloitte.erip.asset.AssetType;
import com.deloitte.erip.asset.Criticality;
import com.deloitte.erip.asset.Exposure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssetRequest(
        @NotBlank String assetTag,
        @NotBlank String name,
        @NotNull AssetType assetType,
        @NotBlank String businessUnit,
        String owner,
        @NotNull Criticality criticality,
        @NotNull Exposure exposure,
        @NotNull AssetEnvironment environment,
        String ipAddress,
        String hostname,
        List<String> tags
) {
}
