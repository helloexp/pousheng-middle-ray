package com.pousheng.middle.web.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by sunbo@terminus.io on 2017/7/24.
 */
@Slf4j
@Component
public class OSSClient {


    @Value("${cos.appId:0}")
    private Long appId;

    @Value("${cos.secretId:}")
    private String secretId;

    @Value("${cos.secretKey:}")
    private String secretKey;

    @Value("${cos.bucketName:}")
    private String bucketName;

//    private COSClient cosClient;


}
