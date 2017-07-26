package com.pousheng.middle.web.export;

import com.pousheng.middle.web.utils.export.ExportContext;
import com.pousheng.middle.web.utils.export.AzureOSSBlobClient;
import com.pousheng.middle.web.utils.export.ExportUtil;
import com.pousheng.middle.web.utils.export.FileRecord;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.common.utils.UserUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
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
@Component
public class ExportService {


    private static final String DEFAULT_CLOUD_PATH = "export";
    /**
     * unit:seconds
     */
    private static final int FILE_RECORD_EXPIRE_TIME = 172800;

    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;

    @Autowired
    private JedisTemplate jedisTemplate;



    public void saveToCloud(ExportContext exportContext) {

        ExportUtil.export(exportContext);

        String fileName = exportContext.getFilename();
        String url;
        if (exportContext.getResultType() == ExportContext.ResultType.BYTE_ARRAY)
            url = azureOssBlobClient.upload(exportContext.getResultByteArray(), fileName, DEFAULT_CLOUD_PATH);
        else
            url = azureOssBlobClient.upload(exportContext.getResultFile(), DEFAULT_CLOUD_PATH);

        jedisTemplate.execute(new JedisTemplate.JedisAction<Boolean>() {
            @Override
            public Boolean action(Jedis jedis) {
                String realName = fileName;
                if (fileName.contains(File.separator)) {
                    realName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
                }
                jedis.setex(key(UserUtil.getUserId(), realName), FILE_RECORD_EXPIRE_TIME, url);
                return true;
            }
        });
    }

    public void saveToDiskAndCloud(ExportContext exportContext) {
        exportContext.setResultType(ExportContext.ResultType.FILE);
        saveToCloud(exportContext);
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
