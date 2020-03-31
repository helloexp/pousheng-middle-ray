package com.pousheng.middle.warehouse;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-09
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ServiceConfiguration.class)
@Transactional
@Rollback
@ActiveProfiles("test")
public class BaseServiceTest {
}
