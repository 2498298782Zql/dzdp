package com.dzdp.dto;

import lombok.Data;

@Data
public class LoginFormDTO {
    private String phone;
    private String code; // 验证码
    private String password;
}
