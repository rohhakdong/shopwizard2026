package com.shopwizard.framework.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    public static String stackTraceToString(Throwable ex) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(b);
        ex.printStackTrace(p);
        p.close();
        String stackTrace = b.toString();
        try { b.close(); } catch (IOException e) { /* ignore */ }
        return convertHtmlBr(stackTrace);
    }

    public static String convertHtmlBr(String comment) {
        if (comment == null) return "";
        String lower = comment.toLowerCase();
        if (lower.contains("<img") || lower.contains("<table") || lower.contains("<p"))
            return comment;

        int length = comment.length();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String tmp = comment.substring(i, i + 1);
            if ("\r".equals(tmp)) {
                tmp = comment.substring(++i, i + 1);
                if ("\n".equals(tmp)) buffer.append("<br>");
            } else if ("\n".equals(tmp)) {
                buffer.append("<br>");
            } else {
                buffer.append(tmp);
            }
        }
        return buffer.toString();
    }

    public static String stripHTML(String htmlStr) {
        String rt = htmlStr;
        String[] patternStr = {
            "\\<head\\>(.*?)\\<\\/head\\>",
            "\\<html(.*?)\\>",
            "\\<body(.*?)\\>",
            "\\<\\/html\\>",
            "\\<\\/body\\>",
            "\\<script[^>]*?>.*?\\<\\/script\\>",
            "\\<style[^>]*?>.*?\\<\\/style\\>"
        };
        for (String ps : patternStr) {
            Pattern p = Pattern.compile(ps, Pattern.CASE_INSENSITIVE);
            rt = p.matcher(rt).replaceAll("");
        }
        return rt;
    }

    public static String stripQuot(String htmlStr) {
        return htmlStr.replaceAll("\"", "").replaceAll("'", "")
                      .replaceAll("\n", "").replaceAll("\r", "");
    }

    public static int stripPrice(String itemName) {
        String[] patterns = {
            "\\(-[0-9]{1,9}원\\)", "\\([+][0-9]{1,9}원\\)",
            "\\(-[0-9]{1,3}원\\)", "\\([+][0-9]{1,3}원\\)"
        };
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(itemName);
            if (matcher.find()) {
                String priceString = matcher.group()
                        .replaceAll(",", "").replaceAll("원", "")
                        .replaceAll("[+]", "").replaceAll("\\(", "").replaceAll("\\)", "");
                return Integer.parseInt(priceString);
            }
        }
        return 0;
    }

    public static int stripPrices(String itemName) {
        Matcher matcher = Pattern.compile("\\s[0-9,]{1,9}원\\s[0-9,]{1,9}개").matcher(itemName);
        int optionAmt = 0;
        while (matcher.find()) {
            String priceString = matcher.group().replaceAll(",", "").replaceAll(" ", "").replaceAll("원", "");
            String[] priceSplit = priceString.split("개");
            if (priceSplit.length > 1) {
                optionAmt += Integer.parseInt(priceSplit[0]) * Integer.parseInt(priceSplit[1]);
            }
        }
        return optionAmt;
    }

    public static boolean isAlphabetNumeric(String value) {
        return Pattern.matches("[a-zA-Z0-9]*", value);
    }

    public static boolean isAlphabetNumericCharacter(String value) {
        return Pattern.matches("([a-zA-Z0-9].*[!,@,#,$,%,^,&,*,?,_,~])|([!,@,#,$,%,^,&,*,?,_,~].*[a-zA-Z0-9])", value);
    }

    public static boolean isAlphabet(String value) {
        return Pattern.matches("[a-zA-Z]*", value);
    }

    public static boolean isNumeric(String value) {
        return Pattern.matches("[0-9.]*", value);
    }

    public static boolean isValidEmail(String email) {
        String regex = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
        return Pattern.compile(regex).matcher(email).matches();
    }

    public static String makePhoneNoFormat(String phoneNo) {
        if (!isNumeric(phoneNo)) return phoneNo;
        if (phoneNo.length() == 8)
            return phoneNo.replaceFirst("^([0-9]{4})([0-9]{4})$", "$1-$2");
        if (phoneNo.length() == 12)
            return phoneNo.replaceFirst("(^[0-9]{4})([0-9]{4})([0-9]{4})$", "$1-$2-$3");
        return phoneNo.replaceFirst("(^02|[0-9]{3})([0-9]{3,4})([0-9]{4})$", "$1-$2-$3");
    }
}
