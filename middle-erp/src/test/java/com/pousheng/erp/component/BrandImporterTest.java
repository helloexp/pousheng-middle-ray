package com.pousheng.erp.component;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-26
 */
public class BrandImporterTest extends BaseServiceTest{

    @Autowired
    private BrandImporter brandImports;

    @Test
    public void refine() throws Exception {

        String cardName ="ADIDAS OUTDOOR(阿迪户外)";
        Map<String, String> cards = brandImports.refine(cardName);
        System.out.println(cards);
    }

}