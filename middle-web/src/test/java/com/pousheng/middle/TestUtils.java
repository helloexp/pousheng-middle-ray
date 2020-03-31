package com.pousheng.middle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousheng.middle.hksyc.utils.Numbers;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Description: 测试工具
 * User: cyy
 * Date: 2017/11/16
 */
public class TestUtils {
    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));


    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper.writeValueAsBytes(object);
    }

    public static void main(String[] args) {
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        System.out.println(serialNo);
        System.out.println(DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
    }
}
