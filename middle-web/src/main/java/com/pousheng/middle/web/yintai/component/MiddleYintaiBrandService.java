package com.pousheng.middle.web.yintai.component;

import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.web.yintai.YintaiConstant;
import com.pousheng.middle.web.yintai.dto.YintaiBrand;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.MappingServiceRegistryCenter;
import io.terminus.open.client.common.mappings.model.BrandMapping;
import io.terminus.open.client.common.mappings.service.OpenClientBrandMappingService;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.brand.service.BrandReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 银泰品牌
 * AUTHOR: zhangbin
 * ON: 2019/6/24
 */
@Service
@Slf4j
public class MiddleYintaiBrandService {

    @Autowired
    private BrandReadService brandReadService;
    @Autowired
    private MappingServiceRegistryCenter center;

    /**
     * 获取中台品牌与银泰品牌映射
     * @param brandType 品牌展示类型
     * @return
     */
    public List<List<String>> getBrandMapping(String brandType) {
        if (YintaiConstant.BRAND_MAPPING.equals(brandType)) {
            return getYintaiBrandList().stream()
                    .map(brand -> {
                        List<String> row = Lists.newArrayList();
                        row.add(String.valueOf(brand.getBrandId()));
                        row.add(brand.getBrandName());
                        row.add(brand.getChannelBrandId());
                        return row;
                    }).collect(Collectors.toList());
        }
        List<Brand> middleBrandList = getMiddleBrandList();

        //渠道品牌映射
        OpenClientBrandMappingService brandService = center.getBrandService(MiddleChannel.YINTAI.getValue());
        List<BrandMapping> channelBrandList = brandService.findBrandListByChannel(MiddleChannel.YINTAI.getValue()).getResult();
        Map<Long, BrandMapping> brandMapping = channelBrandList.stream().collect(Collectors.toMap(BrandMapping::getBrandId, Function.identity(), (a,b)->a));
        //已中台品牌列表为主转换输出
        return middleBrandList.stream().map(brand->{
            List<String> row = Lists.newArrayList();
            row.add(String.valueOf(brand.getId()));
            row.add(brand.getName());
            BrandMapping mapping = brandMapping.get(brand.getId());
            if (mapping != null) {
                row.add(mapping.getChannelBrandId());
            }
            return row;
        }).collect(Collectors.toList());
    }

    /**
     * 中台品牌列表
     * @return
     */
    public List<Brand> getMiddleBrandList() {
        //中台品牌
        int pageNo = 1;
        int pageSize = 100;
        Response<Paging<Brand>> paging = brandReadService.pagination(pageNo, pageSize, null);
        if (!paging.isSuccess()) {
            log.error("export failed , error:{}", paging.getError());
            throw new ServiceException("export.excel.fail");
        }
        Long total = paging.getResult().getTotal();
        List<Brand> middleBrandList = Lists.newArrayList();
        middleBrandList.addAll(paging.getResult().getData());
        int size = middleBrandList.size();
        while (size < total) {
            paging = brandReadService.pagination(++pageNo, pageSize, null);
            if (!paging.isSuccess()) {
                log.error("export failed, error:{}", paging.getError());
                throw new JsonResponseException("export.excel.fail");
            }
            middleBrandList.addAll(paging.getResult().getData());
            if (size == middleBrandList.size()) {
                log.warn("brand pagination response has fail, break. pageNo{}", pageNo);
                break;
            }
            size = middleBrandList.size();
        }
        return middleBrandList;
    }

    /**
     * 银泰关联品牌列表
     * @return
     */
    public List<YintaiBrand> getYintaiBrandList() {
        OpenClientBrandMappingService brandService = center.getBrandService(MiddleChannel.YINTAI.getValue());
        Response<List<BrandMapping>> brandMappingResp = brandService.findBrandListByChannel(MiddleChannel.YINTAI.getValue());
        if (!brandMappingResp.isSuccess()) {
            log.error("find brand mapping fail , error:({})", brandMappingResp.getError());
            return Collections.emptyList();
        }

        List<Long> brandIds = brandMappingResp.getResult().stream().map(BrandMapping::getBrandId).collect(Collectors.toList());
        Response<List<Brand>> middleBrandResp = brandReadService.findByIds(brandIds);
        if (!middleBrandResp.isSuccess()) {
            log.error("find brand list fail, error:({})", middleBrandResp.getError());
            return Collections.emptyList();
        }

        Map<Long, Brand> middleBrandMapping = middleBrandResp.getResult().stream().collect(Collectors.toMap(Brand::getId, Function.identity(), (a, b) -> a));

        return brandMappingResp.getResult().stream()
                .filter(brand->middleBrandMapping.containsKey(brand.getBrandId()))
                .map(brand->{
                    Brand middleBrand = middleBrandMapping.get(brand.getBrandId());
                    YintaiBrand yintaiBrand = new YintaiBrand();
                    yintaiBrand.setBrandId(String.valueOf(middleBrand.getId()));
                    yintaiBrand.setBrandName(middleBrand.getName());
                    yintaiBrand.setChannelBrandId(brand.getChannelBrandId());
                    return yintaiBrand;
                })
                .collect(Collectors.toList());
    }
}
