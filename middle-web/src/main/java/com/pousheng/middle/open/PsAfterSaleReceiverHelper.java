package com.pousheng.middle.open;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleType;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * @author bernie
 * @date 2019/5/14
 */
@Component
@Slf4j
public class PsAfterSaleReceiverHelper {

    @Autowired
    private OpenShopCacher openShopCacher;

    @Autowired
    private RefundReadLogic refundReadLogic;

    @Autowired
    private ExpressCodeReadService expressCodeReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    public  Boolean filterInitStatusWhenPullAfterSaleOrder(OpenClientAfterSale openClientAfterSale){

        OpenShop openShop = openShopCacher.findById(openClientAfterSale.getOpenShopId());
        String channel=openShop.getChannel();

        if(!Objects.equals(channel, MiddleChannel.SUNING.getValue()) && !Objects.equals(channel,MiddleChannel.TAOBAO.getValue())){
            return false;
        }
        if (Objects.nonNull(openShop.getExtra()) && Objects.nonNull(
            openShop.getExtra().get(TradeConstants.PULL_REFUND_EXCHANGE_FLAG_KEY))) {
            if (Objects.equals(openShop.getExtra().get(TradeConstants.PULL_REFUND_EXCHANGE_FLAG_KEY), TradeConstants.PULL_REFUND_EXCHANGE_FLAG_VALUE)) {
               return true;
            }
        }
        return false;
    }

    public Boolean isExpectedStatus(OpenClientAfterSale openClientAfterSale){
        switch (openClientAfterSale.getStatus()) {
            case WAIT_SELLER_CONFIRM_GOODS:
            case SUCCESS:
            case RETURN_CLOSED:
            case EXCHANGE_CLOSED:
            case EXCHANGE_SUCCESS:
            case WAIT_SELLER_SEND_GOODS:
                return true;
            default:
                return false;
        }
    }

    public void fillLogisticsInfo(Refund refund, String shipmentSerialNo, String shipmentCorpCode,
                                  String shipmentCorpName) {
        log.info("fill.fillLogisticsInfo.start.refundId={} shipmentSerialNo={}, shipmentCorpCode={},shipmentCorpName={},refund={}", new Object[]{refund.getId(),shipmentSerialNo,  shipmentCorpCode,
             shipmentCorpName,JsonMapper.nonEmptyMapper().toJson(refund)});
        if (Strings.isNullOrEmpty(shipmentSerialNo)) {
            return;
        }

        Map<String, String> extraMap = refund.getExtra() != null ? refund.getExtra() : Maps.newHashMap();
        RefundExtra refundExtra = null;
        try {
            refundExtra = refundReadLogic.findRefundExtra(refund);
        } catch (JsonResponseException e) {
            log.error("refund(id:{}) extra map not contain key:{}", refund.getId(), TradeConstants.REFUND_EXTRA_INFO);
            return;
        }
        refundExtra.setShipmentSerialNo(shipmentSerialNo);
        //转换为中台的物流信息
        ExpressCodeCriteria criteria = new ExpressCodeCriteria();
        if (!Objects.isNull(shipmentCorpCode) && !Objects.equals(shipmentCorpCode, "")) {
            MiddleChannel channel = MiddleChannel.from(refund.getChannel());
            switch (channel) {
                case JD:
                    criteria.setJdCode(shipmentCorpCode);
                    break;
                case TAOBAO:
                case TFENXIAO:
                    criteria.setTaobaoCode(shipmentCorpCode);
                    break;
                case FENQILE:
                    criteria.setFenqileCode(shipmentCorpCode);
                    break;
                case SUNING:
                case SUNINGSALE:
                    criteria.setSuningCode(shipmentCorpCode);
                    break;
                case OFFICIAL:
                    criteria.setPoushengCode(shipmentCorpCode);
                    break;
                case YUNJUBBC:
                    criteria.setOfficalCode(shipmentCorpCode);
                    break;
                case YUNJUJIT:
                    criteria.setOfficalCode(shipmentCorpCode);
                    break;
                case CODOON:
                    criteria.setCodoonCode(shipmentCorpCode);
                    break;
                case KAOLA:
                    criteria.setKaolaCode(shipmentCorpCode);
                    break;
                case VIPOXO:
                    criteria.setVipCode(shipmentCorpCode);
                    break;
                default:
                    log.error("there is not any express info by channel:{} and poushengCode:{}", channel.getValue(),
                        shipmentCorpCode);
                    throw new JsonResponseException("find.expressCode.failed");
            }
        } else if (!Objects.isNull(shipmentCorpName) && !Objects.equals(shipmentCorpName, "")) {
            criteria.setName(shipmentCorpName);
        }

        Response<Paging<ExpressCode>> response = expressCodeReadService.pagingExpressCode(criteria);
        if (!response.isSuccess()) {
            log.error("failed to pagination expressCode with criteria:{}, error code:{}", criteria,
                response.getError());
            return;
        }
        if (response.getResult().getData().size() == 0) {
            //唯品会找不到物流公司映射时默认品骏
            if (Objects.equals(refund.getChannel(), MiddleChannel.VIPOXO.getValue())) {
                refundExtra.setShipmentCorpName("品骏");
                refundExtra.setShipmentCorpCode("PINJUN");
            } else {
                log.error("there is not any express info by poushengCode:{}", shipmentCorpCode);
                return;
            }
        } else {
            ExpressCode expressCode = response.getResult().getData().get(0);
            refundExtra.setShipmentCorpName(expressCode.getName());
            refundExtra.setShipmentCorpCode(expressCode.getOfficalCode());
        }

        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        refund.setShipmentSerialNo(refundExtra.getShipmentSerialNo());
        refund.setShipmentCorpCode(refundExtra.getShipmentCorpCode());
        refund.setExtra(extraMap);
        log.info("fill.fillLogisticsInfo.end.refundId={},refund={}", new Object[]{refund.getId(),JsonMapper.nonEmptyMapper().toJson(refund)});
<<<<<<< HEAD
=======

>>>>>>> add log for logistic

    }
}
