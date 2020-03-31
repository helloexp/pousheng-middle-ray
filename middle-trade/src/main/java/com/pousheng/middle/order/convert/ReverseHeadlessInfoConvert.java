package com.pousheng.middle.order.convert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.ReverseLogisticExtraKeyConstant;
import com.pousheng.middle.order.dto.reverseLogistic.*;
import com.pousheng.middle.order.enums.HeadlessProcessTypeEnum;
import com.pousheng.middle.order.enums.ReverseHeadlessStatusEnum;
import com.pousheng.middle.order.model.ReverseExpressInfo;
import com.pousheng.middle.order.model.ReverseHeadlessInfo;
import io.terminus.common.utils.JsonMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bernie
 * @date 2019/6/6
 */
public class ReverseHeadlessInfoConvert {

    public static ReverseHeadlessInfo convertHeadLessDtoToDo(ReverseHeadlessDto reverseHeadlessDto,
                                                             ReverseHeadlessInfo reverseHeadlessInfo) {
        if (reverseHeadlessInfo == null) {
            reverseHeadlessInfo = new ReverseHeadlessInfo();
        }
        reverseHeadlessInfo.setCustomer(reverseHeadlessDto.getCustomer());
        reverseHeadlessInfo.setHeadlessNo(reverseHeadlessDto.getHeadlessNo());
        reverseHeadlessInfo.setExpressNo(reverseHeadlessDto.getExpressNo());
        reverseHeadlessInfo.setChannel(reverseHeadlessDto.getChannel());
        reverseHeadlessInfo.setShop(reverseHeadlessDto.getShop());
        reverseHeadlessInfo.setInventoryProfitNo(reverseHeadlessDto.getInventoryProfitNo());
        reverseHeadlessInfo.setPhone(reverseHeadlessDto.getPhone());
        reverseHeadlessInfo.setReason(reverseHeadlessDto.getReason());
        reverseHeadlessInfo.setShipCompany(reverseHeadlessDto.getShipCompany());
        reverseHeadlessInfo.setShipExpressNo(reverseHeadlessDto.getShipExpressNo());
        reverseHeadlessInfo.setRelateAsn(reverseHeadlessDto.getRelateAsn());
        reverseHeadlessInfo.setPlatformNo(reverseHeadlessDto.getPlatformNo());
        reverseHeadlessInfo.setShipMode(reverseHeadlessDto.getShipMode());
        reverseHeadlessInfo.setStatus(ReverseHeadlessStatusEnum.fromDesc(reverseHeadlessDto.getStatus()).name());
        reverseHeadlessInfo.setInventoryProfitNo(reverseHeadlessDto.getInventoryProfitNo());
        reverseHeadlessInfo.setPlatformNo(reverseHeadlessDto.getPlatformNo());
        reverseHeadlessInfo.setUniqueNo(reverseHeadlessDto.getUniqueNo());
        reverseHeadlessInfo.setProcessType(matchHeadLessProcessType(reverseHeadlessInfo).name());
        Map<String, String> extra = reverseHeadlessInfo.getExtra();

        if (Objects.isNull(extra)) {
            extra = Maps.newHashMap();
        }
        if (Objects.nonNull(reverseHeadlessDto.getWaybillItemDto())) {
            extra.put(ReverseLogisticExtraKeyConstant.REVERSE_LOGISTIC_ITEM,
                JsonMapper.nonEmptyMapper().toJson(reverseHeadlessDto.getWaybillItemDto()));

        }
        ReverseLogisticTime logisticTime = reverseHeadlessDto.getTimeInfo();
        if (Objects.nonNull(logisticTime)) {
            if (Objects.nonNull(logisticTime.getCreatedAt())) {
                extra.put(ReverseLogisticExtraKeyConstant.EXPRESS_CREATED_AT,
                    String.valueOf(logisticTime.getCreatedAt().getTime()));
            }
            if (Objects.nonNull(logisticTime.getAsnCreateAt())) {
                extra.put(ReverseLogisticExtraKeyConstant.HEADLESS_ASN_CREATED_AT,
                    String.valueOf(logisticTime.getCreatedAt().getTime()));
            }
            if (Objects.nonNull(logisticTime.getConfirmReceiveDate())) {
                extra.put(ReverseLogisticExtraKeyConstant.CONFRIM_AT,
                    String.valueOf(logisticTime.getCreatedAt().getTime()));
            }
            if (Objects.nonNull(logisticTime.getCloseAt())) {
                reverseHeadlessInfo.setClosedAt(logisticTime.getCloseAt());
                extra.put(ReverseLogisticExtraKeyConstant.HEADLESS_CLOSE_AT,
                    String.valueOf(logisticTime.getCloseAt().getTime()));
            }


        }
        reverseHeadlessInfo.setExtra(extra);
        return reverseHeadlessInfo;
    }


    public static List<ReverseHeadlessInfo> convertHeadLessDtoToDoList(List<ReverseHeadlessDto> reverseHeadlessDtoList) {

        if (CollectionUtils.isEmpty(reverseHeadlessDtoList)) {
            return null;
        }
        List<ReverseHeadlessInfo> reverseHeadlessInfoList= Lists.newArrayList();
        for(ReverseHeadlessDto reverseHeadlessDto:reverseHeadlessDtoList){
            reverseHeadlessInfoList.add(convertHeadLessDtoToDo(reverseHeadlessDto,null));
        }
        return reverseHeadlessInfoList;

    }

    public static HeadlessProcessTypeEnum matchHeadLessProcessType(ReverseHeadlessInfo reverseHeadlessInfo) {

        if (Objects.equals(reverseHeadlessInfo.getStatus(), ReverseHeadlessStatusEnum.RECEIVE_ACCOMPLISH.name())) {
            return HeadlessProcessTypeEnum.INIT;
        }
        if (!StringUtils.isEmpty(reverseHeadlessInfo.getInventoryProfitNo())) {
            return HeadlessProcessTypeEnum.PROFIT;
        }
        if (!StringUtils.isEmpty(reverseHeadlessInfo.getShipMode()) ||!StringUtils.isEmpty(reverseHeadlessInfo.getShipExpressNo())||!StringUtils.isEmpty(reverseHeadlessInfo.getShipCompany())) {
            return HeadlessProcessTypeEnum.REFUSE;
        }
        return HeadlessProcessTypeEnum.NORMAL;
    }

    public static List<ReverseHeadlessDto> convertHeadLessListDoToDto(
        List<ReverseHeadlessInfo> reverseHeadlessInfoList) {

        if (CollectionUtils.isEmpty(reverseHeadlessInfoList)) {
            return null;
        }
        return reverseHeadlessInfoList.stream().map(ReverseHeadlessInfoConvert::convertHeadlessDoToDTo).collect(
            Collectors.toList());
    }

    public static ReverseHeadlessDto convertHeadlessDoToDTo(ReverseHeadlessInfo reverseHeadlessInfo) {

        ReverseHeadlessDto reverseHeadlessDto = ReverseHeadlessDto.builder()
            .customer(reverseHeadlessInfo.getCustomer())
            .expressNo(reverseHeadlessInfo.getExpressNo())
            .headlessNo(reverseHeadlessInfo.getHeadlessNo())
            .inventoryProfitNo(reverseHeadlessInfo.getInventoryProfitNo())
            .phone(reverseHeadlessInfo.getPhone())
            .platformNo(reverseHeadlessInfo.getPlatformNo())
            .reason(reverseHeadlessInfo.getReason())
            .relateAsn(reverseHeadlessInfo.getRelateAsn())
            .shipCompany(reverseHeadlessInfo.getShipCompany())
            .shipExpressNo(reverseHeadlessInfo.getShipExpressNo())
            .shipMode(reverseHeadlessInfo.getShipMode())
            .uniqueNo(reverseHeadlessInfo.getUniqueNo())
            .build();

        if (reverseHeadlessInfo.getExtra() != null) {
            Map<String, String> extra = reverseHeadlessInfo.getExtra();
            if (Objects.nonNull(
                extra.get(ReverseLogisticExtraKeyConstant.REVERSE_LOGISTIC_ITEM))) {
                reverseHeadlessDto.setWaybillItemDto(JsonMapper.nonEmptyMapper()
                    .fromJson(extra.get(ReverseLogisticExtraKeyConstant.REVERSE_LOGISTIC_ITEM),
                        WaybillItemDto.class));
            }

            ReverseLogisticTime reverseLogisticTime = new ReverseLogisticTime();

            if (extra.containsKey(ReverseLogisticExtraKeyConstant.EXPRESS_CREATED_AT)) {
                reverseLogisticTime.setCreatedAt(
                    new Date(Long.parseLong(extra.get(ReverseLogisticExtraKeyConstant.EXPRESS_CREATED_AT))));

            }
            if (extra.containsKey(ReverseLogisticExtraKeyConstant.HEADLESS_ASN_CREATED_AT)) {
                reverseLogisticTime.setAsnCreateAt(
                    new Date(Long.parseLong(extra.get(ReverseLogisticExtraKeyConstant.HEADLESS_ASN_CREATED_AT))));

            }
            if (extra.containsKey(ReverseLogisticExtraKeyConstant.CONFRIM_AT)) {
                reverseLogisticTime.setConfirmReceiveDate(
                    new Date(Long.parseLong(extra.get(ReverseLogisticExtraKeyConstant.CONFRIM_AT))));

            }
            if (extra.containsKey(ReverseLogisticExtraKeyConstant.HEADLESS_CLOSE_AT)) {
                reverseLogisticTime.setCloseAt(
                    new Date(Long.parseLong(extra.get(ReverseLogisticExtraKeyConstant.HEADLESS_CLOSE_AT))));

            }
            reverseHeadlessDto.setTimeInfo(reverseLogisticTime);

        }
        reverseHeadlessDto.setStatus(ReverseHeadlessStatusEnum.fromName(reverseHeadlessInfo.getStatus()).getDesc());
        reverseHeadlessDto.setChannel(reverseHeadlessInfo.getChannel());
        reverseHeadlessDto.setShop(reverseHeadlessInfo.getShop());
        return reverseHeadlessDto;

    }
}
