package com.pousheng.middle.web.job;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.common.utils.component.AzureOSSBlobClient;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.MiddleShipmentCriteria;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.MiddleShipmentReadService;
import com.pousheng.middle.web.utils.mail.MailLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.List;

/**
 * 定时拉取失败的订单上传到微软云并发送邮件通知
 * @author tanlongjun
 */
@Slf4j
@RestController
public class FailedOrderEmailWarningJobs {

    private static final int HOURS = 12;

    private static final int BATCH_SIZE = 100;

    private static final String FILE_NAME_TEMPLATE="异常订单清单-{0}.xls";

    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;

    private static final String REMOTE_CLOUD_PATH = "export";

    private static final String DATETIME_FORMAT="yyyyMMddHHmmssSSS";

    private static final String EMAIL_TITLE="异常订单和发货单汇总";

    private static final String EMAIL_CONTENT_TEMPLATE="您好.\n" +
            "\t已汇总未生成发货单的订单和未同步YYEDI或mPOS的发货单\n" +
            "\t附件链接为{0}\n" +
            "\n" +
            "SYSTEM";

    @Autowired
    private MailLogic mailLogic;

    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;


    @RpcConsumer
    private MiddleShipmentReadService middleShipmentReadService;

    /**
     * 收件人
     */
    @Value("${failed.order.warning.email.receives}")
    private String receives;

    /**
     * 默认为每天6点18点触发
     */
    @Scheduled(cron ="${failed.order.warning.job.cron}")
    public void run(){
        log.info("the email warning job of failed order start");
        try{

            DateTime current=new DateTime();

            String fileName=MessageFormat.format(FILE_NAME_TEMPLATE,current.toString(DATETIME_FORMAT));
            //查询数据
            List<String> orders=queryOrders();

            List<String> shipments=queryShipments();

            if(CollectionUtils.isEmpty(orders)
                && CollectionUtils.isEmpty(shipments)){
                return;
            }
            //保存xls
            byte[] fileBytes=saveExcel(orders,shipments);

            //上传
            String url=uploadToAzureOSS(fileBytes,fileName);

            //发送邮件
            if(StringUtils.isNotBlank(url)) {
                sendMail(url);
            }

        }catch (Exception e){
            log.error("the email warning job error.{}",e);
        }
        log.info("the email warning job of failed order end");
    }


    /**
     * 查询异常订单
     * @return
     */
    private List<String> queryOrders(){
        List<String> result=Lists.newArrayList();
        int pageNo=1;

        int count=0;
        do{
            MiddleOrderCriteria orderCriteria = new MiddleOrderCriteria();
            orderCriteria.setStatus(Lists.newArrayList(MiddleOrderStatus.WAIT_HANDLE.getValue()));

            orderCriteria.setPageNo(pageNo);
            orderCriteria.setPageSize(BATCH_SIZE);
            Response<Paging<ShopOrder>> orderList = middleOrderReadService.pagingShopOrder(orderCriteria);
            if(orderList.isSuccess()
                    && !orderList.getResult().isEmpty()){
                count=orderList.getResult().getData().size();
                pageNo++;

                orderList.getResult().getData().stream().forEach(order->{
                    result.add(order.getOrderCode());
                });
            }
        } while (count % BATCH_SIZE==0 && count!=0);
        return result;
    }

    /**
     * 查询异常发货单
     * @return
     */
    private List<String> queryShipments(){
        List<String> result=Lists.newArrayList();
        int pageNo=1;
        int count=0;
        do{
            MiddleShipmentCriteria criteria = new MiddleShipmentCriteria();
            criteria.setStatusList(Lists.newArrayList(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue(),
                    MiddleShipmentsStatus.SYNC_HK_ING.getValue(),
                    MiddleShipmentsStatus.SYNC_HK_ACCEPT_FAILED.getValue(),
                    MiddleShipmentsStatus.SYNC_HK_FAIL.getValue()));

            criteria.setPageNo(pageNo);
            criteria.setPageSize(BATCH_SIZE);
            Response<Paging<Shipment>> shipmentList = middleShipmentReadService.pagingShipment(criteria);

            if(shipmentList.isSuccess()
                    && !shipmentList.getResult().isEmpty()){
                count=shipmentList.getResult().getData().size();
                pageNo++;

                shipmentList.getResult().getData().stream().forEach(shipment->{
                    result.add(shipment.getShipmentCode());
                });
            }
        } while (count % BATCH_SIZE==0 && count!=0);
        return result;
    }

    /**
     * 保存为本地Excel文件
     * @return
     */
    private byte[] saveExcel(List<String> orders,List<String> shipments){

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
            Workbook wb = new SXSSFWorkbook();
            Sheet sheet = wb.createSheet("sheet");
            int orderSize=orders.size();
            int shipmentSize=shipments.size();
            int maxRows=Math.max(orderSize,shipmentSize);

            Row row = sheet.createRow(0);
            Cell orderTitle = row.createCell(0, CellType.STRING);
            orderTitle.setCellValue("交易单号");

            Cell shipmentTitle = row.createCell(1, CellType.STRING);
            shipmentTitle.setCellValue("发货单号");

            for (int i = 0; i < maxRows; i++) {
                row = sheet.createRow(i+1);
                if(i<orderSize) {
                    Cell orderCode = row.createCell(0, CellType.STRING);
                    orderCode.setCellValue(orders.get(i));
                }
                if(i<shipmentSize){
                    Cell shipmentCode = row.createCell(1, CellType.STRING);
                    shipmentCode.setCellValue(shipments.get(i));
                }

            }
                wb.write(bos);
                return bos.toByteArray();


        } catch (Exception e) {
            log.error("export order template fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(500, "export.order.fail");
        }
    }

    /**
     * 上传文件到云盘
     * @param fileBytes
     * @param fileName
     * @return
     */
    private String uploadToAzureOSS(byte[] fileBytes,String fileName){

        String url;
        try {
            url = azureOssBlobClient.upload(fileBytes,fileName, REMOTE_CLOUD_PATH);
            log.info("the azure blob url:{}", url);
        } catch (Exception e) {
            log.error(" fail upload file {} to azure,cause:{}", fileName, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("fail upload failed order file to azure");
        }
        return url;
    }

    /**
     * 发送邮件
     * @param url
     */
    private void sendMail(String url){
        String content= MessageFormat.format(EMAIL_CONTENT_TEMPLATE,url);

        mailLogic.sendMail(receives,EMAIL_TITLE,content);

    }


}
