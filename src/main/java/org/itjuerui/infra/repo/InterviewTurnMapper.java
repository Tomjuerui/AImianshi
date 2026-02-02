package org.itjuerui.infra.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.itjuerui.domain.interview.entity.InterviewTurn;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试对话Mapper
 */
@Mapper
public interface InterviewTurnMapper extends BaseMapper<InterviewTurn> {
}
