package com.pousheng.middle.auth;

import com.google.common.base.Throwables;
import com.pousheng.auth.model.User;
import com.pousheng.auth.service.UserReadService;
import io.terminus.common.model.Response;
import io.terminus.parana.auth.api.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 中台用户角色加载器
 *
 * @author songrenfei
 */
@Slf4j
@Component
public class MiddleUserRoleLoader implements UserRoleLoader {

    @Autowired
    private UserReadService userReadService;

    @Autowired
    private RoleProviderRegistry roleProviderRegistry;

    @Override
    public Response<RoleContent> hardLoadRoles(Long userId) {
        try {
            if (userId == null) {
                log.warn("hard load roles failed, userId=null");
                return Response.fail("user.id.empty");
            }
            val findResp = userReadService.findById(userId);
            if (!findResp.isSuccess()) {
                log.warn("find user failed, userId={}, error={}", userId, findResp.getError());
                return Response.fail(findResp.getError());
            }
            User user = findResp.getResult();
            if (user == null) {
                // findById 已经保证不会进入这里
                log.warn("hard load roles failed, user not found, id={}", userId);
                return Response.fail("user.not.found");
            }

            if (user.getType() == null) {
                log.warn("user has no type, userId={}, we treat is as empty permission", userId);
                return Response.ok(initRoles());
            }
            int userType = user.getType();

            RoleContent mutableRoles = initRoles();

            List<RoleProvider> roleProviders = roleProviderRegistry.getRoleProviders();
            if (!CollectionUtils.isEmpty(roleProviders)) {
                for (RoleProvider roleProvider : roleProviders) {
                    if (roleProvider.acceptType() != userType) {
                        continue;
                    }
                    Role role = roleProvider.getRoleByUserId(userId);
                    if (role != null) {
                        if (role.getType() == 1) {
                            // static
                            mutableRoles.getRoles().add(role);
                        } else {
                            mutableRoles.getDynamicRoles().add(role);
                        }
                    }
                }
            }
            return Response.ok(mutableRoles);
        } catch (Exception e) {
            log.error("hard load rich roles failed, userId={}, cause:{}",
                    userId, Throwables.getStackTraceAsString(e));
            return Response.fail("user.role.load.fail");
        }
    }

    private RoleContent initRoles() {
        RoleContent non = new RoleContent();
        non.setRoles(new ArrayList<Role>());
        non.setDynamicRoles(new ArrayList<Role>());
        return non;
    }

}
