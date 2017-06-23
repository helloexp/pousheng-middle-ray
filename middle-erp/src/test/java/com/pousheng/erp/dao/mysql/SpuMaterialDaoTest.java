package com.pousheng.erp.dao.mysql;

import com.pousheng.erp.dao.BaseDaoTest;
import com.pousheng.erp.model.SpuMaterial;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;


/**
 * Author: jlchen
 * Desc: spu与material_id的关联Dao 测试类
 * Date: 2017-06-23
 */
public class SpuMaterialDaoTest extends BaseDaoTest {



    @Autowired
    private SpuMaterialDao spuMaterialDao;

    private SpuMaterial spuMaterial;

    @Before
    public void init() {
        spuMaterial = make();

        spuMaterialDao.create(spuMaterial);
        assertNotNull(spuMaterial.getId());
    }

    @Test
    public void findById() {
        SpuMaterial spuMaterialExist = spuMaterialDao.findById(spuMaterial.getId());

        assertNotNull(spuMaterialExist);
    }

    @Test
    public void update() {
        spuMaterial.setSpuId(1L);
        spuMaterialDao.update(spuMaterial);

        SpuMaterial  updated = spuMaterialDao.findById(spuMaterial.getId());
        assertEquals(updated.getSpuId(), Long.valueOf(1));
    }

    @Test
    public void delete() {
        spuMaterialDao.delete(spuMaterial.getId());

        SpuMaterial deleted = spuMaterialDao.findById(spuMaterial.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        params.put("spuId", spuMaterial.getSpuId());
        Paging<SpuMaterial > spuMaterialPaging = spuMaterialDao.paging(0, 20, params);

        assertThat(spuMaterialPaging.getTotal(), is(1L));
        assertEquals(spuMaterialPaging.getData().get(0).getId(), spuMaterial.getId());
    }

    private SpuMaterial make() {
        SpuMaterial spuMaterial = new SpuMaterial();

        
        spuMaterial.setSpuId(3L);
        
        spuMaterial.setMaterialId(23L);
        
        spuMaterial.setMaterialCode("233");
        
        spuMaterial.setCreatedAt(new Date());
        

        return spuMaterial;
    }

}