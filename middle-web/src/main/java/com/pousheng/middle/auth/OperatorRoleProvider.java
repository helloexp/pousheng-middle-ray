package com.pousheng.middle.auth;

import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.parana.auth.api.Role;
import io.terminus.parana.auth.api.RoleProvider;
import io.terminus.parana.auth.api.RoleProviderRegistry;
import io.terminus.parana.auth.model.OperatorRole;
import io.terminus.parana.auth.service.OperatorReadService;
import io.terminus.parana.auth.service.OperatorRoleReadService;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.utils.Iters;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 运营人员
 * @author songrenfei
 */
@Slf4j
@Component
public class OperatorRoleProvider implements RoleProvider {

    @Autowired
    private RoleProviderRegistry roleProviderRegistry;
    @RpcConsumer
    private OperatorReadService operatorReadService;
    @RpcConsumer
    private OperatorRoleReadService operatorRoleReadService;



    @PostConstruct
    public void init() {
        roleProviderRegistry.addRoleProvider(this);
    }

    @Override
    public int acceptType() {
        return UserType.OPERATOR.value();
    }


    @Override
    public Role getRoleByUserId(Long userId) {
        val resp = operatorReadService.findByUserId(userId);
        if (!resp.isSuccess()) {
            log.warn("operator find fail, userId={}, error={}", userId, resp.getError());
            return null;
        }
        Long roleId = resp.getResult().getRoleId();
        List<String> nodes = Lists.newArrayList();
        List<String> names = Lists.newArrayList();
        if (roleId != null) {
            val roleResp = operatorRoleReadService.findById(roleId);
            if (!roleResp.isSuccess()) {
                log.warn("find role(id={}) of operator(userId={}) failed, error={}", roleId, userId, roleResp.getError());
            } else {
                OperatorRole role = roleResp.getResult();
                if (role.isActive()) {
                    nodes.addAll(Iters.nullToEmpty(role.getAllow()));
                    names.add(role.getName());
                }
            }
        }
        return Role.createDynamic(UserRole.OPERATOR.name(), Iters.nullToEmpty(nodes), names);
    }

}
