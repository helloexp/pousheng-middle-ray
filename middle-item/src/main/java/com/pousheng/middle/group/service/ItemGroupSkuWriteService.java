package com.pousheng.middle.group.service;

import com.pousheng.middle.group.model.ItemGroupSku;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */
public interface ItemGroupSkuWriteService {

    /**
     * 创建ItemGroupSku
     * @param itemGroupSku
     * @return 主键id
     */
    Response<Long> create(ItemGroupSku itemGroupSku);

    /**
     * 更新ItemGroupSku
     * @param itemGroupSku
     * @return 是否成功
     */
    Response<Boolean> update(ItemGroupSku itemGroupSku);

    /**
     * 根据主键id删除ItemGroupSku
     * @param itemGroupSkuId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long itemGroupSkuId);


    /**
     * 创建ItemGroupSku同时更新group
     * @param itemGroupSku 映射关系
     * @return 主键id
     */
    Response<Long> createItemGroupSku(ItemGroupSku itemGroupSku);

    /**
     * 批量创建ItemGroupSku同时更新group
     * @param skuCodes
     * @param groupId
     * @param type
     * @param mark
     * @return
     */
    Response<Integer> batchCreate(List<String> skuCodes, Long groupId, Integer type, Integer mark);

    /**
     * 批量删除ItemGroupSku同时更新group 新增mark字段 当mark为0时仅删除自动打标 不为0时 删除全部
     * @param skuCodes
     * @param groupId
     * @param type
     * @param mark  标记位（是否仅自动）
     * @return
     */
    Response<Integer> batchDelete(List<String> skuCodes, Long groupId, Integer type, Integer mark);

    /**
     * 删除ItemGroupSku同时更新group
     * @param groupId
     * @param skuCode
     * @returna
     */
    Response<Boolean> deleteByGroupIdAndSkuCode(Long groupId, String skuCode);

    /**
     * 根据id批量删除ItemGroupSku
     * @param ids
     * @return
     */
    Response<Integer> batchDeleteByids(List<Long> ids);
    
    
}
