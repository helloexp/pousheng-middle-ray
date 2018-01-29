package com.pousheng.middle.item.component;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.item.SearchSkuTemplateProperties;
import com.pousheng.middle.item.dto.IndexedSkuTemplate;
import io.terminus.common.utils.JsonMapper;
import io.terminus.search.core.ESClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

import static io.terminus.common.utils.Arguments.notNull;

/**
 * Created by songrenfei on 2018/1/29
 */
@Component
@Slf4j
public class BatchIndexTask {

    @Autowired
    private ESClient esClient;
    @Autowired
    private SearchSkuTemplateProperties searchItemProperties;



    public void batchDump(List<IndexedSkuTemplate> indexedSkuTemplates){

        StringBuilder str = new StringBuilder();
        if (notNull(indexedSkuTemplates)) {
            for (IndexedSkuTemplate index : indexedSkuTemplates) {
                str.append(JsonMapper.nonEmptyMapper().toJson(
                        ImmutableMap.of("index", ImmutableMap.of("_id", index.getId()))));
                str.append("\n");
                str.append(JsonMapper.nonEmptyMapper().toJson(index));
                str.append("\n");
            }

            String dumpBodyJson = str.toString();

            log.info("start batch dump json:{}",dumpBodyJson);
            esClient.bulk(searchItemProperties.getIndexName(),searchItemProperties.getIndexType(),dumpBodyJson);
            log.info("end batch dump");
        }
    }


}
