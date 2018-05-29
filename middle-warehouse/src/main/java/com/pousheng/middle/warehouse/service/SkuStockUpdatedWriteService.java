package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.SkuStockUpdated;
import io.terminus.common.model.Response;

/**
 * Author:  lyj
 * Date: 2017/10/29
 * pousheng-middle
 */
public interface SkuStockUpdatedWriteService {


    /**
     * 删除skuStockUpdated
     * @return 是否成功
     */
    Response<Boolean> deleteAll();

//    /**
//     * 创建SkuStockUpdated
//     * @param skuStockUpdated 任务
//     * @return 主键id
//     *//*
//    Response<Long> create(SkuStockUpdated skuStockUpdated);
//
//    *//**
//     * 更新skuStockUpdated
//     * @param skuStockUpdated 任务
//     * @return 是否成功
//     *//*
//    Response<Boolean> update(SkuStockUpdated skuStockUpdated);
//
//    *//**
//     * 根据主键id删除skuStockUpdated
//     * @param skuStockUpdatedId 任务
//     * @return 是否成功
//     *//*
//    Response<Boolean> deleteById(Long skuStockUpdatedId);
//
//    *//**
//     * 更新超时时间还处理中还未处理完的任务状态回滚到待处理
//     * @return 是否更新成功
//     *//*
//    Response<Boolean> updateTimeOutHandleTask();
//
//    *//**
//     * 根据主键id更新SkuStockUpdated状态
//     * @param skuStockUpdatedId 任务
//     * @return 是否成功
//     *//*
//    Response<Boolean> updateStatusById(Long skuStockUpdatedId,int status);
}
