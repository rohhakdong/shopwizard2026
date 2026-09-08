package com.shopwizard.catalog.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 상품 대표이미지 업로드 + 대/중/소 등 6단계 썸네일 자동 생성.
 * - 원본 파일 하나만 업로드받아 로컬 디스크(upload.dir, 기본 uploads/)에 저장하고,
 *   Thumbnailator로 50/80/100/160/220/280px 크기의 썸네일을 함께 만든다
 *   (tCatProd/tPrdProd의 ImgUrl50~280 6개 컬럼과 1:1로 대응하는, 기존에 이미 정해져 있던 크기).
 * - 저장 경로/URL은 실제 운영 데이터에서 관찰된 레거시 패턴
 *   (/images/product/{shopCode}/{yyyymmdd}/{prodCode}.jpg)을 그대로 따른다.
 * - DB에는 이 결과로 반환하는 URL 문자열만 저장하면 된다 (ProdMapper.insert/update는
 *   이미 ImgUrl~ImgUrl280 7개 컬럼을 다 받고 있어 별도 매퍼 변경 없이 그대로 쓸 수 있다).
 */
@Service
public class ProdImgUploadService {

    private static final int[] THUMB_SIZES = {50, 80, 100, 160, 220, 280};
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public Map<String, String> upload(MultipartFile file, String shopCode, String prodCode) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일이 없습니다");
        }
        if (!StringUtils.hasText(shopCode) || !StringUtils.hasText(prodCode)) {
            throw new IllegalArgumentException("상점/상품코드가 없습니다");
        }

        String ext = extractExt(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("이미지 파일(jpg/png/gif/webp)만 업로드할 수 있습니다");
        }

        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String relDir = "product/" + shopCode + "/" + today;
        // MultipartFile.transferTo(File)에 상대경로를 넘기면 애플리케이션의 작업 디렉터리가 아니라
        // (Tomcat의) 멀티파트 업로드 임시 디렉터리 기준으로 풀려서 FileNotFoundException이 난다 —
        // 절대경로로 미리 변환해서 넘긴다.
        Path dirPath = Path.of(uploadDir, relDir).toAbsolutePath();
        Files.createDirectories(dirPath);

        String baseName = prodCode + "_" + System.currentTimeMillis(); // 같은 상품 재업로드 시 캐시 URL 충돌 방지
        File originalFile = dirPath.resolve(baseName + "." + ext).toFile();
        file.transferTo(originalFile);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("imgUrl", "/images/" + relDir + "/" + originalFile.getName());

        for (int size : THUMB_SIZES) {
            File thumbFile = dirPath.resolve(baseName + "_" + size + "." + ext).toFile();
            Thumbnails.of(originalFile)
                    .size(size, size)
                    .keepAspectRatio(true)
                    .toFile(thumbFile);
            result.put("imgUrl" + size, "/images/" + relDir + "/" + thumbFile.getName());
        }
        return result;
    }

    private String extractExt(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }
}
