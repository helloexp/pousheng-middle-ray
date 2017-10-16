package com.pousheng.erp.rules;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.pousheng.erp.model.SkuGroupRule;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-23
 */
public enum SkuGroupRuler {

    SPLIT(1) {
        @Override
        public String spuCode(SkuGroupRule skuGroupRule, String materialCode) {
            Character splitChar = skuGroupRule.getSplitChar();

            List<String> parts = Splitter.on(splitChar).omitEmptyStrings().trimResults().splitToList(materialCode);
            return parts.get(0);
        }
    },

    INDEX(2) {
        @Override
        public String spuCode(SkuGroupRule skuGroupRule, String materialCode) {
            Integer lastStart = skuGroupRule.getLastStart();
            int length = materialCode.length();
            return materialCode.substring(0, length - lastStart);
        }
    },

    MIX(3){//如果materialCode中含有分隔符, 则优先使用分隔符, 否则使用xx位
        @Override
        public String spuCode(SkuGroupRule skuGroupRule, String materialCode) {
            if(CharMatcher.is(skuGroupRule.getSplitChar()).matchesAnyOf(materialCode)){
                return SPLIT.spuCode(skuGroupRule, materialCode);
            }else{
                return INDEX.spuCode(skuGroupRule, materialCode);
            }
        }
    },
    LIVES(4){//针对第4种规则进行拼接,例如：7-1-10-9（如果第7位含有1则取前10位,如果没有1则取前9位）
        @Override
        public String spuCode(SkuGroupRule skuGroupRule, String materialCode) {
            String details = String.valueOf(skuGroupRule.getRuleDetail());//类型4有4位数字，第一位是位置,第二位是判断条件，第三位是满足判断条件的取值，第四位是不满足判断条件的取值
            List<String> parts = Splitter.on("-").omitEmptyStrings().trimResults().splitToList(details);
            String first = parts.get(0);
            String second = parts.get(1);
            String third = parts.get(2);
            String fourth = parts.get(3);
            if (Objects.equal(String.valueOf(materialCode.charAt(Integer.valueOf(first)-1)),second)){
                return materialCode.substring(0,Integer.valueOf(third));
            }else{
                return materialCode.substring(0,Integer.valueOf(fourth));
            }
        }
    };

    public final int value;

    SkuGroupRuler(int value) {
        this.value = value;
    }


    public static SkuGroupRuler from(int value){
        for (SkuGroupRuler skuGroupRuler : SkuGroupRuler.values()) {
            if(skuGroupRuler.value == value){
                return skuGroupRuler;
            }
        }
        throw new IllegalArgumentException("unknown rule type: "+value);
    }



    public abstract String spuCode(SkuGroupRule skuGroupRule, String materialCode);

    public boolean support(SkuGroupRule skuGroupRule, String cardId, String kindId) {
        //判断品牌id是否匹配
        if (StringUtils.hasText(skuGroupRule.getCardId()) && !Objects.equal(cardId, skuGroupRule.getCardId())) {
            return false;
        }

        //判断类目id是否匹配(如果存在类目id)
        if (StringUtils.hasText(skuGroupRule.getKindId())
                && !Objects.equal(skuGroupRule.getKindId(), kindId)) {
            return false;

        }
        return true;
    }
}
