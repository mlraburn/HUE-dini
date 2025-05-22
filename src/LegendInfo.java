import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class LegendInfo {

    public static class LegendColor {
        private Color color;
        private Rectangle boundingBox;

        public LegendColor(Color color, Rectangle boundingBox) {
            this.color = color;
            this.boundingBox = boundingBox;
        }

        // Getters
        public Color getColor() { return color; }
        public Rectangle getBoundingBox() { return boundingBox; }

        @Override
        public String toString() {
            return "Color: RGB:(" + color.getRed() + ", "
                                  + color.getGreen() + ", "
                                  + color.getBlue() + "), "
                                  + "Bounds: " + boundingBox;
        }
    }

}
