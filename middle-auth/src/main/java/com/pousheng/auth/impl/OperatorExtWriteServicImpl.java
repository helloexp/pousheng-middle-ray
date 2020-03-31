package com.pousheng.auth.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.pousheng.auth.dao.OperatorExtDao;
import com.pousheng.auth.model.OperatorExt;
import com.pousheng.auth.service.OperatorExtWriteService;

import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.auth.impl.dao.OperatorRoleDao;
import io.terminus.parana.auth.model.OperatorRole;
import lombok.extern.slf4j.Slf4j;

/**
 * 添加realname
 * Created by will.gong on 2019/06/04
 */
@Slf4j
@Service
@RpcProvider
public class OperatorExtWriteServicImpl implements OperatorExtWriteService {

	private static final Logger log = LoggerFactory.getLogger(OperatorExtWriteServicImpl.class);
	@Autowired
	private OperatorRoleDao operatorRoleDao;
	@Autowired
	private OperatorExtDao operatorExtDao;
	
	@Override
	public Response<Long> create(OperatorExt operator) {
		try {
			if (StringUtils.isEmpty(operator)) {
				return Response.fail("operator.can.not.be.null");
			} else if (StringUtils.isEmpty(operator.getUserId())) {
				return Response.fail("operator.userid.can.not.be.null");
			} else if (StringUtils.isEmpty(operator.getRoleId())) {
				return Response.fail("operator.roleid.can.not.be.null");
			}
			operator.setStatus(1);//enable
			operator.setRoleName(findRoleName(operator.getRoleId()));
			operatorExtDao.create(operator);
			return Response.ok(operator.getId());
		} catch (Exception var1) {
			log.error("create operator({}) failed, cause:{}", 
					operator, Throwables.getStackTraceAsString(var1));
			return Response.fail("create.operator.fail");
		}
	}

	private String findRoleName(Long roleId) {
		if (StringUtils.isEmpty(roleId)) {
			return "";
		}
		OperatorRole operatorRole = operatorRoleDao.findById(roleId);
		if (StringUtils.isEmpty(operatorRole)) {
			log.warn("not find role(id{})", roleId);
			return "";
		}
		return Strings.nullToEmpty(operatorRole.getName());
	}

	@Override
	public Response<Boolean> update(OperatorExt operator) {
		try {
			if (StringUtils.isEmpty(operator.getId())) {
				return Response.fail("update.operator.fail");
			} else {
				Long roleId = operator.getRoleId();
				if (StringUtils.isEmpty(roleId)) {
					OperatorExt toRoleOperator = operatorExtDao.findById(operator.getId());
					roleId= toRoleOperator.getRoleId();
				}
				operator.setRoleName(findRoleName(roleId));
				return Response.ok(operatorExtDao.update(operator));
			}
		} catch (Exception var2) {
			log.error("update operator({}) failed, cause:{}", 
					operator, Throwables.getStackTraceAsString(var2));
			return Response.fail("update.operator.fail");
		}
	}
}
