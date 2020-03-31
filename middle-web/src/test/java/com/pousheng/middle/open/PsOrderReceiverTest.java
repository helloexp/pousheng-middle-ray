package com.pousheng.middle.open;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PsOrderReceiverTest {

    @Test
    public void makeParanaOrder() {
        String str="{\"status\":\"PAID\",\"orderId\":\"291162560097857032\","
            + "\"buyerName\":\"~ryf1VBDTx+03pWGhumB8luQJX5U28wD5MyiSVy8gw6M=~1~\",\"originFee\":89700,\"fee\":37154,"
            + "\"discount\":52546,\"items\":[{\"orderId\":\"291162560098857032\",\"status\":\"PAID\","
            + "\"itemId\":\"576742376781\",\"itemName\":\"Levi's李维斯高帮帆布鞋男潮流韩版布鞋百搭板鞋潮鞋加绒冬季鞋子\","
            + "\"itemCode\":\"228385173059\",\"skuId\":\"3970802506536\",\"skuCode\":\"6914279453563\","
            + "\"price\":53900,\"quantity\":1,\"discount\":29993},{\"orderId\":\"291162560099857032\","
            + "\"status\":\"PAID\",\"itemId\":\"575707283162\",\"itemName\":\"Levi's李维斯鞋子潮流男学生休闲鞋男士板鞋英伦平底运动百搭潮鞋\","
            + "\"itemCode\":\"22800779451\",\"skuId\":\"3779248269422\",\"skuCode\":\"6902653965696\","
            + "\"price\":29900,\"quantity\":1,\"discount\":16653},{\"orderId\":\"291162560100857032\","
            + "\"status\":\"PAID\",\"itemId\":\"560056431257\",\"itemName\":\"2018潮袜 高帮长筒袜子\","
            + "\"itemCode\":\"LSFW18SO0001F\",\"price\":5900,\"quantity\":1,\"discount\":5900}],"
            + "\"consignee\":{\"name\":\"~h58YHJekFtDy4sor38uzHQ==~1~\","
            + "\"mobile\":\"$187$q44MDhecUvkOGZ7gxnO4tQ==$1$\",\"province\":\"辽宁省\",\"city\":\"大连市\","
            + "\"region\":\"旅顺口区\",\"detail\":\"龙*街道旅**路西**号大连外**大学 **楼\"},\"invoice\":{},"
            + "\"createdAt\":1544545069000,\"paymentInfo\":{\"paidAt\":1544545072000,"
            + "\"paySerialNo\":\"2018121222001183735425894257\"},\"extra\":{\"platformDiscount\":\"46\"}}";

        OpenClientShop openClientShop=new OpenClientShop();
        openClientShop.setChannel("taobao");
        openClientShop.setOpenShopId(1L);
        openClientShop.setShopName("测试店铺");
        OpenClientFullOrder openClientFullOrder= JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(str,OpenClientFullOrder.class);

        new PsOrderReceiver().makeParanaOrder(openClientShop,openClientFullOrder);
    }

    @Test
    public void calculatePlatformDiscountForSkus() {

        RichSkusByShop richSkusByShop=new RichSkusByShop();
        richSkusByShop.setOriginFee(89700L);
        richSkusByShop.setFee(37154L);
        richSkusByShop.setDiscount(52546);
        List<RichSku> richSkuList= Lists.newArrayList();

        RichSku a=new RichSku();
        a.setOutOrderId("291162560098857032");
        a.setOuterSkuId("6914279453563");
        a.setFee(53900L);
        a.setDiscount(29993L);
        a.setQuantity(1);
        a.setExtra(new HashMap<>());
        richSkuList.add(a);

        RichSku b=new RichSku();
        b.setOutOrderId("291162560098857032");
        b.setOuterSkuId("6902653965696");
        b.setFee(29900L);
        b.setDiscount(16653L);
        b.setQuantity(1);
        b.setExtra(new HashMap<>());
        richSkuList.add(b);

        RichSku c=new RichSku();
        c.setOutOrderId("291162560098857032");
        c.setOuterSkuId("LSFW18SO0001F");
        c.setFee(5900L);
        c.setDiscount(5900L);
        c.setQuantity(1);
        c.setExtra(new HashMap<>());
        richSkuList.add(c);

        richSkusByShop.setRichSkus(richSkuList);

        Map<String, String> extra= Maps.newHashMap();
        extra.put(TradeConstants.PLATFORM_DISCOUNT_FOR_SHOP,"56");
        richSkusByShop.setExtra(extra);
        new PsOrderReceiver().calculatePlatformDiscountForSkus(richSkusByShop);


        log.info("after calc platform discount:{}",JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(richSkusByShop));

    }
}