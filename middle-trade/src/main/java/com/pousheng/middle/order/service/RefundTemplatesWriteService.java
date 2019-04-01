package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.Refundtemplates;
import io.terminus.common.model.Response;

import java.util.List;

public interface RefundTemplatesWriteService {

    /**
     * 批量创建导入的售后单模板
     * @param reftemplates
     * @return
     */
    Response<String> creates(List<Refundtemplates> reftemplates);

    /**
     * 根据id更新售后单创建状态
     * @param id
     * @return
     */
    Response<Boolean> updateApplyStatusByid(Long id);
}
