package com.mendhak.gpslogger.common;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.*;

/**
 * Helper to read maps from a resource onto a LinkedHashMap
 * Created by oceanebelle on 03/04/14.
 *
 * <map>
 *     <entry key="key">value</entry>
 * </map>
 */
public class XmlMap extends LinkedHashMap<String, String> {
    public XmlMap(Resources resources, int xmlResourceId) {
        XmlResourceParser parser = resources.getXml(xmlResourceId);
        if (parser == null) {
            throw new IllegalStateException("Unrecognised resource.");
        }

        parseMap(parser);

    }

    void parseMap (XmlResourceParser parser) {
        try {
            while(parser.next() != XmlResourceParser.END_DOCUMENT) {
                int eventType = parser.getEventType();

                switch (eventType) {
                    case XmlResourceParser.START_TAG:
                        if (parser.getName().equals("entry")) {
                            parseEntry(parser);
                        }
                        break;
                }
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            parser.close();
        }
    }

    void parseEntry (XmlResourceParser parser) throws XmlPullParserException, IOException {
        // pointing at starttag netry
        if (parser.getAttributeCount() != 2) {
            throw new IllegalStateException("XML Map does not have enough attributes.");
        }

        // must match key
        if (!parser.getAttributeName(0).equals("key") ||
                !parser.getAttributeName(1).equals("value")) {
            throw new IllegalStateException("XML Map does not have correct attributes.");
        }

        String key = parser.getAttributeValue(0);

        String value = parser.getAttributeValue(1);

        put(key, value);

    }

    public List<Map<String, String>> toAdapterListMap () {
        List<Map<String, String>> data = new ArrayList<Map<String, String>>(size());

        for (Entry<String, String> pair :  this.entrySet()) {
            Map<String, String> dataItem = new HashMap<String, String>();
            dataItem.put("key", pair.getKey());
            dataItem.put("value", pair.getValue());

            data.add(dataItem);
        }
        return data;
    }
}
