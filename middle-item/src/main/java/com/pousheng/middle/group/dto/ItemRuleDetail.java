package com.pousheng.middle.group.dto;

import com.pousheng.middle.group.model.ItemGroup;
import lombok.Data;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */
@Data
public class ItemRuleDetail {

    private Long id;

    private String shopNames;

    private String warehouseNames;

    private List<ItemGroup> groups;

    public Long getId() {
        return id;
    }

    public ItemRuleDetail id(Long id) {
        this.id = id;
        return this;
    }

    public String getShopNames() {
        return shopNames;
    }

    public ItemRuleDetail shopNames(String shopNames) {
        this.shopNames = shopNames;
        return this;
    }

    public List<ItemGroup> getGroups() {
        return groups;
    }

    public ItemRuleDetail groups(List<ItemGroup> groups) {
        this.groups = groups;
        return this;
    }
}
