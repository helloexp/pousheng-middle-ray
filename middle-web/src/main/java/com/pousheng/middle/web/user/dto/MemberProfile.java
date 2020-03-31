/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.user.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author : panxin
 */
@Data
public class MemberProfile implements Serializable {

    private static final long serialVersionUID = -5298314632808863710L;
    private Long id;                // 会员id
    private Long userId;            // 用户id
    private String outerId;           // 会员卡号
    private String nickname;        // 会员昵称
    private String firstName;
    private String lastName;
    private String gender;
    private Date birthday;
    private Integer type;           // 会员类型
    private String mobile;          // 手机号
    private String email;           // 常用邮箱
    private Integer status;         // 会员状态
    private String from;           // 渠道
    private Integer fromType;
    private Integer level;
    private Long point;             // 会员积分
    private String avatar;          //头像

}
