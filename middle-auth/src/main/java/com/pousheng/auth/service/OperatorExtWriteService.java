package com.pousheng.auth.service;

import com.pousheng.auth.model.OperatorExt;

import io.terminus.common.model.Response;

public interface OperatorExtWriteService {

	Response<Long> create(OperatorExt operator);

	Response<Boolean> update(OperatorExt operator);
	
}
