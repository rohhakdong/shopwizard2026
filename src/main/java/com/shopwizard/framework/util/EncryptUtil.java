package com.shopwizard.framework.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptUtil {

    public static String encryptBySHA(String str) {
        try {
            StringBuilder sb = new StringBuilder();
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(str.getBytes());
            byte[] mdBytes = md.digest();
            for (byte b : mdBytes) {
                sb.append(b);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 암호화 실패", e);
        }
    }

    public static String encryptByMD5WithPHP(String str) {
        try {
            StringBuilder sb = new StringBuilder();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(str.getBytes());
            byte[] mdBytes = md.digest();
            for (byte b : mdBytes) {
                String temp = Integer.toHexString((int) b & 0x000000ff);
                if (temp.length() < 2) temp = "0" + temp;
                sb.append(temp);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 암호화 실패", e);
        }
    }
}
