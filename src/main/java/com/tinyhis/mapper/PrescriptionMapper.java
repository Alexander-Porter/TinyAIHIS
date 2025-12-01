package com.tinyhis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyhis.dto.DrugUsageReportDTO;
import com.tinyhis.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prescription Mapper
 */
@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {

    @Select("<script>" +
            "SELECT d.dept_name as deptName, dd.name as drugName, SUM(p.quantity) as totalQuantity, COUNT(p.pres_id) as totalTimes " +
            "FROM prescription p " +
            "JOIN medical_record mr ON p.record_id = mr.record_id " +
            "JOIN sys_user u ON mr.doctor_id = u.user_id " +
            "JOIN department d ON u.dept_id = d.dept_id " +
            "JOIN drug_dict dd ON p.drug_id = dd.drug_id " +
            "WHERE p.create_time BETWEEN #{startDate} AND #{endDate} " +
            "<if test='deptIds != null and !deptIds.isEmpty()'>" +
            "AND u.dept_id IN " +
            "<foreach collection='deptIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</if>" +
            "GROUP BY d.dept_name, dd.name" +
            "</script>")
    List<DrugUsageReportDTO> getDrugUsageReport(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate, 
                                                @Param("deptIds") List<Long> deptIds);
}
