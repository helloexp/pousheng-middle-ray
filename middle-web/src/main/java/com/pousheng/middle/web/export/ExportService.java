package com.pousheng.middle.web.export;

import com.pousheng.middle.web.utils.export.AzureOSSBlobClient;
import com.pousheng.middle.web.utils.export.ExportContext;
import com.pousheng.middle.web.utils.export.ExportUtil;
import com.pousheng.middle.web.utils.export.FileRecord;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by sunbo@terminus.io on 2017/7/26.
 */
@Slf4j
@Component
public class ExportService {


    /**
     * Azure中默认父路径
     */
    private static final String DEFAULT_CLOUD_PATH = "export";
    /**
     * unit:seconds
     */
    private static final int FILE_RECORD_EXPIRE_TIME = 172800;

    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;

    @Autowired
    private JedisTemplate jedisTemplate;


    /**
     * 保存至Azure
     *
     * @param exportContext
     */
    public void saveToCloud(ExportContext exportContext) {
        log.debug("just save export content to azure");
        exportContext.setResultType(ExportContext.ResultType.BYTE_ARRAY);
        save(exportContext);
    }

    /**
     * 本地存储，并保存至Azure
     *
     * @param exportContext
     */
    public void saveToDiskAndCloud(ExportContext exportContext) {
        log.debug("save export content to local disk and azure");
        exportContext.setResultType(ExportContext.ResultType.FILE);
        save(exportContext);
    }


    private void save(ExportContext exportContext) {
        ExportUtil.export(exportContext);

        String fileName = exportContext.getFilename();
        String url;
        try {
            if (exportContext.getResultType() == ExportContext.ResultType.BYTE_ARRAY)
                url = azureOssBlobClient.upload(exportContext.getResultByteArray(), fileName, DEFAULT_CLOUD_PATH);
            else
                url = azureOssBlobClient.upload(exportContext.getResultFile(), DEFAULT_CLOUD_PATH);
        } catch (Exception e) {
            throw new ServiceException(e);
        }

        log.debug("the azure blob url:{}", url);
        jedisTemplate.execute(new JedisTemplate.JedisAction<Boolean>() {
            @Override
            public Boolean action(Jedis jedis) {
                String realName = fileName;
                if (fileName.contains(File.separator)) {
                    realName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
                }
                Long currentUserID = UserUtil.getUserId();
                jedis.setex(key(currentUserID, realName), FILE_RECORD_EXPIRE_TIME, url);
                log.debug("save user:{}'s export file azure url to redis", currentUserID);
                return true;
            }
        });
    }


    public List<FileRecord> getExportFiles() {
        Long currentUserID = UserUtil.getUserId();
        return jedisTemplate.execute(new JedisTemplate.JedisAction<List<FileRecord>>() {

            @Override
            public List<FileRecord> action(Jedis jedis) {

                List<FileRecord> records = new ArrayList<>();
                jedis.keys("export:" + currentUserID + ":*").forEach(key -> {
                    String url = jedis.get(key);
                    FileRecord record = new FileRecord();
                    String[] attr = key.split("~");
                    record.setExportAt(new Date(Long.parseLong(attr[1])));
                    record.setName(attr[0].substring(("export:" + currentUserID + ":").length()));
                    record.setUrl(url);
                    records.add(record);
                });

                return records.stream().sorted((r1, r2) -> r2.getExportAt().compareTo(r1.getExportAt())).collect(Collectors.toList());
            }
        });
    }

    private String key(Long userId, String fileName) {
        return DEFAULT_CLOUD_PATH + ":" + userId + ":" + fileName + "~" + DateTime.now().toDate().getTime();
    }
}
