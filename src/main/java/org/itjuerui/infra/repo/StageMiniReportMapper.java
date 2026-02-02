package org.itjuerui.infra.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.itjuerui.domain.report.entity.StageMiniReport;

/**
 * 阶段小结Mapper
 */
@Mapper
public interface StageMiniReportMapper extends BaseMapper<StageMiniReport> {
}
