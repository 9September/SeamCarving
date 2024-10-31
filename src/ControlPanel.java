import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;

public class ControlPanel extends JPanel {
    private JButton loadButton;
    private JButton standardSeamButton;
    private JButton forceSeamButton;
    private JButton downloadButton;
    private JTextField widthField;
    private JTextField heightField;
    private JProgressBar progressBar;

    private Consumer<BufferedImage> imageLoadListener;
    private TriConsumer<Integer, Integer, String> imageResizeListener;

    public ControlPanel(JFrame parent) {
        setLayout(new FlowLayout());

        loadButton = new JButton("Load Image");
        add(loadButton);

        add(new JLabel("Width:"));
        widthField = new JTextField(5);
        add(widthField);

        add(new JLabel("Height:"));
        heightField = new JTextField(5);
        add(heightField);

        standardSeamButton = new JButton("Standard Seam Carving");
        standardSeamButton.setEnabled(false);
        add(standardSeamButton);

        forceSeamButton = new JButton("Force Seam Position");
        forceSeamButton.setEnabled(false);
        add(forceSeamButton);

        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);
        add(downloadButton);

        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(200, 20));
        add(progressBar);

        // 액션 리스너 등록
        loadButton.addActionListener(e -> loadImage(parent));
        standardSeamButton.addActionListener(e -> resizeImage("standard"));
        forceSeamButton.addActionListener(e -> resizeImage("forced"));
        downloadButton.addActionListener(e -> downloadImage(parent));
    }

    // 이미지 로드 리스너 설정
    public void setImageLoadListener(Consumer<BufferedImage> listener) {
        this.imageLoadListener = listener;
    }

    // 이미지 변환 리스너 설정
    public void setImageResizeListener(TriConsumer<Integer, Integer, String> listener) {
        this.imageResizeListener = listener;
    }

    private void loadImage(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image Files", "jpg", "jpeg", "png", "bmp");
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                BufferedImage image = ImageIO.read(file);
                if (image != null) {
                    widthField.setText(String.valueOf(image.getWidth()));
                    heightField.setText(String.valueOf(image.getHeight()));
                    standardSeamButton.setEnabled(true);
                    forceSeamButton.setEnabled(true);
                    downloadButton.setEnabled(false);
                    if (imageLoadListener != null) {
                        imageLoadListener.accept(image);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(parent, "이미지를 로드하는 중 오류가 발생했습니다.");
            }
        }
    }

    private void resizeImage(String method) {
        try {
            int targetWidth = Integer.parseInt(widthField.getText());
            int targetHeight = Integer.parseInt(heightField.getText());

            if (targetWidth <= 0 || targetHeight <= 0) {
                JOptionPane.showMessageDialog(this, "Width and height must be positive integers.");
                return;
            }

            // 버튼 비활성화
            standardSeamButton.setEnabled(false);
            forceSeamButton.setEnabled(false);

            // 진행 바 초기화
            progressBar.setValue(0);
            progressBar.setVisible(true);

            // 이미지 변환 리스너 호출
            if (imageResizeListener != null) {
                imageResizeListener.accept(targetWidth, targetHeight, method);
            }

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter valid integers for width and height.");
        }
    }

    private void downloadImage(JFrame parent) {
        // 다운로드 기능 구현
    }

    // 진행 바 업데이트 메서드
    public void updateProgress(int value) {
        progressBar.setValue(value);
    }
    public void updateImageInfo(int width, int height) {
        widthField.setText(String.valueOf(width));
        heightField.setText(String.valueOf(height));
        standardSeamButton.setEnabled(true);
        forceSeamButton.setEnabled(true);
        downloadButton.setEnabled(false);
    }

    // 변환 완료 후 버튼 상태 업데이트
    public void enableButtons() {
        standardSeamButton.setEnabled(true);
        forceSeamButton.setEnabled(true);
        downloadButton.setEnabled(true);
    }
}
