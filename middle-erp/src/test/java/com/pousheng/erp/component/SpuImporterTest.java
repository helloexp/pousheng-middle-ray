package com.pousheng.erp.component;

import com.pousheng.erp.model.PoushengMaterial;
import com.pousheng.erp.model.PoushengSku;
import io.terminus.parana.brand.model.Brand;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-31
 */
public class SpuImporterTest extends BaseServiceTest{

    @Autowired
    private SpuImporter spuImporter;

    @Test
    public void doProcess() throws Exception {

        PoushengMaterial m = new PoushengMaterial();
        m.setMaterialCode("materialCode");
        m.setMaterialName("测试商品");
        m.setForeignName("test item");
        m.setCardID("cardId");
        m.setKindID("kindId");
        m.setSeriesID("seriesId");
        m.setItemID("itemId");
        m.setModelID("modelId");
        m.setStuff("stuff");
        m.setTexture("texture");
        m.setSex("男");
        m.setYearNo(2017);
        m.setInvSpec("invSpec");
        m.setSeason5(true);
        m.setSeason6(true);

        PoushengSku ps1 = new PoushengSku();
        ps1.setBarCode("barCode1");
        ps1.setColorId("colorId1");
        ps1.setColorId("color1");
        ps1.setSizeId("sizeId1");
        ps1.setSizeName("39");
        ps1.setMarketPrice("399");

        PoushengSku ps2 = new PoushengSku();
        ps2.setBarCode("barCode2");
        ps2.setColorId("colorId2");
        ps2.setColorId("color2");
        ps2.setSizeId("sizeId2");
        ps2.setSizeName("40");
        ps2.setMarketPrice("399");

        m.setSkus(Lists.newArrayList(ps1,ps2));

        Brand brand = new Brand();
        brand.setName("brand");
        brand.setId(11L);
        Long spuId =  spuImporter.doProcess(m, brand);

        assertThat(spuId, notNullValue());

    }

}