package com.pousheng.middle.web.warehouses.algorithm;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.pousheng.middle.warehouse.dto.AddressTree;
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
     * @param editable 是否可编辑
     */
    public void markSelected(AddressTree root, Long nodeId, boolean editable) {
        //如果已经找到, 则将整颗子树标记为全选
        if (Objects.equal(root.getCurrent().getId(), nodeId)) {
            markEntireTree(root, editable);
            return;
        }
        //继续搜索搜索孩子
        List<AddressTree> childrenTrees = root.getChildren();
        if (CollectionUtils.isEmpty(childrenTrees)) {
            log.error("address(id={}) not found", nodeId);
            throw new JsonResponseException("addressId.not.exists");
        }
        //查找最后一个不大于nodeId的节点, 因为数据库返回的节点都是按照id排序了的
        AddressTree candidateNode = null;
        for (AddressTree childrenTree : childrenTrees) {
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
        markSelected(candidateNode, nodeId, editable);

        //如果子树已经处理完, 再标记当前节点的选中状态
        int total = 0;
        for (AddressTree childrenTree : childrenTrees) {
            total = total + childrenTree.getSelected();
        }

        int average = total / Iterables.size(childrenTrees);
        if(average == 2){
            root.setSelected(2);
            root.setEditable(editable);
        }else if(total>0){
            root.setSelected(1);
            root.setEditable(true);
        }else {
            root.setSelected(0);
        }
    }

    /**
     * 标记整颗树都被选中
     *
     * @param root 子树的根
     * @param editable 是否可编辑
     */
    private void markEntireTree(AddressTree root, boolean editable) {
        root.setSelected(2);
        root.setEditable(editable);
        for (AddressTree child : root.getChildren()) {
            markEntireTree(child, editable);
        }
    }
}
