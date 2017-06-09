package com.pousheng.middle.warehouses.tree;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.pousheng.middle.warehouse.dto.WarehouseAddressTree;
import io.terminus.common.exception.JsonResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 树标记算法
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-09
 */
@Slf4j
@Component
public class TreeMarker {
    /**
     * 递归查找对应的节点, 并标记相关路径
     *
     * @param root   当前搜索节点
     * @param nodeId 节点id
     */
    public void markSelected(WarehouseAddressTree root, Long nodeId) {
        //如果已经找到, 则将整颗子树标记为全选
        if (Objects.equal(root.getCurrent().getId(), nodeId)) {
            markEntireTree(root);
            return;
        }
        //继续搜索搜索孩子
        List<WarehouseAddressTree> childrenTrees = root.getChildren();
        if (CollectionUtils.isEmpty(childrenTrees)) {
            log.error("address(id={}) not found", nodeId);
            throw new JsonResponseException("addressId.not.exists");
        }
        //查找最后一个不大于nodeId的节点, 因为数据库返回的节点都是按照id排序了的
        WarehouseAddressTree candidateNode = null;
        for (WarehouseAddressTree childrenTree : childrenTrees) {
            if (childrenTree.getCurrent().getId() <= nodeId) {
                candidateNode = childrenTree;
            } else {
                break;
            }
        }
        if (candidateNode == null) {
            log.error("address(id={}) not found", nodeId);
            throw new JsonResponseException("addressId.not.exists");
        }
        //递归标记子树
        markSelected(candidateNode, nodeId);

        //如果子树已经处理完, 再标记当前节点的选中状态
        int total = 0;
        for (WarehouseAddressTree childrenTree : childrenTrees) {
            total = total + childrenTree.getSelected();
        }
        candidateNode.setSelected(total / Iterables.size(childrenTrees));
    }

    /**
     * 标记整颗树都被选中
     *
     * @param root 子树的根
     */
    private void markEntireTree(WarehouseAddressTree root) {
        root.setSelected(2);
        for (WarehouseAddressTree child : root.getChildren()) {
            markEntireTree(child);
        }
    }
}
