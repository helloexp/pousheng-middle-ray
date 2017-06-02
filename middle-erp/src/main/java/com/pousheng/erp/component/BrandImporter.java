package com.pousheng.erp.component;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.dao.mysql.BrandDao;
import com.pousheng.erp.model.PoushengCard;
import io.terminus.common.model.Paging;
import io.terminus.parana.brand.model.Brand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Autowired
    public BrandImporter(BrandDao brandDao) {
        this.brandDao = brandDao;
    }


    /**
     * 增量导入
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 处理的条数
     */
/*    public int process(Date start, Date end) {
        if (start == null) {
            log.error("no start date specified when import brand");
            throw new IllegalArgumentException("start.date.miss");
        }
        int handleCount = 0;
        int offset = 0;
        boolean hasNext = true;
        while (hasNext) {
            Paging<PoushengCard> pCards = cardDao.paging(offset, PAGE_SIZE, start, end);
            offset = offset + PAGE_SIZE;
            List<PoushengCard> cards = pCards.getData();
            hasNext = Objects.equal(cards.size(), PAGE_SIZE);

            List<Brand> brands = createParanaBrands(cards);
            for (Brand brand : brands) {
                brandDao.create(brand);
            }
            handleCount+=cards.size();
        }
        return handleCount;
    }*/

    /**
     * 全量导入
     *
     * @return 处理的条数
     */
    public int all(){
        int handleCount = 0;
        int pageNo = 0;
        boolean hasNext = true;
        while (hasNext) {
            Paging<PoushengCard> pCards =  pagination(pageNo);
            pageNo = pageNo + 1;
            List<PoushengCard> cards = pCards.getData();
            hasNext = Objects.equal(cards.size(), PAGE_SIZE);

            List<Brand> brands = createParanaBrands(cards);
            for (Brand brand : brands) {
                brandDao.create(brand);
            }
            handleCount+=cards.size();
        }
        return handleCount;
    }

    private Paging<PoushengCard> pagination(int offset) {
        //return cardDao.paging(offset, PAGE_SIZE);
        //todo: call
        return Paging.empty();
    }


    private List<Brand> createParanaBrands(List<PoushengCard> cards) {
        List<Brand> results = Lists.newArrayListWithCapacity(cards.size());
        for (PoushengCard card : cards) {
            Brand brand = new Brand();
            brand.setOuterId(card.getCardID());
            Map<String, String> refinedNames = refine(card.getCardName());
            brand.setName(refinedNames.get("name"));
            brand.setEnName(refinedNames.get("enName"));
            brand.setStatus(1);
            results.add(brand);
        }
        return results;
    }

    Map<String, String> refine(String cardName) {
        Map<String, String> result = Maps.newHashMapWithExpectedSize(3);
        if (CharMatcher.is('(').matchesAnyOf(cardName)) {
            int index = cardName.lastIndexOf('(');
            String enName = cardName.substring(0, index);
            String name = cardName.substring(index + 1, cardName.length() - 1);
            result.put("name",name);
            result.put("enName", enName);
        }else{
            result.put("name",cardName);
            result.put("enName", cardName);
        }
        return result;
    }
}
