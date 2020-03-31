package com.pousheng.middle.web.export;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.ReverseLogisticExtraKeyConstant;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessDto;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseLogisticTime;
import com.pousheng.middle.order.dto.reverseLogistic.WaybillItemDto;
import com.pousheng.middle.order.enums.HeadlessProcessTypeEnum;
import com.pousheng.middle.order.enums.ReverseHeadlessStatusEnum;
import com.pousheng.middle.order.model.ReverseHeadlessInfo;
import io.terminus.common.utils.JsonMapper;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bernie
 * @date 2019/6/6
 */
public class HeadlessInfoConvert {

    public static ReverseHeadExportEntity convertHeadLessDtoToExport(ReverseHeadlessDto reverseHeadlessDto) {

        ReverseHeadExportEntity exportEntity = new ReverseHeadExportEntity();
        exportEntity.setCustomer(reverseHeadlessDto.getCustomer());
        exportEntity.setHeadlessNo(reverseHeadlessDto.getHeadlessNo());
        exportEntity.setExpressNo(reverseHeadlessDto.getExpressNo());
        exportEntity.setChannel(reverseHeadlessDto.getChannel());
        exportEntity.setShop(reverseHeadlessDto.getShop());
        exportEntity.setInventoryProfitNo(reverseHeadlessDto.getInventoryProfitNo());
        exportEntity.setPhone(reverseHeadlessDto.getPhone());
        exportEntity.setReason(reverseHeadlessDto.getReason());
        exportEntity.setShipCompany(reverseHeadlessDto.getShipCompany());
        exportEntity.setShipExpressNo(reverseHeadlessDto.getShipExpressNo());
        exportEntity.setRelateAsn(reverseHeadlessDto.getRelateAsn());
        exportEntity.setPlatformNo(reverseHeadlessDto.getPlatformNo());
        exportEntity.setShipMode(reverseHeadlessDto.getShipMode());
        exportEntity.setStatus(ReverseHeadlessStatusEnum.fromDesc(reverseHeadlessDto.getStatus()).name());
        exportEntity.setShipExpressNo(reverseHeadlessDto.getExpressNo());
        exportEntity.setInventoryProfitNo(reverseHeadlessDto.getInventoryProfitNo());
        exportEntity.setPlatformNo(reverseHeadlessDto.getPlatformNo());
        exportEntity.setUniqueNo(reverseHeadlessDto.getUniqueNo());
        ReverseLogisticTime logisticTime = reverseHeadlessDto.getTimeInfo();
        if (Objects.nonNull(logisticTime)) {
            exportEntity.setAsnCreateAt(logisticTime.getAsnCreateAt());
            exportEntity.setCreatedAt(logisticTime.getCreatedAt());
            exportEntity.setCloseAt(logisticTime.getCloseAt());
        }
        WaybillItemDto waybillItemDto = reverseHeadlessDto.getWaybillItemDto();
        if (Objects.nonNull(waybillItemDto)) {
            exportEntity.setGoodsNo(waybillItemDto.getGoodsNo());
            exportEntity.setLeftSize(waybillItemDto.getLeftSize());
            exportEntity.setRightSize(waybillItemDto.getRightSize());
            exportEntity.setWeight(waybillItemDto.getWeight());
            exportEntity.setShoeboxInfo(waybillItemDto.getShoeboxInfo());
            exportEntity.setSize(waybillItemDto.getSize());
            exportEntity.setSkuNo(waybillItemDto.getSkuNo());
            exportEntity.setMaterialInfo(waybillItemDto.getMaterialInfo());
            exportEntity.setQuantity(waybillItemDto.getInstoreQuantity());
        }

        return exportEntity;
    }


}
