package com.hui.myteam.service;/**
 * 作者:灰爪哇
 * 时间:2024-06-19
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hui.myteam.constant.UserConstant;
import com.hui.myteam.model.domain.User;
import com.hui.myteam.model.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Hui
 **/
@SpringBootTest
public class GeoTest {

//    1. 使用 GEOAdd 上传所有用户的经度与纬度信息
//    2. 计算出当前登录用户与所有用户的距离，将其加入到 返回用户信息的列表中
//    3. 计算出当前登录用户 1500 KM内的用户并按照距离降序排序。定义新接口。

    @Resource
    private RedisTemplate<String, String> stringRedisTemplate;

    @Resource
    private UserService userService;

    //    1. 使用 GEOAdd 上传所有用户的经度与纬度信息
    @Test
    public void geoAddUser() {
        System.out.println("geoAddUser");
        //1.查询所有用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.isNotNull("longitude");
        userQueryWrapper.isNotNull("latitude");
        List<User> userList = userService.list(userQueryWrapper);

        //2.将 经度与纬度都不为空的用户插入到redis GEO 中
        if (userList == null) {
            return;
        }
        List<RedisGeoCommands.GeoLocation<String>> locationList = new ArrayList<>(userList.size());
        for (User user : userList) {
            long id = user.getId();
            //long类型转化为String
            String idStr = String.valueOf(id);
            Point point = new Point(user.getLongitude(), user.getLatitude());
            RedisGeoCommands.GeoLocation<String> location = new RedisGeoCommands.GeoLocation<>(idStr, point);
            locationList.add(location);
        }
        stringRedisTemplate.opsForGeo().add(UserConstant.REDIS_GEO_KEY, locationList);
    }

    //    2. 计算出当前登录用户"15"为例，与所有用户的距离，将其加入到 返回用户信息的列表中
    @Test
    public void getDistance() {
        //2.计算当前登录用户与所有用户的距离
        // 2.1 查询所有用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.isNotNull("longitude");
        userQueryWrapper.isNotNull("latitude");
        List<User> userList = userService.list(userQueryWrapper);
        if (userList == null) {
            return;
        }
        ArrayList<UserVO> userVOS = new ArrayList<>(userList.size());
        for (User user : userList) {
            double distance = stringRedisTemplate.opsForGeo().distance(UserConstant.REDIS_GEO_KEY, "15", String.valueOf(user.getId()), RedisGeoCommands.DistanceUnit.KILOMETERS).getValue();
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            userVO.setDistance(distance);
            userVOS.add(userVO);
        }
        //3.返回数据
        System.out.println(userVOS);
    }

    //    3. 计算出当前登录用户 1500 KM内的用户并按照距离降序排序。定义新接口。
    @Test
    public void getRedius() {
        //    3. 计算出当前登录用户"15"为例 1500 KM内的用户并按照距离降序排序
        User loginUser = userService.getById(15);

        //创建距离
        Distance distance = new Distance(1500, RedisGeoCommands.DistanceUnit.KILOMETERS);

        //创建参数
        RedisGeoCommands.GeoRadiusCommandArgs geoRadiusCommandArgs = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeCoordinates().includeDistance().sort(Sort.Direction.ASC);

        //进行计算
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = stringRedisTemplate.opsForGeo().radius(UserConstant.REDIS_GEO_KEY,
                String.valueOf(loginUser.getId()), distance, geoRadiusCommandArgs);

        //遍历数据
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = geoResults.getContent();

        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : content) {
            RedisGeoCommands.GeoLocation<String> location = result.getContent();
            String id = location.getName();
            User user = userService.getById(Long.valueOf(id));
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            userVO.setDistance(result.getDistance().getValue());
            System.out.println(userVO);
        }
    }



}
