package com.shopwizard.framework.util;

import java.io.File;

public class FileUtil {

    public static void makePath(String path) {
        String[] arrPath = path.split("/");
        String fullPath = "/";
        for (int i = 1; i < arrPath.length; i++) {
            File dir = new File(fullPath + arrPath[i] + "/");
            if (!dir.exists()) dir.mkdir();
            fullPath += arrPath[i] + "/";
        }
    }
}
