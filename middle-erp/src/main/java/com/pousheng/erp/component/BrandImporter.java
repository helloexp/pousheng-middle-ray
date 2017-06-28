package com.pousheng.erp.component;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.erp.model.PoushengCard;
import io.terminus.parana.brand.impl.dao.BrandDao;
import io.terminus.parana.brand.model.Brand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 导入宝胜的品牌
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-26
 */
@Component
@Slf4j
public class BrandImporter {
    private static final int PAGE_SIZE = 300;

    private final BrandDao brandDao;

    private final CardFetcher cardFetcher;

    @Autowired
    public BrandImporter(BrandDao brandDao, CardFetcher cardFetcher) {
        this.brandDao = brandDao;
        this.cardFetcher = cardFetcher;
    }


    /**
     * 增量导入
     *
     * @return 处理的条数
     */
    public int process(Date start, Date end) {
        if (start == null) {
            log.error("no start date specified when import brand");
            throw new IllegalArgumentException("start.date.miss");
        }
        int handleCount = 0;
        int pageNo = 1;
        boolean hasNext = true;
        while (hasNext) {
            List<PoushengCard> cards = cardFetcher.fetch(pageNo, PAGE_SIZE, start, end);
            pageNo = pageNo + 1;
            hasNext = Objects.equal(cards.size(), PAGE_SIZE);
            for (PoushengCard card : cards) {
                doProcess(card);
                handleCount++;
            }
        }
        return handleCount;
    }

    public void doProcess(PoushengCard card) {
        try {
            Brand brand = createParanaBrand(card);
            Brand exist = brandDao.findByName(brand.getName());
            if(exist == null) {
                brandDao.create(brand);
            }else{
                brand.setId(exist.getId());
                brandDao.update(brand);
            }
        } catch (Exception e) {
            log.error("failed to process {}, cause:{}",
                    card, Throwables.getStackTraceAsString(e));
        }
    }


    private Brand createParanaBrand(PoushengCard card) {
        Brand brand = new Brand();
        brand.setOuterId(card.getCard_id());
        //Map<String, String> refinedNames = refine(card.getCard_name());
        brand.setName(card.getCard_name());
        brand.setUniqueName(card.getCard_name());
        brand.setDescription(card.getRemark());
//            brand.setName(refinedNames.get("name"));
        //brand.setEnName(refinedNames.get("enName"));
        brand.setStatus(1);
        brand.setOuterId(card.getCard_id());
        return brand;

    }

    Map<String, String> refine(String cardName) {
        Map<String, String> result = Maps.newHashMapWithExpectedSize(3);
        if (CharMatcher.is('(').matchesAnyOf(cardName)) {
            int index = cardName.lastIndexOf('(');
            String enName = cardName.substring(0, index);
            String name = cardName.substring(index + 1, cardName.length() - 1);
            result.put("name", name);
            result.put("enName", enName);
        } else {
            result.put("name", cardName);
            result.put("enName", cardName);
        }
        return result;
    }
}
