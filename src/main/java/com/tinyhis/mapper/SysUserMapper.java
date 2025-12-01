package com.tinyhis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyhis.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * System User Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
