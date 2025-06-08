// File: LegendDetector.java
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

public class LegendDetector {

    static {
        // Load OpenCV native library
        nu.pattern.OpenCV.loadLocally();
    }

    /**
     * Detects potential legend colors in an image
     * @param bufferedImage Input image
     * @return LegendInfo containing detected colors and their locations
     */
    public LegendInfo detectLegendColors(BufferedImage bufferedImage) {
        // Convert BufferedImage to OpenCV Mat
        // This will allow for analysis to be done using OpenCV
        Mat imageMat = bufferedImageToMat(bufferedImage);

        // Create LegendInfo object that will hold the info for the legend in the image
        LegendInfo legendInfo = new LegendInfo();

        // PSEUDOCODE:
        // 1. Apply image processing to identify potential legend areas
        // 2. Detect similarly sized color regions
        // 3. Extract colors from those regions
        // 4. Add results to LegendInfo object

        // Convert Mat 3 Byte BGR image to Mat HSV format
        Mat hsvImage = new Mat();
        Imgproc.cvtColor(imageMat, hsvImage, Imgproc.COLOR_BGR2HSV);  // convert the BGR to HSV

        // Step 2: Find potential legend regions
        // This is a basic implementation - we'll look for contours of similar size/shape
        List<MatOfPoint> contours = findPotentialLegendElements(hsvImage);

        // Step 3: Filter contours to find likely legend color squares/circles
        List<MatOfPoint> legendContours = filterLegendContours(contours);

        // Step 4: Extract colors and add to our result
        for (MatOfPoint contour : legendContours) {
            // Get bounding rectangle
            Rect rect = Imgproc.boundingRect(contour);

            // Extract mean color from this region
            Scalar meanColor = extractMeanColor(imageMat, rect);

            // Convert OpenCV Scalar to Java Color
            Color javaColor = new Color((int)meanColor.val[2],
                    (int)meanColor.val[1],
                    (int)meanColor.val[0]);

            // Add to our legend info
            legendInfo.addLegendColor(javaColor,
                    new Rectangle(rect.x, rect.y, rect.width, rect.height));
        }

        return legendInfo;
    }

    /**
     * Takes a Buffered Image and converts it to Mat
     * It can handle alpha channel files and will convert
     * them to a composite image without alpha channel
     * @param image Image to convert
     * @return a Mat format object
     */
    private Mat bufferedImageToMat(BufferedImage image) {
        // Convert BufferedImage to Mat
        // We create an empty Mat image with a specific format of CV_8UC3
        // CV for the OpenCV
        // 8U for 8 Bit unsigned so we have color channels from 0 - 255
        // C3 for the 3 color channels which will be BGR because OpenCV is weird
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);

        // Check if the image has an alpha channel
        // First we check default case which is just 3 channel image
        if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            // grab actual RGB data from Raster store in byte array
            byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            // add 1D byte array as 2d Mat
            mat.put(0, 0, data);

        // This block runs if the image has an alpha channel (transparency channel) or if it is
        // anything other than a 3 Byte BGR
        } else {
            // create a blank BufferedImage that doesn't have an alpha channel because of
            // the .TYPE_3BYTE_BGR
            BufferedImage convertedImg = new BufferedImage(
                    image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

            // now paint the original image with alpha channel onto new non alpha channel image
            // this will create a composite color based on transparency and background
            // if it doesn't have an alpha channel then it will convert the RGB to a BGR
            // automatically in the .getGraphics.drawImage function
            convertedImg.getGraphics().drawImage(image, 0, 0, null);

            // grab raster 1D array for image
            byte[] data = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
            // add 1D array to 2D Mat file
            mat.put(0, 0, data);
        }

        return mat;
    }

    /**
     * Find all potential legend elements using contour detection
     */
    private List<MatOfPoint> findPotentialLegendElements(Mat image) {
        // First, create a grayscale version for contour detection
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Apply Gaussian blur to reduce noise
        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);

        // Apply adaptive threshold to get binary image
        Mat binaryImage = new Mat();
        Imgproc.adaptiveThreshold(grayImage, binaryImage, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 11, 2);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binaryImage, contours, hierarchy,
                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        return contours;
    }

    /**
     * Filter contours to find likely legend color elements
     * Looks for contours of similar size/shape that are likely part of a legend
     */
    private List<MatOfPoint> filterLegendContours(List<MatOfPoint> contours) {
        List<MatOfPoint> filteredContours = new ArrayList<>();

        // PSEUDOCODE:
        // 1. Find contours of similar size
        // 2. Check if they're arranged in a pattern (like horizontally/vertically aligned)
        // 3. Filter based on shape (squares or circles are common in legends)

        // Simple implementation: filter by size and shape regularity
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            // Ignore very small or very large contours
            if (area < 100 || area > 2000) continue;

            // Check if shape is somewhat regular (square-like or circle-like)
            Rect boundingRect = Imgproc.boundingRect(contour);
            double aspectRatio = (double) boundingRect.width / boundingRect.height;

            // If aspect ratio is close to 1, it's likely a square or circle
            if (aspectRatio >= 0.7 && aspectRatio <= 1.3) {
                filteredContours.add(contour);
            }
        }

        // TODO: In a more sophisticated implementation, we'd also check if
        // the filtered contours are arranged in a pattern typical of legends

        return filteredContours;
    }

    /**
     * Extract the mean color from a region in the image
     */
    private Scalar extractMeanColor(Mat image, Rect region) {
        // Create a mask for the region
        Mat mask = Mat.zeros(image.size(), CvType.CV_8UC1);
        mask.submat(region).setTo(new Scalar(255));

        // Calculate mean color
        return Core.mean(image.submat(region));
    }

    /**
     * Simple test method to verify functionality
     */
    public static void main(String[] args) {
        // TODO: Add test code here
    }
}