/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.events.user;

import io.terminus.parana.common.model.ParanaUser;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-31
 */
@AllArgsConstructor
public class LoginEvent implements Serializable{


    private static final long serialVersionUID = 7387597195528297937L;
    @Getter
    private HttpServletRequest request;

    @Getter
    private HttpServletResponse response;

    @Getter
    private  ParanaUser user;


}
