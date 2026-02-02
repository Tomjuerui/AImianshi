package org.itjuerui.api.controller;

import lombok.RequiredArgsConstructor;
import org.itjuerui.common.dto.ApiResponse;
import org.itjuerui.service.ResumeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历管理控制器
 */
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public ApiResponse<Long> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId) {
        Long resumeId = resumeService.uploadResume(file, userId);
        return ApiResponse.success(resumeId);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getResume(@PathVariable Long id) {
        return ApiResponse.success(resumeService.getResume(id));
    }
}
