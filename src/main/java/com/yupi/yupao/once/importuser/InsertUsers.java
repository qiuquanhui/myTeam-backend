package com.yupi.yupao.once.importuser;

import com.yupi.yupao.mapper.UserMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 导入用户任务
 *
 * 
 * 
 */
@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

}
