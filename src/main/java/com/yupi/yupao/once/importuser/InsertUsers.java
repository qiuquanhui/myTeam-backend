package com.yupi.yupao.once.importuser;

import com.yupi.yupao.mapper.UserMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 导入用户任务
 *
 * @author <a href="https://github.com/liyupi"> </a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

}
