package com.pousheng.erp.component;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.pousheng.erp.cache.ErpBrandCacher;
import com.pousheng.erp.cache.ErpSpuCacher;
import com.pousheng.erp.dao.mysql.SkuGroupRuleDao;
import com.pousheng.erp.dao.mysql.SpuMaterialDao;
import com.pousheng.erp.manager.ErpSpuManager;
import com.pousheng.erp.model.*;
import com.pousheng.erp.rules.SkuGroupRuler;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.category.impl.dao.BackCategoryDao;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.spu.impl.dao.SkuTemplateDao;
import io.terminus.parana.spu.impl.dao.SpuDao;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 导入宝胜商品等信息, 并在导入过程中构建类目
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-26
 */
@Component
@Slf4j
public class SpuImporter {

    private static final int PAGE_SIZE = 200;

    private final ErpBrandCacher brandCacher;

    private final ErpSpuCacher spuCacher;

    private final SkuGroupRuleDao skuGroupRuleDao;

    private final BackCategoryDao categoryDao;

    private final ErpSpuManager spuManager;

    private final SpuMaterialDao spuMaterialDao;

    private final MaterialFetcher materialFetcher;

    private final SkuTemplateDao skuTemplateDao;

    private final SpuDao spuDao;


    @Autowired
    public SpuImporter(ErpBrandCacher brandCacher,
                       ErpSpuCacher spuCacher,
                       SkuGroupRuleDao skuGroupRuleDao,
                       BackCategoryDao categoryDao,
                       ErpSpuManager spuManager,
                       SpuMaterialDao spuMaterialDao,
                       MaterialFetcher materialFetcher, SkuTemplateDao skuTemplateDao, SpuDao spuDao) {
        this.brandCacher = brandCacher;
        this.spuCacher = spuCacher;
        this.skuGroupRuleDao = skuGroupRuleDao;
        this.categoryDao = categoryDao;
        this.spuManager = spuManager;
        this.spuMaterialDao = spuMaterialDao;
        this.materialFetcher = materialFetcher;
        this.skuTemplateDao = skuTemplateDao;
        this.spuDao = spuDao;
    }

    public int process(Date start, Date end) {
        if (start == null) {
            log.error("no start date specified when import material");
            throw new IllegalArgumentException("start.date.miss");
        }
        int handleCount = 0;
        int pageNo = 1;
        boolean hasNext = true;
        while (hasNext) {
            List<PoushengMaterial> materials = materialFetcher.fetch(pageNo, PAGE_SIZE, start, end);
            pageNo = pageNo + 1;
            hasNext = Objects.equal(materials.size(), PAGE_SIZE);
            for (PoushengMaterial material : materials) {
                Brand brand = brandCacher.findByCardName(material.getCard_name());//做品牌映射
                doProcess(material, brand);
            }
            handleCount += materials.size();
        }
        return handleCount;
    }

    /**
     * 处理单个货品信息
     *
     * @param material 货品
     * @param brand    品牌
     * @return spu id
     */
    public Long doProcess(PoushengMaterial material, Brand brand) {
        String materialId = material.getMaterial_id();
        String materialCode = material.getMaterial_code(); //条码

        try {
            log.info("begin to doProcess material(id={},code={})", materialId, materialCode);

            Long leafId = null;
            //检查货品是否已经被同步
            SpuMaterial exist = spuMaterialDao.findByMaterialId(materialId);
            if (exist != null) {
                log.info("material(id={}) has been synchronized, ", materialId);
                Long spuId = exist.getSpuId();
                Spu spu = spuCacher.findById(spuId);
                leafId = spu.getCategoryId();

            }else {
                String kind_name = material.getKind_name();//类别
                String series_name = material.getSeries_name(); //系列
                String model_name = material.getModel_name(); //款型
                String item_name = material.getItem_name(); //项目

                //寻找或者创建(如果没有对应的类目存在)后台类目, 获取叶子类目id, 作为spu的categoryId
                leafId = createBackCategoryTreeIfNotExist(kind_name, series_name, model_name, item_name);
            }
            //根据归组规则, 生成spuCode(如果规则变动，这里的spuCode就会变动)
            String spuCode = refineCode(materialCode, material.getCard_name(), material.getKind_name());

            List<PoushengSku> poushengSkus = createSkuFromMaterial(material);
            Long spuId = spuManager.createOrUpdateSpuRelated(leafId, brand, spuCode, material, poushengSkus);

            if(exist == null) { //创建货品id与spu的映射关系
                SpuMaterial spuMaterial = new SpuMaterial();
                spuMaterial.setSpuId(spuId);
                spuMaterial.setMaterialId(materialId);
                spuMaterial.setMaterialCode(materialCode);
                spuMaterialDao.create(spuMaterial);
            }else {
                if(!Objects.equal(exist.getSpuId(),spuId)){
                    log.error("spu material(id:{}) spu id is:{} not equal current spu id:{}",exist.getId(),exist.getSpuId(),spuId);
                    SpuMaterial spuMaterial = new SpuMaterial();
                    spuMaterial.setId(exist.getId());
                    spuMaterial.setSpuId(spuId);
                    spuMaterialDao.update(spuMaterial);

                    //删除无效的spu
                    deleteInvalidSpu(exist.getSpuId());
                }
            }

            log.info("doProcess material(id={}, code={}) succeed", materialId, materialCode);
            return spuId;
        } catch (Exception e) {
            log.error("failed to doProcess material(code={}), cause:{}",
                    materialCode, Throwables.getStackTraceAsString(e));
            return null;
        }

    }


    private void deleteInvalidSpu(Long spuId){
        List<SkuTemplate> skuTemplates = skuTemplateDao.findBySpuId(spuId);
        //如果spu下没有有效的sku则把spu标记为已删除
        if(CollectionUtils.isEmpty(skuTemplates)){
            spuDao.updateStatus(spuId,-3);
        }

    }

    private List<PoushengSku> createSkuFromMaterial(PoushengMaterial material) {
        if (CollectionUtils.isEmpty(material.getSize())) {
            return Collections.emptyList();
        }
        List<PoushengSku> r = Lists.newArrayListWithCapacity(material.getSize().size());
        String colorName = material.getColor_name();
        String colorId = material.getColor_id();
        for (PoushengSize poushengSize : material.getSize()) {
            PoushengSku poushengSku = new PoushengSku();
            poushengSku.setBarCode(poushengSize.getBarcode());
            poushengSku.setMaterialId(material.getMaterial_id());
            poushengSku.setMaterialCode(material.getMaterial_code());
            poushengSku.setColorId(colorId);
            poushengSku.setColorName(colorName);
            poushengSku.setSizeId(poushengSize.getSize_id());
            poushengSku.setSizeName(poushengSize.getSize_name());
            poushengSku.setMarketPrice(String.valueOf(material.getPro_retail_price()));
            r.add(poushengSku);
        }
        return r;
    }

    /**
     * 根据分组规则处理货号
     *
     * @param materialCode 货号
     * @param card_name    品牌
     * @param kind_name    分类
     * @return 如果找到对应的处理规则, 则返回对应的处理结果, 否则返回materialCode
     */
    private String refineCode(String materialCode, String card_name, String kind_name) {
        List<SkuGroupRule> skuGroupRules = skuGroupRuleDao.findByCardId(card_name);
        for (SkuGroupRule skuGroupRule : skuGroupRules) {
            SkuGroupRuler skuGroupRuler = SkuGroupRuler.from(skuGroupRule.getRuleType());
            if (skuGroupRuler.support(skuGroupRule, card_name, kind_name)) {
                return skuGroupRuler.spuCode(skuGroupRule, materialCode);
            }
        }
        log.warn("no sku group rule found for material(code={}, cardName={}), use material code as spu code",
                materialCode, card_name);
        return materialCode;
    }


    /**
     * 根据需要创建类目树
     *
     * @param kind_name   类别id
     * @param series_name 系列id
     * @param model_name  款型id
     * @param item_name   项目id
     * @return 叶子类目id
     */
    private Long createBackCategoryTreeIfNotExist(String kind_name, String series_name, String model_name, String item_name) {
        List<String> categoryNames = ImmutableList.of(kind_name, series_name, model_name, item_name);
        BackCategory parent = new BackCategory();
        parent.setId(0L);
        parent.setLevel(0);
        for (String categoryName : categoryNames) {
            parent = createBackCategoryIfNotExist(parent, categoryName);
        }
        return parent.getId();
    }

    /**
     * 如果不存在则创建对应的类目, 否则返回已存在的类目
     *
     * @param parent       父类目
     * @param categoryName 类目名称
     * @return 对应名字的类目
     */
    private BackCategory createBackCategoryIfNotExist(BackCategory parent, String categoryName) {
        BackCategory backCategory;
        try {
            backCategory = categoryDao.findChildrenByName(parent.getId(), categoryName);
        } catch (Exception e) {
            log.error("duplicated categoryName {} where pid={}", categoryName, parent.getId());
            throw e;
        }
        if (backCategory != null) {
            return backCategory;
        } else {
            BackCategory child = new BackCategory();
            child.setLevel(parent.getLevel() + 1);
            child.setPid(parent.getId());
            child.setName(categoryName);
            child.setStatus(1);
            if (Objects.equal(child.getLevel(), 4)) { //宝胜导入数据的时候, 一定有4级类目
                child.setHasSpu(true);
                child.setHasChildren(false);
            } else {
                child.setHasSpu(false);
                child.setHasChildren(true);
            }
            categoryDao.create(child);
            return child;
        }
    }

    /**
     * 根据skuCode拉取货品信息
     * @param skuCode
     * @return
     */
    public int processPullMarterials(String skuCode) {
        if (StringUtils.isEmpty(skuCode)){
            log.error("no skuCode when import material");
            throw new IllegalArgumentException("skuCode.is.null");
        }
        List<PoushengMaterial> materials = materialFetcher.fetchByBarCode(skuCode);
        for (PoushengMaterial material : materials) {
            Brand brand = brandCacher.findByCardName(material.getCard_name());//做品牌映射
            doProcess(material, brand);
        }
        return materials.size();
    }

}
