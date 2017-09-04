package com.pousheng.middle.warehouse.cache;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.pousheng.middle.warehouse.dto.AddressNode;
import com.pousheng.middle.warehouse.dto.AddressTree;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressDao;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.utils.Joiners;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@Component
@Slf4j
public class WarehouseAddressCacher {

    private final Multimap<Long, WarehouseAddress> byPid = ArrayListMultimap.create(500, 10);

    private final Map<Long, WarehouseAddress> byId = Maps.newHashMapWithExpectedSize(5000);

    //pid和name用:拼接
    private final Map<String, WarehouseAddress> byPidAndName = Maps.newHashMapWithExpectedSize(10000);

    @Autowired
    public WarehouseAddressCacher(final WarehouseAddressDao warehouseAddressDao) {
        int offset = 0;
        int pageSize = 500;
        while (true) {
            Paging<WarehouseAddress> p = warehouseAddressDao.paging(offset, pageSize);

            List<WarehouseAddress> data = p.getData();
            for (WarehouseAddress datum : data) {
                byPid.put(datum.getPid(), datum);
                byId.put(datum.getId(), datum);
                byPidAndName.put(Joiners.COLON.join(datum.getPid(),datum.getName()),datum);
                //取地址名称的前两位，避免上海市 、上海直辖市这样的不一致
                String splitName = datum.getName().substring(0,2);
                byPidAndName.put(Joiners.COLON.join(datum.getPid(),splitName),datum);
            }
            if (data.size() < pageSize) {
                break;
            }
            offset = offset + pageSize;
        }
    }

    /**
     * 根据id查找对应的地址信息
     *
     * @param id 地址id
     * @return 对应的地址信息
     */
    public WarehouseAddress findById(Long id){
        if(!byId.containsKey(id)){
            log.error("address(id={}) not found", id);
            throw new ServiceException("address.not.found");
        }
        return byId.get(id);
    }

    /**
     * 根据pid 和名称 查找对应的地址信息
     *
     * @param pid 地址pid
     * @param name 名称
     * @return 对应的地址信息
     */
    public Optional<WarehouseAddress> findByPidAndName(Long pid, String name){
        String pidAndName = Joiners.COLON.join(pid,name);
        if(!byPidAndName.containsKey(pidAndName)){
            log.warn("address(pid={} , name:{}) not found", pid,name);
        }
        return Optional.fromNullable(byPidAndName.get(pidAndName));
    }


    /**
     * 根据pid查找地址信息
     * @param pid
     * @return
     */
    public List<WarehouseAddress> findByPid(Long pid){
        if (!byPid.containsKey(pid)){
            log.error("address(id={}) not found", pid);
            throw new ServiceException("address.not.found");
        }
        return (List<WarehouseAddress>) byPid.get(pid);
    }
    /**
     * @param maxDepth 构建树的最大深度, 如果深度<=0, 表示不限制深度
     * @return 构建完成的地址树
     */
    public AddressTree buildTree(int maxDepth) {
        AddressNode addressNode = new AddressNode(1L, "root", 0L, 0);
        AddressTree tree = new AddressTree();
        tree.setCurrent(addressNode);
        buildTree(tree, maxDepth);
        return tree;
    }

    /**
     * 递归构建子树
     *
     * @param tree 当前树
     * @param maxDepth 最大深度
     */
    private void buildTree(AddressTree tree, int maxDepth) {
        AddressNode current = tree.getCurrent();
        Long currentId = current.getId();
        List<AddressTree> childrenTree = Lists.newArrayList();

        if (maxDepth>0 && current.getLevel() < maxDepth) { //当前树的深度尚未达到最大深度, 继续递归子树
            Iterable<WarehouseAddress> children = byPid.get(currentId);
            for (WarehouseAddress child : children) {
                AddressNode addressNode = new AddressNode(child.getId(), child.getName(), child.getPid(), child.getLevel());
                AddressTree childTree = new AddressTree();
                childTree.setCurrent(addressNode);
                //递归构建子树
                buildTree(childTree, maxDepth);
                //将子树加到当前孩子集合中
                childrenTree.add(childTree);
            }
        }
        //设置子树
        tree.setChildren(childrenTree);
        //默认不选中
        tree.setSelected(0);
    }
}
