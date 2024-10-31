import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


public class Main extends JFrame {
    private ImagePanel imagePanel;
    private ControlPanel controlPanel;
    private BufferedImage originalImage;
    private BufferedImage resizedImage;

    public Main() {
        setTitle("Seam Carving Resizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 메인 패널 설정
        JPanel mainPanel = new JPanel(new BorderLayout());
        setContentPane(mainPanel);

        // 이미지 패널 생성
        imagePanel = new ImagePanel();
        //mainPanel.add(imagePanel, BorderLayout.CENTER);

        // 스크롤 가능한 패인에 이미지 패널을 추가
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setPreferredSize(new Dimension(600, 500));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 컨트롤 패널 생성
        controlPanel = new ControlPanel(this);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        // 이미지 로드 시 이벤트 처리
        controlPanel.setImageLoadListener(image -> {
            originalImage = image;
            resizedImage = image;
            imagePanel.setImage(image);
            // ControlPanel의 너비와 높이 필드를 업데이트
            controlPanel.updateImageInfo(image.getWidth(), image.getHeight());
        });

        // ImagePanel의 이미지 로드 시 이벤트 처리
        imagePanel.setImageLoadListener(image -> {
            originalImage = image;
            resizedImage = image;
            // ControlPanel의 너비와 높이 필드를 업데이트
            controlPanel.updateImageInfo(image.getWidth(), image.getHeight());
        });

        // 이미지 변환 시 이벤트 처리
        controlPanel.setImageResizeListener((width, height, method) -> {
            ImageResizer resizer = new ImageResizer(originalImage);
            ImageResizeTask task = new ImageResizeTask(resizer, width, height, method, resizedImage -> {
                this.resizedImage = resizedImage;
                imagePanel.setImage(resizedImage);
                // 변환 완료 후 버튼 활성화
                controlPanel.enableButtons();
            }, controlPanel);
            task.execute();
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
