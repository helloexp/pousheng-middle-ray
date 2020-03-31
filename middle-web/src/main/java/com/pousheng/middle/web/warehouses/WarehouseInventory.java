package com.pousheng.middle.web.warehouses;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.model.SpuMaterial;
import com.pousheng.erp.service.SpuMaterialReadService;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryDTO;
import com.pousheng.middle.warehouse.model.SkuInventory;
import com.pousheng.middle.web.utils.export.ExportLoadTemplateUtil;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 库存管理相关接口
 *
 * @author feisheng.ch
 * @date 2018-07-15
 */
@RestController
@RequestMapping("/api/warehouse/inventory")
@Slf4j
@Api(description = "库存管理相关接口")
public class WarehouseInventory {

    @Autowired
    private InventoryClient inventoryClient;
    @Autowired
    private SkuTemplateReadService skuTemplateReadService;
    @Autowired
    private SpuMaterialReadService spuMaterialReadService;
    @Autowired
    private CompensateBizLogic compensateBizLogic;
    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;

    /**
     * sku库存分页
     *
     * @param pageNo       起始页码
     * @param pageSize     每页返回条数
     * @param materialCode 货号
     * @param skuCode      sku码查询
     * @return 查询结果
     */
    @ApiOperation("库存汇总分页列表")
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SkuInventory> findBy(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                       @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                       @RequestParam(required = false, value = "materialCode") @ApiParam(value = "货号") String materialCode,
                                       @RequestParam(required = false, value = "skuCode") @ApiParam(value = "货品条码") String skuCode,
                                       @RequestParam(required = false, value = "warehouseName") @ApiParam(value = "仓库名称") String warehouseName,
                                       @RequestParam(required = false, value = "realQuantity") @ApiParam(value = "基准库存数量") Integer realQuantity) {

        // 检查，如果参数都是空，直接返回空页
        if (ObjectUtils.isEmpty(materialCode) && ObjectUtils.isEmpty(skuCode) && ObjectUtils.isEmpty(warehouseName)) {
            return Paging.empty();
        }

        List<String> skuCodeList = Lists.newArrayList();

        // 根据货号查找到skucode列表，参数传入
        if (!ObjectUtils.isEmpty(materialCode)) {
            // 先根据货号查询spuId
            Response<Optional<SpuMaterial>> spuMaterialRes = spuMaterialReadService.findbyMaterialCode(materialCode);
            if (!spuMaterialRes.isSuccess() || !spuMaterialRes.getResult().isPresent()) {
                log.error("materialCode [{}] has no spu matched, so return empty data", materialCode);
                return Paging.empty();
            }

            // 根据SPUID获取skuCode
            Response<List<SkuTemplate>> skuTemplateRes = skuTemplateReadService.findBySpuId(spuMaterialRes.getResult().get().getSpuId());
            if (!skuTemplateRes.isSuccess() || ObjectUtils.isEmpty(skuTemplateRes.getResult())) {
                log.error("spuId [{}] has no sku matched, so return empty data", spuMaterialRes.getResult().get().getSpuId());
                return Paging.empty();
            }

            List<String> skuCodes = Lists.newArrayList(Lists.transform(skuTemplateRes.getResult(), input -> input.getSkuCode()));
            if (!ObjectUtils.isEmpty(skuCode)) {
                if (!skuCodes.contains(skuCode)) {
                    return Paging.empty();
                }

                skuCodeList.add(skuCode);
            } else {
                skuCodeList.addAll(skuCodes);
            }
        } else {
            if (!ObjectUtils.isEmpty(skuCode)) {
                skuCodeList.add(skuCode);
            }
        }

        Paging<SkuInventory> retPage = inventoryClient.inventoryPaging(pageNo, pageSize, skuCodeList, warehouseName, realQuantity);

        // 补充货号信息
        if (!ObjectUtils.isEmpty(retPage.getData())) {

            // 一次性查出来所有skutemplate
            List<String> skuCodeQuery = Lists.newArrayList();
            retPage.getData().stream().forEach(input -> skuCodeQuery.add(input.getSkuCode()));
            Response<List<SkuTemplate>> templateRes = skuTemplateReadService.findBySkuCodes(skuCodeQuery);
            if (!templateRes.isSuccess() || ObjectUtils.isEmpty(templateRes.getResult())) {
                log.error("no template find by sku list: {}", JSON.toJSONString(retPage.getData()));
                return retPage;
            }

            Map<String, SkuTemplate> skuTemplateMap = templateRes.getResult().stream().collect(Collectors.toMap(SkuTemplate::getSkuCode, input -> input, (k1, k2) -> k1));

            for (SkuInventory inventory : retPage.getData()) {
                if (!ObjectUtils.isEmpty(materialCode)) {
                    inventory.setMaterialCode(materialCode);
                } else {
                    // 查询货号
                    if (skuTemplateMap.containsKey(inventory.getSkuCode())) {
                        Map<String, String> extra = skuTemplateMap.get(inventory.getSkuCode()).getExtra();
                        if (!ObjectUtils.isEmpty(extra) && extra.containsKey("materialCode")) {
                            inventory.setMaterialCode(extra.get("materialCode"));
                        }
                    }
                }
                // 添加尺码展示
                if (skuTemplateMap.containsKey(inventory.getSkuCode())) {
                    List<SkuAttribute> attrs = skuTemplateMap.get(inventory.getSkuCode()).getAttrs();
                    for (SkuAttribute attr : attrs) {
                        if (Objects.equals(attr.getAttrKey(), "尺码")) {
                            inventory.setSize(attr.getAttrVal());
                            break;
                        }
                    }
                }
            }
        }

        return retPage;
    }

    /**
     * 根据warehouseId和skuCode查询库存情况
     *
     * @param warehouseId
     * @param skuCode
     * @return
     */
    @RequestMapping(value = "/by/id-skucode", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<InventoryDTO> findByWarehouseIdAndSkuCode(@RequestParam Long warehouseId, @RequestParam String skuCode) {
        Response<InventoryDTO> response = inventoryClient.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
        return response;
    }

    /**
     * 批量导入根据货号设置安全库存
     * 
     * @param url
     */
    @ApiOperation("批量导入设置安全库存")
    @RequestMapping(value = "/import/setSafeStocks", method = RequestMethod.POST)
    @OperationLogType("批量导入设置安全库存")
    public void importWarehouseSafeStocks(String url){
        if (StringUtils.isEmpty(url)) {
            log.error("faild import file(path:{}) ,cause: file path is empty", url);
            throw new JsonResponseException("file.path.is.empty");
        }
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_SETTING_WAREHOUSE_SAFE_STOCK.toString());
        biz.setContext(url);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
    }

    /**
     * 查询导入文件的处理记录
     * 
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation("查询导入文件的处理记录")
    @RequestMapping(value = "/import/result/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("查询导入文件的处理记录")
    public Paging<PoushengCompensateBiz> create(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                @RequestParam(required = false, value = "pageSize") Integer pageSize) {
        PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setBizType(PoushengCompensateBizType.IMPORT_SETTING_WAREHOUSE_SAFE_STOCK.name());
        Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.pagingForShow(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @ApiOperation("批量设置安全库存模板下载")
    @GetMapping("/setSafeStocks/template/downlaod")
    public void settingSafeStocksExportTemplate(HttpServletRequest request, HttpServletResponse response) {
        try {
            String fileName = "批量设置安全库存模板.xlsx";
            List<String> headers = Lists.newArrayList("仓库账套(必填)", "仓库外码(必填)", "货号(选填。如果填写,则表示设置该仓库下,货号对应的所有条码)", "安全库存(必填)");
            ExportLoadTemplateUtil.exportTemplateExcel(fileName,"批量设置安全库存", headers,request, response);
        } catch (Exception e) {
            log.error("download setSafeStocks template failed. cause: {}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(500, "下载批量设置安全库存模板失败！");
        }
    }

}
