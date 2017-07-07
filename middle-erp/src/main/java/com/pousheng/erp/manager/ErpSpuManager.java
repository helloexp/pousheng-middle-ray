package com.pousheng.erp.manager;

import com.google.common.base.MoreObjects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.pousheng.erp.dao.mysql.ErpSpuDao;
import com.pousheng.erp.model.PoushengMaterial;
import com.pousheng.erp.model.PoushengSku;
import io.terminus.parana.attribute.dto.GroupedOtherAttribute;
import io.terminus.parana.attribute.dto.GroupedSkuAttribute;
import io.terminus.parana.attribute.dto.OtherAttribute;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.spu.impl.dao.SkuTemplateDao;
import io.terminus.parana.spu.impl.dao.SpuAttributeDao;
import io.terminus.parana.spu.impl.dao.SpuDao;
import io.terminus.parana.spu.impl.dao.SpuDetailDao;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.model.SpuAttribute;
import io.terminus.parana.spu.model.SpuDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 导入宝胜的商品信息
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-31
 */
@Component
public class ErpSpuManager {

    private final SpuDao spuDao;

    private final ErpSpuDao erpSpuDao;

    private final SkuTemplateDao skuTemplateDao;

    private final SpuDetailDao spuDetailDao;

    private final SpuAttributeDao spuAttributeDao;

    @Autowired
    public ErpSpuManager(SpuDao spuDao,
                         ErpSpuDao erpSpuDao,
                         SkuTemplateDao skuTemplateDao,
                         SpuDetailDao spuDetailDao,
                         SpuAttributeDao spuAttributeDao) {
        this.spuDao = spuDao;
        this.erpSpuDao = erpSpuDao;
        this.skuTemplateDao = skuTemplateDao;
        this.spuDetailDao = spuDetailDao;
        this.spuAttributeDao = spuAttributeDao;
    }


    /**
     * 如果不存在对应的spu, 则创建spu, 否则返回对应已存在的spu, 根据prd的要求, spu一旦创建, 就不再会因为导入而引发更新
     *
     * @param leafId 叶子类目id
     * @param brand 品牌
     * @param spuCode spu 编码
     * @param material 货品信息
     * @return 对应的spu id
     */
    @Transactional
    public Long createSpuRelatedIfNotExist(Long leafId, Brand brand,
                                            String spuCode,
                                           PoushengMaterial material,
                                           List<PoushengSku> skus) {
        //因为不同leafId下, 即使spuCode一样, 也视为不同的spu
        Spu spu = erpSpuDao.findByCategoryIdAndCode(leafId, spuCode);
        Long spuId;
        if(spu!=null){ //已经存在对应的spu
            spuId =  spu.getId();
        }else{
            spu = new Spu();
            spu.setCategoryId(leafId);
            spu.setBrandId(brand.getId());
            spu.setBrandName(brand.getName());
            spu.setSpuCode(spuCode);
            spu.setName(MoreObjects.firstNonNull(material.getMaterial_name(), spuCode));
            spu.setType(1);
            spu.setStatus(1);
            spu.setStockType(1);
            int highPrice = 0;
            int lowPrice = Integer.MAX_VALUE;
            for (PoushengSku poushengSku : skus) {
                int price = priceFen(poushengSku);
                if(lowPrice>price){
                    lowPrice = price;
                }
                if(highPrice<price){
                    highPrice = price;
                }
            }
            spu.setLowPrice(lowPrice);
            spu.setHighPrice(highPrice);
            Map<String, String> extra = Maps.newHashMapWithExpectedSize(1);
            extra.put("source", "import");
            spu.setExtra(extra);
            spuDao.create(spu);
            spuId = spu.getId();

            //创建对应的spu详情
            SpuDetail spuDetail = new SpuDetail();
            spuDetail.setSpuId(spuId);
            spuDetailDao.create(spuDetail);

            //创建对应的spuAttribute
            SpuAttribute spuAttribute = new SpuAttribute();
            spuAttribute.setSpuId(spuId);
            GroupedOtherAttribute goa = new GroupedOtherAttribute();
            goa.setGroup("SPU");
            List<OtherAttribute> otherAttributes = makeOtherAttributes(material);
            goa.setOtherAttributes(otherAttributes);
            List<GroupedSkuAttribute> skuAttributes = makeSkuAttributes(skus);
            spuAttribute.setSkuAttrs(skuAttributes);
            spuAttributeDao.create(spuAttribute);
        }

        //判断对应的skuCode是否已经存在, 如果存在, 则更新, 否则为对应的spu生成skuTemplate
        createSkuTemplatesIfNotExist(spuId, material.getMaterial_name(),skus);
        return spuId;
    }


    /**
     * 根据material的信息生成spu属性信息
     *
     * @param material 货品
     * @return spu属性信息
     */
    private List<OtherAttribute> makeOtherAttributes(PoushengMaterial material) {
        List<OtherAttribute> otherAttributes = Lists.newArrayList();
        if(StringUtils.hasText(material.getForeign_name())) {
            OtherAttribute foreignName = new OtherAttribute();
            foreignName.setAttrKey("外文名称");
            foreignName.setAttrVal(material.getForeign_name());
            otherAttributes.add(foreignName);
        }
        if(StringUtils.hasText(material.getStuff())){
            OtherAttribute stuff = new OtherAttribute();
            stuff.setAttrKey("面料");
            stuff.setAttrKey(material.getStuff());
        }
        if(StringUtils.hasText(material.getTexture())){
            OtherAttribute texture = new OtherAttribute();
            texture.setAttrKey("材质说明");
            texture.setAttrVal(material.getTexture());
        }
        if(StringUtils.hasText(material.getSex())){
            OtherAttribute sex = new OtherAttribute();
            sex.setAttrKey("性别");
            sex.setAttrVal(material.getSex());
        }
        if(material.getYear_no()!=null){
            OtherAttribute yearNo = new OtherAttribute();
            yearNo.setAttrKey("年份");
            yearNo.setAttrVal(material.getYear_no().toString());
        }
        if(StringUtils.hasText(material.getInv_spec())){
            OtherAttribute invSpec = new OtherAttribute();
            invSpec.setAttrKey("规格");
            invSpec.setAttrVal(material.getInv_spec());
        }
        OtherAttribute always = new OtherAttribute();
        always.setAttrKey("长青");
        if(material.getSeason5()!=null && material.getSeason5()){
            always.setAttrVal("是");
        }else{
            always.setAttrVal("否");
        }
        otherAttributes.add(always);

        OtherAttribute cont = new OtherAttribute();
        cont.setAttrKey("延续");
        if(material.getSeason6()!=null && material.getSeason6()){
            cont.setAttrVal("是");
        }else{
            cont.setAttrVal("否");
        }

        for (OtherAttribute otherAttribute : otherAttributes) {
            otherAttribute.setGroup("SPU");
            otherAttribute.setReadOnlyBySeller(true);
        }
        return otherAttributes;
    }


    /**
     * 根据货品信息生成sku 属性
     *
     * @param skus sku列表
     * @return sku属性列表
     */
    private List<GroupedSkuAttribute> makeSkuAttributes(List<PoushengSku> skus) {
        List<GroupedSkuAttribute> groupedSkuAttributes = Lists.newArrayList();
        Multimap<String, SkuAttribute> byKey = groupSkuAttributes(skus);
        for (String key : byKey.keySet()) {
            GroupedSkuAttribute gsa = new GroupedSkuAttribute(key, Lists.newArrayList(byKey.get(key)));
            groupedSkuAttributes.add(gsa);
        }
        return groupedSkuAttributes;
    }

    private Multimap<String, SkuAttribute> groupSkuAttributes(List<PoushengSku> skus) {
        List<SkuAttribute> skuAttributes = Lists.newArrayList();
        for (PoushengSku poushengSku : skus) {
            SkuAttribute color = new SkuAttribute();
            color.setAttrKey("颜色");
            color.setAttrVal(poushengSku.getColorName());
            color.setShowImage(false);

            SkuAttribute size = new SkuAttribute();
            size.setAttrKey("尺码");
            size.setAttrVal(poushengSku.getSizeName());
            size.setShowImage(false);
            skuAttributes.add(color);
            skuAttributes.add(size);
        }
        Multimap<String, SkuAttribute> byKey = HashMultimap.create();
        for (SkuAttribute skuAttribute : skuAttributes) {
            byKey.put(skuAttribute.getAttrKey(), skuAttribute);
        }
        return byKey;
    }

    /**
     * 根据需要创建对应的skuTemplate
     *
     * @param spuId  spu id
     * @param name  对应的货品名称
     * @param skus  sku列表
     */
    private void createSkuTemplatesIfNotExist(Long spuId, String name, List<PoushengSku> skus) {
        //通过spuId及skuCode来确定
        for (PoushengSku poushengSku : skus) {
            String skuCode = poushengSku.getBarCode();
            SkuTemplate skuTemplate = skuTemplateDao.findBySpuIdAndSkuCode(spuId, skuCode);
            if(skuTemplate!=null){
                continue;
            }
            skuTemplate = new SkuTemplate();
            skuTemplate.setSpuId(spuId);
            skuTemplate.setSkuCode(skuCode);
            skuTemplate.setName(name);

            SkuAttribute color = new SkuAttribute();
            color.setAttrKey("颜色");
            color.setAttrVal(poushengSku.getColorName());
            color.setShowImage(false);

            SkuAttribute size = new SkuAttribute();
            size.setAttrKey("尺码");
            size.setAttrVal(poushengSku.getSizeName());
            size.setShowImage(false);
            skuTemplate.setAttrs(Lists.newArrayList(color,size));
            int priceFen = priceFen(poushengSku);
            skuTemplate.setPrice(priceFen);
            skuTemplate.setStatus(1);
            skuTemplate.setStockType(1);

            Map<String, String> extra = Maps.newHashMap();
            extra.put("colorId", poushengSku.getColorId());
            extra.put("size_id", poushengSku.getSizeId());
            skuTemplate.setExtra(extra);

            skuTemplateDao.create(skuTemplate);
        }

        //视需要添加spuAttribute中的skuAttribute
        Multimap<String, SkuAttribute> byKey = groupSkuAttributes(skus);
        SpuAttribute spuAttribute = spuAttributeDao.findBySpuId(spuId);
        for (GroupedSkuAttribute groupedSkuAttribute : spuAttribute.getSkuAttrs()) {
            byKey.putAll(groupedSkuAttribute.getAttrKey(), groupedSkuAttribute.getSkuAttributes());
        }

        List<GroupedSkuAttribute> groupedSkuAttributes = Lists.newArrayList();
        for (String key : byKey.keySet()) {
            GroupedSkuAttribute gsa = new GroupedSkuAttribute(key, Lists.newArrayList(byKey.get(key)));
            groupedSkuAttributes.add(gsa);
        }

        SpuAttribute u = new SpuAttribute();
        u.setSpuId(spuId);
        u.setSkuAttrs(groupedSkuAttributes);
        spuAttributeDao.updateBySpuId(u);
    }

    /**
     * 将价格调整为分
     *
     * @param poushengSku 宝胜的sku
     * @return 价格
     */
    private int priceFen(PoushengSku poushengSku) {
        Double price = Double.parseDouble(poushengSku.getMarketPrice()) * 100;
        return price.intValue();
    }
}
