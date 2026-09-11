package com.deloitte.erip.asset;

import com.deloitte.erip.asset.dto.AssetRequest;
import com.deloitte.erip.asset.dto.AssetResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AssetMapper {

    private final ObjectMapper objectMapper;

    public Asset toEntity(AssetRequest request) {
        return Asset.builder()
                .assetTag(request.assetTag())
                .name(request.name())
                .assetType(request.assetType())
                .businessUnit(request.businessUnit())
                .owner(request.owner())
                .criticality(request.criticality())
                .exposure(request.exposure())
                .environment(request.environment())
                .ipAddress(request.ipAddress())
                .hostname(request.hostname())
                .tags(objectMapper.valueToTree(request.tags() == null ? List.of() : request.tags()))
                .active(true)
                .build();
    }

    public void applyUpdate(Asset asset, AssetRequest request) {
        asset.setAssetTag(request.assetTag());
        asset.setName(request.name());
        asset.setAssetType(request.assetType());
        asset.setBusinessUnit(request.businessUnit());
        asset.setOwner(request.owner());
        asset.setCriticality(request.criticality());
        asset.setExposure(request.exposure());
        asset.setEnvironment(request.environment());
        asset.setIpAddress(request.ipAddress());
        asset.setHostname(request.hostname());
        asset.setTags(objectMapper.valueToTree(request.tags() == null ? List.of() : request.tags()));
    }

    public AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getAssetTag(),
                asset.getName(),
                asset.getAssetType(),
                asset.getBusinessUnit(),
                asset.getOwner(),
                asset.getCriticality(),
                asset.getExposure(),
                asset.getEnvironment(),
                asset.getIpAddress(),
                asset.getHostname(),
                tagsToList(asset.getTags()),
                asset.isActive(),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }

    private List<String> tagsToList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        return objectMapper.convertValue(node, new TypeReference<List<String>>() {
        });
    }
}
