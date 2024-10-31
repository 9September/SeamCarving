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
        AtomicInteger completedSteps = new AtomicInteger(0);

        // 너비 조정
        if (deltaWidth != 0) {
            if (method.equals("standard")) {
                tempImage = seamCarver.resizeWidthStandard(tempImage, deltaWidth, progress -> {
                    completedSteps.addAndGet(progress);
                    int percentage = (int) ((completedSteps.get() / (double) totalSteps) * 100);
                    progressCallback.accept(percentage);
                });
            } else {
                tempImage = seamCarver.resizeWidthForced(tempImage, deltaWidth, progress -> {
                    completedSteps.addAndGet(progress);
                    int percentage = (int) ((completedSteps.get() / (double) totalSteps) * 100);
                    progressCallback.accept(percentage);
                });
            }
        }

        // 높이 조정
        if (deltaHeight != 0) {
            if (method.equals("standard")) {
                tempImage = seamCarver.resizeHeightStandard(tempImage, deltaHeight, progress -> {
                    completedSteps.addAndGet(progress);
                    int percentage = (int) ((completedSteps.get() / (double) totalSteps) * 100);
                    progressCallback.accept(percentage);
                });
            } else {
                tempImage = seamCarver.resizeHeightForced(tempImage, deltaHeight, progress -> {
                    completedSteps.addAndGet(progress);
                    int percentage = (int) ((completedSteps.get() / (double) totalSteps) * 100);
                    progressCallback.accept(percentage);
                });
            }
        }

        return tempImage;
    }
}
