package com.aisearch.util;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class PDFImageExtractor {

    public static void main(String[] args) throws Exception {
        PDDocument document = PDDocument.load(new File("/input/input6.pdf"));
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            PDPage pdPage = document.getPage(page);

            PDResources resources = pdPage.getResources();

            int imageIndex = 0;
            for (COSName xObjectName : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(xObjectName);
                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;
                    BufferedImage bImage = image.getImage();
                    String imageIndexText = String.format("%03d", imageIndex);
                    String filePath = String.format("/output1/image_%03d_%s.jpg", page, imageIndexText);
                    System.out.println("Extracting image from page " + (page + 1) + ", filePath: " + filePath);
                    ImageIO.write(bImage, "jpg", new File(filePath));
                    imageIndex++;
                }
            }
        }
        document.close();
    }
}
