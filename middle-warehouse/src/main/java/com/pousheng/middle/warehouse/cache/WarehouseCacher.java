package com.pousheng.middle.warehouse.cache;

import com.google.common.base.Optional;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-19
 */
@Component
@Slf4j
public class WarehouseCacher {
    private final ConcurrentMap<Long, WarehouseDTO> byId;

    private final ConcurrentMap<String, WarehouseDTO> byCode;

    private final ConcurrentMap<String, WarehouseDTO> byOutCodeBizId;

    private final WarehouseClient warehouseClient;

    @Autowired
    public WarehouseCacher(WarehouseClient warehouseClient) {
        this.warehouseClient = warehouseClient;
        byId = new ConcurrentHashMap<>();
        byCode = new ConcurrentHashMap<>();
        byOutCodeBizId = new ConcurrentHashMap<>();
    }

    public WarehouseDTO findById(Long id){
        return byId.computeIfAbsent(id, id1 -> {
            Response<WarehouseDTO> r =  warehouseClient.findById(id1);
            if(!r.isSuccess()){
                log.error("failed to find warehouse(id={}), error code:{}", id1, r.getError());
                throw new ServiceException(r.getError());
            }

            return r.getResult();
        });
    }

    public WarehouseDTO findByCode(String code){
        return byCode.computeIfAbsent(code, code1 -> {
            Response<Optional<WarehouseDTO>> r =  warehouseClient.findByCode(code1);
            if(!r.isSuccess()){
                log.error("failed to find warehouse(code={}), error code:{}", code1, r.getError());
                throw new ServiceException(r.getError());
            }
            Optional<WarehouseDTO> result = r.getResult();
            if(result.isPresent()) {
                return result.get();
            }else{
                log.error("warehouse(code={}) not found", code1);
                throw new ServiceException("warehouse.not.found");
            }
        });
    }

    public WarehouseDTO findByOutCodeAndBizId(String outCode, String bizId){
        return byOutCodeBizId.computeIfAbsent(outCode+"_"+bizId, code1 -> {
            Response<WarehouseDTO> r =  warehouseClient.findByOutCodeBizId(code1.split("_")[0], code1.split("_")[1]);
            if(!r.isSuccess() || null == r.getResult()){
                log.error("failed to find warehouse(code={}), error code:{}", code1, r.getError());
                throw new ServiceException(r.getError());
            }

            return r.getResult();
        });
    }

    public WarehouseDTO findByShopInfo(String shopInfo) {
        return byCode.computeIfAbsent(shopInfo, shopInfo1 -> {
            List<String> infos = Splitters.UNDERSCORE.splitToList(shopInfo1);
            Response<WarehouseDTO> r = warehouseClient.findByOutCodeBizId(infos.get(0), infos.get(1));
            if (!r.isSuccess()) {
                log.error("failed to find warehouse(info={}), error code:{}", shopInfo1, r.getError());
                throw new ServiceException(r.getError());
            }

            return r.getResult();
        });
    }


    public void refreshById(Long id){
        Response<WarehouseDTO> r =  warehouseClient.findById(id);
        if(!r.isSuccess()){
            log.error("failed to find warehouse(id={}), error code:{}", id, r.getError());
            throw new ServiceException(r.getError());
        }
        WarehouseDTO exist = r.getResult();

        if(byId.containsKey(id)){
            byId.replace(id,exist);
        }else {
            byId.putIfAbsent(id,exist);
        }

        if(byCode.containsKey(exist.getWarehouseCode())){
            byCode.replace(exist.getWarehouseCode(),exist);
        }else {
            byCode.putIfAbsent(exist.getWarehouseCode(),exist);
        }
    }



}
