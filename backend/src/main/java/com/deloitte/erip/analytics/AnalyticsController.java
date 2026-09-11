package com.deloitte.erip.analytics;

import com.deloitte.erip.analytics.dto.AnalystDashboardResponse;
import com.deloitte.erip.analytics.dto.ExecutiveDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Aggregate analytics for the analyst and executive dashboards")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/analyst-dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_ANALYST')")
    @Operation(summary = "Assets, vulnerabilities, incidents, anomalies and top risk for analysts")
    public ResponseEntity<AnalystDashboardResponse> analystDashboard() {
        return ResponseEntity.ok(analyticsService.analystDashboard());
    }

    @GetMapping("/executive-dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    @Operation(summary = "Enterprise risk, business-unit breakdown and compliance posture for executives")
    public ResponseEntity<ExecutiveDashboardResponse> executiveDashboard() {
        return ResponseEntity.ok(analyticsService.executiveDashboard());
    }
}
