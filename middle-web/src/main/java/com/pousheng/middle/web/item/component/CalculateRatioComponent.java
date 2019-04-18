package com.pousheng.middle.web.item.component;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.ShopStockRule;
import com.pousheng.middle.web.item.cacher.ItemMappingCacher;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2019/4/19
 */
@Component
@Slf4j
public class CalculateRatioComponent {

    @Autowired
    private ItemMappingCacher itemMappingCacher;


    /**
     * 计算当前商品的推送比例
     */
    public Integer getRatio(ItemMapping itemMapping, ShopStockRule shopStockRule) {



        String skuCode = itemMapping.getSkuCode();
        //sku code 不存在
        if (Strings.isNullOrEmpty(skuCode)){
            return 0;
        }

        //状态不正确
        if (!Objects.equals(itemMapping.getStatus(),1)){
            return 0;
        }

        List<ItemMapping> itemMappings = itemMappingCacher.findBySkuCodeAndShopId(skuCode, itemMapping.getOpenShopId());

       /* Response<List<ItemMapping>> response = mappingReadService.listBySkuCodeAndOpenShopId(skuCode,itemMapping.getOpenShopId());
        if (!response.isSuccess()){
           log.error("find item mapping by sku code:{} and open shop id:{} fail,error:{}",skuCode,itemMapping.getOpenShopId(),response.getError());
            return 0;
        }

        List<ItemMapping> itemMappings = response.getResult();*/
        List<ItemMapping> validItemMappings = itemMappings.stream().filter(itemMapping1 -> itemMapping.getStatus().equals(1)).collect(Collectors.toList());

        //店铺库存推送规则是否启用
        //店铺推送比例是否存
        if (null == shopStockRule){
            log.error("not find shop rule by shop id:{}",itemMapping.getOpenShopId());
            if (Arguments.isNull(itemMapping.getRatio())){
                return 100;
            }
            return itemMapping.getRatio();
        }


        //未开启平均比例分配
        //如果关闭了平均分配比例，均以原来用户手工设置的比例为准(如果多条都没有设置比例，则第一条推送100%，其他的推0)
        //例如，开启平均分配比例前有三个映射，分别为30%，40%，空；当开关开启后，分配比例均为33%，等再关闭后，三个映射的比例分别为30%，40%，0
        //例如，开启平均分配比例前有三个映射，分别为空，空，空；当开关开启后，分配比例均为33%，等再关闭后，三个映射的比例分别为100%，0，0
        if (!shopStockRule.getIsAverageRatio()){
            return notAverageRatio(itemMapping,validItemMappings);
        } else {
            //开启了平均比例分配
            return averageSplitRatio(validItemMappings);
        }

    }


    private Integer notAverageRatio(ItemMapping itemMapping,List<ItemMapping> validItemMappings){

        //如果有效的映射只有一条记录
        if (Objects.equals(validItemMappings.size(),1)){
            //没有设置则推100
            if (Arguments.isNull(itemMapping.getRatio())){
                return 100;
            }
            return itemMapping.getRatio();
        }


        Map<Long,Integer> idRatioMaps = Maps.newHashMap();
        //过滤出设置过比例的
        List<ItemMapping> settingRatioItemMappings = validItemMappings.stream().filter(itemMapping1 -> Arguments.notNull(itemMapping.getRatio())).collect(Collectors.toList());

        //为空则说明没有设置过,第一个推送100，其他的为0
        if (CollectionUtils.isEmpty(settingRatioItemMappings)){
            for (ItemMapping notSettingMapping : validItemMappings){
                //若都没有设置则第一条100，其他的0
                if (Objects.equals(notSettingMapping.getId(),validItemMappings.get(0).getId())){
                    idRatioMaps.put(notSettingMapping.getId(),100);
                } else {
                    idRatioMaps.put(notSettingMapping.getId(),0);
                }
            }

            return idRatioMaps.get(itemMapping.getId());
        }

        //存在设置过的，如果设置过则按照设置的，无则比例为0
        for (ItemMapping im : validItemMappings){
            if (Arguments.isNull(im.getRatio())){
                idRatioMaps.put(im.getId(),0);
            } else{
                idRatioMaps.put(im.getId(),im.getRatio());
            }
        }

        return idRatioMaps.get(itemMapping.getId());
    }



    /**
     * 平均拆分推送比例
     */
    private Integer  averageSplitRatio(List<ItemMapping> validItemMappings) {

        //如果有效的映射只有一条记录
        if (Objects.equals(validItemMappings.size(),1)){
            return 100;
        }

        return calculateRatio(validItemMappings.size());
    }

    private static Integer calculateRatio(Integer discount){
        BigDecimal ratio = new BigDecimal("1");
        BigDecimal discountDecimal = new BigDecimal(discount);
        BigDecimal percentDecimal =  ratio.divide(discountDecimal,2, BigDecimal.ROUND_DOWN);
        return percentDecimal.multiply(BigDecimal.valueOf(100)).intValue();

    }
}
