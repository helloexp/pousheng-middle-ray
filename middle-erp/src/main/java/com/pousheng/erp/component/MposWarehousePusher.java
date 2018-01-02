package com.pousheng.erp.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pousheng.erp.dao.mysql.SpuMaterialDao;
import com.pousheng.erp.model.SpuMaterial;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 同步恒康 新增或删除mpos仓或门店
 * Author:  songrenfei
 * Date: 2018-01-02
 */
@Component
@Slf4j
public class MposWarehousePusher {

    private final ErpClient erpClient;

    private final SpuMaterialDao spuMaterialDao;

    @Autowired
    public MposWarehousePusher(ErpClient erpClient, SpuMaterialDao spuMaterialDao) {
        this.erpClient = erpClient;
        this.spuMaterialDao = spuMaterialDao;
    }

    /**
     * 添加新推送的仓
     * @param companyId 公司id
     * @param stockId 外码
     */
    public void addWarehouses(String companyId,String stockId){
        MposStock mposStock = new MposStock();
        mposStock.setCompany_id(companyId);
        mposStock.setStock_id(stockId);
        List<MposStock> mposStocks = Lists.newArrayList(mposStock);
        Map<String,Object> params = Maps.newHashMap();
        params.put("stock_list",mposStocks);
        String json = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(params);
        log.info("add mpos warehouse to erp, data:{}", json);
        erpClient.postJson("e-commerce-api/v1/create-mposstock-mapper",
                json);
    }


    /**
     * 删除推送的仓
     * @param companyId 公司id
     * @param stockId 外码
     */
    public void removeWarehouses(String companyId,String stockId){
        MposStock mposStock = new MposStock();
        mposStock.setCompany_id(companyId);
        mposStock.setStock_id(stockId);
        List<MposStock> mposStocks = Lists.newArrayList(mposStock);
        Map<String,Object> params = Maps.newHashMap();
        params.put("stock_list",mposStocks);
        String json = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(params);
        log.info("remove mpos warehouse from erp, data:{}", json);
        erpClient.postJson("e-commerce-api/v1/remove-mposstock-mapper",
                json);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MposStock implements Serializable{
        private static final long serialVersionUID = 8187196806389326644L;
        private String company_id;
        private String stock_id;
    }


}
