package com.pousheng.middle.shop.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
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

import java.util.List;
import java.util.Map;

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
    public Response<Paging<Shop>> pagination(String name, Long userId, Integer type, Integer status, String outerId, Long businessId, List<String> zoneIds, Integer pageNo, Integer pageSize) {
        try {
            Map<String, Object> criteria = Maps.newHashMap();
            criteria.put("name",Params.trimToNull(name));
            criteria.put("userId",userId);
            criteria.put("type",type);
            criteria.put("status",status);
            criteria.put("outerId",outerId);
            criteria.put("businessId",businessId);
            criteria.put("zoneIds",zoneIds);

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
            return Response.ok(Optional.fromNullable(shopExtDao.findByOuterIdAndBusinessId(outerId, businessId)));
        } catch (Exception e){
            log.error("find shop by outer id:{} business id:{},cause:{}",outerId,businessId,Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.fail");
        }
    }

    @Override
    public Response<Shop> findShopById(Long id) {
        try {
            return Response.ok(shopExtDao.findShopById(id));
        } catch (Exception e){
            log.error("find shop by id:{},cause:{}",id);
            return Response.fail("shop.find.fail");
        }
    }

    @Override
    public Response<List<Shop>> findAllShopsOn() {
        try {
            return Response.ok(shopExtDao.findAllShopsOn());
        }catch (Exception e){
            log.error("find all shops ,cause:{}",Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.fail");
        }
    }

    @Override
    public Response<Shop> findByCompanyCodeAndOutId(String username) {
        try {
            return Response.ok(shopExtDao.findByUsername(username));
        } catch (Exception e) {
            log.error("find shop faild ,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<List<Shop>> findByOuterIds(List<String> outerIds) {
        try {
            return Response.ok(shopExtDao.findByOuterIds(outerIds));
        } catch (Exception e) {
            log.error("[findByOuterIds]  outerIds:({}), cause:({})", outerIds, e);
            return Response.fail(e.getMessage());
        }
    }

    public Response<Shop> findShopByUserName(String userName) {
        try {
            return Response.ok(shopExtDao.findShopByUserName(userName));
        } catch (Exception e) {
            log.error("find shop by userName:{},cause:{}", userName);
            return Response.fail("shop.find.fail");
        }
    }
}
