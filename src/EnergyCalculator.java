import java.awt.image.BufferedImage;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class EnergyCalculator {
    /**
     * Computes the energy of each pixel in the given image using the dual-gradient energy function.
     *
     * @param image The input BufferedImage.
     * @return A 2D array of doubles representing the energy of each pixel.
     */
    public double[][] computeEnergy(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] energy = new double[height][width];

        // Iterate over each pixel and compute its energy
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                energy[y][x] = computeEnergyAtPixel(image, x, y);
            }
        }

        return energy;
    }

    /**
     * Computes the energy of a single pixel at (x, y) using the dual-gradient energy function.
     *
     * @param image The input BufferedImage.
     * @param x     The x-coordinate of the pixel.
     * @param y     The y-coordinate of the pixel.
     * @return The energy of the pixel as a double.
     */
    private double computeEnergyAtPixel(BufferedImage image, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Handle border pixels by treating the image as if it wraps around
        int left = (x == 0) ? width - 1 : x - 1;
        int right = (x == width - 1) ? 0 : x + 1;
        int up = (y == 0) ? height - 1 : y - 1;
        int down = (y == height - 1) ? 0 : y + 1;

        // Get RGB components of neighboring pixels
        Color colorLeft = new Color(image.getRGB(left, y));
        Color colorRight = new Color(image.getRGB(right, y));
        Color colorUp = new Color(image.getRGB(x, up));
        Color colorDown = new Color(image.getRGB(x, down));

        // Compute the differences in the x direction
        double deltaXRed = colorRight.getRed() - colorLeft.getRed();
        double deltaXGreen = colorRight.getGreen() - colorLeft.getGreen();
        double deltaXBlue = colorRight.getBlue() - colorLeft.getBlue();

        // Compute the differences in the y direction
        double deltaYRed = colorDown.getRed() - colorUp.getRed();
        double deltaYGreen = colorDown.getGreen() - colorUp.getGreen();
        double deltaYBlue = colorDown.getBlue() - colorUp.getBlue();

        // Compute the square of the gradients
        double deltaXSquared = deltaXRed * deltaXRed + deltaXGreen * deltaXGreen + deltaXBlue * deltaXBlue;
        double deltaYSquared = deltaYRed * deltaYRed + deltaYGreen * deltaYGreen + deltaYBlue * deltaYBlue;

        // The energy is the sum of the squares of the gradients
        double energy = deltaXSquared + deltaYSquared;

        return energy;
    }
}

