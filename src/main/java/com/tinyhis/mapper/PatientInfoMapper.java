package com.tinyhis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyhis.entity.PatientInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * Patient Info Mapper
 */
@Mapper
public interface PatientInfoMapper extends BaseMapper<PatientInfo> {
}
