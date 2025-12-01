package com.tinyhis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyhis.entity.MedicalRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * Medical Record Mapper
 */
@Mapper
public interface MedicalRecordMapper extends BaseMapper<MedicalRecord> {
}
