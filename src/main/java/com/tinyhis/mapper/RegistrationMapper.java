package com.tinyhis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyhis.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * Registration Mapper
 */
@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    @Update("UPDATE registration r " +
            "INNER JOIN schedule s ON r.schedule_id = s.schedule_id " +
            "SET r.status = 6 " +
            "WHERE r.status IN (0, 1, 2) AND s.schedule_date < CURDATE()")
    int expirePastRegistrations();
}

