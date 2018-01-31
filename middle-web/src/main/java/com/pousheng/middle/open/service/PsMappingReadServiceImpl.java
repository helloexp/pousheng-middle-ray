package com.pousheng.middle.open.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.mapping.impl.dao.AttributeMappingDao;
import io.terminus.open.client.mapping.impl.dao.BrandMappingDao;
import io.terminus.open.client.mapping.impl.dao.CategoryMappingDao;
import io.terminus.open.client.mapping.impl.dao.ItemMappingDao;
import io.terminus.open.client.mapping.impl.service.MappingReadServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by songrenfei on 2018/1/18
 */
@Primary
@Service
@RpcProvider
@Slf4j
public class PsMappingReadServiceImpl extends MappingReadServiceImpl implements MappingReadService{

    private final BrandMappingDao brandMappingDao;

    private final CategoryMappingDao categoryMappingDao;

    private final AttributeMappingDao attributeMappingDao;

    private final ItemMappingDao itemMappingDao;

    @Value("${mpos.open.shop.id}")
    private Long mposOpenShopId;
    @Autowired
    private OpenShopCacher openShopCacher;




    @Autowired
    public PsMappingReadServiceImpl(BrandMappingDao brandMappingDao,
                                  CategoryMappingDao categoryMappingDao,
                                  AttributeMappingDao attributeMappingDao,
                                  ItemMappingDao itemMappingDao) {
        super(brandMappingDao, categoryMappingDao, attributeMappingDao, itemMappingDao);
        this.brandMappingDao = brandMappingDao;
        this.categoryMappingDao = categoryMappingDao;
        this.attributeMappingDao = attributeMappingDao;
        this.itemMappingDao = itemMappingDao;

    }


    @Override
    public Response<Optional<ItemMapping>> findBySkuCodeAndOpenShopId(String skuCode, Long openShopId) {
        try {
            //mpos的商品映射全部在总店上
            OpenShop openShop = openShopCacher.findById(openShopId);
            if(openShop.getShopName().startsWith("mpos")){
                openShopId = mposOpenShopId;
            }
            List<ItemMapping> itemMappings = itemMappingDao.findBySkuCodeAndOpenShopId(skuCode, openShopId);
            return Response.ok(Optional.fromNullable(itemMappings!=null&&!itemMappings.isEmpty()?itemMappings.get(0):null));
        } catch (Exception e) {
            log.error("fail to find item mapping by skuCode={},openShopId={},cause:{}",
                    skuCode, openShopId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.mapping.find.fail");
        }
    }

    @Override
    public Response<Optional<ItemMapping>> findByChannelSkuIdAndOpenShopId(String channelSkuId, Long openShopId) {
        try {
            //mpos的商品映射全部在总店上
            OpenShop openShop = openShopCacher.findById(openShopId);
            if(openShop.getShopName().startsWith("mpos")){
                openShopId = mposOpenShopId;
            }
            ItemMapping itemMapping = itemMappingDao.findByChannelSkuIdAndOpenShopId(channelSkuId, openShopId);
            return Response.ok(Optional.fromNullable(itemMapping));
        } catch (Exception e) {
            log.error("fail to find item mapping by channelSkuId={},openShopId={},cause:{}",
                    channelSkuId, openShopId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.mapping.find.fail");
        }
    }
}
