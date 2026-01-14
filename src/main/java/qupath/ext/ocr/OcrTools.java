package qupath.ext.ocr;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class OcrTools {
    
    private static final Logger logger = LoggerFactory.getLogger(OcrTools.class);
    private static ITesseract tesseract;

    /**
     * Initialize Tesseract with default settings.
     * This is called automatically, but can be called manually to reinitialize.
     */
    public static ITesseract initTesseract() {
        tesseract = new Tesseract();
        Path tessDataPath = OcrExtension.getTessDataPath();
        
        if (tessDataPath == null) {
            logger.error("Tessdata path not initialized!");
            return tesseract;
        }

        tesseract.setDatapath(tessDataPath.toString());
        tesseract.setLanguage("eng");
        tesseract.setOcrEngineMode(1); // Use LSTM engine only
        tesseract.setPageSegMode(6);  // Assume uniform block of text

        logger.debug("Tesseract initialized with OK settings (still needs appropriate crop)");
        return tesseract;
    }

    /**
     * Get the current Tesseract instance.
     * Initializes with defaults if not already initialized.
     * 
     * @return The ITesseract instance that can be configured from Groovy
     */
    public static ITesseract getTesseract() {
        if (tesseract == null) {
            initTesseract();
        }
        return tesseract;
    }

    /**
     * Efficiently rotates an image by 90, 180, or 270 degrees.
     */
    public static BufferedImage rotateOrthogonal(BufferedImage img, int angle) {
        angle = angle % 360;

        int w = img.getWidth();
        int h = img.getHeight();
        int newW = (angle % 180 == 0) ? w : h;
        int newH = (angle % 180 == 0) ? h : w;

        BufferedImage rotated = new BufferedImage(newW, newH, img.getType());
        Graphics2D g2d = rotated.createGraphics();

        if (angle == 90 || angle == -270) {
            g2d.translate(newW, 0);
            g2d.rotate(Math.toRadians(90));
        } else if (angle == 180 || angle == -180) {
            g2d.translate(newW, newH);
            g2d.rotate(Math.toRadians(180));
        } else if (angle == 270 || angle == -90) {
            g2d.translate(0, newH);
            g2d.rotate(Math.toRadians(270));
        }

        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return rotated;
    }

    /**
     * Search for the best available label image (label > macro > thumbnail)
     */
    private static String findLabelName(ImageServer<?> server) {
        Collection<String> associatedImages = server.getAssociatedImageList();
        logger.debug("Available associated images: {}", associatedImages);
        
        List<String> preferredNames = List.of("label", "macro", "thumbnail");
        for (String name : preferredNames) {
            if (associatedImages.contains(name)) {
                return name;
            }
        }
        return null;
    }

    /**
     * Retrieve image by name. If name is null, uses search logic. 
     */
    public static BufferedImage getLabelImage(ImageData<?> imageData, String labelName) {
        if (imageData == null) return null;
        ImageServer<?> server = imageData.getServer();
        Collection<String> available = server.getAssociatedImageList();

        String target = labelName;
        if (target == null) {
            target = findLabelName(server);
        } else if (!available.contains(target)) {
            logger.error("Requested associated image '{}' not found. Available: {}", target, available);
            return null;
        }

        if (target == null) return null;

        try {
            return (BufferedImage) server.getAssociatedImage(target);
        } catch (Exception e) {
            logger.error("Error reading associated image: " + target, e);
            return null;
        }
    }

    /**
     * Get the dimensions of the label image.
     */
    public static Dimension getLabelDimensions(ImageData<?> imageData, String labelName) {
        BufferedImage img = getLabelImage(imageData, labelName);
        return (img != null) ? new Dimension(img.getWidth(), img.getHeight()) : null;
    }

    /**
     * OCR on full image using best guess
     */
    public static String readLabelText(ImageData<?> imageData, int angle) {
        return readLabelText(imageData, null, angle);
    }

    /**
     * OCR on full named image
     */
    public static String readLabelText(ImageData<?> imageData, String labelName, int angle) {
        BufferedImage img = getLabelImage(imageData, labelName);
        return (img != null) ? performOcr(img, angle) : "";
    }

    /**
     * OCR on cropped named image
     */
    public static String readLabelText(ImageData<?> imageData, String labelName, int x, int y, int w, int h, int angle) {
        BufferedImage img = getLabelImage(imageData, labelName);
        if (img == null) return "";

        // Bounds checking
        int cropX = Math.max(0, x);
        int cropY = Math.max(0, y);
        int cropW = Math.min(w, img.getWidth() - cropX);
        int cropH = Math.min(h, img.getHeight() - cropY);

        if (cropW <= 0 || cropH <= 0) return "";

        BufferedImage croppedImg = img.getSubimage(cropX, cropY, cropW, cropH);
        return performOcr(croppedImg, angle);
    }

    /**
     * Perform OCR on an image using the current Tesseract configuration.
     * 
     * @param inputImage The image to process
     * @param angle Rotation angle (0, 90, 180, or 270)
     * @return The extracted text, or empty string if OCR fails
     */
    private static String performOcr(BufferedImage inputImage, int angle) {
        try {
            ITesseract tess = getTesseract();
            BufferedImage image = rotateOrthogonal(inputImage, angle);
            return tess.doOCR(image).trim();
        } catch (TesseractException e) {
            logger.error("OCR Error: " + e.getMessage());
            return "";
        }
    }
}