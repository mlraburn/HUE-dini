
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        String filePath = "C:\\Users\\Matth\\IdeaProjects\\HUE-dini\\test_infographic.jpg";

        BufferedImage testImage = ImageHandler.loadImageFromFilePath(filePath);
    }
}