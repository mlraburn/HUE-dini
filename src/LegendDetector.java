// File: LegendDetector.java
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
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
        List<MatOfPoint> contours = findPotentialLegendElementsNew(hsvImage, imageMat);

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
     * Improved contour detection that focuses on finding coherent colored regions
     * rather than noisy edge detection
     */
    private List<MatOfPoint> findPotentialLegendElementsNew(Mat hsvImage, Mat originalBgr) {
        saveMatAsImage(originalBgr, "debug_01_original_hsv.png");

        // Strategy 1: Use color-based segmentation instead of edge detection
        // This should find coherent regions of similar color
        List<MatOfPoint> colorRegions = findColorRegions(hsvImage, originalBgr);

        return colorRegions;
    }

    /**
     * Find coherent regions of similar colors using multiple approaches
     */
    private List<MatOfPoint> findColorRegions(Mat hsvImage, Mat originalBgr) {
        List<MatOfPoint> allRegions = new ArrayList<>();

        // Approach 1: Simplify the image first to reduce noise
        Mat simplifiedImage = simplifyImage(hsvImage);
        saveMatAsImage(simplifiedImage, "debug_02_simplified.png");

        // Approach 2: Use color quantization to reduce the number of colors
        Mat quantizedImage = quantizeColors(simplifiedImage);
        saveMatAsImage(quantizedImage, "debug_03_quantized.png");

        // Approach 3: Find contours based on color differences, not just brightness
        List<MatOfPoint> contours = findContoursFromColorSegmentation(quantizedImage);

        // Debug: show all found contours ON THE ORIGINAL IMAGE
        Mat contoursOverlay = originalBgr.clone(); // Now using the original BGR image!
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(0, 255, 0); // Green
            Imgproc.drawContours(contoursOverlay, contours, i, color, 2);
        }
        saveMatAsImage(contoursOverlay, "debug_04_color_contours.png");

        System.out.println("Found " + contours.size() + " color-based contours");
        return contours;
    }

    /**
     * Simplify the image by removing noise while preserving color regions
     */
    private Mat simplifyImage(Mat hsvImage) {
        Mat result = new Mat();

        // Convert back to BGR for processing (some operations work better in BGR)
        Mat bgrImage = new Mat();
        Imgproc.cvtColor(hsvImage, bgrImage, Imgproc.COLOR_HSV2BGR);

        // Apply bilateral filter - reduces noise while keeping sharp edges
        // This is much better than Gaussian blur for preserving boundaries
        Imgproc.bilateralFilter(bgrImage, result, 15, 80, 80);

        // Convert back to HSV
        Mat hsvResult = new Mat();
        Imgproc.cvtColor(result, hsvResult, Imgproc.COLOR_BGR2HSV);

        return hsvResult;
    }

    /**
     * Reduce the number of colors in the image to make segmentation easier
     */
    private Mat quantizeColors(Mat hsvImage) {
        // Convert to BGR for k-means clustering
        Mat bgrImage = new Mat();
        Imgproc.cvtColor(hsvImage, bgrImage, Imgproc.COLOR_HSV2BGR);

        // Reshape image to be a list of pixels
        Mat data = bgrImage.reshape(1, bgrImage.rows() * bgrImage.cols());
        data.convertTo(data, CvType.CV_32F);

        // Apply k-means clustering to reduce colors
        // This number is on of the free parameters
        // 3 tends to work best for most images
        int k = 3;
        Mat labels = new Mat();
        Mat centers = new Mat();

        System.out.println("Starting k-means with k=" + k + ", data points=" + data.rows());

        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 20, 1.0);
        double compactness = Core.kmeans(data, k, labels, criteria, 3, Core.KMEANS_PP_CENTERS, centers);

        System.out.println("K-means compactness: " + compactness);
        System.out.println("Centers shape: " + centers.rows() + "x" + centers.cols());

        // Convert centers back to 8-bit
        centers.convertTo(centers, CvType.CV_8U);

        // Create the quantized data Mat with the correct type from the start
        Mat quantizedData = new Mat(data.rows(), data.cols(), CvType.CV_32F);

        // Get labels as array
        int[] labelsArray = new int[(int) labels.total()];
        labels.get(0, 0, labelsArray);

        // Create output directly from labels and centers
        // First convert centers back to float for lookup
        Mat centersFloat = new Mat();
        centers.convertTo(centersFloat, CvType.CV_32F);

        // Build the quantized image by replacing each pixel with its cluster center
        for (int i = 0; i < labelsArray.length; i++) {
            int clusterIdx = labelsArray[i];
            float[] centerValues = new float[3];
            centersFloat.get(clusterIdx, 0, centerValues);

            // Put the center values directly into the quantizedData Mat
            quantizedData.put(i, 0, centerValues);
        }

        // Convert to 8-bit unsigned
        quantizedData.convertTo(quantizedData, CvType.CV_8U);

        // Reshape back to image
        Mat quantized = quantizedData.reshape(3, bgrImage.rows());

        saveMatAsImage(quantized, "debug_kmeans_output_bgr.png");

        // Convert back to HSV
        Mat hsvResult = new Mat();
        Imgproc.cvtColor(quantized, hsvResult, Imgproc.COLOR_BGR2HSV);

        return hsvResult;
    }

    /**
     * Find contours using color-based segmentation rather than edge detection
     */
    private List<MatOfPoint> findContoursFromColorSegmentation(Mat hsvImage) {
        List<MatOfPoint> allContours = new ArrayList<>();

        // Convert to BGR for final processing
        Mat bgrImage = new Mat();
        Imgproc.cvtColor(hsvImage, bgrImage, Imgproc.COLOR_HSV2BGR);

        // Method 1: Watershed segmentation for region detection
        List<MatOfPoint> watershedContours = findWatershedRegions(bgrImage);  // ✅ Correct type
        allContours.addAll(watershedContours);

        // Method 2: Simple connected components on quantized image
        List<MatOfPoint> componentContours = findConnectedComponents(bgrImage);  // ✅ Correct type
        allContours.addAll(componentContours);

        return allContours;
    }

    /**
     * Use watershed algorithm to find distinct regions
     */
    private List<MatOfPoint> findWatershedRegions(Mat image) {
        List<MatOfPoint> contours = new ArrayList<>();

        // Convert to grayscale for watershed
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        // Apply threshold to get binary image
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        // Remove noise with morphological operations
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Mat opening = new Mat();
        Imgproc.morphologyEx(binary, opening, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), 2);

        // Sure background area
        Mat sureBg = new Mat();
        Imgproc.dilate(opening, sureBg, kernel, new Point(-1, -1), 3);

        // Finding sure foreground area
        Mat distTransform = new Mat();
        Imgproc.distanceTransform(opening, distTransform, Imgproc.DIST_L2, 5);

        Mat sureFg = new Mat();
        Imgproc.threshold(distTransform, sureFg, 0.4 * 255, 255, Imgproc.THRESH_BINARY);

        // Convert to proper type for findContours
        sureFg.convertTo(sureFg, CvType.CV_8U);

        // Find contours in the sure foreground
        Mat hierarchy = new Mat();
        Imgproc.findContours(sureFg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        return contours;
    }

    /**
     * Find connected components of similar colors
     */
    private List<MatOfPoint> findConnectedComponents(Mat image) {
        List<MatOfPoint> contours = new ArrayList<>();

        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        // Use OTSU thresholding instead of adaptive - often better for distinct regions
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        saveMatAsImage(binary, "debug_05_otsu_binary.png");

        // Clean up with morphological operations
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));

        // Close small gaps
        Mat closed = new Mat();
        Imgproc.morphologyEx(binary, closed, Imgproc.MORPH_CLOSE, kernel);

        // Remove small noise
        Mat cleaned = new Mat();
        Imgproc.morphologyEx(closed, cleaned, Imgproc.MORPH_OPEN, kernel);

        saveMatAsImage(cleaned, "debug_06_cleaned_binary.png");

        // Find contours
        Mat hierarchy = new Mat();
        Imgproc.findContours(cleaned, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        return contours;
    }

    /**
     * List returns a list of MatOfPoints which are basically
     * Contours or outlines within a Mat file
     *
     * @param image in the Mat format that is HSV formated
     * @return a list of MatOfPoints which are really contours
     */
    private List<MatOfPoint> findPotentialLegendElements(Mat image) {
        // First, create a grayscale version for contour detection
        // We do this because its much easier to find boundaries with gray scale
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        saveMatAsImage(grayImage, "debug_01_grayscale.png");

        // Apply Gaussian blur to reduce noise
        // Allows us to not find tons of contours
        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
        saveMatAsImage(grayImage, "debug_02_blurred.png");

        // Apply adaptive threshold to get binary image
        Mat binaryImage = new Mat();  // This is a black and white image
        Imgproc.adaptiveThreshold(grayImage, binaryImage, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 11, 2);
        saveMatAsImage(binaryImage, "debug_03_binary.png");

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binaryImage, contours, hierarchy,
                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Optional: Create an image showing all detected contours
        Mat contoursImage = Mat.zeros(image.size(), CvType.CV_8UC3);
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(0, 255, 0); // Green contours
            Imgproc.drawContours(contoursImage, contours, i, color, 2);
        }
        saveMatAsImage(contoursImage, "debug_05_all_contours.png");

        // Create an overlay image showing contours on the original image
        Mat overlayImage = image.clone(); // Start with a copy of the original

        // Draw all contours on the original image
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(0, 255, 0); // Green contours (BGR format)
            Imgproc.drawContours(overlayImage, contours, i, color, 2);
        }
        saveMatAsImage(overlayImage, "debug_06_contours_overlay.png");

        // Optional: Also create a version with contour numbers for detailed analysis
        Mat numberedOverlay = image.clone();
        for (int i = 0; i < contours.size(); i++) {
            // Draw the contour
            Scalar green = new Scalar(0, 255, 0);
            Imgproc.drawContours(numberedOverlay, contours, i, green, 2);

            // Add contour number as text
            Rect boundingBox = Imgproc.boundingRect(contours.get(i));
            Point textLocation = new Point(boundingBox.x, boundingBox.y - 5);
            Scalar red = new Scalar(0, 0, 255); // Red text
            Imgproc.putText(numberedOverlay, String.valueOf(i), textLocation,
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, red, 1);
        }
        saveMatAsImage(numberedOverlay, "debug_07_numbered_contours.png");

        System.out.println("Found " + contours.size() + " total contours");
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
     * Saves a Mat object as an image file for debugging purposes
     * @param mat The Mat to save
     * @param filename The output filename (should include .png or .jpg extension)
     */
    private void saveMatAsImage(Mat mat, String filename) {
        try {
            // Convert Mat back to BufferedImage for saving
            BufferedImage bufferedImage = matToBufferedImage(mat);
            File outputFile = new File(filename);
            ImageIO.write(bufferedImage, "PNG", outputFile);
            System.out.println("Saved debug image: " + filename);
        } catch (IOException e) {
            System.err.println("Error saving debug image " + filename + ": " + e.getMessage());
        }
    }

    /**
     * Converts an OpenCV Mat back to a BufferedImage
     * @param mat The Mat to convert (can be grayscale or color)
     * @return BufferedImage for saving or display
     */
    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        // Calculate the size of the buffer needed (channels * columns * rows)
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] buffer = new byte[bufferSize];
        mat.get(0, 0, buffer); // Get all pixel data

        // Create blank BufferedImage of correct syze and type
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);

        return image;
    }

    /**
     * Simple test method to verify functionality
     */
    public static void main(String[] args) {
        // TODO: Add test code here
    }
}