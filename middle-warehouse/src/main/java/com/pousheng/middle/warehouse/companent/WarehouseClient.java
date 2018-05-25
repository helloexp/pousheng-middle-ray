package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 仓库信息查询API调用
 *
 * @auther feisheng.ch
 * @time 2018/5/23
 */
@Component
@Slf4j
public class WarehouseClient {

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 根据ID获取
     * @param warehouseId
     * @return
     */
    public Response<WarehouseDTO> findById (Long warehouseId) {
        try {
            return Response.ok((WarehouseDTO) inventoryBaseClient.get("api/inventory/warehouse/id/"+warehouseId,
                    null, null, Maps.newHashMap(), WarehouseDTO.class, false));
        } catch (Exception e) {
            log.error("find warehouse by id fail, warehouseId:{}, cause:{}", warehouseId, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }

    }

    /**
     * 根据ID批量获取
     * @param warehouseIds
     * @return
     */
    public Response<List<WarehouseDTO>> findByIds (List<Long> warehouseIds) {
        try {
            return Response.ok((List<WarehouseDTO>) inventoryBaseClient.get("api/inventory/warehouse/query",
                    null, null, ImmutableMap.of("idList", JSON.toJSONString(warehouseIds)), WarehouseDTO.class, true));
        } catch (Exception e) {
            log.error("find warehouse by idList fail, warehouseId:{}, cause:{}", warehouseIds, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }

    }

    /**
     * 根据code获取
     * @param warehouseCode
     * @return
     */
    public Response<Optional<WarehouseDTO>> findByCode (String warehouseCode) {
        try {
            WarehouseDTO warehouseDTO = (WarehouseDTO) inventoryBaseClient.get("api/inventory/warehouse/code/"+warehouseCode,
                    null, null, Maps.newHashMap(), WarehouseDTO.class, false);

            return Response.ok(Optional.fromNullable(warehouseDTO));
        } catch (Exception e) {
            log.error("find warehouse by code fail, warehouseCode:{}, cause:{}", warehouseCode, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 根据外码和业务ID获取
     * @param outCode
     * @param bizId
     * @return
     */
    public Response<WarehouseDTO> findByOutCodeBizId (String outCode, String bizId) {
        try {
            WarehouseDTO warehouseDTO = (WarehouseDTO) inventoryBaseClient.get("api/inventory/warehouse/findByOutCodeBizId",
                    null, null, ImmutableMap.of("outCode", outCode, "bizId", bizId), WarehouseDTO.class, false);

            return Response.ok(warehouseDTO);
        } catch (Exception e) {
            log.error("find warehouse by code fail, outCode:{}, bizId:{} cause:{}", outCode, bizId, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 给仓库打标或者取消标签
     *
     * @param outCode  仓库内码
     * @param companyId  公司别名
     * @param isMpos  true: 打标  |  false: 取消
     * @return
     */
    public Response<Boolean> markMposOrNotWithOutCode (String outCode, String companyId, Boolean isMpos) {
        if (StringUtils.isEmpty(outCode) || StringUtils.isEmpty(companyId) || null == isMpos) {
            return Response.fail("warehouse.mpos.fail.parameter");
        }
        try {
            Response<WarehouseDTO> warehouseDTOResponse = findByOutCodeBizId(outCode, companyId);
            return Response.ok((Boolean) inventoryBaseClient.put("api/inventory/warehouse/"+
                    warehouseDTOResponse.getResult().getWarehouseCode()+"/"+
                    (isMpos?"make":"cancel") + "/flag" , Maps.newHashMap(), Boolean.class));
        } catch (Exception e) {
            log.error("fail to mark warehouse on mpos, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 给仓库打标或者取消标签
     *
     * @param innerCode  仓库内码
     * @param companyId  公司别名
     * @param isMpos  true: 打标  |  false: 取消
     * @return
     */
    public Response<Boolean> markMposOrNot (String innerCode, String companyId, Boolean isMpos) {
        if (StringUtils.isEmpty(innerCode) || StringUtils.isEmpty(companyId) || null == isMpos) {
            return Response.fail("warehouse.mpos.fail.parameter");
        }
        try {
            return Response.ok((Boolean) inventoryBaseClient.put("api/inventory/warehouse/"+
                            companyId+"-"+innerCode+"/"+
                            (isMpos?"make":"cancel") + "/flag" , Maps.newHashMap(), Boolean.class));
        } catch (Exception e) {
            log.error("fail to mark warehouse on mpos, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 给仓库更新状态
     *
     * @param warehouseId  仓库内码
     * @param status
     * @return
     */
    public Response<Boolean> updateStatus (Long warehouseId, Integer status) {
        if (null == warehouseId || null == status) {
            return Response.fail("warehouse.update.fail.parameter");
        }
        try {
            Map<String, Object> params = Maps.newHashMap();
            params.put("warehouseId", warehouseId);
            params.put("status", status);

            return Response.ok((Boolean) inventoryBaseClient.put("api/inventory/warehouse/updateWarehouseStatus", params, Boolean.class));
        } catch (Exception e) {
            log.error("fail to mark warehouse on mpos, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

}
