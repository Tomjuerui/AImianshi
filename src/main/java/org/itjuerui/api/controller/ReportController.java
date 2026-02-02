package org.itjuerui.api.controller;

import lombok.RequiredArgsConstructor;
import org.itjuerui.common.dto.ApiResponse;
import org.itjuerui.service.ReportService;
import org.springframework.web.bind.annotation.*;

/**
 * 报告管理控制器
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{sessionId}")
    public ApiResponse<?> getReport(@PathVariable Long sessionId) {
        return ApiResponse.success(reportService.getReport(sessionId));
    }

    @GetMapping("/{sessionId}/export")
    public ApiResponse<?> exportReport(
            @PathVariable Long sessionId,
            @RequestParam(value = "format", defaultValue = "json") String format) {
        return ApiResponse.success(reportService.exportReport(sessionId, format));
    }
}
