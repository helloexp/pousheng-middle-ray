package com.pousheng.middle.open.api.skx;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.pousheng.erp.cache.ErpSpuCacher;
import com.pousheng.middle.open.api.skx.dto.OnSaleItem;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.dto.ItemMappingCriteria;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.Setter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Description: 测试Skx开放平台
 * Author: xiao
 * Date: 2018/05/31
 */
@Setter
public class SkxItemOpenApiTest extends AbstractOpenTest {

    @Configuration
    public static class MockitoBeans {
        @MockBean
        private MappingReadService mappingReadService;
        @MockBean
        private SkuTemplateReadService skuTemplateReadService;
        @MockBean
        private ErpSpuCacher erpSpuCacher;

        @SpyBean
        SkxItemOpenApi api;
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();



    MappingReadService mappingReadService;
    SkuTemplateReadService skuTemplateReadService;
    ErpSpuCacher erpSpuCacher;
    SkxItemOpenApi api;


    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }


    @Override
    protected void init() {
        mappingReadService = get(MappingReadService.class);
        skuTemplateReadService = get(SkuTemplateReadService.class);
        erpSpuCacher = get(ErpSpuCacher.class);
        api = get(SkxItemOpenApi.class);

        api.setMappingReadService(mappingReadService);
        api.setSkuTemplateReadService(skuTemplateReadService);


        Spu spu1 = new Spu();
        spu1.setId(1L);
        spu1.setName("Name1");
        when(erpSpuCacher.findById(1L)).thenReturn(spu1);

        Spu spu2 = new Spu();
        spu2.setId(2L);
        spu2.setName("Name2");
        when(erpSpuCacher.findById(2L)).thenReturn(spu2);


        ItemMapping mapping1 = new ItemMapping();
        mapping1.setItemId(1L);
        mapping1.setItemName("Name1");
        mapping1.setSkuCode("code1");

        ItemMapping mapping2 = new ItemMapping();
        mapping2.setItemId(2L);
        mapping2.setItemName("Name2");
        mapping2.setSkuCode("code2");


        when(mappingReadService.paging(any(ItemMappingCriteria.class))).thenReturn(Response.ok(
            new Paging<>(2L, Lists.newArrayList(
                    mapping1, mapping2
            ))
        ));

        SkuAttribute skuAttribute = new SkuAttribute();
        skuAttribute.setAttrKey("尺码");
        skuAttribute.setAttrVal("13#");


        SkuTemplate skuTemplate1 = new SkuTemplate();
        skuTemplate1.setId(1L);
        skuTemplate1.setSkuCode("code1");
        skuTemplate1.setExtra(ImmutableMap.of("materialId", "1"));
        skuTemplate1.setAttrs(Lists.newArrayList(
                skuAttribute
        ));

        SkuTemplate skuTemplate2 = new SkuTemplate();
        skuTemplate2.setId(1L);
        skuTemplate2.setSkuCode("code2");
        skuTemplate2.setExtra(ImmutableMap.of("materialId", "2"));
        skuTemplate2.setAttrs(Lists.newArrayList(
                skuAttribute
        ));


        when(skuTemplateReadService.findBy(0, 1, ImmutableMap.of("status", 1, "skuCode", mapping1.getSkuCode())))
                .thenReturn(Response.ok(new Paging<>(1L, Lists.newArrayList(skuTemplate1))));


        when(skuTemplateReadService.findBy(0, 1, ImmutableMap.of("status", 1, "skuCode", mapping2.getSkuCode())))
                .thenReturn(Response.ok(new Paging<>(1L, Lists.newArrayList(skuTemplate2))));
    }



    @Test
    public void testPagingWithPageNo() {
        Paging<OnSaleItem> paging = api.getOnSaleItem(0, 2,
                "19000101000000", "21000101000000");
        assertThat(paging, notNullValue());
        assertThat(paging.getData(), notNullValue());
        assertThat(paging.getTotal(), is(2L));
        assertThat(paging.getData().get(0).getItemId(), is(1L));
        assertThat(paging.getData().get(0).getItemName(), is("Name1"));
        assertThat(paging.getData().get(0).getSkus().size(), is(1));
        assertThat(paging.getData().get(0).getSkus().get(0).getBarCode(), is("code1"));
        assertThat(paging.getData().get(0).getSkus().get(0).getMaterialId(), is("1"));
        assertThat(paging.getData().get(0).getSkus().get(0).getSkuId(), is(1L));
        assertThat(paging.getData().get(0).getSkus().get(0).getSize(), is("13#"));
    }


    @Test
    public void testFindWithCheckPointEmpty() {
        thrown.expect(OPServerException.class);
        thrown.expectMessage("SkxItemOpenApi.failToQueryMapping");
        when(mappingReadService.paging(any())).thenReturn(Response.fail("xxx"));
        api.getOnSaleItem(0, 2,
                System.currentTimeMillis() - 1000 + "", System.currentTimeMillis() + "");
    }

    @Test
    public void testFindWithWrongDateInput() {
        thrown.expect(OPServerException.class);
        thrown.expectMessage("SkxItemOpenApi.failToConvertStartDate");
        api.getOnSaleItem(0, 2,
                "xxx", System.currentTimeMillis() + "");
    }

    @Test
    public void testFindWithWrongEndDateInput() {
        thrown.expect(OPServerException.class);
        thrown.expectMessage("SkxItemOpenApi.failToConvertEndDate");
        api.getOnSaleItem(0, 2,
                System.currentTimeMillis() - 1000 + "", "xxx");
    }

    @Test
    public void testFindWithNpe() {
        thrown.expect(OPServerException.class);
        thrown.expectMessage("SkxItemOpenApi.failToQueryOnSaleItem");
        when(mappingReadService.paging(any())).thenThrow(new NullPointerException());
        api.getOnSaleItem(0, 2,
                System.currentTimeMillis() - 1000 + "", System.currentTimeMillis() + "");
    }

    @Test
    public void testFindWithSkuNotFound() {
        thrown.expect(OPServerException.class);
        thrown.expectMessage("SkxItemOpenApi.notFoundSku");
        when(skuTemplateReadService.findBy(any(), any(), any())).thenReturn(Response.fail("xxxx"));
        api.getOnSaleItem(0, 2,
                System.currentTimeMillis() - 1000 + "", System.currentTimeMillis() + "");
    }

    @Test
    public void testFindWithSkuNotFound2() {
        thrown.expect(OPServerException.class);
        thrown.expectMessage("SkxItemOpenApi.notFoundSku");
        when(skuTemplateReadService.findBy(any(), any(), any()))
                .thenReturn(Response.ok(new Paging<>(0L, Lists.newArrayList())));
        api.getOnSaleItem(0, 2,
                System.currentTimeMillis() - 1000 + "", System.currentTimeMillis() + "");
    }


    @Test
    public void testFindWithWrongMaterialId() {
        thrown.expect(OPServerException.class);
        thrown.expectMessage("SkxItemOpenApi.withoutMaterialIdOrEmpty");

        SkuTemplate skuTemplate1 = new SkuTemplate();
        skuTemplate1.setId(1L);
        skuTemplate1.setSkuCode("code1");

        when(skuTemplateReadService.findBy(0, 1, ImmutableMap.of("status", 1, "skuCode", "code1")))
                .thenReturn(Response.ok(new Paging<>(1L, Lists.newArrayList(skuTemplate1))));

        api.getOnSaleItem(0, 2,
                System.currentTimeMillis() - 1000 + "", System.currentTimeMillis() + "");
    }

    @Test
    public void testFindWithWrongAttrs() {
        thrown.expect(OPServerException.class);
        thrown.expectMessage("SkxItemOpenApi.withIncorrectAttrs");

        SkuTemplate skuTemplate1 = new SkuTemplate();
        skuTemplate1.setId(1L);
        skuTemplate1.setSkuCode("code1");
        skuTemplate1.setExtra(ImmutableMap.of("materialId", "1"));

        when(skuTemplateReadService.findBy(0, 1, ImmutableMap.of("status", 1, "skuCode", "code1")))
                .thenReturn(Response.ok(new Paging<>(1L, Lists.newArrayList(skuTemplate1))));

        api.getOnSaleItem(0, 2,
                System.currentTimeMillis() - 1000 + "", System.currentTimeMillis() + "");
    }

    @Test
    public void testFindWithWrongSize() {
        thrown.expect(OPServerException.class);
        thrown.expectMessage("SkxItemOpenApi.withIncorrectSize");

        SkuTemplate skuTemplate1 = new SkuTemplate();
        skuTemplate1.setId(1L);
        skuTemplate1.setSkuCode("code1");
        skuTemplate1.setExtra(ImmutableMap.of("materialId", "1"));
        SkuAttribute skuAttribute = new SkuAttribute();
        skuAttribute.setAttrKey("颜色");
        skuAttribute.setAttrVal("13#");
        skuTemplate1.setAttrs(Lists.newArrayList(skuAttribute));

        when(skuTemplateReadService.findBy(0, 1, ImmutableMap.of("status", 1, "skuCode", "code1")))
                .thenReturn(Response.ok(new Paging<>(1L, Lists.newArrayList(skuTemplate1))));

        api.getOnSaleItem(0, 2,
                System.currentTimeMillis() - 1000 + "", System.currentTimeMillis() + "");
    }



}
