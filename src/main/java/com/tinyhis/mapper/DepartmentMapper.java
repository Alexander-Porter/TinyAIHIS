package com.tinyhis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyhis.entity.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * Department Mapper
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
