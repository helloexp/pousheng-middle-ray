package com.pousheng.middle.web.brand;

import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.open.client.parana.dto.OpenClientBrand;
import io.terminus.open.client.parana.item.SyncParanaBrandService;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.brand.service.BrandReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by songrenfei on 2017/6/7
 */
@RestController
@Slf4j
@RequestMapping("/api/brand")
public class SyncParanaBrands {


    @RpcConsumer
    private BrandReadService brandReadService;
    @Autowired
    private SyncParanaBrandService syncParanaBrandService;

    @RequestMapping(value = "/{id}/sync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> synBrand(@PathVariable(name = "id") Long brandId){

        Response<Brand> brandRes = brandReadService.findById(brandId);
        if(!brandRes.isSuccess()){
            log.error("find brand by id:{} fail,error:{}",brandId,brandRes.getError());
            throw new JsonResponseException(brandRes.getError());
        }
        List<OpenClientBrand> openClientBrands = Lists.newArrayList();
        OpenClientBrand openClientBrand = new OpenClientBrand();
        Brand brand = brandRes.getResult();
        BeanMapper.copy(brand,openClientBrand);
        openClientBrands.add(openClientBrand);
        return syncParanaBrandService.syncBrands(openClientBrands);
    }
}
