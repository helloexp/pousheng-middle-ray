package com.pousheng.middle.open.api;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.terminus.common.utils.JsonMapper;
import org.junit.Test;
import vipapis.marketplace.invoice.ConfirmInvoiceRequest;
import vipapis.order.Einvoice;
import vipapis.order.OrderInvoiceReq;
import vipapis.order.PaperInvoice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-19
 */
public class VipOxoInvoiceApiTest {

    @Test
    public void onStockChanged() throws Exception {
        for (int k = 0;k < 1;k++){
            Map<String, String> params = Maps.newTreeMap();
            params.put("appKey","pousheng");
            params.put("pampasCall","vip.oxo.invoice.api");
            OrderInvoiceReq data = new OrderInvoiceReq();
            data.setOrder_id("18113011667643");
            data.setInvoice_type(3);
            List<Einvoice> eList= new ArrayList<Einvoice>();
            Einvoice einvoice = new Einvoice();
            einvoice.setE_invoice_url("11url");
            einvoice.setE_invoice_code("2222code");
            einvoice.setE_invoice_serial_no("3333serialno");
            einvoice.setVendor_tax_pay_no("4444payno");
            einvoice.setIs_writeback("0");
            eList.add(einvoice);
            data.setE_invoice(eList);
            PaperInvoice p=new PaperInvoice();
            p.setAddress("xiangxidizhi");
            long timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));//秒
            p.setInvoice_delivery_type(new Byte("2"));
            p.setEstimate_delivery_time(timestamp);
            p.setPackage_no("1111");
            p.setProvince("江苏");
            p.setCity("南京");
            p.setRegion("雨花台");
            p.setTown("铁心桥");
            p.setAddress("定坊");
            p.setContacts("chen");
            p.setMobile("18796220234");
            p.setTel("83447813");
            data.setPaper_invoice(p);
            params.put("order_invoice", JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(data));
            params.put("sign", sign(params, "6a0e@93204aefe45d47f6e488"));

            //HttpRequest r = HttpRequest.post("http://middle-api-prepub.pousheng.com/api/gateway", params, true);
            //HttpRequest r = HttpRequest.post("http://devt-api-middle.pousheng.com/api/gateway", params, true);
            HttpRequest r = HttpRequest.post("http://localhost:8095/api/gateway", params, true);
            System.out.println(r.body());
        }
    }

    /**
     * 对参数列表进行签名
     */
    private  String sign(Map<String, String> params, String secret) {
        try {
            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
            System.out.println(toVerify);

            String sign = Hashing.md5().newHasher()
                    .putString(toVerify, Charsets.UTF_8)
                    .putString(secret, Charsets.UTF_8).hash().toString();
            System.out.println(sign);

            return sign;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getData(String dir){
        Path path = Paths.get(dir);
        try {
            List<String> inputs = Files.readAllLines(path, Charsets.UTF_8);
            return inputs.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}