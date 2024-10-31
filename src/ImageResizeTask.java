import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

public class ImageResizeTask extends SwingWorker<BufferedImage, Integer> {
    private ImageResizer resizer;
    private int targetWidth;
    private int targetHeight;
    private String method;
    private Consumer<BufferedImage> doneCallback;
    private ControlPanel controlPanel;

    public ImageResizeTask(ImageResizer resizer, int targetWidth, int targetHeight, String method, Consumer<BufferedImage> doneCallback, ControlPanel controlPanel) {
        this.resizer = resizer;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.method = method;
        this.doneCallback = doneCallback;
        this.controlPanel = controlPanel;
    }

    @Override
    protected BufferedImage doInBackground() throws Exception {
        return resizer.resize(targetWidth, targetHeight, method, progress -> {
            setProgress(progress);
            publish(progress);
        });
    }

    @Override
    protected void process(List<Integer> chunks) {
        int value = chunks.get(chunks.size() - 1);
        // 진행 바 업데이트
        controlPanel.updateProgress(value);
    }

    @Override
    protected void done() {
        try {
            BufferedImage resultImage = get();
            if (doneCallback != null) {
                doneCallback.accept(resultImage);
            }
            // 버튼 활성화 등 UI 업데이트
            controlPanel.updateProgress(100);
            controlPanel.enableButtons();
        } catch (Exception ex) {
            ex.printStackTrace();
            // 예외 처리
        }
    }
}
