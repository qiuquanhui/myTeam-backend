package com.hui.myteam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hui.myteam.model.domain.Team;
import com.hui.myteam.model.domain.User;
import com.hui.myteam.model.dto.TeamQuery;
import com.hui.myteam.model.request.TeamJoinRequest;
import com.hui.myteam.model.request.TeamQuitRequest;
import com.hui.myteam.model.request.TeamUpdateRequest;
import com.hui.myteam.model.vo.TeamUserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 队伍服务
 *
 */
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser,List<String> tags);

    /**
     * 搜索队伍
     *
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 获取我创建的队伍
     *
     * @return
     */
    List<TeamUserVO> listMyCreateTeams(long userId);

    /**
     * 更新队伍
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     *
     * @param teamJoinRequest
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);


    /**
     * 删除（解散）队伍
     *
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id, User loginUser);

    /**
     * 根据队伍id 获取所有加入队伍的用户信息
     *
     * @param id
     * @return
     */
    List<User> listJoinUsers(Long id);

    int teamUpdateTags(Long teamId, User loginUser, List<String> tags);

    /**
     * 修改队伍头像
     *
     * @param file
     * @param teamId
     * @return
     */
    int updateTeamUrl(MultipartFile file, Long teamId);
}
