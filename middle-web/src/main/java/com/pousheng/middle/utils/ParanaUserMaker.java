/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.utils;

import com.google.common.base.Strings;
import com.pousheng.auth.model.User;
import io.terminus.common.utils.BeanMapper;
import io.terminus.parana.common.model.ParanaUser;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-31
 */
public abstract class ParanaUserMaker {
    public static ParanaUser from(User user){
        ParanaUser paranaUser = new ParanaUser();
        BeanMapper.copy(user, paranaUser);
        paranaUser.setName(getUserName(user));
        return paranaUser;
    }

    public static String getUserName(User user) {
        if (!Strings.isNullOrEmpty(user.getName())) {
            return user.getName();
        }
        return "";
    }
}
