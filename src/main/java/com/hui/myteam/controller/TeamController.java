package com.hui.myteam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hui.myteam.common.BaseResponse;
import com.hui.myteam.common.DeleteRequest;
import com.hui.myteam.common.ErrorCode;
import com.hui.myteam.common.ResultUtils;
import com.hui.myteam.exception.BusinessException;
import com.hui.myteam.model.domain.Team;
import com.hui.myteam.model.domain.User;
import com.hui.myteam.model.domain.UserTeam;
import com.hui.myteam.model.dto.TeamQuery;
import com.hui.myteam.model.request.TeamAddRequest;
import com.hui.myteam.model.request.TeamJoinRequest;
import com.hui.myteam.model.request.TeamQuitRequest;
import com.hui.myteam.model.request.TeamUpdateRequest;
import com.hui.myteam.model.vo.TeamUserVO;
import com.hui.myteam.model.vo.UserVO;
import com.hui.myteam.service.TeamService;
import com.hui.myteam.service.UserService;
import com.hui.myteam.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 队伍接口
 *
 */
@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        List<String> tags = teamAddRequest.getTags();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, loginUser,tags);
        return ResultUtils.success(teamId);
    }
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<TeamUserVO> getTeamById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //根据队伍Id，查team
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        TeamUserVO teamUserVo = new TeamUserVO();
        BeanUtils.copyProperties(team,teamUserVo);
        //根据队伍Id，查询user_team表，获取加入的用户
        List<User> joinUser = teamService.listJoinUsers(team.getId());
        teamUserVo.setJoinUsers(joinUser);
        //设置创建人的userID
        Long userId = team.getUserId();
        User safetyUser = userService.getSafetyUser(userService.getById(userId));
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(safetyUser,userVO);
        teamUserVo.setCreateUser(userVO);

        return ResultUtils.success(teamUserVo);
    }


    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery,HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        Integer status = teamQuery.getStatus();
        if (status == null){
            status = 0;
        }
        queryWrapper.eq("status",status);
        //根据关键词查询
        if(teamQuery.getSearchText() != null){
            queryWrapper.and(qw -> qw.like("name", teamQuery.getSearchText()).or().like("description", teamQuery.getSearchText()));
        }
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        //查看当前用户是否加入队伍
        List<Team> records = resultPage.getRecords();
        //获取当前用户id
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        for (Team record: records){
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("teamId", record.getId());
            userTeamQueryWrapper.eq("userId", userId);
            List<UserTeam> list = userTeamService.list(userTeamQueryWrapper);
            if (list.size() != 0){
                record.setHasJoin(true);
            }
        }
        return ResultUtils.success(resultPage);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    //退出队伍
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 修改队伍头像
     *
     * @param file
     * @param teamId
     * @return
     */
    @PostMapping("/updateTeamUrl")
    public BaseResponse<Integer> updateTeamUrl(@RequestParam(value = "file") MultipartFile file, Long teamId){
        //校验参数是否为空
        if (teamId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //触发更新
        int result = teamService.updateTeamUrl(file, teamId);
        return ResultUtils.success(result);
    }

    /**
     * 用户标签
     *
//     * @param teamUpdateRequest
     * @param request
     * @return
     */
//    @PostMapping("/updateTags")
//    public BaseResponse<Integer> updateTags(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request){
//        //校验参数是否为空
//        if (teamUpdateRequest == null){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        //队伍的id
//        Long teamId = teamUpdateRequest.getId();
//        //修改者的标签
////        List<String> tags = teamUpdateRequest.getTags();
//        //校验权限（需要拿到当前用户的用户登录态）
//        User loginUser = userService.getLoginUser(request);
//        //触发更新
////        int result = teamService.teamUpdateTags(teamId, loginUser, tags);
////        return ResultUtils.success(result);
//    }
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getTeamId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getTeamId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }


    /**
     * 获取我创建的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/create1")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams1(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我创建的队伍
     *
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<TeamUserVO> teamList = teamService.listMyCreateTeams(loginUser.getId());
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我加入的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        // 取出不重复的队伍 id
        // teamId userId
        // 1, 2
        // 1, 3
        // 2, 3
        // result
        // 1 => 2, 3
        // 2 => 3
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        List<TeamUserVO> result = teamList.stream().map(teamUserVO -> {
            teamUserVO.setHasJoin(true);
            return teamUserVO;
        }).collect(Collectors.toList());
        return ResultUtils.success(result);
    }
}



























