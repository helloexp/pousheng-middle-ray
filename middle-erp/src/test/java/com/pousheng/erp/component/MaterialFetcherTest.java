package com.pousheng.erp.component;

import com.pousheng.erp.model.PoushengMaterial;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-28
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
public class MaterialFetcherTest {
    @Autowired
    private MaterialFetcher materialFetcher;

    @Test
    public void fetch() throws Exception {
        Date start = DateTime.now().minusMonths(1).toDate();
        Date end = DateTime.now().plusDays(1).toDate();
        List<PoushengMaterial> materials = materialFetcher.fetch(1, 20, start, end);
        System.out.println(materials);
        assertThat(materials, notNullValue());
    }

}