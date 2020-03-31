package com.pousheng.middle.warehouse.cache;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-19
 */
@Component
@Slf4j
public class WarehouseCacher {
    private final LoadingCache<Long, WarehouseDTO> byId;

    private final LoadingCache<String, WarehouseDTO> byCode;

    private final LoadingCache<String, WarehouseDTO> byOutCodeBizId;

    private final WarehouseClient warehouseClient;

    @Autowired
    public WarehouseCacher(WarehouseClient warehouseClient) {
        this.warehouseClient = warehouseClient;
        byId = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(CacheLoader.from(id -> {
                    Response<WarehouseDTO> r = warehouseClient.findById(id);
                    if (!r.isSuccess()) {
                        log.error("failed to find warehouse(id={}), error code:{}", id, r.getError());
                        throw new ServiceException(r.getError());
                    }

                    return r.getResult();
                }));

        byCode = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(CacheLoader.from(code -> {
                    Response<Optional<WarehouseDTO>> r = warehouseClient.findByCode(code);
                    if (!r.isSuccess()) {
                        log.error("failed to find warehouse(code={}), error code:{}", code, r.getError());
                        throw new ServiceException(r.getError());
                    }
                    Optional<WarehouseDTO> result = r.getResult();
                    if (result.isPresent()) {
                        return result.get();
                    } else {
                        log.error("warehouse(code={}) not found", code);
                        throw new ServiceException("warehouse.not.found");
                    }
                }));

        byOutCodeBizId = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(CacheLoader.from(code -> {
                    List<String> codes = Splitters.UNDERSCORE.splitToList(code);
                    Response<WarehouseDTO> r = warehouseClient.findByOutCodeBizId(codes.get(0), codes.get(1));
                    if (!r.isSuccess() || null == r.getResult()) {
                        log.error("failed to find warehouse(code={}), error code:{}", code, r.getError());
                        throw new ServiceException(r.getError());
                    }

                    return r.getResult();
                }));
    }

    public WarehouseDTO findById(Long id) {
        return byId.getUnchecked(id);
    }

    public WarehouseDTO findByCode(String code) {
        return byCode.getUnchecked(code);
    }

    public WarehouseDTO findByOutCodeAndBizId(String outCode, String bizId) {
        return byOutCodeBizId.getUnchecked(outCode + "_" + bizId);
    }

    public WarehouseDTO findByShopInfo(String shopInfo) {
        return byOutCodeBizId.getUnchecked(shopInfo);
    }


    public void refreshById(Long id) {
        Response<WarehouseDTO> r = warehouseClient.findById(id);
        if (!r.isSuccess()) {
            log.error("failed to find warehouse(id={}), error code:{}", id, r.getError());
            throw new ServiceException(r.getError());
        }
        WarehouseDTO exist = r.getResult();

        byId.invalidate(id);
        byCode.invalidate(exist.getWarehouseCode());
        byOutCodeBizId.invalidate(exist.getOutCode() + "_" + exist.getCompanyId());
    }
}
