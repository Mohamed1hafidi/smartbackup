package com.smartbackup.controller;

import com.smartbackup.dto.response.ApiResponse;
import com.smartbackup.dto.response.DashboardStatsResponse;
import com.smartbackup.security.UserPrincipal;
import com.smartbackup.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Statistiques du tableau de bord")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Consulter les statistiques globales de l'utilisateur")
    public ApiResponse<DashboardStatsResponse> stats(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(dashboardService.getStats(principal.getId()), null);
    }
}
