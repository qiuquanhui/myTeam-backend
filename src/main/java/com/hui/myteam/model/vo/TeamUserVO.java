package com.hui.myteam.model.vo;

import com.hui.myteam.model.domain.User;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 队伍和用户信息封装类（脱敏）
 *
 * 
 * 
 */
@Data
public class TeamUserVO implements Serializable {

    private static final long serialVersionUID = 1899063007109226944L;

    /**
     * id
     */
    private Long id;

    /**
     * 队伍头像
     */
    private String teamUrl;

    /**
     * 标签列表 json
     */
    private String tags;

    /**
     * 队伍名称
     */
    private String name;


    /**
     * 描述
     */
    private String description;

    /**
     * 加入队伍的用户信息
     */
    private List<User> joinUsers;

    /**
     * 已加入队伍人数
     */
    private long hasJoinNum;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人用户信息
     */
    private UserVO createUser;


    /**
     * 是否已加入队伍
     */
    private boolean hasJoin = false;
}
