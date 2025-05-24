import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.Scaler;

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

}
