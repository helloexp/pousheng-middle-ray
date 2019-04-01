package com.pousheng.middle.auth;

import io.terminus.parana.auth.api.Role;
import io.terminus.parana.auth.api.RoleProvider;
import io.terminus.parana.auth.api.RoleProviderRegistry;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.enums.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 超级管理员
 * @author songrenfei
 */
@Component
public class AdminRoleProvider implements RoleProvider {

    @Autowired
    private RoleProviderRegistry roleProviderRegistry;

    @PostConstruct
    public void init() {
        roleProviderRegistry.addRoleProvider(this);
    }

    @Override
    public int acceptType() {
        return UserType.ADMIN.value();
    }

    @Override
    public Role getRoleByUserId(Long userId) {
        return Role.createStatic(UserRole.ADMIN.name());
    }
}
