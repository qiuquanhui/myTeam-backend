package com.hui.myteam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hui.myteam.service.UserTeamService;
import com.hui.myteam.model.domain.UserTeam;
import com.hui.myteam.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
 * 用户队伍服务实现类
 *
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
        implements UserTeamService {

}




