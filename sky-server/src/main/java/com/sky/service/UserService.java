package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.vo.UserLoginVO;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    UserLoginVO wxLogin(UserLoginDTO userLoginDTO);
}
