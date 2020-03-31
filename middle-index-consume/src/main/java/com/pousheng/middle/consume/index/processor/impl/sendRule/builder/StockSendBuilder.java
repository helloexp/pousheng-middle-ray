package com.pousheng.middle.consume.index.processor.impl.sendRule.builder;

import com.pousheng.inventory.domain.dto.PoushengWarehouseDTO;
import com.pousheng.middle.consume.index.processor.impl.CommonBuilder;
import com.pousheng.middle.consume.index.processor.impl.sendRule.dto.StockSendDocument;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-14 14:47<br/>
 */
@Slf4j
@Component
public class StockSendBuilder extends CommonBuilder {

    public StockSendDocument build(Long ruleId, Object id, Shop shop, OpenShop openShop, PoushengWarehouseDTO warehouse) {
        StockSendDocument document = new StockSendDocument();
        document.setId(id);
        document.setRuleId(ruleId);

        // parana open shops
     //   log.info("OpenShop:{}", openShop);
        document.setShopId(openShop.getId());
        String shopName = openShop.getShopName();
        document.setShopName(shopName);
        if (StringUtils.hasText(shopName)) {
            //线下
            if (shopName.startsWith("mpos")) {
                document.setShopType(1L);
            } else {
                //线上
                document.setShopType(2L);
            }
        }
        Map<String, String> extra = openShop.getExtra();
        if (! CollectionUtils.isEmpty(extra)) {
            document.setCompanyCode(extra.get("companyCode"));
            document.setShopOutCode(extra.get("hkPerformanceShopOutCode"));
            //先把zone信息补全，如果shop为空，就以此为准。因为只有parana shops是线下门店的时候才有这两个值
            String zoneIdStr = extra.get("zoneId");
            if (StringUtils.hasText(zoneIdStr) && (! "null".equals(zoneIdStr))) {
                document.setZoneId(Long.valueOf(extra.get("zoneId")));
            }
            document.setZoneName(extra.get("zoneName"));
        }
        // parana shops
        if (shop != null) {
//            if (document.getZoneId() == null) {
//                document.setZoneId(parseLong(shop.getZoneId()));
//            }
//            if (document.getZoneName() == null) {
//                document.setZoneName(shop.getZoneName());
//            }
            document.setWarehouseType(shop.getType());
            document.setWarehouseStatus(shop.getStatus());
        }
        // warehouse
        document.setWarehouseId(warehouse.getId());
        document.setWarehouseName(warehouse.getWarehouseName());
        document.setWarehouseOutCode(warehouse.getOutCode());
        document.setWarehouseCompanyCode(warehouse.getCompanyId());
    //    log.info("Document:{}", document);
        return document;
    }
}
