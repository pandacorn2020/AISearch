package com.aisearch.util;

import com.aisearch.service.GraphSearch;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.PDFStreamEngine;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

/**
 * This is an example on how to get the x/y coordinates of image locations.
 *
 * @author Ben Litchfield
 */
public class PDFImageProcessEngine extends PDFStreamEngine
{

    private static final Logger logger = LoggerFactory.getLogger(PDFImageProcessEngine.class.getSimpleName());

    private List<TextPosition> pageTextPositions;
    private int pageIndex;

    private float pageHeight;

    /**
     * Default constructor.
     *
     * @throws IOException If there is an error loading text stripper properties.
     */
    public PDFImageProcessEngine(int pageIndex,
                                 float pageHeight,
                                 List<TextPosition> pageTextPositions) throws IOException
    {
        this.pageIndex = pageIndex;
        this.pageHeight = pageHeight;
        this.pageTextPositions = pageTextPositions;
        addOperator(new Concatenate());
        addOperator(new DrawObject());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new SetMatrix());
    }

    private int imageIndex = 0;

    /**
     * This is used to handle an operation.
     *
     * @param operator The operation to perform.
     * @param operands The list of arguments.
     *
     * @throws IOException If there is an error processing the operation.
     */
    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException
    {
        String operation = operator.getName();

        if( "Do".equals(operation) )
        {
            COSName objectName = (COSName) operands.get( 0 );
            PDXObject xobject = getResources().getXObject( objectName );
            if( xobject instanceof PDImageXObject)
            {
                PDImageXObject image = (PDImageXObject)xobject;
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();


                Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
                float imageXScale = ctmNew.getScalingFactorX();
                float imageYScale = ctmNew.getScalingFactorY();
                float imageX = ctmNew.getTranslateX();
                // lower left corner y coordinate
                float imageY = ctmNew.getTranslateY();
                String imageIdText = String.format("%03d", imageIndex++);
                String pageIndexText = String.format("%03d", pageIndex);

                // Save image
                String imagePath = "/output/image_" + pageIndexText + "_" + imageIdText + ".png";
                BufferedImage bufferedImage = image.getImage();
                Path path = Paths.get(imagePath);
                if (Files.exists(path)) {
                    Files.delete(path); // Delete existing file if it exists
                }
                ImageIO.write(bufferedImage, "PNG", new File(imagePath));// Convert image to byte array

                // page height is from top to bottom, so we need to convert the y coordinate
                float newY = pageHeight - imageY;
                String description = extractTextBelowImage(imageX, newY, imageWidth);
                logger.info("Image file: {}, description: {}", imagePath, description);
            }
            else if(xobject instanceof PDFormXObject)
            {
                PDFormXObject form = (PDFormXObject)xobject;
                showForm(form);
            }
        }
        else
        {
            super.processOperator( operator, operands);
        }
    }

    private String extractTextBelowImage(float imageLeft, float imageBottom, float imageWidth) {
        List<TextPosition> textPositions = pageTextPositions;

        // Group text into lines
        Map<Float, List<TextPosition>> textLines = new TreeMap<>();
        for (TextPosition text : textPositions) {
            float lineY = Math.round(text.getY() * 10) / 10f; // Group by similar Y positions
            textLines.computeIfAbsent(lineY, k -> new ArrayList<>()).add(text);
        }

        // Define search area below image
        List<String> belowLines = new ArrayList<>();

        float lastY = imageBottom;
        float lastFontSize = 0;
        for (Map.Entry<Float, List<TextPosition>> entry : textLines.entrySet()) {
            if (belowLines.size() >= 3) {
                break;
            }
            float lineY = entry.getKey();
            List<TextPosition> line = entry.getValue();
            if (lineY < imageBottom || line.isEmpty()) {
                continue;
            }
            TextPosition firstText = line.get(0);
            float fontSize = firstText.getFontSize();
            float horizontalTolerance = 20;
            float verticalTolerance = 2 * fontSize; // Look within 50 points below image
            if (firstText.getX() < imageLeft - horizontalTolerance ||
                firstText.getX() > imageLeft + imageWidth + horizontalTolerance) {
                continue; // Skip lines that are not horizontally aligned with the image
            }
            if (lastFontSize != 0 && fontSize != lastFontSize) {
                break;
            }
            TextPosition lastText = line.get(line.size() - 1);
            if (lastText.getX() < imageLeft - horizontalTolerance ||
                lastText.getX() > imageLeft + imageWidth + horizontalTolerance) {
                // text line is wider than image, should not be a description for the image
                break;
            }
            if (lineY > lastY + verticalTolerance) {
                // line is far away from last line
                break;
            }
            lastY = lineY;
            belowLines.add(line.stream()
                    .sorted(Comparator.comparing(TextPosition::getX))
                    .map(TextPosition::getUnicode)
                    .collect(Collectors.joining())
                    .trim());
        }

        return String.join("\n", belowLines);
    }

    private static byte[] convertImageToByteArray(PDImageXObject image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image.getImage(), "png", baos); // Write the image to the ByteArrayOutputStream
        baos.flush(); // Ensure all data is written
        return baos.toByteArray(); // Convert to byte array
    }

}
