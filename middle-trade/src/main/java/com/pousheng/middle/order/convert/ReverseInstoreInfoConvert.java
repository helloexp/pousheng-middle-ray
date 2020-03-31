package com.pousheng.middle.order.convert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.ReverseLogisticExtraKeyConstant;
import com.pousheng.middle.order.dto.reverseLogistic.*;
import com.pousheng.middle.order.enums.ReverseInstoreStatusEnum;
import com.pousheng.middle.order.model.ReverseExpressInfo;
import com.pousheng.middle.order.model.ReverseHeadlessInfo;
import com.pousheng.middle.order.model.ReverseInstoreInfo;
import io.terminus.common.utils.JsonMapper;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bernie
 * @date 2019/6/5 逆向物流转货类
 */
public class ReverseInstoreInfoConvert {


    public static ReverseInstoreInfo convertDtoToDomain(ReverseInstoreDto reverseInstoreDto,
                                                  ReverseInstoreInfo reverseInstoreInfo) {
        if (Objects.isNull(reverseInstoreInfo)) {
            reverseInstoreInfo = new ReverseInstoreInfo();
        }
        reverseInstoreInfo.setAccomplishAt(reverseInstoreDto.getTimeInfo().getAccomplishAt());
        reverseInstoreInfo.setCarrierExpressNo(reverseInstoreDto.getCarrierExpressNo());
        reverseInstoreInfo.setRealExpressNo(reverseInstoreDto.getRealExpressNo());
        reverseInstoreInfo.setChannel(reverseInstoreDto.getChannel());
        reverseInstoreInfo.setShop(reverseInstoreDto.getShop());
        reverseInstoreInfo.setStatus(ReverseInstoreStatusEnum.fromDesc(reverseInstoreDto.getStatus()).name() );
        reverseInstoreInfo.setErpNo(reverseInstoreDto.getErpNo());
        reverseInstoreInfo.setInFlowAt(reverseInstoreDto.getTimeInfo().getInFlowAt());
        reverseInstoreInfo.setInstoreDetailNo(reverseInstoreDto.getInstoreDetailNo());
        reverseInstoreInfo.setInstoreNo(reverseInstoreDto.getInstoreNo());
        reverseInstoreInfo.setPlatformNo(reverseInstoreDto.getPlatformNo());
        reverseInstoreInfo.setCustomerNote(reverseInstoreDto.getCustomerNote());
        reverseInstoreInfo.setArriveWmsAt(reverseInstoreDto.getTimeInfo().getArriveWmsDate());
        Map<String, String> extra = reverseInstoreInfo.getExtra();
        if (Objects.isNull(extra)) {
            extra = Maps.newHashMap();
        }
        if (Objects.nonNull(reverseInstoreDto.getWaybillItemDto())) {
            extra.put(ReverseLogisticExtraKeyConstant.REVERSE_LOGISTIC_ITEM,
                JsonMapper.nonEmptyMapper().toJson(reverseInstoreDto.getWaybillItemDto()));
        }
        extra.put(ReverseLogisticExtraKeyConstant.REVERSE_INSTORE_ANOMALY_BIG, reverseInstoreDto.getRtxAnomalyBig());
        extra.put(ReverseLogisticExtraKeyConstant.REVERSE_INSTORE_ANOMALY_SMALL,
            reverseInstoreDto.getRtxAnomalySmall());
        extra.put(ReverseLogisticExtraKeyConstant.EXPRESS_CREATED_BY, reverseInstoreDto.getCreatedBy());
        reverseInstoreInfo.setExtra(extra);
        return reverseInstoreInfo;
    }


    public static List<ReverseInstoreInfo> convertDtoToDomainList(List<ReverseInstoreDto> reverseInstoreDtoList) {

        if (CollectionUtils.isEmpty(reverseInstoreDtoList)) {
            return null;
        }
        List<ReverseInstoreInfo> reverseInstoreInfoList= Lists.newArrayList();
        for(ReverseInstoreDto reverseInstoreDto:reverseInstoreDtoList){
            reverseInstoreInfoList.add(convertDtoToDomain(reverseInstoreDto,null));
        }
        return reverseInstoreInfoList;

    }

    public static ReverseInstoreDto convertDomainToDto(ReverseInstoreInfo reverseInstoreInfo) {

        ReverseInstoreDto reverseInstoreDto = new ReverseInstoreDto();
        reverseInstoreDto.setCarrierExpressNo(reverseInstoreInfo.getCarrierExpressNo());
        reverseInstoreDto.setRealExpressNo(reverseInstoreInfo.getRealExpressNo());
        reverseInstoreDto.setChannel(reverseInstoreInfo.getChannel());
        reverseInstoreDto.setShop(reverseInstoreInfo.getShop());
        reverseInstoreDto.setStatus(ReverseInstoreStatusEnum.fromName(reverseInstoreInfo.getStatus()).getDesc());
        reverseInstoreDto.setErpNo(reverseInstoreInfo.getErpNo());
        reverseInstoreDto.setInstoreDetailNo(reverseInstoreInfo.getInstoreDetailNo());
        reverseInstoreDto.setInstoreNo(reverseInstoreInfo.getInstoreNo());
        reverseInstoreDto.setPlatformNo(reverseInstoreInfo.getPlatformNo());
        ReverseLogisticTime reverseLogisticTime = new ReverseLogisticTime();
        reverseLogisticTime.setAccomplishAt(reverseInstoreInfo.getAccomplishAt());
        reverseLogisticTime.setInFlowAt(reverseInstoreInfo.getInFlowAt());
        reverseLogisticTime.setArriveWmsDate(reverseInstoreInfo.getArriveWmsAt());
        reverseInstoreDto.setCustomerNote(reverseInstoreInfo.getCustomerNote());

        Map<String, String> extra = reverseInstoreInfo.getExtra();
        if (!CollectionUtils.isEmpty(extra)) {
            if (extra.containsKey(ReverseLogisticExtraKeyConstant.REVERSE_LOGISTIC_ITEM)) {
                reverseInstoreDto.setWaybillItemDto(JsonMapper.nonEmptyMapper()
                    .fromJson(extra.get(ReverseLogisticExtraKeyConstant.REVERSE_LOGISTIC_ITEM),
                        WaybillItemDto.class));
            }
            reverseInstoreDto.setRtxAnomalyBig(extra.get(ReverseLogisticExtraKeyConstant.REVERSE_INSTORE_ANOMALY_BIG));
            reverseInstoreDto.setRtxAnomalySmall(
                extra.get(ReverseLogisticExtraKeyConstant.REVERSE_INSTORE_ANOMALY_SMALL));
            reverseInstoreDto.setCreatedBy(extra.get(ReverseLogisticExtraKeyConstant.EXPRESS_CREATED_BY));
        }
        reverseInstoreDto.setTimeInfo(reverseLogisticTime);

        return reverseInstoreDto;
    }

    public static List<ReverseInstoreDto> convertInstoreListDoToDto(List<ReverseInstoreInfo> instoreInfoList) {

        if (CollectionUtils.isEmpty(instoreInfoList)) {
            return null;
        }
        return instoreInfoList.stream().map(ReverseInstoreInfoConvert::convertDomainToDto).collect(Collectors.toList());

    }

}
