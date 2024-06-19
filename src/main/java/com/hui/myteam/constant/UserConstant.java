package com.hui.myteam.constant;

/**
 * 用户常量
 *
 * 
 * 
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "userLoginState";

    //  ------- 权限 --------

    /**
     * 默认权限
     */
    int DEFAULT_ROLE = 0;

    /**
     * 管理员权限
     */
    int ADMIN_ROLE = 1;

    /**
     * 管理员权限
     */
    String REDIS_GEO_KEY = "user:key:geo";


    /**
     * 管理员权限
     */
    String REDIS_USER_KEY = "user:key:recommend";

}
