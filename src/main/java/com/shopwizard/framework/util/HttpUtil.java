package com.shopwizard.framework.util;

import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class HttpUtil {

    public static String urlCall(String requesturl) {
        StringBuilder requestMsg = new StringBuilder();
        try {
            URL url = new URL(requesturl);
            try (BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()))) {
                String line;
                while ((line = input.readLine()) != null) requestMsg.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return requestMsg.toString();
    }

    public static String getStrEncode(String inpt) throws Exception {
        return URLEncoder.encode(inpt, "EUC-KR");
    }

    @SuppressWarnings("unchecked")
    public static String getRequestParamString(HttpServletRequest request) {
        Enumeration<String> param = request.getParameterNames();
        StringBuilder strParam = new StringBuilder();
        while (param.hasMoreElements()) {
            String name = param.nextElement();
            String value = request.getParameter(name);
            strParam.append(name).append("=").append(value).append("&");
        }
        return strParam.toString();
    }
}
