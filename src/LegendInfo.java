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

    private List<LegendColor> legendColors;
    private Rectangle legendBoundingBox;

    public LegendInfo() {
        legendColors = new ArrayList<>();
    }

    // methods to add colors and set legend areas
    public void addLegendColor(Color color, Rectangle boundingBox) {
        legendColors.add(new LegendColor(color, boundingBox));
    }

    public void setLegendBoundingBox(Rectangle legendBoundingBox) {
        this.legendBoundingBox = legendBoundingBox;
    }

    // Getters
    public List<LegendColor> getLegendColors() { return legendColors; }
    public Rectangle getLegendBoundingBox() { return legendBoundingBox; }
}
