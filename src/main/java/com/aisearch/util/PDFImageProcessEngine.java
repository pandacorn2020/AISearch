package com.aisearch.util;

import com.aisearch.entity.KGImage;
import com.aisearch.repository.JdbcRepository;
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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

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

    private int pageCount;

    private float pageHeight;

    private JdbcRepository jdbcRepository;
    private String schema;

    private String pageText;

    /**
     * Default constructor.
     *
     * @throws IOException If there is an error loading text stripper properties.
     */
    public PDFImageProcessEngine(int pageIndex,
                                 int pageCount,
                                 float pageHeight,
                                 List<TextPosition> pageTextPositions,
                                 String pageText,
                                 JdbcRepository jdbcRepository,
                                 String schema) throws IOException
    {
        this.pageIndex = pageIndex;
        this.pageCount = pageCount;
        this.pageHeight = pageHeight;
        this.pageTextPositions = pageTextPositions;
        this.pageText = pageText;
        this.jdbcRepository = jdbcRepository;
        this.schema = schema;
        addOperator(new Concatenate(this));
        addOperator(new DrawObject(this));
        addOperator(new SetGraphicsStateParameters(this));
        addOperator(new Save(this));
        addOperator(new Restore(this));
        addOperator(new SetMatrix(this));
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

                // Save image: convert to compressed byte array
                byte[] bytes = convertImageToByteArray(image); // Convert image to byte array

                // page height is from top to bottom, so we need to convert the y coordinate
                float newY = pageHeight - imageY;
                String description = extractTextBelowImage(imageX, newY, imageXScale);
                logger.info("Extracted description: {}", description);
                if (description == null || description.isEmpty()) {
                    if (imageWidth <= 50 || imageHeight <= 50) {
                        // Skip very small images
                        return;
                    }
                    description = pageText; // Fallback to the entire page text if no specific description found
                }
                KGImage kgImage = new KGImage(bytes, description);
                jdbcRepository.saveImages(schema, Arrays.asList(kgImage));
                int len = Math.min(128, description.length());
                logger.info("Image saved: size {}, page: {}/{}, description: {}", bytes.length, (pageIndex + 1), pageCount,
                        description.substring(0, len));
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

    public String extractTextBelowImage(float imageLeft, float imageBottom, float imageWidth) {
        List<String> collectedText = new ArrayList<>();
        boolean firstTextFound = false;
        float currentCollectedLineY = imageBottom;
        int lineCount = 1;
        TextPosition lastCollectText = null;
        for (TextPosition text : pageTextPositions) {
            float textX = text.getX();
            float textY = text.getY();
            float fontSize = text.getFontSize();
            float verticalTolerance = 1 * fontSize; // Allowable vertical distance
            float horizontalTolerance = 20; // Allowable horizontal margin


            // Check if the text is below the image and within the x-range
            if (isBelowAndInXRange(imageLeft, imageBottom, imageWidth, textY, textX, horizontalTolerance)) {
                if (textY - currentCollectedLineY > verticalTolerance) {
                    // If the new line is too far from the last collected text, reset currentLineY
                    continue;
                }
                String unitCode = text.getUnicode().trim();
                // Collect the text
                if (lastCollectText != null && lastCollectText.getY() != textY) {
                    // If the last collected text is on a different line, reset the current line Y position
                    collectedText.add("\n");
                }
                collectedText.add(unitCode);
                lastCollectText = text;

                currentCollectedLineY = textY; // Update the current line Y position
                if (unitCode.contains("\n") || unitCode.contains("\r")) {
                    // If the text is a newline character, skip it
                    lineCount++;
                }
                if (lineCount >= 3) {
                    break;
                }
            }
        }

        return String.join("", collectedText);
    }

    private static boolean isBelowAndInXRange(float imageLeft, float imageBottom, float imageWidth, float textY,
                                              float textX, float horizontalTolerance) {
        return textY > imageBottom &&
                textX >= imageLeft - horizontalTolerance &&
                textX <= imageLeft + imageWidth + horizontalTolerance;
    }

    private static final int MAX_IMAGE_DIMENSION = 1920;
    private static final float JPEG_QUALITY = 0.75f;

    private static byte[] convertImageToByteArray(PDImageXObject image) throws IOException {
        BufferedImage original = image.getImage();
        int origWidth = original.getWidth();
        int origHeight = original.getHeight();
        BufferedImage processed = compressImage(original);
        // Flush original to free native memory if it's not the same object as processed
        if (original != processed) {
            original.flush();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(processed, "jpg", baos);
        baos.flush();
        byte[] result = baos.toByteArray();
        int procWidth = processed.getWidth();
        int procHeight = processed.getHeight();
        // Flush processed image to free native memory
        processed.flush();
        logger.info("Image compressed: {}x{} -> {}x{}, {} bytes",
                origWidth, origHeight, procWidth, procHeight, result.length);
        return result;
    }

    private static BufferedImage compressImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Scale down if too large
        if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
            double scale = Math.min(
                    (double) MAX_IMAGE_DIMENSION / width,
                    (double) MAX_IMAGE_DIMENSION / height);
            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);
            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, 0, 0, newWidth, newHeight, null);
            g.dispose();
            return resized;
        }

        // Convert to RGB for JPEG encoding if needed
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            return rgb;
        }

        return image;
    }

}
