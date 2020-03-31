package com.pousheng.middle.consume.index.processor.impl.refund.builder;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.pousheng.middle.consume.index.processor.impl.CommonBuilder;
import com.pousheng.middle.consume.index.processor.impl.refund.dto.RefundDocument;
import com.pousheng.middle.consume.index.processor.impl.refund.dto.RefundExtraDTO;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-21 20:50<br/>
 */
@Slf4j
public class RefundDocumentBuilder extends CommonBuilder {
    public RefundDocument build(Refund refund) {
        RefundDocument document = new RefundDocument();
        document.setId(refund.getId());
        document.setRefundCode(refund.getRefundCode());
        document.setReleOrderCode(refund.getReleOrderCode());
        document.setStatus(refund.getStatus());
        document.setRefundType(refund.getRefundType());
        document.setReturnSerialNo(refund.getRefundSerialNo());
        document.setShopId(refund.getShopId());
        document.setShopName(refund.getShopName());
        document.setBuyerName(refund.getBuyerName());
        // 时间相关
        document.setRefundAt(timeString(refund.getRefundAt()));
        document.setUpdatedAt(timeString(refund.getUpdatedAt()));
        document.setCreatedAt(timeString(refund.getCreatedAt()));

        document.setShipmentCorpCode(refund.getShipmentCorpCode());
        document.setShipmentSerialNo(refund.getShipmentSerialNo());
        // 额外信息
        Map<String, String> extra = MoreObjects.firstNonNull(refund.getExtra(), Collections.emptyMap());
        if (StringUtils.hasText(extra.get("refundExtraInfo"))) {
            try {
                // 退货入库时间
                RefundExtraDTO dto = JsonMapper.nonEmptyMapper().fromJson(extra.get("refundExtraInfo"), RefundExtraDTO.class);
                document.setHkReturnDoneAt(timeString(dto.getHkReturnDoneAt()));
                // 物流信息
                if (StringUtils.isEmpty(document.getShipmentCorpCode())) {
                    document.setShipmentCorpCode(dto.getShipmentCorpCode());
                }
                if (StringUtils.isEmpty(document.getShipmentSerialNo())) {
                    document.setShipmentSerialNo(dto.getShipmentSerialNo());
                }
                document.setShipmentCorpName(dto.getShipmentCorpName());
            } catch (Exception e) {
                log.error("failed to get hkReturnDoneAt from refundExtraInfo, extra: {}, cause: {}", refund.getExtraJson(), Throwables.getStackTraceAsString(e));
            }
        }
        // 单据完善
        if (StringUtils.hasText(document.getShipmentCorpName()) && StringUtils.hasText(document.getShipmentSerialNo())) {
            document.setCompleteReturn(1);
        } else {
            document.setCompleteReturn(0);
        }
        return document;
    }
}
