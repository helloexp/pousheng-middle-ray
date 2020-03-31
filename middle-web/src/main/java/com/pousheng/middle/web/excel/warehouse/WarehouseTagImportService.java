package com.pousheng.middle.web.excel.warehouse;

import com.alibaba.fastjson.JSONObject;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.warehouse.companent.ChannelInventoryClient;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @Desc   导入仓库标签
 * @Author GuoFeng
 * @Date 2019/6/5
 */
@Service
@Slf4j
public class WarehouseTagImportService {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ChannelInventoryClient channelInventoryClient;

    @Autowired
    private UploadFileComponent uploadFileComponent;

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Response<String> importFile(ImportRequest request){
        try {
            Response<String> response = Response.ok("SUCCESS");
            String filePath = request.getFilePath();
            if (StringUtils.isEmpty(filePath)) {
                String errorMsg = "要导入的仓库标签文件地址不存在," + request.getFileName();
                log.error(errorMsg);
                return Response.fail(errorMsg);
            }

            List<WarehouseTagsExcelBean> data = HandlerFileUtil.getInstance().handleWarehouseTagsExcel(request);
            if (data.size() > 0) {
                //调用库存接口
                String json = JSONObject.toJSONString(data);
                Response<String> responseClient = channelInventoryClient.updateWarehouseTags(json);
                if (responseClient.isSuccess()) {
                    String result = responseClient.getResult();
                    if (! StringUtils.isEmpty(request)) {
                        WarehouseTagsResponse responseData = JSONObject.parseObject(result, WarehouseTagsResponse.class);
                        if (responseData.isSuccess()) {
                            List<WarehouseTagsFaildBean> faildBeans = responseData.getFaildBeans();
                            if (faildBeans.size() > 0) {
                                ExcelExportHelper<WarehouseTagsFaildBean> errorExcel = ExcelExportHelper.newExportHelper(WarehouseTagsFaildBean.class);
                                errorExcel.appendToExcel(faildBeans);
                                String format = sdf.format(new Date());
                                File file = errorExcel.transformToFile("导入失败明细" + format + ".xlsx");
                                String abnormalUrl = uploadFileComponent.exportAbnormalRecord(file);
                                response.setResult(abnormalUrl);
                            }
                        } else {
                            return Response.fail(responseData.getErrorMsg());
                        }
                    }
                } else {
                    response.setError("导入仓库标签失败");
                }
                return response;
            } else {
                log.error("上传的文件没有数据:{}", filePath);
                return Response.fail("上传的文件没有数据");
            }
        } catch (Exception e) {
            String message = getMessage(e.getMessage());
            log.error("处理售后单导入出错, 文件:{}, 错误:{}", request.getFileName(), message);
            e.printStackTrace();
            return Response.fail(message);
        }
    }


    /**
     * 获取国际化信息
     * @param msgCode
     * @return
     */
    public String getMessage(String msgCode){
        String message = null;
        try {
            message = messageSource.getMessage(msgCode, null, Locale.CHINA);
            if (StringUtils.isEmpty(message)) {
                return msgCode;
            }
        } catch (Exception e) {
            message = msgCode;
        }
        return message;
    }
}
