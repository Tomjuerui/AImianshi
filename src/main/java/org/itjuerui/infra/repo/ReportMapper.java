package org.itjuerui.infra.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.itjuerui.domain.report.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报告Mapper
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
