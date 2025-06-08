
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        String filePath = "C:\\Users\\Matth\\IdeaProjects\\HUE-dini\\test_infographic.jpg";

        BufferedImage testImage = ImageHandler.loadImageFromFilePath(filePath);

        // find the legend in the image
        LegendDetector detector = new LegendDetector();

        // Detect Legend Colors
        LegendInfo legendInfo = detector.detectLegendColors(testImage);

        System.out.println("Detected: " + legendInfo.getLegendColors().size() + " potential legend colors:");
        for (LegendInfo.LegendColor color : legendInfo.getLegendColors()) {
            System.out.println(color);
        }


    }
}