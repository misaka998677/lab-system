package com.lab.module.system.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String confirmPassword;
    private String realName;
    private String phone;
    private String email;
    private String roleType;
    private Long deptId;
}
