package com.pousheng.middle.hksyc.pos.api;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class SycHkShipmentPosApiTest {


    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void doSyncShipmentPos() {
        String paramJson="{\"sid\":\"PS_ERP_POS_netsalshop\",\"tranReqDate\":\"2018-11-26 13:44:25\","
            + "\"bizContent\":{\"channeltype\":\"b2c\",\"companyid\":\"240\",\"shopcode\":\"SP310449\","
            + "\"voidstockcode\":\"POS预收\",\"netcompanyid\":\"244\",\"netshopcode\":\"SP321099\","
            + "\"netstockcode\":\"POS预收\",\"netbillno\":\"SHP103481\",\"billdate\":\"2018-11-23 23:30:02\","
            + "\"operator\":\"MPOS_EDI\",\"islock\":\"0\",\"netsalorder\":{\"manualbillno\":\"wuwen-3367839+121\","
            + "\"paymentdate\":\"2018-11-26 11:31:10\",\"buyercode\":\"wuwen\",\"buyermobiletel\":\"18021529596\","
            + "\"consigneename\":\"悟问\",\"payamountbakup\":\"0\",\"expresscost\":\"10.00\",\"codcharges\":\"0\","
            + "\"isinvoice\":\"0\",\"province\":\"江苏省\",\"city\":\"南京市\",\"area\":\"江宁区\",\"address\":\"胜利路89号\","
            + "\"sellcode\":\"wuwen\",\"expresstype\":\"express\",\"consignmentdate\":\"2018-11-26 13:44:25\","
            + "\"weight\":\"0.0\",\"parcelweight\":\"0.0\"}}}";

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String gateway ="https://esbt.pousheng.com/common/erp/pos/addnetsalshop";
        String responseBody = HttpRequest.post(gateway)
            .header("verifycode","b82d30f3f1fc4e43b3f427ba3d7b9a50")
            .header("serialNo",serialNo)
            .header("sendTime", DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
            .contentType("application/json")
            .trustAllHosts().trustAllCerts()
            .send(paramJson)
            .connectTimeout(10000).readTimeout(10000)
            .body();

        log.info("doRequest for paramJson:{}\n,result:{}",paramJson,responseBody);

    }

    @Test
    public void doSyncRefundPos() {
    }

    @Test
    public void doSyncSaleRefuse() {
    }

    @Test
    public void doSyncShipmentDone() {
    }
}