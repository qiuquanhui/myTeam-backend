package com.hui.myteam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hui.myteam.model.domain.User;
import com.hui.myteam.model.request.UserRegisterRequest;
import com.hui.myteam.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务
 *
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userRegisterRequest   注册用户请求
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList
     * @return
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 更新用户信息
     * @param user
     * @return
     */
    int updateUser(User user, User loginUser);

    /**
     * 获取当前登录用户信息
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    boolean isAdmin(User loginUser);

    /**
     * 匹配用户
     * @param pageSize
     * @param pageNum
     * @param loginUser
     * @return
     */
    List<User> matchUsers(long pageSize, long pageNum, User loginUser);

    int updateTags(Long userId, User loginUser, List<String> tags);

    /**
     * 修改用户头像
     *
     * @param user
     * @param loginUser
     * @param file
     * @return
     */
    int updateImg(User user, User loginUser, MultipartFile file);

    List<UserVO> searchNearUser(Integer radius, HttpServletRequest request);

    Boolean updateUserStatusStrategy(Long id, Integer type);
}
