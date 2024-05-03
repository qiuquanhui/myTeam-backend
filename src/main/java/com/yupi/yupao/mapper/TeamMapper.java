package com.yupi.yupao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.yupao.model.domain.Team;
import com.yupi.yupao.model.domain.User;

import java.util.List;

/**
 * 队伍 Mapper
 *
 */
public interface TeamMapper extends BaseMapper<Team> {

    List<User> selectJoinUsers(Long id);
}




