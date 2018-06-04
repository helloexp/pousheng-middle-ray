package com.pousheng.middle.open.api;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.pousheng.middle.item.PsItemTool;
import com.pousheng.middle.open.api.dto.SkuIsMposDto;
import com.pousheng.middle.shop.service.PsShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2018/1/16
 */
@OpenBean
@Slf4j
public class ItemOpenApi {

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private PsShopReadService psShopReadService;

    /**
     * 判断多个sku是否参与mpos
     * @param barCodes 商品编码，多个用逗号隔开 ,约定单笔最大500个
     * @return 各个商品是否参与mpos
     */
    @OpenMethod(key = "check.sku.is.mpos.api", paramNames = {"barCodes","companyId","shopOuterCode"}, httpMethods = RequestMethod.POST)
    public List<SkuIsMposDto> helloWord(@NotEmpty(message = "barCodes.empty") String barCodes,
                                        @NotEmpty(message = "company.id.empty") String companyId,
                                        @NotEmpty(message = "shop.outer.code.empty") String shopOuterCode) {
        log.info("HK-CHECK-MPOS-START param barcodes is:{} companyId is:{} shopOuterCode is:{} ", barCodes,companyId,shopOuterCode);
        try{

            boolean isNumber=companyId.matches("[0-9]+");
            if(!isNumber){
                log.error("company id:{} not number",companyId);
                throw new OPServerException(200,"company.id.non-numeric");
            }

            Response<Optional<Shop>> response = psShopReadService.findByOuterIdAndBusinessId(shopOuterCode,Long.valueOf(companyId));
            if(!response.isSuccess()){
                log.error("find shop by outer id:{} business id:{} fail,error:{}",shopOuterCode,companyId,response.getError());
                throw new OPServerException(200,response.getError());
            }
            Optional<Shop> shopOptional = response.getResult();
            if(!shopOptional.isPresent()){
                log.error("not find shop by outer id:{} business id:{}",shopOuterCode,companyId);
                throw new OPServerException(200,"shop.not.exist");
            }

            List<String> barcodeList = Splitters.COMMA.splitToList(barCodes);

            //以查询的sku一定是中台已经有的前提
            Response<List<SkuTemplate>> skuTemplatesRes = skuTemplateReadService.findBySkuCodes(barcodeList);
            if(!skuTemplatesRes.isSuccess()){
                log.error("find sku template by barcodes:{} fail,error:{}",barcodeList,skuTemplatesRes.getError());
                throw new OPServerException(200,skuTemplatesRes.getError());
            }
            List<SkuTemplate> skuTemplates = skuTemplatesRes.getResult().stream().filter(skuTemplate -> !Objects.equal(skuTemplate.getStatus(),-3)).collect(Collectors.toList());

            if(!Objects.equal(skuTemplates.size(),barcodeList.size())){
                log.error("some barcode:{} middle not exist",barcodeList);
                throw new OPServerException(200,"some.barcode.middle.not.exist");
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
            log.error("find mpos sku codes failed,caused by {}", e.getCause());
            throw new OPServerException(200, e.getMessage());
        }
    }

}
