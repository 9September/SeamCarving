import java.awt.image.BufferedImage;

public class EnergyCalculator {
    public double[][] computeEnergy(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] energy = new double[height][width];

        // 에너지 계산 로직 구현
        // ...

        return energy;
    }

    private double computeEnergyAtPixel(BufferedImage image, int x, int y) {
        // 개별 픽셀의 에너지 계산
        // ...
        return 0.0;
    }
}
