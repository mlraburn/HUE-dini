import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * LegendInfo Class contains the information for the legend
 */
public class LegendInfo {

    /**
     * LegendColor Class is for one of the colors
     * in the Legend.
     * Many LegendColors make up a LegendInfo.
     */
    public static class LegendColor {
        // the class has 3 properties
        // each legend element has a color, bounding box, and a label
        private Color color;
        private Rectangle boundingBox;
        private String label;

        /**
         * Creates a LegendColor object containing all three properties
         * @param color the color of the region
         * @param boundingBox the bounding box of the region
         * @param label the label for the region
         */
        public LegendColor(Color color, Rectangle boundingBox, String label) {
            this.color = color;
            this.boundingBox = boundingBox;
            this.label = label;
        }

        /**
         * Creates a LegendColor object without knowing the label
         * @param color Color of the region
         * @param boundingBox bounding box of the region
         */
        public LegendColor(Color color, Rectangle boundingBox) {
            this.color = color;
            this.boundingBox = boundingBox;
            this.label = "Unknown";
        }

        // Getters
        public Color getColor() { return color; }
        public Rectangle getBoundingBox() { return boundingBox; }
        public String getLabel() { return label; }

        @Override
        public String toString() {
            // prints out a string with each RGB component Bounding box and the label
            return "Color: RGB:(" + color.getRed() + ", "
                                  + color.getGreen() + ", "
                                  + color.getBlue() + "), "
                                  + "Bounds: " + boundingBox + ", "
                                  + "Label: " + label;
        }
    }

    // The LegendInfo Class has 2 properties
    // One is a list of LegendColors
    // The other is the entire bounding box of the Legend Colors
    private List<LegendColor> legendColors;
    private Rectangle legendBoundingBox;

    public LegendInfo() {
        legendColors = new ArrayList<>();
    }

    // methods to add colors and set legend areas
    public void addLegendColor(Color color, Rectangle boundingBox) {
        legendColors.add(new LegendColor(color, boundingBox));
    }

    // overloaded method to handle adding something with a known label
    public void addLegendColor(Color color, Rectangle boundingBox, String label) {
        legendColors.add(new LegendColor(color, boundingBox, label));
    }

    public void setLegendBoundingBox(Rectangle legendBoundingBox) {
        this.legendBoundingBox = legendBoundingBox;
    }

    // Getters
    public List<LegendColor> getLegendColors() { return legendColors; }
    public Rectangle getLegendBoundingBox() { return legendBoundingBox; }
}
