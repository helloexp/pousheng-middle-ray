package com.pousheng.middle.shop.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.shop.impl.dao.ShopExtDao;
import com.pousheng.middle.shop.service.PsShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Params;
import io.terminus.parana.shop.impl.dao.ShopDao;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by songrenfei on 2017/12/6
 */
@Slf4j
@Service
@RpcProvider
public class PsShopReadServiceImpl implements PsShopReadService{

    @Autowired
    private ShopDao shopDao;
    @Autowired
    private ShopExtDao shopExtDao;


    @Override
    public Response<Paging<Shop>> pagination(String name, Long userId, Integer type, Integer status,String outerId,Long businessId, Integer pageNo, Integer pageSize) {
        try {
            Shop criteria = new Shop();
            criteria.setName(Params.trimToNull(name));
            criteria.setUserId(userId);
            criteria.setType(type);
            criteria.setStatus(status);
            criteria.setOuterId(outerId);
            criteria.setBusinessId(businessId);
            criteria.setStatus(status);
            PageInfo page = new PageInfo(pageNo, pageSize);
            return Response.ok(shopDao.paging(page.getOffset(), page.getLimit(), criteria));
        } catch (Exception e) {
            log.error("paging shop failed, name={}, userId={}, type={}, status={},outerId={},business={}, pageNo={}, pageSize={}, cause:{}",
                    name, userId, type, status,outerId,businessId, pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.fail");
        }
    }

    @Override
    public Response<Optional<Shop>> findByOuterIdAndBusinessId(String outerId, Long businessId) {
        try {
            return Response.ok(Optional.fromNullable(shopExtDao.findByOuterIdAndBusinessId(outerId,businessId)));
        }catch (Exception e){
            log.error("find shop by outer id:{} business id:{},cause:{}",outerId,businessId,Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.fail");
        }
    }


}
