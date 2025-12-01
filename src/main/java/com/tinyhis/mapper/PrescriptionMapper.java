package com.tinyhis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyhis.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;

/**
 * Prescription Mapper
 */
@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {
}
