package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Maps;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.impl.service.PoushengCompensateBizWriteServiceImpl;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.mappings.service.MappingWriteService;
import io.terminus.open.client.mapping.impl.service.MappingReadServiceImpl;
import io.terminus.open.client.mapping.impl.service.MappingWriteServiceImpl;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Description:
 * User: support 9
 * Date: 2018/9/3
 */
public class ImportItemPushRatioTest extends AbstractRestApiTest {

    private ImportItemPushRatioService importItemPushRatioService;

    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    private MappingReadService mappingReadService;

    private MappingWriteService mappingWriteService;

    private UploadFileComponent uploadFileComponent;

    @Configuration
    private static class MockitoBeans {
        @SpyBean
        private ImportItemPushRatioService importItemPushRatioService;
        @MockBean
        private PoushengCompensateBizWriteServiceImpl poushengCompensateBizWriteService;
        @MockBean
        private MappingReadServiceImpl mappingReadService;
        @MockBean
        private MappingWriteServiceImpl mappingWriteService;
        @MockBean
        private UploadFileComponent uploadFileComponent;
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    public void init() {
        importItemPushRatioService = get(ImportItemPushRatioService.class);
        poushengCompensateBizWriteService = get(PoushengCompensateBizWriteServiceImpl.class);
        mappingReadService = get(MappingReadServiceImpl.class);
        mappingWriteService = get(MappingWriteServiceImpl.class);
        uploadFileComponent = get(UploadFileComponent.class);
    }

    public void testProcess() {
        importItemPushRatioService.doProcess(makePoushengCompensateBiz());
    }

    private PoushengCompensateBiz makePoushengCompensateBiz() {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        Map<String, Object> context = Maps.newHashMap();
        context.put("import", "http://test.excel");
        return biz;
    }
}
