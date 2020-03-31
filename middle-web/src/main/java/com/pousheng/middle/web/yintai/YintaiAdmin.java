package com.pousheng.middle.web.yintai;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Splitter;
import com.pousheng.middle.item.constant.ItemPushStatus;
import com.pousheng.middle.group.model.ChannelItemPush;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.web.shop.dto.Channel;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.yintai.biz.YintaiImportFileInfo;
import com.pousheng.middle.web.yintai.component.MiddleYintaiBrandService;
import com.pousheng.middle.web.yintai.component.MiddleYintaiItemService;
import com.pousheng.middle.web.yintai.component.MiddleYintaiLogService;
import com.pousheng.middle.web.yintai.component.MiddleYintaiShopService;
import com.pousheng.middle.web.yintai.dto.ExcelModel;
import com.pousheng.middle.web.yintai.dto.YintaiItemPushStatus;
import com.pousheng.middle.web.yintai.dto.YintaiPushItemLogDTO;
import com.pousheng.middle.web.yintai.dto.YintaiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AUTHOR: zhangbin
 * ON: 2019/6/21
 */
@Api(description = "银泰API")
@Slf4j
@RestController
@RequestMapping("/api/yintai")
public class YintaiAdmin {

    //中台品牌
    private static final List<String> BRAND_HEADER = Lists.newArrayList("中台品牌ID", "中台品牌名称", "银泰品牌ID");
    private static final String BRAND_TEMPLATE_FILE_NAME = "中台品牌与银泰品牌映射模板";
    private static final String BRAND_MAPPING_FILE_NAME = "银泰品牌映射";

    //中台店铺
    private static final List<String> SHOP_TEMPLATE_HEADER = Lists.newArrayList("公司码", "门店外码", "银泰专柜ID");
    private static final List<String> SHOP_MAPPING_HEADER = Lists.newArrayList("公司码", "门店外码", "店铺名称", "银泰专柜ID");
    private static final String SHOP_TEMPLATE_FILE_NAME = "中台与银泰店柜映射模板";
    private static final String SHOP_MAPPING_FILE_NAME = "银泰店柜映射";

    @Autowired
    private MiddleYintaiBrandService middleYintaiBrandService;
    @Autowired
    private MiddleYintaiShopService middleYintaiShopService;
    @Autowired
    private MiddleYintaiItemService middleYintaiItemService;
    @Autowired
    private MiddleYintaiLogService middleYintaiLogService;
    @Autowired
    private CompensateBizLogic compensateBizLogic;
    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;

    @ApiOperation("导出中台品牌与银泰品牌映射模板")
    @GetMapping("/export/brand/template")
    public void exportBrandTemplate(HttpServletResponse response) {
        String fileName = new String(BRAND_TEMPLATE_FILE_NAME.getBytes(), Charset.forName("iso8859-1"));
        ExcelModel downExcel = new ExcelModel();
        downExcel.setSheetName(BRAND_TEMPLATE_FILE_NAME);
        downExcel.setHeader(BRAND_HEADER);
        downExcel.setData(middleYintaiBrandService.getBrandMapping(YintaiConstant.BRAND_TEMPLATE));//行数据
        exportExcel(fileName, downExcel, response);

    }

    @ApiOperation("导出品牌映射")
    @GetMapping("/export/brand/mapping")
    public void exportBrandMapping(HttpServletResponse response) {
        String fileName = new String(BRAND_MAPPING_FILE_NAME.getBytes(), Charset.forName("iso8859-1"));
        ExcelModel downExcel = new ExcelModel();
        downExcel.setSheetName(BRAND_MAPPING_FILE_NAME);
        downExcel.setHeader(BRAND_HEADER);
        downExcel.setData(middleYintaiBrandService.getBrandMapping(YintaiConstant.BRAND_MAPPING));
        exportExcel(fileName, downExcel, response);
    }

    @ApiOperation("导出中台与银泰店柜映射模板")
    @GetMapping("/export/shop/template")
    public void exportShopTemplate(HttpServletResponse response) {
        String fileName = new String(SHOP_TEMPLATE_FILE_NAME.getBytes(), Charset.forName("iso8859-1"));
        ExcelModel downExcel = new ExcelModel();
        downExcel.setSheetName(SHOP_TEMPLATE_FILE_NAME);
        downExcel.setHeader(SHOP_TEMPLATE_HEADER);
        downExcel.setData(middleYintaiShopService.getShopMapping(YintaiConstant.SHOP_TEMPLATE));
        exportExcel(fileName, downExcel, response);
    }

    @ApiOperation("导出银泰店柜映射")
    @GetMapping("/export/shop/mapping")
    public void exportShopMapping(HttpServletResponse response) {
        String fileName = new String(SHOP_MAPPING_FILE_NAME.getBytes(), Charset.forName("iso8859-1"));
        ExcelModel downExcel = new ExcelModel();
        downExcel.setSheetName(SHOP_MAPPING_FILE_NAME);
        downExcel.setHeader(SHOP_MAPPING_HEADER);
        downExcel.setData(middleYintaiShopService.getShopMapping(YintaiConstant.SHOP_MAPPING));
        exportExcel(fileName, downExcel, response);
    }


    @ApiOperation("银泰配置页面")
    @GetMapping("/config/render")
    public YintaiResponse render() {
        YintaiResponse response = new YintaiResponse();
        response.setBrandList(middleYintaiBrandService.getYintaiBrandList());
        response.setShopList(middleYintaiShopService.getShopList());
        return response;
    }

    @OperationLogType("导入银泰品牌映射")
    @ApiOperation("导入品牌映射")
    @RequestMapping(value = "/import/brand", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public long importBrand(YintaiImportFileInfo info) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_YINTAI_BRAND_MAPPING.toString());
        biz.setContext(JSON.toJSONString(info));
        biz.setBizId(UUID.randomUUID().toString());
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
    }

    @OperationLogType("导入银泰店铺映射")
    @ApiOperation("导入店铺映射")
    @RequestMapping(value = "/import/shop", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public long importShop(YintaiImportFileInfo info) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_YINTAI_SHOP_MAPPING.toString());
        biz.setContext(JSON.toJSONString(info));
        biz.setBizId(UUID.randomUUID().toString());
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
    }

    private void exportExcel(String fileName, ExcelModel downExcel, HttpServletResponse response) {

        try {
            OutputStream out = response.getOutputStream();
            //清空输出流
            response.reset();
            //设置响应头和下载保存的文件名
            response.setHeader("content-disposition","attachment;filename="+fileName+".xlsx");
            //定义输出类型
            response.setContentType("APPLICATION/msexcel");

            ExcelWriter writer = new ExcelWriter(out, ExcelTypeEnum.XLSX, true);
            Sheet sheet = new Sheet(1);
            sheet.setSheetName(downExcel.getSheetName());
            List<List<String>> list = Lists.newArrayList();
            for (int i = 0; i < downExcel.getHeader().size(); i++) {
                list.add(i, Lists.newArrayList(downExcel.getHeader().get(i)));
            }

            sheet.setHead(list);
            writer.write0(downExcel.getData(), sheet);
            writer.finish();

            response.flushBuffer();
        } catch (Exception e) {
            log.error("export failed file{}, error:{}", fileName, e);
            throw new JsonResponseException("export.excel.fail");
        }
    }


    @ApiOperation("查询银泰店铺导入的处理记录")
    @GetMapping(value = "/import/shop/result")
    public Paging<PoushengCompensateBiz> shopImportResult(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                      @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                      Integer bizId) {
        return importResult(pageNo, pageSize, bizId, PoushengCompensateBizType.IMPORT_YINTAI_SHOP_MAPPING);
    }

    @ApiOperation("查询银泰品牌导入的处理记录")
    @GetMapping(value = "/import/brand/result")
    public Paging<PoushengCompensateBiz> brandImportResult(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                          @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                          Integer bizId) {
        return importResult(pageNo, pageSize, bizId, PoushengCompensateBizType.IMPORT_YINTAI_BRAND_MAPPING);
    }

    private Paging<PoushengCompensateBiz> importResult(Integer pageNo, Integer pageSize, Integer bizId, PoushengCompensateBizType bizType) {
        PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        if (bizId != null) {
            criteria.setBizId(bizId.toString());
        }
        criteria.setBizType(bizType.name());
        Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.pagingForShow(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @ApiOperation("商品信息同步日志分页查询")
    @GetMapping(value = "/item/push/log")
    public Paging<YintaiPushItemLogDTO> pagingLog(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                  @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                  @RequestParam(required = false, value = "channel") String channel,
                                                  @RequestParam(required = false, value = "spuCode") String spuCode,
                                                  @RequestParam(required = false, value = "skuCode") String skuCode,
                                                  @RequestParam(required = false, value = "brandId") Long brandId,
                                                  @RequestParam(required = false, value = "status") Integer status) {
        YintaiPushItemLogDTO search = new YintaiPushItemLogDTO();
        search.setChannel(channel);
        search.setSpuCode(spuCode);
        search.setSkuCode(skuCode);
        search.setBrandId(brandId);
        if (status != null) {
            search.setSyncStatus(String.valueOf(status));
        }
        return middleYintaiLogService.paging(pageNo, pageSize, search);
    }

    @ApiOperation("同步渠道列表信息")
    @RequestMapping(value = "/channel", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Channel> findChannels() {
        List<Channel> list = Lists.newArrayList();
        list.add(new Channel().name(MiddleChannel.YINTAI.getDesc()).code(MiddleChannel.YINTAI.getValue()));
        return list;
    }

    @ApiOperation("同步状态列表信息")
    @RequestMapping(value = "/sync/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<YintaiItemPushStatus> findSyncStatus() {
        List<YintaiItemPushStatus> list = Lists.newArrayList();
        for (ItemPushStatus value : ItemPushStatus.values()) {
            list.add(YintaiItemPushStatus.builder().code(value.getValue()).name(value.getDesc()).build());
        }
        return list;
    }

    @ApiOperation("手动上传商品")
    @GetMapping(value = "/operator/upload/{hour}")
    public String operatorUpload(@PathVariable Integer hour) {
        log.info("[operatorUpload]  hour:({}), userId:({})", hour, UserUtil.getUserId());
        middleYintaiItemService.uploadTask(hour);
        return "ok";
    }


    @ApiOperation("手动上传商品")
    @GetMapping(value = "/operator/upload")
    public String operatorUpload(String skuCodes) {
        log.info("[operatorUpload]  skuCode:({}) userId:({})", skuCodes, UserUtil.getUserId());
        List<String> skuCodeList = Lists.newArrayList(Splitter.on(",").split(skuCodes));
        Response<Map<String, String>> mapResponse = middleYintaiItemService.uploadBySkuCodes(skuCodeList);
        return JSON.toJSONString(mapResponse);
    }

    @ApiOperation("触发同步")
    @GetMapping(value = "/trigger/upload")
    public Response<Boolean> asynTrigger(@RequestParam(required = false, value = "spuCode") String spuCode,
                                         @RequestParam(required = false, value = "skuCode") String skuCode,
                                         @RequestParam(required = false, value = "brandId") Long brandId,
                                         @RequestParam(required = false, value = "status") Integer status) {
        if (Strings.isNullOrEmpty(spuCode) && Strings.isNullOrEmpty(skuCode) && brandId == null) {
            return Response.fail("请额外输入货号/条码/品牌");
        }
//        if (ItemPushStatus.from(status) != ItemPushStatus.FAIL) {
//            return Response.fail("仅同步同步状态失败的商品");
//        }
        status = ItemPushStatus.FAIL.getValue();
        List<ChannelItemPush> channelItemPushList = middleYintaiLogService.findBy(spuCode, skuCode, brandId, status);
        if (CollectionUtils.isEmpty(channelItemPushList)) {
            log.info("[syncTrigger] 没有同步失败的商品需要推送 spuCode:({}), skuCode:({}), brandId:({}), status:({})", spuCode, skuCode, brandId, status);
            return Response.ok(Boolean.TRUE);
        }
        Response<Map<String, String>> mapResponse = middleYintaiItemService.uploadBySkuCodes(channelItemPushList.stream().map(ChannelItemPush::getSkuCode).collect(Collectors.toList()));
        log.info("[syncTrigger]  spuCode:({}), skuCode:({}), brandId:({}), status:({}), resule:{}", spuCode, skuCode, brandId, status, JSON.toJSONString(mapResponse));
        return Response.ok(Boolean.TRUE);
    }
}
