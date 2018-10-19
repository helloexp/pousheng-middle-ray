package com.pousheng.middle;

import com.google.common.base.Objects;
import com.pousheng.middle.gd.MapSearchResponse;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.web.export.OrderExportEntity;
import com.pousheng.middle.web.utils.export.ExportContext;
import com.pousheng.middle.web.utils.export.ExportTitleContext;
import com.pousheng.middle.web.utils.export.ExportUtil;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.utils.AESEncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
@Slf4j
public class ExportUtilTest {

    @Test
    public void test() {

        ExportContext context = new ExportContext(Collections.singletonList(new OrderExportEntity()));
        context.addTitle(new ExportTitleContext("店铺编号"));
        ExportUtil.export(context);

    }




    @Test
    public void test2() {

        String json ="{\"status\":\"1\",\"count\":\"60\",\"info\":\"OK\",\"infocode\":\"10000\",\"suggestion\":{\"keywords\":[],\"cities\":[]},\"pois\":[{\"id\":[],\"name\":\"江苏省苏州市昆山市周市镇新浦花园12号\",\"type\":[],\"typecode\":[],\"biz_type\":[],\"address\":\"周市镇新浦花园12号\",\"location\":\"120.996336,31.400310\",\"tel\":[],\"postcode\":[],\"website\":[],\"email\":[],\"pcode\":[],\"pname\":\"江苏省\",\"citycode\":[],\"cityname\":\"苏州市\",\"adcode\":\"320583\",\"adname\":[],\"importance\":[],\"shopid\":[],\"poiweight\":[],\"gridcode\":[],\"distance\":[],\"navi_poiid\":[],\"entr_location\":[],\"business_area\":[],\"exit_location\":[],\"match\":[],\"recommend\":[],\"timestamp\":[],\"alias\":[],\"indoor_map\":\"0\",\"indoor_src\":[],\"groupbuy_num\":\"0\",\"discount_num\":\"0\",\"event\":[],\"children\":[],\"photos\":[]},{\"id\":\"B020015YWV\",\"parent\":[],\"name\":\"新浦花园\",\"tag\":[],\"type\":\"商务住宅;住宅区;住宅小区\",\"typecode\":\"120302\",\"biz_type\":[],\"address\":\"周市镇青阳北路与春辉路口向东20米\",\"location\":\"120.997027,31.400844\",\"tel\":[],\"postcode\":[],\"website\":[],\"email\":[],\"pcode\":\"320000\",\"pname\":\"江苏省\",\"citycode\":\"0512\",\"cityname\":\"苏州市\",\"adcode\":\"320583\",\"adname\":\"昆山市\",\"importance\":[],\"shopid\":[],\"shopinfo\":\"0\",\"poiweight\":[],\"gridcode\":\"4720078902\",\"distance\":[],\"navi_poiid\":\"H51F008008_15368\",\"entr_location\":\"120.997303,31.399691\",\"business_area\":[],\"exit_location\":[],\"match\":\"0\",\"recommend\":\"3\",\"timestamp\":[],\"alias\":[],\"indoor_map\":\"0\",\"indoor_data\":{\"cpid\":[],\"floor\":[],\"truefloor\":[],\"cmsid\":[]},\"groupbuy_num\":\"0\",\"discount_num\":\"0\",\"biz_ext\":{\"rating\":[],\"cost\":\"12391.00\"},\"event\":[],\"children\":[],\"photos\":[{\"title\":[],\"url\":\"http://store.is.autonavi.com/showpic/971d043511ae6c7b119932b5213d2a58\"}]}]}";

        MapSearchResponse response = JsonMapper.nonEmptyMapper().fromJson(json, MapSearchResponse.class);
        if(Arguments.isNull(response)){
            log.error("amap response poi is empty, resp = {}", json);
        }
        if (Objects.equal(response.getStatus(), MapSearchResponse.Status.FAILED.value())) {
            log.error("amap response poi is empty, resp = {}", json);
        }


    }



    @Test
    public void test3() throws Exception {

            String encrypted="/2iolKPHf6GwgH9F5QGq8auAEK32gIViIN7QqgGQB2LhVBfl6GHol+miThOwuoT0uuABZ/w1bbh3\n" +
                    "lQkuElHoVlHpFI15Z+FQz7apN9vIMOdrCgiUhMJDjt80As0HWjAch3vOAVkNVYHSKNT5I8oVi+DQ\n" +
                    "nnYo69sdYALM9eUqMUctPboVfj58x4sUhXwgTLOkYXgYQN9kPn+tiiZhIXNNyueCx1SPPU3CoS/S\n" +
                    "67fxtiUbwjKSGetmtQaBkmJUpsuuuY7vr2/wAud9JEGjne+IQ3Ypy1oxpuofos5RYkAEqdjPnVPs\n" +
                    "dgiNCTPl7J9677p5stzJEbGvUFuQTu6ZwLSFKQ==";
            String decrypted= AESEncryptUtil.aesDecrypt(encrypted, AESEncryptUtil.RECEIVER_INFO_ENCRYPT_KEY);
            System.out.println("解密结果:");
            System.out.println(decrypted);

    }




    @Test
    public void test4() throws Exception {

        String str="{\"mobile\":\"15027815507\",\"province\":\"北京\",\"provinceId\":110000,\"city\":\"北京市\",\"cityId\":110100,\"region\":\"东城区\",\"regionId\":110101,\"detail\":\"北京东城区内环到三环里东直门北小街11号张记焖窰\",\"extra\":{\"handleResult\":\"{\\\"success\\\":true}\"}}";
        String encrypted= AESEncryptUtil.aesEncrypt(str, AESEncryptUtil.RECEIVER_INFO_ENCRYPT_KEY);
        System.out.println("加密结果:");
        System.out.println(encrypted);

    }




    @Test
    public void orderExportTest() {

        List<OrderExportEntity> entities = new ArrayList<>();

        OrderExportEntity orderExport = new OrderExportEntity();
        orderExport.setOrderCode(String.valueOf(System.currentTimeMillis()));
        orderExport.setShopName("张三的店");
        entities.add(orderExport);

        OrderExportEntity orderExport2 = new OrderExportEntity();
        orderExport2.setOrderCode(String.valueOf(System.currentTimeMillis()));
        orderExport2.setShopName("里斯的店");
        entities.add(orderExport2);

        OrderExportEntity orderExport3 = new OrderExportEntity();
        orderExport3.setOrderCode(String.valueOf(System.currentTimeMillis()));
        orderExport3.setShopName("王武的店");
        entities.add(orderExport3);

        ExportContext context = new ExportContext(entities);
        context.setResultType(ExportContext.ResultType.FILE);
        ExportUtil.export(context);
    }


    @Test
    public void orderTest() {
        OrderExportEntity orderExport = new OrderExportEntity();
        orderExport.setOrderCode("3434888387674");
        orderExport.setShopName("shangzhaer");
        orderExport.setPaymentDate(new Date());
        orderExport.setOrderStatus(MiddleOrderStatus.CONFIRMED.getName());
        orderExport.setFee(448.9);

        ExportContext context = new ExportContext(Collections.singletonList(orderExport));
        context.setResultType(ExportContext.ResultType.FILE);
        ExportUtil.export(context);
    }
}
