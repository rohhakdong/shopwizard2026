package com.shopwizard.framework.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FileUploadUtil {

    public static String uploadFormFile(MultipartFile formFile, String realPath) {
        return uploadFormFile(formFile, realPath, UUID.randomUUID().toString());
    }

    public static String uploadFormFile(MultipartFile formFile, String realPath, String fileName) {
        String originalFileName = formFile.getOriginalFilename();
        String ext = originalFileName.substring(originalFileName.lastIndexOf("."));
        String tempFileName = fileName + ext;

        File file = new File(realPath + tempFileName);
        if (file.exists()) file.delete();

        try (InputStream stream = formFile.getInputStream();
             OutputStream bos = new FileOutputStream(realPath + tempFileName)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(buffer, 0, 8192)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFileName;
    }

    public static int moveFile(String tempPath, String realPath) {
        fileMove(tempPath, realPath);
        return 1;
    }

    public static boolean fileIsLive(String isLivefile) {
        return new File(isLivefile).exists();
    }

    public static void fileMake(String makeFileName) {
        try {
            new File(makeFileName).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fileDelete(String deleteFileName) {
        new File(deleteFileName).delete();
    }

    public static void fileCopy(String inFileName, String outFileName) {
        try (FileInputStream fis = new FileInputStream(inFileName);
             FileOutputStream fos = new FileOutputStream(outFileName)) {
            int data;
            while ((data = fis.read()) != -1) fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fileMove(String inFileName, String outFileName) {
        fileCopy(inFileName, outFileName);
        fileDelete(inFileName);
    }

    public static List<File> getDirFileList(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists()) return Arrays.asList(dir.listFiles());
        return null;
    }
}
