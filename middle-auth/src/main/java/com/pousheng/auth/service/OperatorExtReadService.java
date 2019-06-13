package com.pousheng.auth.service;

import com.pousheng.auth.model.OperatorExt;

import io.terminus.common.model.Response;

public interface OperatorExtReadService {

	Response<OperatorExt> findByUserId(Long userId);

	Response<OperatorExt> findByUserName(String name);

}
