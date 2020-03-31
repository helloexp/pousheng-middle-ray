package com.pousheng.middle.web.biz.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.PoushengSettlementPosCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.search.dto.StockSendCriteria;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.biz.dto.ExportTradeBillDTO;
import com.pousheng.middle.web.events.trade.ExportTradeBillEvent;
import com.pousheng.middle.web.events.trade.listener.ExportTradeBillListener;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

/**
 * Export Trade Bill
 *
 * @author tanlongjun
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.EXPORT_TRADE_BILL)
@Service
@Slf4j
public class ExportTradeBillService implements CompensateBizService {

    @Autowired
    private ExportTradeBillListener exportTradeBillListener;

    private static final JsonMapper MAPPER = JsonMapper.JSON_NON_EMPTY_MAPPER;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("JITUnlockStockService.doProcess params is null");
            return;
        }

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("ExportTradeBillService context is null");
            throw new BizException("ExportTradeBillService context is null");
        }
        //转换数据对象
        ExportTradeBillEvent event = convert(poushengCompensateBiz);
        exportTradeBillListener.onExportTradeBill(event);
    }

    /**
     * 转换数据对象
     *
     * @param data
     * @return
     * @throws IOException
     */
    protected ExportTradeBillEvent convert(PoushengCompensateBiz data) {
        TypeReference typeReference = null;
        switch (data.getBizId()) {
            case TradeConstants.EXPORT_ORDER:
                typeReference = new TypeReference<ExportTradeBillDTO<MiddleOrderCriteria>>() {};
                break;
            case TradeConstants.EXPORT_REFUND:
                typeReference = new TypeReference<ExportTradeBillDTO<MiddleRefundCriteria>>() {};
                break;
            case TradeConstants.EXPORT_SHIPMENT:
                typeReference = new TypeReference<ExportTradeBillDTO<OrderShipmentCriteria>>() {};
                break;
            case TradeConstants.EXPORT_POS:
                typeReference = new TypeReference<ExportTradeBillDTO<PoushengSettlementPosCriteria>>() {};
                break;
            case TradeConstants.EXPORT_STOCK_SEND:
                typeReference = new TypeReference<ExportTradeBillDTO<StockSendCriteria>>() {};
                break;
            case TradeConstants.EXPORT_REVERSE_HEADLESS:
                typeReference = new TypeReference<ExportTradeBillDTO<ReverseHeadlessCriteria>>() {
                };
                break;

            default:
                break;
        }

        if (Objects.isNull(typeReference)) {
            log.warn("not support type of export trade bill.type:{}", data.getBizId());
            return null;
        }
        ExportTradeBillDTO dto = null;
        try {
            dto = MAPPER.getMapper().readValue(data.getContext(),
                    typeReference);
        } catch (IOException e) {
            log.error("failed to read json of export trade bill.param:{}", data.getContext(), e);
            return null;
        }

        if (Objects.isNull(dto)) {
            log.warn("json convert result is null.skip.biz content:{}", data.getContext());
            return null;
        }
        ExportTradeBillEvent result = new ExportTradeBillEvent();
        result.setType(data.getBizId());
        result.setUserId(dto.getUserId());
        result.setCriteria(dto.getCriteria());
        return result;
    }

}
