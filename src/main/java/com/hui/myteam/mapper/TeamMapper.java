package com.hui.myteam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hui.myteam.model.domain.Team;
import com.hui.myteam.model.domain.User;

import java.util.List;

/**
 * 队伍 Mapper
 *
 */
public interface TeamMapper extends BaseMapper<Team> {

    List<User> selectJoinUsers(Long id);
}




