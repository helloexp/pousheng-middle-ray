package com.pousheng.middle.common.utils.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-20 11:40<br/>
 */
@Slf4j
@Component
public class FileUtils {
    public String downloadUrl(String dir, String prefix, String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        String suffix = url.substring(url.lastIndexOf("."));

        try {
            File file = newTempFile(dir, prefix, suffix);

            HttpRequest requst = HttpRequest.get(url);
            requst.receive(file);

            return file.getPath();
        } catch (Exception e) {
            log.error("failed to download {} from oss, cause: {}", url, Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    private File newTempFile(String path, String prefix, String suffix) throws IOException {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = File.createTempFile(prefix, suffix, dir);
        log.info("file exist: {}", file.exists());
        log.info("file path: {}", file.getPath());
        if (!log.isDebugEnabled()) {
            file.deleteOnExit();
        }
        return file;
    }
}
