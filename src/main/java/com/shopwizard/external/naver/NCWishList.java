package com.shopwizard.external.naver;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

public class NCWishList {

    public static String callWishList(HttpServletRequest request, String targetUrl,
                                      List<NCItemStackInWishList> itemList, String merchantId) {
        StringBuilder params = new StringBuilder();
        try {
            params.append("merchantId=").append(URLEncoder.encode(merchantId, "UTF-8"));

            if (itemList != null) {
                for (int i = 0; i < itemList.size(); i++) {
                    NCItemStackInWishList item = itemList.get(i);
                    params.append("&itemId[").append(i).append("]=").append(URLEncoder.encode(item.getItemId(), "UTF-8"));
                    params.append("&itemName[").append(i).append("]=").append(URLEncoder.encode(item.getItemName(), "UTF-8"));
                    params.append("&itemUPrice[").append(i).append("]=").append(item.getItemUPrice());
                    if (item.getItemImage() != null)
                        params.append("&itemImage[").append(i).append("]=").append(URLEncoder.encode(item.getItemImage(), "UTF-8"));
                    if (item.getItemThumb() != null)
                        params.append("&itemThumb[").append(i).append("]=").append(URLEncoder.encode(item.getItemThumb(), "UTF-8"));
                    if (item.getItemUrl() != null)
                        params.append("&itemUrl[").append(i).append("]=").append(URLEncoder.encode(item.getItemUrl(), "UTF-8"));
                }
            }

            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    params.append("&").append(URLEncoder.encode(cookie.getName(), "UTF-8"))
                          .append("=").append(URLEncoder.encode(cookie.getValue(), "UTF-8"));
                }
            }

            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                writer.write(params.toString());
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
