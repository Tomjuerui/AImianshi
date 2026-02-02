package org.itjuerui.infra.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.itjuerui.domain.interview.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试会话Mapper
 */
@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {
}
