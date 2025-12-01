package com.tinyhis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Patient Register Request DTO
 */
@Data
public class PatientRegisterRequest {
    
    @NotBlank(message = "姓名不能为空")
    private String name;
    
    private String idCard;
    
    @NotBlank(message = "手机号不能为空")
    private String phone;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private Integer gender;
    
    private Integer age;
}
