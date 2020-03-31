package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.common.utils.batchhandle.SettingSafeStockRecord;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.utils.VerifyUtil;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.InventoryDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.msg.common.StringUtil;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_SETTING_WAREHOUSE_SAFE_STOCK)
@Service
@Slf4j
public class ImportWarehouseSettingSafeStockService implements CompensateBizService {
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private UploadFileComponent uploadFileComponent;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private InventoryClient inventoryClient;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private SkuTemplateSearchReadService skuTemplateSearchReadService;
    
    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("import set warehouse safe stocks start ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        if (null == poushengCompensateBiz) {
            log.warn("ImportWarehouseStockRuleService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("ImportWarehouseSettingSafeStockService.doProcess context is null");
            return;
        }
        poushengCompensateBiz = settingSafeStock(poushengCompensateBiz);

        if (!StringUtils.isEmpty(poushengCompensateBiz.getLastFailedReason())) {
            PoushengCompensateBiz update = new PoushengCompensateBiz();
            update.setId(poushengCompensateBiz.getId());
            update.setLastFailedReason(poushengCompensateBiz.getLastFailedReason());
            poushengCompensateBizWriteService.update(update);
        }

        log.info("import set warehouse safe stocks end ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        
    }
    
    private PoushengCompensateBiz settingSafeStock(PoushengCompensateBiz poushengCompensateBiz){
        String url = poushengCompensateBiz.getContext();
        ExcelExportHelper<SettingSafeStockRecord> helper = ExcelExportHelper.newExportHelper(SettingSafeStockRecord.class);
        List<String[]> list = HandlerFileUtil.getInstance().handlerExcel(url);
        for (int i = 1; i < list.size(); i++) {
            String[] strs = list.get(i);
            String failReason = "";
            try {
                if(!StringUtils.hasText(strs[0])){
                    failReason = "账套不可为空";
                    continue; 
                }
                if(!StringUtils.hasText(strs[1])){
                    failReason = "外码不可为空";
                    continue;
                }
                if(!StringUtils.hasText(strs[3]) || !strs[3].matches("\\d+")){
                    failReason = "安全库存不可为空,并且为大于等于0的数字";
                    continue;
                }
                String companyId = strs[0].replace("\"", "").trim();
                String outerId = strs[1].replace("\"", "").trim();
                String spuCode = strs[2];
                String safeStock = strs[3].replace("\"", "").trim();
               //String warnEmail = strs[4];
                // 校验仓库是否存在
                WarehouseDTO warehousedto = validateWarehouse(outerId,companyId);
                if(warehousedto == null){
                    failReason = "该仓库不存在";
                    continue;
                }
                /*if(StringUtils.hasText(warnEmail)){
                    List<String> emails = Splitters.COMMA.splitToList(warnEmail);
                    // 校验邮箱有效性
                    for(String email : emails){
                        if(!VerifyUtil.verifyEmail(email)){
                            failReason = "邮箱无效,格式错误";
                            break;
                        }
                    }                  
                }*/
                if(StringUtils.hasText(failReason)){
                    continue;
                }
                
                // 如果货号不为空，则为设置该仓库下指定商品条码的安全库存，否则设置该仓库的安全库存。
                if(StringUtils.hasText(spuCode)){
                    spuCode = strs[2].replace("\"", "").trim();
                    failReason = settingSafeStocksByspucode(warehousedto.getId(),spuCode,Integer.valueOf(safeStock));                    
                }else{
                    Response<Boolean> updateResult = warehouseClient.updateSafeStock(warehousedto.getId(),Integer.valueOf(safeStock));
                    if(!updateResult.isSuccess()){
                        failReason = updateResult.getError(); 
                    }
                }                
                
            }catch (Exception e){
                log.error("failed to import setSafeStock url:{},cause:{}", url, Throwables.getStackTraceAsString(e));
                failReason = "系统异常" ;
            } finally {
                if (!StringUtils.isEmpty(failReason)) {
                    SettingSafeStockRecord record = new SettingSafeStockRecord();
                    if (!StringUtils.isEmpty(strs[0])) {
                        record.setCompanyId(strs[0].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(strs[1])) {
                        record.setOuterId(strs[1].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(strs[2])) {
                        record.setSpuCode(strs[2].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(strs[3])) {
                        record.setSafeStock(strs[3].replace("\"", ""));
                    }
                    record.setFailReason(failReason);
                    helper.appendToExcel(record);
                }
            }
        }
        if (helper.size() > 0) {
            String abnormalUrl = uploadFileComponent.exportAbnormalRecord(helper.transformToFile());
            poushengCompensateBiz.setLastFailedReason(abnormalUrl);
            poushengCompensateBiz.setUpdatedAt(DateTime.now().toDate());
        }
        return poushengCompensateBiz;
    }
    
    private WarehouseDTO validateWarehouse(String outerId, String companyId){
        WarehouseDTO result = warehouseCacher.findByOutCodeAndBizId(outerId, companyId);
        return result;
    }
    
    private String settingSafeStocksByspucode(Long warehouseid, String spuCode, Integer safeQuantity){
        // 查询该货号下的商品
        Map<String, String> params = Maps.newHashMap();
        params.put("spuCode", spuCode);
        WithAggregations<SearchSkuTemplate> searchResult = searchByParams(params, 300, 1);
        if (searchResult.getTotal() == 0) {
            return "该货号没有匹配的条码";
        }
        
        List<String> skuCodes = Lists.transform(searchResult.getData(), input -> input.getSkuCode());
        
        // 验证该仓库下的商品是否有效。（通过skucode和仓库代码获取inventory的id）
        Response<List<InventoryDTO>> inventoryDTOs = inventoryClient.findSkuStocks(warehouseid,skuCodes);
        if(!inventoryDTOs.isSuccess()){
            return inventoryDTOs.getError();
        }
        if(!CollectionUtils.isEmpty(inventoryDTOs.getResult())){
            StringBuilder sb = new StringBuilder();
            // 获取该spu货号下的skucode不在该仓库下的数据。
            List<String> warehouseSkuCodes = Lists.transform(inventoryDTOs.getResult(), inventoryDto -> inventoryDto.getSkuCode());
            List<String> excludeSkucodes = skuCodes.stream().filter(inputSku ->!warehouseSkuCodes.contains(inputSku)).distinct().collect(Collectors.toList());
            inventoryDTOs.getResult().forEach(inventorydto -> {
                Response<String> result = inventoryClient.setSkuInventorySafeStock(inventorydto.getId(), safeQuantity);
                if(!result.isSuccess()){
                    sb.append(inventorydto.getId()+",");
                }
            });
           if(StringUtils.hasText(sb)){
               sb.append("以上的inventory库存id设置安全库存失败。");
           }
           if(!CollectionUtils.isEmpty(excludeSkucodes)){
               sb.append(excludeSkucodes.toString() + "条码商品不存在该仓库下，可忽略");  
           }
           return sb.toString();
        }        
        return "";
    }

    private WithAggregations<SearchSkuTemplate> searchByParams(Map<String, String> params, Integer pageSize, Integer pageNo) {
        String templateName = "ps_search.mustache";
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(pageNo, pageSize, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by params:{}  fail,error:{}", params, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }
    
}
