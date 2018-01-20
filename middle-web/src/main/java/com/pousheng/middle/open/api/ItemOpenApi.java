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
import java.util.stream.Collectors;

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
     * @param barCodes 商品编码，多个用逗号隔开 ,约定单笔最大500个
     * @return 各个商品是否参与mpos
     */
    @OpenMethod(key = "check.sku.is.mpos.api", paramNames = {"barCodes"}, httpMethods = RequestMethod.POST)
    public List<SkuIsMposDto> helloWord(@NotEmpty(message = "barCodes.empty") String barCodes) {
        log.info("HK-CHECK-MPOS-START param barcodes is:{} ", barCodes);
        try{
            List<String> barcodeList = Splitters.COMMA.splitToList(barCodes);

            //以查询的sku一定是中台已经有的前提
            Response<List<SkuTemplate>> skuTemplatesRes = skuTemplateReadService.findBySkuCodes(barcodeList);
            if(!skuTemplatesRes.isSuccess()){
                log.error("find sku template by barcodes:{} fail,error:{}",barcodeList,skuTemplatesRes.getError());
                throw new OPServerException(skuTemplatesRes.getError());
            }
            List<SkuTemplate> skuTemplates = skuTemplatesRes.getResult().stream().filter(skuTemplate -> !Objects.equal(skuTemplate.getStatus(),-3)).collect(Collectors.toList());

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
        }catch (Exception e){
            log.error("create open  order failed,caused by {}", e.getCause());
            throw new OPServerException(200, e.getMessage());
        }
    }

}
