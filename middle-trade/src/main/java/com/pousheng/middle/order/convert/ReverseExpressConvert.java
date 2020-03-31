package com.pousheng.middle.order.convert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.ReverseLogisticExtraKeyConstant;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseExpressInfoDto;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseLogisticTime;
import com.pousheng.middle.order.dto.reverseLogistic.SenderDto;
import com.pousheng.middle.order.dto.reverseLogistic.WaybillDto;
import com.pousheng.middle.order.model.ReverseExpressInfo;
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
public class ReverseExpressConvert {

    public static ReverseExpressInfo convertExpressDtoToDo(ReverseExpressInfoDto expressDto,
                                                           ReverseExpressInfo expressInfo) {
        if (expressInfo == null) {
            expressInfo = new ReverseExpressInfo();
        }
        expressInfo.setBuyerMemo(expressDto.getWaybillDto().getBuyerMemo());
        expressInfo.setCarrierCode(expressDto.getWaybillDto().getCarrierCode());
        expressInfo.setCarrierName(expressDto.getWaybillDto().getCarrierName());
        expressInfo.setChannel(expressDto.getChannel());
        expressInfo.setShop(expressDto.getShop());
        expressInfo.setHasOrder(expressDto.getHasOrder());
        expressInfo.setInstoreNo(expressDto.getInstoreNo());
        expressInfo.setLineNo(expressDto.getLineNo());
        expressInfo.setPaidAfterDelivery(expressDto.getWaybillDto().getPaidAfterDelivery());
        expressInfo.setSenderMobile(expressDto.getWaybillDto().getSenderInfo().getSenderMobile());
        expressInfo.setSenderName(expressDto.getWaybillDto().getSenderInfo().getSenderName());
        expressInfo.setExpressNo(expressDto.getWaybillDto().getExpressNo());
        expressInfo.setStatus(expressDto.getStatus());
        expressInfo.setTransferOrderId(expressDto.getTransferOrderId());
        expressInfo.setOutCreatedAt(expressDto.getTimeInfo().getCreatedAt());

        Map<String, String> extra = Maps.newHashMap();
        extra.put(ReverseLogisticExtraKeyConstant.EXPRESS_SENDER_INFO,
            JsonMapper.nonEmptyMapper().toJson(expressDto.getWaybillDto().getSenderInfo()));
        extra.put(ReverseLogisticExtraKeyConstant.EXPRESS_CREATED_BY, expressDto.getCreatedBy());
        extra.put(ReverseLogisticExtraKeyConstant.EXPRESS_SHIPER, expressDto.getCreatedBy());
        extra.put(ReverseLogisticExtraKeyConstant.EXPRESS_CREATED_AT,
            String.valueOf(expressDto.getTimeInfo().getCreatedAt().getTime()));
        extra.put(ReverseLogisticExtraKeyConstant.EXPRESS_PAYER, expressDto.getWaybillDto().getPayer());
        extra.put(ReverseLogisticExtraKeyConstant.EXPRESS_FEE, expressDto.getWaybillDto().getFee().toString());
        expressInfo.setExtra(extra);
        return expressInfo;
    }

    public static List<ReverseExpressInfoDto> convertExpressListDoToDto(List<ReverseExpressInfo> expressInfoList) {

        if (CollectionUtils.isEmpty(expressInfoList)) {
            return null;
        }
        return expressInfoList.stream().map(ReverseExpressConvert::convertExpressDoToDTo).collect(Collectors.toList());

    }

    public static List<ReverseExpressInfo> convertExpressListDtoToDoList(List<ReverseExpressInfoDto> expressInfoList) {

        if (CollectionUtils.isEmpty(expressInfoList)) {
            return null;
        }
        List<ReverseExpressInfo> reverseExpressInfoList=Lists.newArrayList();
        for(ReverseExpressInfoDto expressInfoDto:expressInfoList){
            reverseExpressInfoList.add(convertExpressDtoToDo(expressInfoDto,null));
        }
        return reverseExpressInfoList;

    }



    public static ReverseExpressInfoDto convertExpressDoToDTo(ReverseExpressInfo expressInfo) {

        WaybillDto waybillDto = WaybillDto.builder()
            .carrierCode(expressInfo.getCarrierCode())
            .carrierName(expressInfo.getCarrierName())
            .expressNo(expressInfo.getExpressNo())
            .paidAfterDelivery(expressInfo.getPaidAfterDelivery())
            .buyerMemo(expressInfo.getBuyerMemo())
            .build();

        if (expressInfo.getExtra() != null) {
            if (Objects.nonNull(expressInfo.getExtra().get(ReverseLogisticExtraKeyConstant.EXPRESS_SENDER_INFO))) {
                SenderDto senderInfo = JsonMapper.nonEmptyMapper().fromJson(expressInfo.getExtra().get("sender_info"),
                    SenderDto.class);
                waybillDto.setSenderInfo(senderInfo);
            }
            if (Objects.nonNull(expressInfo.getExtra().get(ReverseLogisticExtraKeyConstant.EXPRESS_FEE))) {
                waybillDto.setFee(
                    Long.valueOf(expressInfo.getExtra().get(ReverseLogisticExtraKeyConstant.EXPRESS_FEE)));

            }
            waybillDto.setPayer(expressInfo.getExtra().get(ReverseLogisticExtraKeyConstant.EXPRESS_PAYER));
            waybillDto.setShipper(expressInfo.getExtra().get(ReverseLogisticExtraKeyConstant.EXPRESS_SHIPER));
        }

        ReverseLogisticTime reverseLogisticTime = ReverseLogisticTime.builder()
            .createdAt(expressInfo.getOutCreatedAt())
            .build();

        ReverseExpressInfoDto expressInfoDto = ReverseExpressInfoDto.builder()
            .waybillDto(waybillDto)
            .createdBy(expressInfo.getCreatedBy())
            .timeInfo(reverseLogisticTime)
            .lineNo(expressInfo.getLineNo())
            .instoreNo(expressInfo.getInstoreNo())
            .hasOrder(expressInfo.getHasOrder())
            .transferOrderId(expressInfo.getTransferOrderId())
            .timeInfo(reverseLogisticTime)
            .createdBy(expressInfo.getCreatedBy())
            .createdAt(expressInfo.getCreatedAt())
            .build();
        expressInfoDto.setStatus(expressInfo.getStatus());
        expressInfoDto.setChannel(expressInfo.getChannel());
        expressInfoDto.setShop(expressInfo.getShop());
        return expressInfoDto;

    }
}
