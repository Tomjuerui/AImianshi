package org.itjuerui.infra.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.itjuerui.domain.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历Mapper
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
