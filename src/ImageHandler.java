
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageHandler {

    public static BufferedImage loadImageFromFilePath(String filePath) {
        try {
            File imageFile = new File(filePath); // point at file
            return ImageIO.read(imageFile);      // read it as an image (automatically detect format)
        } catch (IOException e) {
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("Error loading image from file: " + filePath);
            return null;
        }
    }
}
