package com.pousheng.middle.search.stock;

import com.pousheng.middle.item.service.CriteriasWithShould;
import com.pousheng.middle.search.dto.StockSendCriteria;
import com.pousheng.middle.shop.service.PsShopReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;
import io.terminus.search.api.Searcher;
import io.terminus.search.api.model.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-18 14:13<br/>
 */
@Slf4j
@Component
public class StockSendSearchComponent {
    @Value("${order.search.indexName:stocks}")
    private String indexName;
    @Value("${order.search.indexType:stock_send}")
    private String indexType;
    @Value("${search.template:ps_search.mustache}")
    private String searchTemplate;

    private final Searcher searcher;
    private final StockSendCriteiraBuilder criteiraBuilder;

    @Autowired
    private PsShopReadService psShopReadService;

    public StockSendSearchComponent(Searcher searcher, StockSendCriteiraBuilder criteiraBuilder) {
        this.searcher = searcher;
        this.criteiraBuilder = criteiraBuilder;
    }

    public Paging<StockSendRuleDTO> search(StockSendCriteria criteria) {
        CriteriasWithShould c = criteiraBuilder.build(criteria);
   //     log.info("search param:{}", JSONObject.toJSONString(c));
        Pagination<StockSendRuleDTO> page = searcher.search(indexName, indexType, searchTemplate, c, StockSendRuleDTO.class);
        //实时更新仓状态和类型
        List<StockSendRuleDTO> data = page.getData();
        if (data != null && data.size() > 0) {
            for (StockSendRuleDTO rule : data) {
                if ((rule.getWarehouseType() != null) && (rule.getWarehouseStatus() != null)) {
                    String key = rule.getWarehouseCompanyCode() + "-" + rule.getWarehouseOutCode();
                    Response<Shop> shopResponse = psShopReadService.findByCompanyCodeAndOutId(key);
                    if (shopResponse.isSuccess()) {
                        Shop shop = shopResponse.getResult();
                        rule.setWarehouseStatus(shop.getStatus());
                        rule.setWarehouseType(shop.getType());
                    }
                }
            }
        }
        return buildResult(page);
    }

    private Paging<StockSendRuleDTO> buildResult(Pagination<StockSendRuleDTO> page) {
        return new Paging<>(page.getTotal(), page.getData());
    }
}
