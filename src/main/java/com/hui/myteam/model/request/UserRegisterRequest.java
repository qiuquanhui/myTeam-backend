package com.hui.myteam.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String userAccount;

    private String username;

    private String userPassword;

    private String checkPassword;

    private Integer gender;

    private String phone;

    private String email;

}
