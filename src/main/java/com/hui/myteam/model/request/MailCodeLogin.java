package com.hui.myteam.model.request;/**
 * 作者:灰爪哇
 * 时间:2024-06-25
 */

import lombok.Data;

/**
 *
 *
 * @author: Hui
 **/
@Data
public class MailCodeLogin {


    /**
     * 邮箱
     */
    private String email;

    private String code;
}
