package com.sky.controller.admin;

import com.github.pagehelper.Page;
import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.*;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 新增员工
     * @param employeeDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增员工")
    public Result<String> save(@RequestBody EmployeeDTO employeeDTO) {
        log.info("新增员工：{}", employeeDTO);
        // 前端已经包含对电话号码和身份证号的校验
        employeeService.save(employeeDTO);
        return Result.success("新增员工成功");
    }

    @GetMapping("/page")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO) {

        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);

        return Result.success(pageResult);
    }

    @PostMapping("/status/{status}")
    public Result updateStatus(@PathVariable Integer status,Long id) {
        employeeService.updateStatus(status, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }
    @PutMapping
    public Result update(@RequestBody EmployeeDTO employeeDTO) {
         employeeService.update(employeeDTO);
        return Result.success();
    }

    @PutMapping("/editPassword")
    public Result updatePassword(@RequestBody PasswordEditDTO passwordEditDTO) {
        //       log.info("原始密码为：{}，更改后密码为：{}", oldPassword, newPassword);
//        log.info("员工id为：{}", employeeUpdatePasswordDTO.getEmpId());
        if(passwordEditDTO.getOldPassword().equals(passwordEditDTO.getNewPassword())) {
            return Result.error("新密码不能与旧密码相同");
        }
        employeeService.updatePassword(passwordEditDTO);
        return Result.success();
    }

}
