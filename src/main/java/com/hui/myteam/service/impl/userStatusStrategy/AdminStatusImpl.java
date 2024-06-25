package com.hui.myteam.service.impl.userStatusStrategy;/**
 * 作者:灰爪哇
 * 时间:2024-06-13
 */

import com.hui.myteam.common.ErrorCode;
import com.hui.myteam.exception.BusinessException;
import com.hui.myteam.mapper.UserMapper;
import com.hui.myteam.model.domain.User;
import com.hui.myteam.service.UpateStatusChangeStrategy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 *
 *
 * @author: Hui
 **/
@Service
public class AdminStatusImpl implements UpateStatusChangeStrategy {

    @Resource
    private UserMapper userMapper;

    @Override
    public Boolean updateUserStatus(Long id) {

        //校验参数
        if (id < 0 || id == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User user = userMapper.selectById(id);
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //修改状态值
        user.setUserStatus(1);
        int  result = userMapper.updateById(user);

        if (result != 0){
            return true;
        }
        return false;
    }
}
