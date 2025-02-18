package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeeUpdatePasswordDTO implements Serializable {
    // 员工账号
    private Long empId;
    // 原始密码
    private String oldPassword;
    // 更改后的密码
    private String newPassword;

}
