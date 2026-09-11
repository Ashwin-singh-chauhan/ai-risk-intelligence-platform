package com.deloitte.erip.risk;

import com.deloitte.erip.common.dto.PageResponse;
import com.deloitte.erip.risk.dto.RiskScoreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk-scores")
@RequiredArgsConstructor
@Tag(name = "Risk Engine", description = "Explainable 0-100 enterprise risk scoring")
public class RiskController {

    private final RiskScoreService riskScoreService;

    @GetMapping("/assets/{assetId}/latest")
    @Operation(summary = "Get the most recently calculated risk score for an asset, with full explanation")
    public ResponseEntity<RiskScoreResponse> latest(@PathVariable UUID assetId) {
        return ResponseEntity.ok(riskScoreService.getLatest(assetId));
    }

    @GetMapping("/assets/{assetId}/history")
    @Operation(summary = "Paginated risk score history/trend for an asset")
    public ResponseEntity<PageResponse<RiskScoreResponse>> history(@PathVariable UUID assetId, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(riskScoreService.history(assetId, pageable)));
    }

    @PostMapping("/assets/{assetId}/recompute")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_ANALYST')")
    @Operation(summary = "Force an immediate recomputation of an asset's risk score")
    public ResponseEntity<RiskScoreResponse> recompute(@PathVariable UUID assetId) {
        return ResponseEntity.ok(riskScoreService.computeForAsset(assetId));
    }

    @GetMapping("/latest")
    @Operation(summary = "Latest risk score for every asset (enterprise risk overview)")
    public ResponseEntity<List<RiskScoreResponse>> latestForAll() {
        return ResponseEntity.ok(riskScoreService.latestForAllAssets());
    }
}
