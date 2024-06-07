package com.hui.myteam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hui.myteam.common.ErrorCode;
import com.hui.myteam.constant.UserConstant;
import com.hui.myteam.exception.BusinessException;
import com.hui.myteam.mapper.UserMapper;
import com.hui.myteam.model.domain.User;
import com.hui.myteam.model.request.UserRegisterRequest;
import com.hui.myteam.service.UserService;
import com.hui.myteam.utils.AlgorithmUtils;
import com.hui.myteam.utils.TencentCOSUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hui.myteam.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "huilai";

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String username = userRegisterRequest.getUsername();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        Integer gender = userRegisterRequest.getGender();
        String phone = userRegisterRequest.getPhone();
        String email = userRegisterRequest.getEmail();

        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 6 || checkPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户不能包含特殊字符");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和校验密码不相同");
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUsername(username);
        user.setAvatarUrl("https://friend-1314004726.cos.ap-guangzhou.myqcloud.com/image%2Ffriend%2Fduolaameng.jpg");
        user.setPhone(phone);
        user.setProfile("该用户很懒，什么也没留下");
        user.setGender(gender);
        //设置标签
        String[] data = {"男"};
        Gson gson = new Gson();
        String man_json = gson.toJson(data); // 将数组转化为JSON格式的字符串
        if (gender == 0) {
            user.setTags(man_json);
        }
        if (gender == 1) {
            String[] lady = {"女"};
            String Lady_json = gson.toJson(lady); // 将数组转化为JSON格式的字符串
            user.setTags(Lady_json);
        }
        user.setEmail(email);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 3) {
            return null;
        }
        if (userPassword.length() < 6) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户（内存过滤）
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1. 先查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        // 2. 在内存中判断是否包含要求的标签
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    @Override
    public int updateUser(User user, User loginUser) {
        long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 补充校验，如果用户没有传任何要更新的值，就直接报错，不用执行 update 语句
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"没有转递参数");
        }
        // 如果是管理员，允许更新任意用户
        // 如果不是管理员，只允许更新当前（自己的）信息
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public List<User> matchUsers(long pageSize, long pageNum, User loginUser) {
        // 查询所有有标签的用户，并直接计算相似度
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>()
                .isNotNull("tags");

        List<User> userList = this.list(queryWrapper);
        String loginTags = loginUser.getTags();
        List<String> loginTagList = new Gson().fromJson(loginTags, new TypeToken<List<String>>() {
        }.getType());

        // 计算并排序相似度
        // 计算当前用户与所有用户的标签匹配度，并按照匹配度进行排序，获取的一个 pair 键值对列表。
        List<Pair<User, Long>> sortedPairs = userList.stream()
                .filter(user -> !StringUtils.isBlank(user.getTags()) && user.getId() != loginUser.getId())
                .map(user -> {
                    List<String> userTagList = new Gson().fromJson(user.getTags(), new TypeToken<List<String>>() {
                    }.getType());
                    long distance = AlgorithmUtils.minDistance(loginTagList, userTagList);
                    return new Pair<>(user, distance);
                })
                .sorted(Comparator.comparingLong(Pair::getValue))
                .collect(Collectors.toList());

        // 分页
        int startIndex = (int) ((pageNum - 1) * pageSize);
        int endIndex = (int) (pageNum * pageSize);
        if (endIndex > sortedPairs.size()) {
            endIndex = sortedPairs.size();
        }
        //将键值对列表进行分页，并获取用户列表。
        List<User> subList = sortedPairs.subList(startIndex, endIndex).stream()
                .map(Pair::getKey)
                .collect(Collectors.toList());

        //脱敏
        List<User> result = subList.stream().map(this::getSafetyUser).collect(Collectors.toList());

        return result;
    }


    @Override
    public int updateTags(Long userId, User loginUser, List<String> tags) {
        //更新用户信息，先获取到用户的 id，判断 id < 0 就没有此用户
        if (userId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //校验tags是否为空
        if (CollectionUtils.isEmpty(tags)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"标签数组为空");
        }
        //将拿到的标签列表转换成 Json格式
        Gson gson = new Gson();
        String StrToJson = gson.toJson(tags);
        //拿到要修改的用户id，然后修改
        //当前用户态的用户（已登录）不是管理员，并且修改的用户信息也不是已登录用户的（修改的不是自己的），就抛异常
        if (!isAdmin(loginUser) && !Objects.equals(userId, loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User user = new User();
        user.setId(userId);
        user.setTags(StrToJson);
        //触发更新
        return this.baseMapper.updateById(user);
    }

    @Override
    public int updateImg(User user, User loginUser, MultipartFile file) {
        //更新用户信息，先获取到用户的 id，判断 id < 0 就没有此用户
        Long userId = user.getId();
        if (userId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //校验权限（需要获取到用户登录态）
        //1.管理员可以更新任意信息
        //2.用户只能更新自己的信息

        //当前用户态的用户（已登录）不是管理员，并且修改的用户信息也不是已登录用户的（修改的不是自己的），就抛异常
        if (!isAdmin(loginUser) && !Objects.equals(userId, loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = this.getById(user.getId());
        //修改的用户不存在
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //n=0,图片存入用户图片的地址
        int n = 0;
        //上传图片并拿到对象地址
        String newAvatarUrl = TencentCOSUtils.uploadFile(file, n);
        //对象地址就是图片地址
        user.setAvatarUrl(newAvatarUrl);

        //触发更新
        return this.baseMapper.updateById(user);
    }

    /**
     * 根据标签搜索用户（SQL 查询版）
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Deprecated
    private List<User> searchUsersByTagsBySQL(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 拼接 and 查询
        // like '%Java%' and like '%Python%'
        for (String tagName : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

}




