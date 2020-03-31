package com.pousheng.middle.web.excel.aftersale;

import com.alibaba.fastjson.JSONObject;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.EditSubmitRefundItem;
import com.pousheng.middle.order.dto.SubmitRefundInfo;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Desc 售后单导入
 * @Author GuoFeng
 * @Date 2019/5/29
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_AFTERSALE_ORDER)
@Service
@Slf4j
public class ImportAftersaleOrderTask implements CompensateBizService {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private RefundWriteLogic refundWriteLogic;

    @Autowired
    private UploadFileComponent uploadFileComponent;

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        String bizID = poushengCompensateBiz.getBizId();
        log.info("ImportAftersaleOrderTask==> start to process aftersale order import task ID:{}", bizID);
        poushengCompensateBiz = processFile(poushengCompensateBiz);
        if (!StringUtils.isEmpty(poushengCompensateBiz.getLastFailedReason())) {
            PoushengCompensateBiz update = new PoushengCompensateBiz();
            update.setId(poushengCompensateBiz.getId());
            update.setUpdatedAt(new Date());
            update.setLastFailedReason(poushengCompensateBiz.getLastFailedReason());
            poushengCompensateBizWriteService.update(update);
        }
        log.info("ImportAftersaleOrderTask==> process aftersale order import task ID:{} finished", bizID);
    }

    public PoushengCompensateBiz processFile(PoushengCompensateBiz poushengCompensateBiz){
        String bizID = poushengCompensateBiz.getBizId();
        try {
            String context = poushengCompensateBiz.getContext();
            if (StringUtils.isEmpty(context)) {
                log.error("ImportAftersaleOrderTask==> task ID:{} content is empty !", bizID);
                throw new BizException("任务参数为空");
            }
            AftersaleImportFileInfo fileInfo = JSONObject.parseObject(context, AftersaleImportFileInfo.class);
            //1.解析excel数据
            String fileUrl = fileInfo.getFilePath();
            if (StringUtils.isEmpty(fileUrl)) {
                String errorMsg = "task ID:" + bizID + ",文件地址为空";
                log.error(errorMsg);
                throw new BizException(errorMsg);
            }
            //导入失败需要记录的数据日志excel
            List<FaildExcelBean> errorData = Lists.newArrayList();
            ExcelExportHelper<FaildExcelBean> errorExcel = ExcelExportHelper.newExportHelper(FaildExcelBean.class);

            //导入的excel数据
            List<FileImportExcelBeanWrapper> data = HandlerFileUtil.getInstance().handleAftersaleOrderExcel(fileUrl);

            //创建售后单，并分析出可用数据和不可用数据
            if (data.size() > 0) {
                for(FileImportExcelBeanWrapper wrapper : data) {
                    FileImportExcelBean fileImportExcelBean = wrapper.getFileImportExcelBean();
                    try {
                        //当前数据行有错误
                        if (wrapper.isHasError()) {
                            FaildExcelBean faildExcelBean = toFaildExcelBean(wrapper);
                            errorData.add(faildExcelBean);
                        } else {
                            //查询交易单
                            Response<ShopOrder> shopOrderResponse = middleOrderReadService.findByOutIdAndOutFrom(fileImportExcelBean.getOutOrderNumber(), MiddleChannel.JD.getValue());
                            String errorMsg = "";
                            if (shopOrderResponse.isSuccess() && shopOrderResponse.getResult() != null) {
                                ShopOrder shopOrder = shopOrderResponse.getResult();
                                Long id = shopOrder.getId();
                                //查询发货单
                                List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(id);
                                if (shipments != null && shipments.size() > 0) {
                                    //筛选状态大于0的
                                    List<Shipment> availiableShipments = shipments.stream().filter(s -> s.getStatus() > 0).collect(Collectors.toList());
                                    if (availiableShipments.size() > 0) {
                                        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItemsForList(availiableShipments);
                                        if (shipmentItems != null && shipmentItems.size() > 0) {
                                            List<ShipmentItem> availiableShipmentItems = shipmentItems.stream().filter(s -> {
                                                int remainQuantity = s.getQuantity() - s.getRefundQuantity();
                                                if (s.getStatus() > 0 && remainQuantity > 0) {
                                                    return true;
                                                }
                                                return false;
                                            }).collect(Collectors.toList());
                                            if (availiableShipmentItems.size() > 0) {
                                                ShipmentItem bestShipmentItem = getBestShipmentItem(availiableShipmentItems, Integer.valueOf(fileImportExcelBean.getQuantity()), fileImportExcelBean.getOutSkuCode());
                                                log.info("item信息:{}", bestShipmentItem);
                                                if (bestShipmentItem != null) {
                                                    Long shipmentId = bestShipmentItem.getShipmentId();
                                                    Shipment targetShipment = null;
                                                    for (Shipment shipment : availiableShipments) {
                                                        if (Objects.equals(shipmentId, shipment.getId())) {
                                                            targetShipment = shipment;
                                                            break;
                                                        }
                                                    }
                                                    if (targetShipment != null) {
                                                        SubmitRefundInfo submitRefundInfo = new SubmitRefundInfo();
                                                        submitRefundInfo.setOrderCode(shopOrder.getOrderCode());
                                                        submitRefundInfo.setRefundType(MiddleRefundType.AFTER_SALES_RETURN.value());
                                                        submitRefundInfo.setShipmentCode(targetShipment.getShipmentCode());
                                                        submitRefundInfo.setReleOrderNo(shopOrder.getOrderCode());

                                                        submitRefundInfo.setShipmentCorpName(fileImportExcelBean.getLogisticsCompany());
                                                        submitRefundInfo.setShipmentSerialNo(fileImportExcelBean.getTrackingNumber());
                                                        //子单
                                                        submitRefundInfo.setReleOrderType(1);
                                                        //操作方式是保存，状态就会是待处理
                                                        submitRefundInfo.setOperationType(1);
                                                        //添加售后单sku信息
                                                        List<EditSubmitRefundItem> editSubmitRefundItems = Lists.newArrayList();
                                                        EditSubmitRefundItem item = new EditSubmitRefundItem();
                                                        item.setRefundSkuCode(fileImportExcelBean.getOutSkuCode());
                                                        item.setRefundQuantity(Integer.valueOf(fileImportExcelBean.getQuantity()));
                                                        //设置退货仓库
                                                        Long shopId = shopOrder.getShopId();
                                                        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopId);
                                                        Map<String, String> extra = openShop.getExtra();
                                                        String defaultReWarehouseId = extra.get(TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID);
                                                        if (! StringUtils.isEmpty(defaultReWarehouseId)) {
                                                            submitRefundInfo.setWarehouseId(Long.valueOf(defaultReWarehouseId));
                                                        }

                                                        item.setSkuOrderId(bestShipmentItem.getSkuOrderId());
                                                        item.setRefundOutSkuCode(bestShipmentItem.getOutSkuCode());
                                                        Long cleanFee = Long.valueOf(bestShipmentItem.getCleanFee());
                                                        Long refundFee = cleanFee/bestShipmentItem.getQuantity();
                                                        item.setFee(refundFee*item.getRefundQuantity());

                                                        submitRefundInfo.setFee(item.getFee());
                                                        editSubmitRefundItems.add(item);
                                                        submitRefundInfo.setEditSubmitRefundItems(editSubmitRefundItems);

                                                        refundWriteLogic.createRefund(submitRefundInfo);
                                                    } else {
                                                        //没有查找到对应的发货单
                                                        errorMsg = "没有查找到对应的发货单";
                                                        log.error("没有查找到对应的发货单, 外部订单号:{}",fileImportExcelBean.getOutOrderNumber());
                                                    }
                                                } else {
                                                    //没有找到该外部skuCode对应的发货商品
                                                    errorMsg = "没有找到该外部skuCode对应的发货商品";
                                                    log.error("没有找到该外部skuCode对应的发货商品, 外部订单号:{}, outSkuCode:{}",fileImportExcelBean.getOutOrderNumber(), fileImportExcelBean.getOutSkuCode());
                                                }
                                            } else {
                                                //没有查询到有效的发货单明细
                                                log.error("没有查询到有效的发货单明细, 外部订单号:{}",fileImportExcelBean.getOutOrderNumber());
                                                FaildExcelBean faildExcelBean = toFaildExcelBean(wrapper);
                                                faildExcelBean.setFaildReason("申请数量非法");
                                                errorData.add(faildExcelBean);
                                                continue;
                                            }
                                        } else {
                                            //没有查询到发货单明细
                                            errorMsg = "没有查询到发货单明细";
                                            log.error("没有查询到发货单明细, 外部订单号:{}",fileImportExcelBean.getOutOrderNumber());
                                        }
                                    } else {
                                        //没有查询到有效的发货单
                                        errorMsg = "没有查询到有效的发货单";
                                        log.error("没有查询到有效的发货单, 外部订单号:{}",fileImportExcelBean.getOutOrderNumber());
                                    }
                                } else {
                                    //没有查询到发货单
                                    errorMsg = "没有查询到发货单";
                                    log.error("没有查询到发货单, 外部订单号:{}",fileImportExcelBean.getOutOrderNumber());
                                }
                            } else {
                                //订单查询失败
                                errorMsg = "订单查询失败";
                                log.error("订单查询失败, 外部订单号:{}",fileImportExcelBean.getOutOrderNumber());
                            }
                            if (! StringUtils.isEmpty(errorMsg)) {
                                FaildExcelBean faildExcelBean = toFaildExcelBean(wrapper);
                                faildExcelBean.setFaildReason("导入的参数错误,请核验");
                                errorData.add(faildExcelBean);
                            }
                        }
                    } catch (Exception e) {
                        //将数据加入到错误日志excel
                        log.error("处理导入售后单出错,外部交易单号:{}", fileImportExcelBean.getOutOrderNumber());
                        e.printStackTrace();
                        String message = getMessage(e.getMessage());
                        FaildExcelBean faildExcelBean = toFaildExcelBean(wrapper);
                        faildExcelBean.setFaildReason(message);
                        errorData.add(faildExcelBean);
                    }

                }
            }

            //如果不可用数据不为空，写入错误日志excel
            if (errorData.size() > 0) {
                errorExcel.appendToExcel(errorData);
                File file = errorExcel.transformToFile();
                poushengCompensateBiz.setLastFailedReason("导入出错");
                String abnormalUrl = uploadFileComponent.exportAbnormalRecord(file);
                poushengCompensateBiz.setLastFailedReason(abnormalUrl);
            }

        } catch (Exception e) {
            String message = getMessage(e.getMessage());
            log.error("处理售后单导入出错, 任务ID:{}, 错误:{}", bizID, message);
            e.printStackTrace();
            poushengCompensateBiz.setLastFailedReason(message);
        }
        return poushengCompensateBiz;
    }


    /**
     * 获取国际化信息
     * @param msgCode
     * @return
     */
    public String getMessage(String msgCode){
        String message = null;
        try {
            message = messageSource.getMessage(msgCode, null, Locale.CHINA);
            if (StringUtils.isEmpty(message)) {
                return msgCode;
            }
        } catch (Exception e) {
            message = msgCode;
        }
        return message;
    }

    public FaildExcelBean toFaildExcelBean(FileImportExcelBeanWrapper wrapper){
        FileImportExcelBean fileImportExcelBean = wrapper.getFileImportExcelBean();
        FaildExcelBean faildExcelBean = new FaildExcelBean();
        faildExcelBean.setLogisticsCompany(fileImportExcelBean.getLogisticsCompany());
        faildExcelBean.setTrackingNumber(fileImportExcelBean.getTrackingNumber());
        faildExcelBean.setOutSkuCode(fileImportExcelBean.getOutSkuCode());
        faildExcelBean.setFaildReason(wrapper.getErrorMsg());
        faildExcelBean.setOutOrderNumber(fileImportExcelBean.getOutOrderNumber());
        faildExcelBean.setQuantity(fileImportExcelBean.getQuantity());
        faildExcelBean.setType(fileImportExcelBean.getType());
        return faildExcelBean;
    }

    //挑选数量最小且满足退货要求的
    public ShipmentItem getBestShipmentItem(List<ShipmentItem> shipmentItems, int quantity, String outSkuCode){
        if (shipmentItems != null && shipmentItems.size() > 0) {
            List<ShipmentItem> collectItems = Lists.newArrayList();
            for (ShipmentItem item : shipmentItems) {
                if (outSkuCode.equals(item.getOutSkuCode())) {
                    collectItems.add(item);
                }
            }
            if (collectItems.size() > 0) {
                Collections.sort(collectItems, Comparator.comparing(ShipmentItem::getQuantity));
                for (ShipmentItem item : collectItems) {
                    if (item.getQuantity() >= quantity) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

}
