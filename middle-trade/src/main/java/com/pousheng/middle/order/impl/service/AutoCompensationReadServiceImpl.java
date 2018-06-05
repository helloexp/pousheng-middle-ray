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
public class AutoCompensationReadServiceImpl implements AutoCompensationReadService {

    @Autowired
    private AutoCompensationDao autoCompensationDao;

    @Override
    public Response<Paging<AutoCompensation>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> param) {
        try {
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<AutoCompensation> p = autoCompensationDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), param);
            return Response.ok(p);
        } catch (Exception e) {
            log.error("failed to pagination autoCompensation with params:{}, cause:{}",
                    param, Throwables.getStackTraceAsString(e));
            return Response.fail("autoCompensation.find.fail");
        }
    }

    @Override
    public Response<List<AutoCompensation>> findByIds(List<Long> ids) {
        try {
            return Response.ok(autoCompensationDao.findByIds(ids));
        } catch (Exception e) {
            log.error("fail to find task by ids");
            return Response.fail("find.async.task.by.id.fail");
        }
    }
}
