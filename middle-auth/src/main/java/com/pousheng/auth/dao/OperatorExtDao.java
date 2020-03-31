package com.pousheng.auth.dao;

import org.springframework.stereotype.Repository;

import com.pousheng.auth.model.OperatorExt;

import io.terminus.common.mysql.dao.MyBatisDao;

@Repository
public class OperatorExtDao extends MyBatisDao<OperatorExt>{

	public OperatorExt findByUserId(Long userId) {
		return this.getSqlSession().selectOne(sqlId("findByUserId"), userId);
	}

	public OperatorExt findByUserName(String userName) {
		return this.getSqlSession().selectOne(sqlId("findByUserName"), userName);
	}

}
