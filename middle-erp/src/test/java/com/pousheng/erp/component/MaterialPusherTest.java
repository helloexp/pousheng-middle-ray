package com.pousheng.erp.component;

import com.google.common.collect.Lists;
import io.terminus.common.utils.JsonMapper;
import org.junit.Test;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-24
 */
public class MaterialPusherTest {

    private ErpClient erpClient = new ErpClient("http://esbt.pousheng.com","b82d30f3f1fc4e43b3f427ba3d7b9a50");
    @Test
    public void addSpus() throws Exception {
        String r = erpClient.postJson("common/erp/base/creatematerialmapper",
                JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(new MaterialPusher.MaterialIds(Lists.newArrayList("1","2"))));
        System.out.println(r);
        r = erpClient.postJson("common/erp/base/removematerialmapper",
                JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(new MaterialPusher.MaterialIds(Lists.newArrayList("1","2"))));
        System.out.println(r);
    }

}