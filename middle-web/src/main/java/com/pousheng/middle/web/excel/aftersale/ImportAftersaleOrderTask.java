package com.pousheng.middle.web.excel.aftersale;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.common.utils.batchhandle.ItemSupplyRuleAbnormalRecord;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.EditSubmitRefundItem;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.SubmitRefundInfo;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.utils.JsonMapper;
import io.terminus.excel.model.ExcelReadErrorMsgModel;
import io.terminus.excel.read.ExcelReader;
import io.terminus.excel.read.ExcelReaderFactory;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    private OrderReadLogic orderReadLogic;

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

            //排重
            Map<String, String> filter = Maps.newHashMap();
            //创建售后单，并分析出可用数据和不可用数据
            if (data.size() > 0) {
                for(FileImportExcelBeanWrapper wrapper : data) {
                    FileImportExcelBean fileImportExcelBean = wrapper.getFileImportExcelBean();
                    //当前数据行有错误
                    String key = fileImportExcelBean.getShipmentOrderNumber() + fileImportExcelBean.getBarCode();
                    if (wrapper.isHasError()) {
                        FaildExcelBean faildExcelBean = toFaildExcelBean(wrapper);
                        errorData.add(faildExcelBean);
                    } else if(!StringUtils.isEmpty(filter.get(key))) {
                        //重复sku，报错，手动合并
                        FaildExcelBean faildExcelBean = toFaildExcelBean(wrapper);
                        faildExcelBean.setFaildReason("同一商品重复申请，请合并数量");
                        errorData.add(faildExcelBean);
                    } else {
                        SubmitRefundInfo submitRefundInfo = new SubmitRefundInfo();
                        submitRefundInfo.setOrderCode(fileImportExcelBean.getOrderNumber());
                        submitRefundInfo.setRefundType(MiddleRefundType.AFTER_SALES_RETURN.value());
                        submitRefundInfo.setShipmentCode(fileImportExcelBean.getShipmentOrderNumber());
                        submitRefundInfo.setReleOrderNo(fileImportExcelBean.getOrderNumber());
                        //子单
                        submitRefundInfo.setReleOrderType(2);
                        //操作方式是保存，状态就会是待处理
                        submitRefundInfo.setOperationType(1);
                        //添加售后单sku信息
                        List<EditSubmitRefundItem> editSubmitRefundItems = Lists.newArrayList();
                        EditSubmitRefundItem item = new EditSubmitRefundItem();
                        item.setRefundSkuCode(fileImportExcelBean.getBarCode());
                        item.setRefundQuantity(fileImportExcelBean.getQuantity());
                        //设置退货仓库
                        ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(fileImportExcelBean.getOrderNumber());
                        Long shopId = shopOrder.getShopId();
                        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopId);
                        Map<String, String> extra = openShop.getExtra();
                        String defaultReWarehouseId = extra.get("companyCode");
                        if (! StringUtils.isEmpty(defaultReWarehouseId)) {
                            submitRefundInfo.setWarehouseId(Long.valueOf(defaultReWarehouseId));
                        }

                        Shipment shipments = shipmentReadLogic.findShipmentByShipmentCode(fileImportExcelBean.getShipmentOrderNumber());


                        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipments);
                        if (shipmentItems != null) {
                            for (ShipmentItem shipmentItem : shipmentItems) {
                                if (fileImportExcelBean.getBarCode().equals(shipmentItem.getSkuCode())) {
                                    item.setSkuOrderId(shipmentItem.getSkuOrderId());
                                    item.setRefundOutSkuCode(shipmentItem.getOutSkuCode());
                                    Long cleanFee = Long.valueOf(shipmentItem.getCleanFee());
                                    Long refundFee = cleanFee/shipmentItem.getQuantity();
                                    item.setFee(refundFee);
                                }
                            }
                        }
                        submitRefundInfo.setFee(item.getFee());
                        editSubmitRefundItems.add(item);
                        submitRefundInfo.setEditSubmitRefundItems(editSubmitRefundItems);

                        filter.put(key, "1");
                        try {
                            refundWriteLogic.createRefund(submitRefundInfo);
                        } catch (Exception e) {
                            //将数据加入到错误日志excel
                            String msgCode = e.getMessage();
                            log.error("处理导入售后单出错,交易单号:{}, {}", fileImportExcelBean.getOrderNumber(), msgCode);
                            String message = getMessage(msgCode);
                            FaildExcelBean faildExcelBean = toFaildExcelBean(wrapper);
                            faildExcelBean.setFaildReason(message);
                            errorData.add(faildExcelBean);
                        }

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
            log.error("处理售后单导入出错, 任务ID:{}", bizID);
            e.printStackTrace();
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
        faildExcelBean.setBarCode(fileImportExcelBean.getBarCode());
        faildExcelBean.setFaildReason(wrapper.getErrorMsg());
        faildExcelBean.setOrderNumber(fileImportExcelBean.getOrderNumber());
        faildExcelBean.setQuantity(fileImportExcelBean.getQuantity());
        faildExcelBean.setShipmentOrderNumber(fileImportExcelBean.getShipmentOrderNumber());
        faildExcelBean.setType(fileImportExcelBean.getType());
        return faildExcelBean;
    }

}
