package com.pousheng.middle.web.export;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.web.utils.export.AzureOSSBlobClient;
import com.pousheng.middle.web.utils.export.ExportContext;
import com.pousheng.middle.web.utils.export.ExportUtil;
import com.pousheng.middle.web.utils.export.FileRecord;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.common.utils.UserUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;
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


    @Value("${export.local.temp.file.location:}")
    private String localFileLocation;

    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;

    @Autowired
    private JedisTemplate jedisTemplate;

    @Autowired
    private EventBus eventBus;


    @PostConstruct
    public void init() {
        eventBus.register(this);
    }


    /**
     * 保存至Azure
     *
     * @param exportContext
     */
    public void saveToCloud(ExportContext exportContext) {
        log.info("just save export content to azure");
        exportContext.setResultType(ExportContext.ResultType.BYTE_ARRAY);
        save(exportContext);
    }

    /**
     * 本地存储，并保存至Azure
     *
     * @param exportContext
     */
    public void saveToDiskAndCloud(ExportContext exportContext) {
        log.info("save export content to local disk and azure");
        exportContext.setResultType(ExportContext.ResultType.FILE);
        if (StringUtils.isNotBlank(localFileLocation)) {
            exportContext.setPath(localFileLocation);
        }
        save(exportContext);
    }


    private void save(ExportContext exportContext) {

        try {
            if (null == exportContext.getData() || exportContext.getData().isEmpty()) {
                throw new JsonResponseException("export.data.empty");
            }

            ExportUtil.export(exportContext);
            eventBus.post(new AzureOSSUploadEvent(exportContext, UserUtil.getUserId()));

        } catch (Exception e) {
            throw new JsonResponseException(e.getMessage());
        }
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

    @Deprecated
    public void deleteExportFile(String name) {
        Long currentUserID = UserUtil.getUserId();
        jedisTemplate.execute(new JedisTemplate.JedisAction<Boolean>() {

            @Override
            public Boolean action(Jedis jedis) {
                jedis.keys("export:" + currentUserID + ":" + name).forEach(key -> {
                    jedis.del(key);
                });
                return true;
            }
        });
    }

    private String key(Long userId, String fileName) {
        return DEFAULT_CLOUD_PATH + ":" + userId + ":" + fileName + "~" + DateTime.now().toDate().getTime();
    }

    @Subscribe
    public void uploadToAzureOSS(AzureOSSUploadEvent event) {

        ExportContext exportContext = event.getExportContext();
        String fileName = StringUtils.isBlank(exportContext.getFilename()) ? (System.nanoTime() + ".xls") : exportContext.getFilename();
        String url;
        if (exportContext.getResultType() == ExportContext.ResultType.BYTE_ARRAY)
            url = azureOssBlobClient.upload(exportContext.getResultByteArray(), fileName, DEFAULT_CLOUD_PATH);
        else
            url = azureOssBlobClient.upload(exportContext.getResultFile(), DEFAULT_CLOUD_PATH);

        log.info("the azure blob url:{}", url);
        boolean isStoreToRedisFileInfoSuccess = jedisTemplate.execute(new JedisTemplate.JedisAction<Boolean>() {
            @Override
            public Boolean action(Jedis jedis) {
                try {
                    String realName = fileName;
                    if (fileName.contains(File.separator)) {
                        realName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
                    }
                    Long currentUserID = event.getCurrentUserID();
                    if (null == currentUserID) {
                        log.error("cant't get current login user");
                        return false;
                    }
                    jedis.setex(key(currentUserID, realName), FILE_RECORD_EXPIRE_TIME, url);
                    log.info("save user:{}'s export file azure url to redis", currentUserID);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        if (isStoreToRedisFileInfoSuccess && exportContext.getResultType() == ExportContext.ResultType.FILE) {
            log.info("delete local file:{}", exportContext.getResultFile().getPath());
            if (!exportContext.getResultFile().delete())
                log.warn("delete local file fail:{}", exportContext.getResultFile().getPath());
        }
    }


    class AzureOSSUploadEvent {
        @Getter
        private ExportContext exportContext;
        @Getter
        private Long currentUserID;

        public AzureOSSUploadEvent(ExportContext exportContext, Long currentUserID) {
            this.exportContext = exportContext;
            this.currentUserID = currentUserID;
        }
    }
}
