package com.shopwizard.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * KCP 서비스 인증서(kcp_cert_info)와 개인키 서명(kcp_sign_data). 파일은 처음 쓸 때 읽어 메모리에 두고,
 * 내용(인증서·키·비밀번호)은 로그/예외 메시지에 절대 넣지 않는다.
 */
@Component
@RequiredArgsConstructor
public class KcpCredentials {

    private final KcpProperties props;

    private volatile String certInfo;
    private volatile PrivateKey privateKey;

    /** kcp_cert_info — PEM 파일 전체(BEGIN/END 줄 포함). 줄바꿈은 LF 로 통일한다. */
    public String certInfo() {
        String c = certInfo;
        if (c == null) {
            synchronized (this) {
                if (certInfo == null) certInfo = readCert(props.getCertPath());
                c = certInfo;
            }
        }
        return c;
    }

    /** 서명 문자열(UTF-8)을 개인키로 SHA256withRSA 서명해 Base64 로 돌려준다. */
    public String sign(String data) {
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(key());
            s.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(s.sign());
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("KCP 서명에 실패했습니다: " + e.getClass().getSimpleName(), e);
        }
    }

    /** 기동 시 설정을 점검한다 — 파일이 없거나 비밀번호가 틀리면 여기서 바로 실패한다. */
    public void verifyReady() {
        certInfo();
        sign("kcp-startup-check");
    }

    private PrivateKey key() {
        PrivateKey k = privateKey;
        if (k == null) {
            synchronized (this) {
                if (privateKey == null) privateKey = readKey(props.getPrivateKeyPath(), props.getPrivateKeyPassword());
                k = privateKey;
            }
        }
        return k;
    }

    static String readCert(String path) {
        if (path == null || path.isBlank()) throw new IllegalStateException("kcp.cert-path 가 설정되지 않았습니다.");
        try {
            String text = Files.readString(Path.of(path.trim()), StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n");
            if (!text.contains("-----BEGIN CERTIFICATE-----")) {
                throw new IllegalStateException("kcp.cert-path 파일이 PEM 인증서 형식이 아닙니다.");
            }
            return text.trim() + "\n";
        } catch (java.io.IOException e) {
            throw new IllegalStateException("KCP 인증서 파일을 읽을 수 없습니다: " + path, e);
        }
    }

    static PrivateKey readKey(String path, String password) {
        if (path == null || path.isBlank()) throw new IllegalStateException("kcp.private-key-path 가 설정되지 않았습니다.");
        try {
            return parsePrivateKey(Files.readString(Path.of(path.trim()), StandardCharsets.UTF_8), password);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("KCP 개인키 파일을 읽을 수 없습니다: " + path, e);
        }
    }

    /** PKCS8 PEM — 암호화된 것(ENCRYPTED PRIVATE KEY, 비밀번호 필요)과 평문(PRIVATE KEY) 둘 다 읽는다. */
    static PrivateKey parsePrivateKey(String pem, String password) {
        boolean encrypted = pem.contains("BEGIN ENCRYPTED PRIVATE KEY");
        if (!encrypted && !pem.contains("BEGIN PRIVATE KEY")) {
            throw new IllegalStateException("KCP 개인키가 PKCS8 PEM 형식이 아닙니다.");
        }
        // 줄바꿈 종류(CRLF 등)와 무관하게 Base64 문자만 남긴다.
        byte[] der = Base64.getDecoder().decode(pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("[^A-Za-z0-9+/=]", ""));
        try {
            PKCS8EncodedKeySpec spec;
            if (encrypted) {
                if (password == null || password.isEmpty()) {
                    throw new IllegalStateException("KCP 개인키가 암호화되어 있습니다. 개인키 비밀번호(kcp.private-key-password / KCP_PRIVATE_KEY_PASSWORD)를 설정하세요.");
                }
                javax.crypto.EncryptedPrivateKeyInfo info = new javax.crypto.EncryptedPrivateKeyInfo(der);
                SecretKeyFactory f = SecretKeyFactory.getInstance(info.getAlgName());
                Cipher c = Cipher.getInstance(info.getAlgName());
                c.init(Cipher.DECRYPT_MODE, f.generateSecret(new PBEKeySpec(password.toCharArray())), info.getAlgParameters());
                spec = info.getKeySpec(c);
            } else {
                spec = new PKCS8EncodedKeySpec(der);
            }
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (java.security.GeneralSecurityException | java.io.IOException e) {
            // 비밀번호가 틀리면 여기로 온다. 예외 메시지에 입력값이 섞이지 않도록 종류만 알린다.
            throw new IllegalStateException("KCP 개인키를 열 수 없습니다(비밀번호가 틀렸거나 지원하지 않는 형식): " + e.getClass().getSimpleName());
        }
    }
}
