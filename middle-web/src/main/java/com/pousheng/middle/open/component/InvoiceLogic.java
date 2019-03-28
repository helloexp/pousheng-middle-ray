package com.pousheng.middle.open.component;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.order.dto.OpenClientOrderInvoice;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.service.InvoiceWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Objects;

/**
 * @author Xiongmin
 * 2019/3/1
 */
@Component
@Slf4j
public class InvoiceLogic {

    @RpcConsumer
    private InvoiceWriteService invoiceWriteService;

    /**
     * 从 PsOrderReceiver 重构过来
     * @param openClientOrderInvoice
     * @return
     */
    public Long addInvoice(OpenClientOrderInvoice openClientOrderInvoice) {
        try {
            //获取发票类型
            Integer invoiceType = Integer.valueOf(openClientOrderInvoice.getType());
            //获取detail
            Map<String, String> detail = openClientOrderInvoice.getDetail();
            if (detail == null) {
                detail = Maps.newHashMap();
            } else {
                detail.put("type", String.valueOf(invoiceType));
            }

            Invoice newInvoice = new Invoice();
            if (Objects.equals(invoiceType, 2)) {
                //2，增值税发票
                //公司
                detail.put("titleType", "2");
                //获取抬头(公司名称吧)
                String title = openClientOrderInvoice.getDetail().get("companyName");
                if (Arguments.isNull(title)){
                    title="";
                }
                newInvoice.setTitle(title);
            }else if(Objects.equals(invoiceType, 3)||Objects.equals(invoiceType, 1)){
                //3，电子发票
                String titleType= detail.get("titleType");
                if (Objects.equals(titleType,"1")){
                    //个人电子发票
                    newInvoice.setTitle("个人");
                }else if (Objects.equals(titleType,"2")){
                    //公司 电子发票
                    String title = openClientOrderInvoice.getDetail().get("companyName");
                    if (Arguments.isNull(title)){
                        title="";
                    }
                    newInvoice.setTitle(title);
                }
            }else {
                newInvoice.setTitle("其他");
            }
            newInvoice.setStatus(1);
            newInvoice.setIsDefault(false);
            newInvoice.setDetail(detail);
            Response<Long> response = invoiceWriteService.createInvoice(newInvoice);
            if (!response.isSuccess()) {
                log.error("create invoice failed,caused by {}", response.getError());
                throw new ServiceException("create.invoice.failed");
            }
            return response.getResult();
        } catch (Exception e) {
            log.error("create invoice failed,caused by {}", Throwables.getStackTraceAsString(e));
        }
        return null;
    }
}
