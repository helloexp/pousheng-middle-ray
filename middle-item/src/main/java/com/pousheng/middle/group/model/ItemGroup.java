package com.pousheng.middle.group.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.pousheng.middle.item.dto.ItemGroupAutoRule;
import com.pousheng.middle.item.enums.AttributeEnum;
import io.terminus.common.utils.JsonMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */
@Data
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
public class ItemGroup {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.nonEmptyMapper().getMapper();

    private Long id;

    private String name;

    private Boolean auto;

    private Long relatedNum;

    @JsonIgnore
    private String groupRuleJson;

    private List<ItemGroupAutoRule> groupRule;

    private Date createdAt;

    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public ItemGroup id(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public ItemGroup name(String name) {
        this.name = name;
        return this;
    }

    public Long getRelatedNum() {
        return relatedNum;
    }

    public ItemGroup relatedNum(Long relatedNum) {
        this.relatedNum = relatedNum;
        return this;
    }

    public Boolean getAuto() {
        return auto;
    }

    public ItemGroup auto(Boolean auto) {
        this.auto = auto;
        return this;
    }

    public List<ItemGroupAutoRule> getGroupRule() {
        return groupRule;
    }

    public ItemGroup groupRule(List<ItemGroupAutoRule> groupRule) {
        this.groupRule = groupRule;
        return this;
    }


    public String getGroupRuleJson() {
        return groupRuleJson;
    }

    public ItemGroup groupRuleJson(String groupRuleJson) {
        this.groupRuleJson = groupRuleJson;
        return this;
    }

    /**
     * 设置groupRule并自动构建groupRuleJson
     *
     * @param groupRule 分组规则
     */
    public void setGroupRule(List<ItemGroupAutoRule> groupRule) {
        this.groupRule = groupRule;
        if (groupRule != null) {
            try {
                this.groupRuleJson = OBJECT_MAPPER.writeValueAsString(groupRule);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * 设置groupRuleJson并自动构建groupRule
     *
     * @param groupRuleJson 分组规则的json串
     */
    public void setGroupRuleJson(String groupRuleJson) {
        this.groupRuleJson = groupRuleJson;
        if (groupRuleJson != null) {
            try {
                JavaType javaType = getCollectionType(ArrayList.class, ItemGroupAutoRule.class);
                this.groupRule = OBJECT_MAPPER.readValue(groupRuleJson, javaType);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return OBJECT_MAPPER.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ItemGroup createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public ItemGroup updatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    /**
     * 初始规则信息给前端展示
     *
     * @return
     */
    public ItemGroup initRule(ItemGroup itemGroup) {
        List<ItemGroupAutoRule> groupRules = Lists.newArrayList();
        for (AttributeEnum e : AttributeEnum.values()) {
            groupRules.add(new ItemGroupAutoRule().name(e.value()));
        }
        if (!CollectionUtils.isEmpty(itemGroup.getGroupRule())) {
            for (int i = 0; i < groupRules.size(); i++) {
                for (ItemGroupAutoRule e : itemGroup.getGroupRule()) {
                    if (e.getName().equals(groupRules.get(i).getName())) {
                        groupRules.get(i).value(e.getValue());
                        groupRules.get(i).relation(e.getRelation());
                        continue;
                    }
                }
            }
        }
        this.setGroupRule(groupRules);
        return this;
    }
}
