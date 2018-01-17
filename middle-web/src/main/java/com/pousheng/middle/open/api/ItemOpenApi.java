package com.pousheng.middle.open.api;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.item.PsItemTool;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.open.api.dto.SkuIsMposDto;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2018/1/16
 */
@OpenBean
@Slf4j
public class ItemOpenApi {

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    /**
     * 判断多个sku是否参与mpos
     * @param barcodes 商品编码，多个用逗号隔开 ,约定单笔最大500个
     * @return 各个商品是否参与mpos
     */
    @OpenMethod(key = "check.sku.is.mpos.api", paramNames = {"barcodes"}, httpMethods = RequestMethod.POST)
    public List<SkuIsMposDto> helloWord(@NotEmpty(message = "barcodes.empty") String barcodes) {
        log.info("HK-CHECK-MPOS-START param barcodes is:{} ", barcodes);
        List<String> barcodeList = Splitters.COMMA.splitToList(barcodes);

        //以查询的sku一定是中台已经有的前提
        Response<List<SkuTemplate>> skuTemplatesRes = skuTemplateReadService.findBySkuCodes(barcodeList);
        if(!skuTemplatesRes.isSuccess()){
            log.error("find sku template by barcodes:{} fail,error:{}",barcodeList,skuTemplatesRes.getError());
            throw new OPServerException(skuTemplatesRes.getError());
        }
        List<SkuTemplate> skuTemplates = skuTemplatesRes.getResult();

        if(!Objects.equal(skuTemplates.size(),barcodeList.size())){
            log.error("some barcode:{} middle not exist",barcodeList);
            throw new OPServerException("some.barcode.middle.not.exist");
        }

        List<SkuIsMposDto> skuIsMposDtos = Lists.newArrayListWithCapacity(barcodeList.size());
        for (SkuTemplate skuTemplate : skuTemplates){
            SkuIsMposDto skuIsMposDto = new SkuIsMposDto();
            skuIsMposDto.setBarcode(skuTemplate.getSkuCode());
            skuIsMposDto.setIsMpos(PsItemTool.isMopsItem(skuTemplate));
            skuIsMposDtos.add(skuIsMposDto);
        }
        log.info("HK-CHECK-MPOS-END");
        return skuIsMposDtos;
    }

}
