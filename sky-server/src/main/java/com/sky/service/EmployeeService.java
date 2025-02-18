package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

import java.util.List;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);


    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);


    void update(EmployeeDTO employeeDTO);

    void updateStatus(Integer status,Long id);

    Employee getById(Long id);

    void updatePassword(PasswordEditDTO passwordEditDTO);


}
