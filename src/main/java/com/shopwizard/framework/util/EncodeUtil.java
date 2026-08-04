package com.shopwizard.framework.util;

import java.io.UnsupportedEncodingException;

public class EncodeUtil {

    public static String ascToKsc(String value) {
        try {
            return new String(value.getBytes("8859_1"), "KSC5601");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String kscToAsc(String value) {
        try {
            return new String(value.getBytes("KSC5601"), "8859_1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String ascToUtf8(String value) {
        try {
            return new String(value.getBytes("8859_1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String utf8ToAsc(String value) {
        try {
            return new String(value.getBytes("UTF-8"), "8859_1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String kscToUtf8(String value) {
        try {
            return new String(value.getBytes("KSC5601"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String utf8ToKsc(String value) {
        try {
            return new String(value.getBytes("UTF-8"), "KSC5601");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
