package com.pousheng.auth.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.base.Throwables;
import com.pousheng.auth.dao.OperatorExtDao;
import com.pousheng.auth.model.OperatorExt;
import com.pousheng.auth.service.OperatorExtReadService;

import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * 添加 realName
 * Created by will.gong on 2019/06/04
 */
@Slf4j
@Service
@RpcProvider
public class OperatorExtReadServiceImpl implements OperatorExtReadService {
	
	private static final Logger log = LoggerFactory.getLogger(OperatorExtReadServiceImpl.class);
	
	@Autowired
	private OperatorExtDao operatorExtDao;
	
	@Override
	public Response<OperatorExt> findByUserId(Long userId) {
		try {
			OperatorExt exist = operatorExtDao.findByUserId(userId);
			if (StringUtils.isEmpty(exist)) {
				log.warn("operator(userId={}) not exist", userId);
				return Response.fail("operator.not.exist");
			} else {
				return Response.ok(exist);
			}
		} catch (Exception var1) {
			log.error("find operator(userid={}) failed, cause:{}", 
					userId, Throwables.getStackTraceAsString(var1));
			return Response.fail("operator.find.fail");
		}
	}

	@Override
	public Response<OperatorExt> findByUserName(String userName) {
		try {
			OperatorExt exist = operatorExtDao.findByUserName(userName);
			if (StringUtils.isEmpty(exist)) {
				log.warn("operator(username={}) not exist", userName);
				return Response.fail("operator.not.exist");
			} else {
				return Response.ok(exist);
			}
		} catch (Exception var2) {
			log.error("find operator(username={} failed, cause:{})", 
					userName, Throwables.getStackTraceAsString(var2));
			return Response.fail("operator.find.fail");
		}
	}

}
