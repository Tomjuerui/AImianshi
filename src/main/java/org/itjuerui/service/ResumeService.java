package org.itjuerui.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 简历服务接口
 */
public interface ResumeService {
    /**
     * 上传并解析简历
     */
    Long uploadResume(MultipartFile file, Long userId);

    /**
     * 获取简历信息
     */
    Object getResume(Long id);
}
