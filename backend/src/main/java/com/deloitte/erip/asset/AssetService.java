package com.deloitte.erip.asset;

import com.deloitte.erip.asset.dto.AssetRequest;
import com.deloitte.erip.asset.dto.AssetResponse;
import com.deloitte.erip.audit.AuditService;
import com.deloitte.erip.common.exception.BadRequestException;
import com.deloitte.erip.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;
    private final AuditService auditService;

    @Transactional
    public AssetResponse createAsset(AssetRequest request) {
        if (assetRepository.existsByAssetTagIgnoreCase(request.assetTag())) {
            throw new BadRequestException("Asset tag already exists: " + request.assetTag());
        }
        Asset saved = assetRepository.save(assetMapper.toEntity(request));
        auditService.record("ASSET_CREATED", "ASSET", saved.getId().toString(),
                Map.of("assetTag", saved.getAssetTag(), "criticality", saved.getCriticality().name()));
        return assetMapper.toResponse(saved);
    }

    public AssetResponse getAsset(UUID id) {
        return assetMapper.toResponse(findOrThrow(id));
    }

    public Page<AssetResponse> search(String businessUnit, Criticality criticality, AssetType assetType,
                                       String query, Pageable pageable) {
        return assetRepository.search(businessUnit, criticality, assetType, query, pageable)
                .map(assetMapper::toResponse);
    }

    @Transactional
    public AssetResponse updateAsset(UUID id, AssetRequest request) {
        Asset asset = findOrThrow(id);
        assetMapper.applyUpdate(asset, request);
        Asset saved = assetRepository.save(asset);
        auditService.record("ASSET_UPDATED", "ASSET", id.toString(), null);
        return assetMapper.toResponse(saved);
    }

    @Transactional
    public void deactivateAsset(UUID id) {
        Asset asset = findOrThrow(id);
        asset.setActive(false);
        assetRepository.save(asset);
        auditService.record("ASSET_DEACTIVATED", "ASSET", id.toString(), null);
    }

    public List<String> listBusinessUnits() {
        return assetRepository.findDistinctBusinessUnits();
    }

    public Asset findOrThrow(UUID id) {
        return assetRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Asset", id));
    }
}
