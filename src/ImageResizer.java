import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ImageResizer {
    private BufferedImage image;
    private SeamCarver seamCarver;

    public ImageResizer(BufferedImage image) {
        this.image = image;
        this.seamCarver = new SeamCarver();
    }

    public BufferedImage resize(int targetWidth, int targetHeight, String method, Consumer<Integer> progressCallback) {
        BufferedImage tempImage = image;

        int currentWidth = tempImage.getWidth();
        int currentHeight = tempImage.getHeight();

        int deltaWidth = targetWidth - currentWidth;
        int deltaHeight = targetHeight - currentHeight;

        int totalSteps = Math.abs(deltaWidth) + Math.abs(deltaHeight);
        if (totalSteps == 0) {
            progressCallback.accept(100);
            return tempImage;
        }

        AtomicInteger completedSteps = new AtomicInteger(0);

        // Width adjustment
        if (deltaWidth != 0) {
            if (method.equals("standard")) {
                tempImage = seamCarver.resizeWidthStandard(tempImage, deltaWidth, progress -> {
                    int newCompleted = completedSteps.addAndGet(progress);
                    double rawPercentage = (newCompleted / (double) totalSteps) * 100;
                    int percentage = (int) Math.min(rawPercentage, 100);
                    progressCallback.accept(percentage);
                });
            } else {
                tempImage = seamCarver.resizeWidthForced(tempImage, deltaWidth, progress -> {
                    int newCompleted = completedSteps.addAndGet(progress);
                    double rawPercentage = (newCompleted / (double) totalSteps) * 100;
                    int percentage = (int) Math.min(rawPercentage, 100);
                    progressCallback.accept(percentage);
                });
            }
        }

        // Height adjustment
        if (deltaHeight != 0) {
            if (method.equals("standard")) {
                tempImage = seamCarver.resizeHeightStandard(tempImage, deltaHeight, progress -> {
                    int newCompleted = completedSteps.addAndGet(progress);
                    double rawPercentage = (newCompleted / (double) totalSteps) * 100;
                    int percentage = (int) Math.min(rawPercentage, 100);
                    progressCallback.accept(percentage);
                });
            } else {
                tempImage = seamCarver.resizeHeightForced(tempImage, deltaHeight, progress -> {
                    int newCompleted = completedSteps.addAndGet(progress);
                    double rawPercentage = (newCompleted / (double) totalSteps) * 100;
                    int percentage = (int) Math.min(rawPercentage, 100);
                    progressCallback.accept(percentage);
                });
            }
        }

        // Ensure progress is set to 100% at the end
        progressCallback.accept(100);

        return tempImage;
    }
}