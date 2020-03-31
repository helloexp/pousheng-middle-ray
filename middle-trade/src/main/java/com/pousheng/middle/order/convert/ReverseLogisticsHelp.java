package com.pousheng.middle.order.convert;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseExpressCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseInstoreCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseLogisticsQueryRequest;
import com.pousheng.middle.order.enums.ReverseLogisticsTypeEnum;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author bernie
 * @date 2019/6/11
 */
public class ReverseLogisticsHelp {

    public static List<ReverseLogisticsTypeEnum> checkQueryList(ReverseLogisticsQueryRequest request) {

        List<ReverseLogisticsTypeEnum> reverseLogisticsTypeEnumList = Lists.newArrayList();
        if (!StringUtils.isEmpty(request.getCurrentType())) {
            ReverseLogisticsTypeEnum reverseLogisticsTypeEnum = ReverseLogisticsTypeEnum.fromName(
                request.getCurrentType());
            if (Objects.nonNull(reverseLogisticsTypeEnum)) {
                reverseLogisticsTypeEnumList.add(reverseLogisticsTypeEnum);
            }
        }
        if (!StringUtils.isEmpty(request.getPhone())) {
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.EXPRESS)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.EXPRESS);
            }
            if (reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.HEADLESS)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.HEADLESS);
            }

        }
        //快递交接时间 实际以快递交接单的创建时间
        if (!StringUtils.isEmpty(request.getStartInstoreDate())) {
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.EXPRESS)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.EXPRESS);
            }
        }

        if (!StringUtils.isEmpty(request.getExpressNo())) {
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.EXPRESS)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.EXPRESS);
            }
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.HEADLESS)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.HEADLESS);
            }
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.INSTORE)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.INSTORE);
            }
        }

        if (!StringUtils.isEmpty(request.getRefundCode())) {
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.HEADLESS)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.HEADLESS);
            }
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.INSTORE)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.INSTORE);
            }

        }

        if (!StringUtils.isEmpty(request.getOrderCode())) {
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.HEADLESS)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.HEADLESS);
            }
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.INSTORE)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.INSTORE);
            }

        }

        if (!StringUtils.isEmpty(request.getEndInstoreDate())) {
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.INSTORE)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.INSTORE);
            }
        }

        if (!StringUtils.isEmpty(request.getCustomerNote())) {
            if (!reverseLogisticsTypeEnumList.contains(ReverseLogisticsTypeEnum.INSTORE)) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.INSTORE);
            }
        }

        return reverseLogisticsTypeEnumList;
    }

    public static ReverseInstoreCriteria convertInstoreCriteria(ReverseLogisticsQueryRequest request) {
        ReverseInstoreCriteria criteria = new ReverseInstoreCriteria();

        if (!StringUtils.isEmpty(request.getRefundCode())) {
            criteria.setErpNo(request.getRefundCode());
        }

        if (!StringUtils.isEmpty(request.getOrderCode())) {
            criteria.setPlatformNo(request.getOrderCode());
        }

        if (!StringUtils.isEmpty(request.getShop())) {
            criteria.setShop(request.getShop());
        }

        if (!StringUtils.isEmpty(request.getCustomerNote())) {
            criteria.setCustomerNote(request.getCustomerNote());
        }

        if (!Objects.nonNull(request.getStartInstoreDate())) {
            criteria.setArriveWmsStartAt(request.getStartInstoreDate());
        }

        if (Objects.nonNull(request.getEndInstoreDate())) {
            criteria.setArriveWmsEndAt(request.getEndInstoreDate());

        }

        if (!StringUtils.isEmpty(request.getExpressNo())) {
            criteria.setRealExpressNo(request.getExpressNo());
        }
        criteria.setPageNo(request.getPageNo());
        criteria.setPageSize(request.getPageSize());
        return criteria;

    }

    public static ReverseHeadlessCriteria convertHeadlessCriteria(ReverseLogisticsQueryRequest request) {
        ReverseHeadlessCriteria criteria = new ReverseHeadlessCriteria();

        if (!StringUtils.isEmpty(request.getPhone())) {
            criteria.setPhone(request.getPhone());
        }

        if (!StringUtils.isEmpty(request.getRefundCode())) {
            criteria.setRelateAsn(request.getRefundCode());

        }

        if (!StringUtils.isEmpty(request.getOrderCode())) {
            criteria.setPlatformNo(request.getOrderCode());
        }

        if (!StringUtils.isEmpty(request.getExpressNo())) {
            criteria.setExpressNo(request.getExpressNo());
        }
        if (!StringUtils.isEmpty(request.getShop())) {
            criteria.setShop(request.getShop());
        }
        criteria.setPageNo(request.getPageNo());
        criteria.setPageSize(request.getPageSize());

        return criteria;

    }

    public static ReverseExpressCriteria convertExpressCriteria(ReverseLogisticsQueryRequest request) {
        ReverseExpressCriteria criteria = new ReverseExpressCriteria();

        if (!StringUtils.isEmpty(request.getPhone())) {
            criteria.setSenderMobile(request.getPhone());
        }

        if (!StringUtils.isEmpty(request.getExpressNo())) {
            criteria.setExpressNo(request.getExpressNo());
        }
        if (!StringUtils.isEmpty(request.getShop())) {
            criteria.setShop(request.getShop());
        }
        criteria.setStartAt(request.getStartInstoreDate());
        if (Objects.nonNull(request.getEndInstoreDate())) {
            DateTime dateTime = new DateTime(request.getEndInstoreDate()).plusDays(1);
            criteria.setEndAt(dateTime.toDate());
        }

        criteria.setPageNo(request.getPageNo());
        criteria.setPageSize(request.getPageSize());
        return criteria;
    }

}
