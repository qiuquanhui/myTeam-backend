package com.hui.myteam.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hui.myteam.common.BaseResponse;
import com.hui.myteam.common.ErrorCode;
import com.hui.myteam.common.ResultUtils;
import com.hui.myteam.constant.RedisConstant;
import com.hui.myteam.exception.BusinessException;
import com.hui.myteam.model.domain.User;
import com.hui.myteam.model.request.UserLoginRequest;
import com.hui.myteam.model.request.UserRegisterRequest;
import com.hui.myteam.model.request.UserUpdateTagsRequest;
import com.hui.myteam.model.vo.UserVO;
import com.hui.myteam.service.UserService;
import com.hui.myteam.utils.SendMailUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.hui.myteam.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisTemplate<String, String> stringRedisTemplate;

    private final ReentrantLock lock = new ReentrantLock();

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    /**
     * 用户标签
     *
     * @param userUpdateTagsRequest
     * @param request
     * @return
     */
    @PostMapping("/updateTags")
    public BaseResponse<Integer> updateTags(@RequestBody UserUpdateTagsRequest userUpdateTagsRequest, HttpServletRequest request) {
        //校验参数是否为空
        if (userUpdateTagsRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //修改者的id
        Long userId = userUpdateTagsRequest.getId();
        //修改者的标签
        List<String> tags = userUpdateTagsRequest.getTags();
        //校验权限（需要拿到当前用户的用户登录态）
        User loginUser = userService.getLoginUser(request);
        //触发更新
        int result = userService.updateTags(userId, loginUser, tags);
        return ResultUtils.success(result);
    }


    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return ResultUtils.success(currentUser);
    }

    /**
     * 获取个人用户信息
     *
     * @return
     * @Param   id
     */
    @GetMapping("/getById/{id}")
    public BaseResponse<User> getUserById(@PathVariable("id") Long id) {

        //随机时间：用于解决缓存雪崩问题
        long minValue = 20L;
        long maxValue = 50L;
        long randomNumber = minValue + new Random().nextLong() % (maxValue - minValue + 1L);

        //校验参数是否正常
        if (id == null || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1. 从缓存中查询用户缓存
        String Key = RedisConstant.REDIS_USER_CATCH + id;
        String value = stringRedisTemplate.opsForValue().get(Key);

        // 2. 如果不为空且不为""就返回
        if (StringUtils.isNotBlank(value)) {
            //如果不为空就返回
            User user = JSONUtil.toBean(value, User.class);
            return ResultUtils.success(user);
        }
        //3.不为空且为""就返回错误信息，是为了防止缓存穿透所以存储了空值""
        if (value != null) {
            return ResultUtils.error(500001, "查询id不存在");
        }

        //为空且不为""查询数据库
        //4.1加入锁更新缓存
        try {
            //加锁
            lock.lock();
            //4.2 缓存更新
            User user = userService.getById(id);
            //5.数据库也没有当前用户就直接缓存空值
            if (user == null) {
                //将数据写入redis
                stringRedisTemplate.opsForValue().set(Key, "", RedisConstant.REDIS_NULL_TTL + randomNumber, TimeUnit.MINUTES);
                return ResultUtils.error(500001, "该用户不存在");
            }
            //6.数据库有当前数据，缓存当前用户
            stringRedisTemplate.opsForValue().set(Key, JSONUtil.toJsonStr(user), RedisConstant.REDIS_CATCH_TTL + randomNumber, TimeUnit.MINUTES);
            return ResultUtils.success(user);
        } catch (Exception e) {
            log.info(e.getMessage());
        } finally {
            lock.unlock();
        }
        //7.返回错误信息
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 修改用户头像
     *
     * @param user
     * @return
     */
    @PostMapping("/updateImg")
    public BaseResponse<Integer> updateImg(@RequestParam(value = "file") MultipartFile file, User user, HttpServletRequest request) {
        //校验参数是否为空
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //校验权限（需要拿到当前用户的用户登录态）
        User loginUser = userService.getLoginUser(request);
        //触发更新
        int result = userService.updateImg(user, loginUser, file);
        return ResultUtils.success(result);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * @param pageSize
     * @param pageNum
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        String redisKey = String.format("huilai:usewtecommend:%s", 1L); //与缓存预热对应上
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        // 如果有缓存，直接读缓存
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }
        // 无缓存，查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        // 写缓存
        try {
            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return ResultUtils.success(userPage);
    }

    @GetMapping("/searchNearUser")
    public BaseResponse<List<UserVO>> searchNearUser(Integer radius, HttpServletRequest request) {
        if (radius == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        return ResultUtils.success(userService.searchNearUser(radius, request));
    }


    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        // 校验参数是否为空
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        int result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(userService.removeById(id));
    }

    /**
     * 获取最匹配的用户
     *
     * @param pageSize
     * @param pageNum
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long pageSize, long pageNum, HttpServletRequest request) {
        if (pageSize <= 0 || pageSize > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(pageSize, pageNum, user));
    }

    /**
     * 修改用户状态位 -- 使用策略模式
     *
     * @return
     */
    @PutMapping("/adminStatus")
    public BaseResponse<Boolean> adminStatus(Long id, Integer type) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = userService.updateUserStatusStrategy(id, type);

        return ResultUtils.success(result);
    }

    @PostMapping("/getCode")
    public BaseResponse<String> mail(String targetEmail) {
        String yzm = stringRedisTemplate.opsForValue().get(targetEmail);
        // 判断是否存在
        if (yzm == null) {
            // 生成六位数验证码
            String authCode = String.valueOf(new Random().nextInt(899999) + 100000);
            SendMailUtil.sendEmailCode(targetEmail, "你的验证码为:" + authCode + "(一分钟内有效)");
            stringRedisTemplate.opsForValue().set(targetEmail, authCode, 1, TimeUnit.MINUTES);
            return ResultUtils.success("短信发送成功");
        }
        // 随机生成六位数验证码
        return ResultUtils.error(40001, "验证请发送，不重复发送");
    }

//    @PostMapping("/mailCodeLogin")
//    public BaseResponse<User> mailCodeLogin(@RequestBody MailCodeLogin mailCodeLogin, HttpServletRequest request) {
//        if (mailCodeLogin == null) {
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
//        }
//
//        String code = mailCodeLogin.getCode();
//        String email = mailCodeLogin.getEmail();
//
//        if (StringUtils.isAnyBlank(email,code)) {
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
//        }
////        User user = userService.userLogin(email,code, request);
//        //1.根据邮箱从 Redis 中获取验证码，判断是否存在，验证码是否相同，相同则根据邮箱查询用户信息并存储在httpServletRequest中
//        return ResultUtils.success(user);
//    }
}
