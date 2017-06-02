package com.pousheng.erp.component;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.pousheng.erp.cache.BrandCacher;
import com.pousheng.erp.dao.mysql.BackCategoryDao;
import com.pousheng.erp.dao.mysql.SkuGroupRuleDao;
import com.pousheng.erp.manager.SpuManager;
import com.pousheng.erp.model.PoushengMaterial;
import com.pousheng.erp.model.SkuGroupRule;
import com.pousheng.erp.rules.SkuGroupRuler;
import io.terminus.common.model.Paging;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.category.model.BackCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    private static final int PAGE_SIZE = 300;

    private final BrandCacher brandCacher;

    private final SkuGroupRuleDao skuGroupRuleDao;

    private final BackCategoryDao categoryDao;

    private final SpuManager spuManager;


    @Autowired
    public SpuImporter(BrandCacher brandCacher,
                       SkuGroupRuleDao skuGroupRuleDao,
                       BackCategoryDao categoryDao,
                       SpuManager spuManager) {
        this.brandCacher = brandCacher;
        this.skuGroupRuleDao = skuGroupRuleDao;
        this.categoryDao = categoryDao;
        this.spuManager = spuManager;
    }

    public int process(Date start, Date end){
        if (start == null) {
            log.error("no start date specified when import material");
            throw new IllegalArgumentException("start.date.miss");
        }
        int handleCount = 0;
        int pageNo = 1;
        boolean hasNext = true;
        while (hasNext) {
            Paging<PoushengMaterial> pMaterial = findMaterials(pageNo, start, end);
            pageNo = pageNo + 1;
            List<PoushengMaterial> materials = pMaterial.getData();
            hasNext = Objects.equal(materials.size(), PAGE_SIZE);
            for (PoushengMaterial material : materials) {
                Brand brand = brandCacher.findByOuterId(material.getCardID());//做品牌映射
                doProcess(material, brand);
            }
            handleCount+=materials.size();
        }
        return handleCount;
    }

    private Paging<PoushengMaterial> findMaterials(int pageNo, Date start, Date end) {
        //return materialDao.paging(pageNo, PAGE_SIZE, start, end);
        //todo: read data from erp endpoint
        return Paging.empty();
    }

    /**
     * 处理单个货品信息
     *
     * @param material 货品
     * @param brand 品牌
     *
     * @return spu id
     */
    Long doProcess(PoushengMaterial material, Brand brand) {
        //String materialId = material.getMaterialID();
        String materialCode = material.getMaterialCode(); //条码

        try {
            log.info("begin to doProcess material(code={})", materialCode);

            String kindId = material.getKindID();//类别
            String seriesId = material.getSeriesID(); //系列
            String modelId = material.getModelID(); //款型
            String itemId = material.getItemID(); //项目

            //寻找或者创建(如果没有对应的类目存在)后台类目, 获取叶子类目id, 作为spu的categoryId
            Long leafId = createBackCategoryTreeIfNotExist(kindId, seriesId, modelId, itemId);

            //根据归组规则, 生成spuCode
            String spuCode = refineCode(materialCode, material.getCardID(), material.getKindID());

            //判断spuCode是否已经存在, 如果存在, 直接返回对应spuId, 如果不存在, 则生成相应的spu信息, 并返回spuId
            Long spuId = spuManager.createSpuRelatedIfNotExist(leafId, brand, spuCode,  material);
            log.info("doProcess material(code={}) succeed", materialCode);
            return spuId;
        } catch (Exception e) {
            log.error("failed to doProcess material(code={}), cause:{}",
                    materialCode, Throwables.getStackTraceAsString(e));
            return null;
        }

    }

    /**
     * 根据分组规则处理货号
     *
     * @param materialCode 货号
     * @param cardID 品牌
     * @param kindID 分类
     * @return 如果找到对应的处理规则, 则返回对应的处理结果, 否则返回materialCode
     */
    private String refineCode(String materialCode, String cardID, String kindID) {
        List<SkuGroupRule> skuGroupRules = skuGroupRuleDao.findByCardId(cardID);
        for (SkuGroupRule skuGroupRule : skuGroupRules) {
            SkuGroupRuler skuGroupRuler = SkuGroupRuler.from(skuGroupRule.getRuleType());
            if(skuGroupRuler.support(skuGroupRule, cardID,kindID)){
                return skuGroupRuler.spuCode(skuGroupRule, materialCode);
            }
        }
        log.warn("no sku group rule found for material(code={}, cardId={}), use material code as spu code",
                materialCode, cardID);
        return materialCode;
    }


    /**
     * 根据需要创建类目树
     *
     * @param kindId 类别id
     * @param seriesId  系列id
     * @param modelId  款型id
     * @param itemId 项目id
     * @return  叶子类目id
     */
    private Long createBackCategoryTreeIfNotExist(String kindId, String seriesId, String modelId, String itemId) {
        List<String> categoryNames = ImmutableList.of(kindId, seriesId, modelId, itemId);
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
     * @param parent  父类目
     * @param categoryName  类目名称
     * @return 对应名字的类目
     */
    private BackCategory createBackCategoryIfNotExist(BackCategory parent, String categoryName) {
        BackCategory backCategory = categoryDao.findChildrenByName(parent.getId(), categoryName);
        if(backCategory!=null){
            return backCategory;
        }else{
            BackCategory child = new BackCategory();
            child.setLevel(parent.getLevel()+1);
            child.setPid(parent.getId());
            child.setName(categoryName);
            child.setStatus(1);
            if(Objects.equal(child.getLevel(),4) ){ //宝胜导入数据的时候, 一定有4级类目
                child.setHasSpu(true);
                child.setHasChildren(false);
            }else{
                child.setHasChildren(true);
            }
            categoryDao.create(child);
            return child;
        }
    }
}
