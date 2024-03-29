package com.dzdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzdp.dto.LoginFormDTO;
import com.dzdp.dto.Result;
import com.dzdp.entity.User;

import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {

    Result sendMailCode(String phone);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();
}
