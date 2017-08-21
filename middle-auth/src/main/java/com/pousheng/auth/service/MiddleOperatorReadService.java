package com.pousheng.auth.service;

import com.google.common.base.Throwables;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.auth.impl.dao.OperatorDao;
import io.terminus.parana.auth.model.Operator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by tony on 2017/8/12.
 * pousheng-middle
 */
@Service
@Slf4j
public class MiddleOperatorReadService {
    @Autowired
    private OperatorDao operatorDao;

    public Response<Paging<Operator>> pagination(Long roleId, Long userId,String userName,String roleName,Integer status, Integer pageNo, Integer size) {
        try {
            PageInfo page = new PageInfo(pageNo, size);
            Operator criteria = new Operator();
            criteria.setStatus(status);
            criteria.setRoleId(roleId);
            criteria.setUserId(userId);
            criteria.setUserName(userName);
            criteria.setRoleName(roleName);
            return Response.ok(this.operatorDao.paging(page.getOffset(), page.getLimit(), criteria));
        } catch (Exception var7) {
            log.error("paging operator by roleId={}, status={}, pageNo={} size={} failed, cause:{}", new Object[]{roleId, status, pageNo, size, Throwables.getStackTraceAsString(var7)});
            return Response.fail("operator.find.fail");
        }
    }
}
