package com.deloitte.erip.asset;

import com.deloitte.erip.asset.dto.AssetRequest;
import com.deloitte.erip.asset.dto.AssetResponse;
import com.deloitte.erip.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Tag(name = "Asset Inventory", description = "Enterprise asset inventory with business criticality and exposure")
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_ANALYST')")
    @Operation(summary = "Register a new asset")
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.createAsset(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an asset by id")
    public ResponseEntity<AssetResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getAsset(id));
    }

    @GetMapping
    @Operation(summary = "Search and paginate assets by business unit, criticality, type and free-text query")
    public ResponseEntity<PageResponse<AssetResponse>> search(
            @RequestParam(required = false) String businessUnit,
            @RequestParam(required = false) Criticality criticality,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) String query,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                assetService.search(businessUnit, criticality, assetType, query, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_ANALYST')")
    @Operation(summary = "Update an existing asset")
    public ResponseEntity<AssetResponse> update(@PathVariable UUID id, @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetService.updateAsset(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate (soft-delete) an asset")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        assetService.deactivateAsset(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/business-units")
    @Operation(summary = "List distinct business units represented in the asset inventory")
    public ResponseEntity<List<String>> listBusinessUnits() {
        return ResponseEntity.ok(assetService.listBusinessUnits());
    }
}
