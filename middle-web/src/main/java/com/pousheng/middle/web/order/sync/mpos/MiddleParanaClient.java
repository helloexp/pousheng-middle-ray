package com.pousheng.middle.web.order.sync.mpos;

import com.github.kevinsawicki.http.HttpRequest;
import io.terminus.open.client.parana.component.ParanaClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/6/8
 * pousheng-middle
 */
@Data
@Slf4j
@Component
public class MiddleParanaClient extends ParanaClient {
    @Override
    public String post(String url, Map<String, Object> params) {
        HttpRequest request = HttpRequest.post(url).connectTimeout(1000000).readTimeout(1000000).form(params);
        if (!request.ok()) {
            String result = request.body();
            log.debug("post request url:{} result:{}", url, result);
            return result;
        } else {
            String result = request.body();
            log.debug("post request url:{} result:{}", url, result);
            return result;
        }
    }
}
