package com.shopwizard.external.name;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.security.Key;
import java.util.Base64;

/**
 * NICE 실명확인 웹서비스 연동
 * 레거시: org.apache.axis.client + sun.misc.BASE64 → Java 21: java.net.http.HttpClient + java.util.Base64
 */
public class OivsObject {

    private static final String SERVICE_URL = "https://oivs.kisvan.or.kr/oivs/services/RName";

    private String svcNo;
    private String authKey;
    private String cipherKey;

    public OivsObject(String svcNo, String authKey, String cipherKey) {
        this.svcNo = svcNo;
        this.authKey = authKey;
        this.cipherKey = cipherKey;
    }

    public String checkName(String custName, String regNo) throws Exception {
        String encCustName = encryptTripleDes(custName);
        String encRegNo    = encryptTripleDes(regNo);

        String soapBody = buildSoapRequest(encCustName, encRegNo);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVICE_URL))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(soapBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response.body());
    }

    private String encryptTripleDes(String plainText) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(cipherKey);
        DESedeKeySpec keySpec = new DESedeKeySpec(keyBytes);
        Key key = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec);

        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        byte[] encBytes = cipher.doFinal(plainText.getBytes(Charset.forName("EUC-KR")));
        return Base64.getEncoder().encodeToString(encBytes);
    }

    private String buildSoapRequest(String encCustName, String encRegNo) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:rnam="http://rname.oivs.kisvan.or.kr">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <rnam:rNameCheck>
                      <svcNo>%s</svcNo>
                      <authKey>%s</authKey>
                      <custName>%s</custName>
                      <regNo>%s</regNo>
                    </rnam:rNameCheck>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(svcNo, authKey, encCustName, encRegNo);
    }

    private String parseResponse(String responseBody) {
        // 응답에서 resultCode 추출
        String marker = "<resultCode>";
        int start = responseBody.indexOf(marker);
        if (start < 0) return "ERROR";
        start += marker.length();
        int end = responseBody.indexOf("</resultCode>", start);
        if (end < 0) return "ERROR";
        return responseBody.substring(start, end).trim();
    }
}
