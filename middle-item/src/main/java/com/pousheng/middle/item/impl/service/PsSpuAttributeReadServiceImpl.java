package com.pousheng.middle.item.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.item.service.PsSpuAttributeReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.impl.dao.SpuAttributeDao;
import io.terminus.parana.spu.model.SpuAttribute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by songrenfei on 2017/12/7
 */
@Slf4j
@Component
@RpcProvider
@SuppressWarnings("unused")
public class PsSpuAttributeReadServiceImpl implements PsSpuAttributeReadService{

    @Autowired
    private SpuAttributeDao spuAttributeDao;
    @Override
    public Response<List<SpuAttribute>> findBySpuIds(List<Long> spuIds) {
        try {
            return Response.ok(spuAttributeDao.findBySpuIds(spuIds));
        }catch (Exception e){
            log.error("find spu attribute by spu ids:{} fail,cause:{}",spuIds, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.attribute.find.fail");
        }
    }
}
