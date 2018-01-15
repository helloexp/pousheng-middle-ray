package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.AutoCompensationDao;
import com.pousheng.middle.order.model.AutoCompensation;
import com.pousheng.middle.order.service.AutoCompensationReadService;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by penghui on 2018/1/15
 */
@Service
@Slf4j
public class AutoCompensationReadServiceImpl implements AutoCompensationReadService{

    @Autowired
    private AutoCompensationDao autoCompensationDao;

    @Override
    public Response<List<AutoCompensation>> findAutoCompensationTask(Integer type, Integer status) {
        try {
            List<AutoCompensation> list = autoCompensationDao.findAutoCompensationTask(type, status);
            return Response.ok(list);
        } catch (Exception e) {
            log.error("failed to find autoCompensation task, type={},statu={}, cause:{}", type,status, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.expressCode.find.fail");
        }
    }

    @Override
    public Response<Paging<AutoCompensation>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> param) {
        try{
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<AutoCompensation> p = autoCompensationDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), param);
            return Response.ok(p);
        }catch (Exception e){
            log.error("failed to pagination warehouse with params:{}, cause:{}",
                    param, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.find.fail");
        }
    }
}
