package com.pousheng.middle.utils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppDriver;

import java.util.TreeMap;

/**
 * Created by cp on 8/22/17.
 */
public abstract class XmlUtils {

    public static <T> T toPojo(String xml, Class<T> klass) {
        XStream xs = new XStream(new XppDriver());
        xs.autodetectAnnotations(true);
        xs.ignoreUnknownElements();
        xs.processAnnotations(klass);
        return (T) xs.fromXML(xml);
    }

    public static String toXml(Object obj) {
        XStream xStream = new XStream(new XppDriver());
        xStream.autodetectAnnotations(true);
        xStream.alias("xml", TreeMap.class);
        return xStream.toXML(obj);
    }
}
