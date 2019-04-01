package com.pousheng.middle.web.warehouses;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pousheng.erp.service.SpuMaterialReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.ChannelInventoryClient;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.model.PoushengChannelDTO;
import com.pousheng.middle.web.shop.cache.MiddleOpenShopCacher;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import com.pousheng.middle.web.warehouses.dto.PoushengChannelImportDTO;
import io.swagger.annotations.Api;
import io.terminus.applog.annotation.LogMe;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

/**
 * 渠道库存管理相关接口
 *
 * @author feisheng.ch
 * @date 2018-07-15
 *
 */
@RestController
@RequestMapping("/api/warehouse/inventory/channel")
@Slf4j
@Api(description = "渠道库存管理相关接口")
public class ChannelInventory {

    @Autowired
    private ChannelInventoryClient channelInventoryClient;
    @Autowired
    private OpenShopReadService openShopReadService;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private MiddleOpenShopCacher middleOpenShopCacher;

    /**
     * 导入指定库存
     *
     * @param filePath
     * @return
     */
    @RequestMapping(value = "/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @LogMe(description = "导入指定库存（渠道库存）", ignore = true)
    public Boolean importChannelInventory(@RequestParam(value = "filePath") String filePath) {
        if (ObjectUtils.isEmpty(filePath)) {
            throw new JsonResponseException("传入的文件地址是空的，请确认是否上传成功");
        }
        log.info("[CHANNEL INVENTORY][IMPORT]start to import channel inventory from file [{}]", filePath);

        try {
            String[] pathPart = filePath.split("\\.");
            if (pathPart.length < 1) {
                throw new JsonResponseException("文件格式不正确, 请上传正确的文件，xlsx文件");
            }

            if (!"xlsx".equalsIgnoreCase(pathPart[pathPart.length-1])) {
                throw new JsonResponseException("文件格式不正确, 请上传正确的文件，xlsx文件");
            }

            URL url = new URL(filePath);
            HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
            urlCon.setConnectTimeout(5000);
            urlCon.setReadTimeout(5000);

            //解析文件
            List<PoushengChannelImportDTO> channelImportDTOS = HandlerFileUtil.getInstance().handlerExcelChannelInventory(urlCon.getInputStream());

            if (log.isDebugEnabled()){
                log.debug("channel inventory parsed data: {}", channelImportDTOS);
            }

            // 开始校验
            if (ObjectUtils.isEmpty(channelImportDTOS)) {
                throw new JsonResponseException("Excel文件中没有找到配置数据，请确认后重新导入");
            }

            List<PoushengChannelDTO> createData = Lists.newArrayList();
            int i = 2;
            for (PoushengChannelImportDTO importDTO : channelImportDTOS) {
                // 校验仓库是否存在
                String[] bizOutCodes = importDTO.getBizOutCode().split("-");
                if (bizOutCodes.length != 2) {
                    throw new JsonResponseException("第"+i+"行，仓库标识格式不正确：账套-外码");
                }

                WarehouseDTO warehouseDTO = null;
                try {
                    warehouseDTO = warehouseCacher.findByOutCodeAndBizId(bizOutCodes[1], bizOutCodes[0]);
                } catch (Exception e) {
                    throw new JsonResponseException("第"+i+"行，仓库没有找到");
                }
                if (null == warehouseDTO) {
                    throw new JsonResponseException("第"+i+"行，仓库没有找到");
                }

                // 校验库存是否存在 - 放到库存那边去校验

                // 校验库存是否充足 - 放到库存那边去校验

                // 校验店铺是否存在
                String[] shopOuts = importDTO.getShopOutCode().split("-");
                if (shopOuts.length != 2) {
                    throw new JsonResponseException("第"+i+"行，店铺标识格式不正确：账套-外码");
                }
                OpenShop openShop = middleOpenShopCacher.findByAppKey(importDTO.getShopOutCode());
                if (null == openShop) {
                    throw new JsonResponseException("第"+i+"行，店铺没有找到");
                }

                PoushengChannelDTO dto = new PoushengChannelDTO();
                dto.setSkuCode(importDTO.getSkuCode());
                dto.setWarehouseId(warehouseDTO.getId());
                dto.setWarehouseName(warehouseDTO.getWarehouseName());
                dto.setOpenShopId(openShop.getId());
                dto.setOpenShopName(openShop.getShopName());
                dto.setChannelQuantity(Long.parseLong(importDTO.getChannelQuantity()));

                createData.add(dto);

                ++i;
            }

            Response<String> createRet = channelInventoryClient.batchCreate(createData);

            if (!createRet.isSuccess()) {
                throw new JsonResponseException(createRet.getError());
            }

            if (!ObjectUtils.isEmpty(createRet.getResult())) {
                throw new JsonResponseException("导入出错：" + createRet.getResult());
            }

            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("import channel inventory failed,caused by {}", Throwables.getStackTraceAsString(e));

            if (e instanceof MalformedURLException) {
                throw new JsonResponseException("文件没有找到，请确认文件是否上传成功");
            }

            throw new JsonResponseException(e.getMessage());
        }
    }

}
