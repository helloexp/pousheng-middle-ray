package com.pousheng.middle.item.component;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.item.SearchSkuTemplateProperties;
import com.pousheng.middle.item.SearchStockLogProperties;
import com.pousheng.middle.item.dto.IndexedSkuTemplate;
import com.pousheng.middle.item.dto.IndexedStockLog;
import io.terminus.common.utils.JsonMapper;
import io.terminus.search.core.ESClient;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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
    @Autowired
    private SearchStockLogProperties searchStockLogProperties;


    public void batchDump(List<IndexedSkuTemplate> indexedSkuTemplates, Integer type) {

        String dumpBodyJson = "";
        try {
            StringBuilder str = new StringBuilder();
            if (notNull(indexedSkuTemplates)) {
                for (IndexedSkuTemplate index : indexedSkuTemplates) {
                    if (type != 0) {
                        index.setType(type);
                    }
                    str.append(JsonMapper.nonEmptyMapper().toJson(
                            ImmutableMap.of("index", ImmutableMap.of("_id", index.getId()))));
                    str.append("\n");
                    str.append(JsonMapper.nonEmptyMapper().toJson(index));
                    str.append("\n");
                }
                dumpBodyJson = str.toString();
                log.info("start batch dump size:{}", indexedSkuTemplates.size());
                esClient.bulk(searchItemProperties.getIndexName(), searchItemProperties.getIndexType(), dumpBodyJson);
                log.info("end batch dump");
            }

        } catch (Exception e) {
            log.error("batch dump json:{} fail,cause:{}", dumpBodyJson, Throwables.getStackTraceAsString(e));
        }
    }


    public Boolean batchDelete(String index, String type, Integer days) {
        DateTime today = new DateTime().dayOfWeek().roundFloorCopy();
        String criteria = "{\"query\":{\"bool\":{\"must\":[{\"range\":{\"createdAt\":{\"lte\":\"" + today.minusDays(days).toDate().getTime() + "\"}}}]}}}";
        return esClient.deleteByQuery(index, type, criteria);
    }


    public void batchDumpLogs(List<IndexedStockLog> logs) {
        String dumpBodyJson = "";
        try {
            StringBuilder str = new StringBuilder();
            if (notNull(logs)) {
                for (IndexedStockLog index : logs) {
                    str.append(JsonMapper.nonEmptyMapper().toJson(
                            ImmutableMap.of("index", ImmutableMap.of("_id", UUID.randomUUID().toString().replace("-", "")))));
                    str.append("\n");
                    str.append(JsonMapper.nonEmptyMapper().toJson(index));
                    str.append("\n");
                }
                dumpBodyJson = str.toString();
                log.info("start batch dump size:{}", logs.size());
                esClient.bulk(searchStockLogProperties.getIndexName(), searchStockLogProperties.getIndexType(), dumpBodyJson);
                log.info("end batch dump");
            }
        } catch (Exception e) {
            log.error("batch dump json:{} fail,cause:{}", dumpBodyJson, Throwables.getStackTraceAsString(e));
        }
    }


}
