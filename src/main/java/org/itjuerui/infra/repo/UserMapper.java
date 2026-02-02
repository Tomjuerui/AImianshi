package org.itjuerui.infra.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.itjuerui.domain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
