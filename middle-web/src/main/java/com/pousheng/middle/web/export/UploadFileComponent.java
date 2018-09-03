package com.pousheng.middle.web.export;

import com.google.common.base.Throwables;
import com.pousheng.middle.common.utils.component.AzureOSSBlobClient;
import io.terminus.common.exception.JsonResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Description: 上传文件
 * User: support 9
 * Date: 2018/9/3
 */
@Component
@Slf4j
public class UploadFileComponent {

    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;

    private static final String DEFAULT_CLOUD_PATH_EXPORT = "export";

    public String exportAbnormalRecord(File file) {
        String url;
        try {
            url = azureOssBlobClient.upload(file, DEFAULT_CLOUD_PATH_EXPORT);
            log.info("the azure blob url:{}", url);
            log.info("delete local file:{}", file.getPath());
            if (!file.delete()) {
                log.warn("delete local file fail:{}", file.getPath());
            }
        } catch (Exception e) {
            log.error(" fail upload file {} to azure,cause:{}", file.getName(), Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("fail upload file to azure");
        }
        return url;
    }

}
