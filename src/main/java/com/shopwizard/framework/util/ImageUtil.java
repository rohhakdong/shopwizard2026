package com.shopwizard.framework.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageUtil {

    public static void createImage(String loadFile, String saveFile, int width, int height) throws IOException {
        BufferedImage src = ImageIO.read(new File(loadFile));
        BufferedImage thumb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = thumb.createGraphics();
        g2.drawImage(src, 0, 0, width, height, null);
        g2.dispose();
        ImageIO.write(thumb, "jpg", new File(saveFile));
    }

    public static void createThumbnail(String source, String target, int targetW) throws Exception {
        BufferedImage src = ImageIO.read(new File(source));
        int oldW = src.getWidth();
        int oldH = src.getHeight();
        int newW = targetW;
        int newH = (targetW * oldH) / oldW;

        BufferedImage thumb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = thumb.createGraphics();
        Image scaled = src.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        ImageIO.write(thumb, "jpg", new File(target));
    }

    public static void createThumbnailRectangle(String source, String target, int targetW, int targetH) throws Exception {
        BufferedImage src = ImageIO.read(new File(source));
        BufferedImage thumb = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = thumb.createGraphics();
        Image scaled = src.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        ImageIO.write(thumb, "jpg", new File(target));
    }
}
