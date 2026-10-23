package com.competition.energycontrollerplatform.miniapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.miniapp.entity.MiniAppUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 小程序用户 Mapper
 */
@Mapper
public interface MiniAppUserMapper extends BaseMapper<MiniAppUser> {
}
