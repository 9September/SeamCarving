import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class ImagePanel extends JPanel {
    private JLabel imageLabel;
    private Consumer<BufferedImage> imageLoadListener;

    public ImagePanel() {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);

        imageLabel = new JLabel("이미지를 여기에 드래그 & 드롭 하세요.");
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);

        // 텍스트 스타일 설정
        imageLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        imageLabel.setForeground(Color.GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(imageLabel, gbc);

        setTransferHandler(new ImageTransferHandler());
    }

    public void setImage(BufferedImage image) {
        if (image != null) {
            imageLabel.setIcon(new ImageIcon(image));
            imageLabel.setText(null); // 이미지가 있으면 텍스트 제거
        } else {
            imageLabel.setIcon(null);
            imageLabel.setText("이미지를 여기에 드래그 앤 드롭 하세요.");
        }
        revalidate();
        repaint();
    }

    // 드래그 앤 드롭을 위한 TransferHandler 구현
    private class ImageTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                Transferable transferable = support.getTransferable();
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (!files.isEmpty()) {
                    File file = files.get(0);
                    BufferedImage image = ImageIO.read(file);
                    setImage(image);
                    // 이미지 로드 리스너 호출
                    if (imageLoadListener != null) {
                        imageLoadListener.accept(image);
                    }
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }
    }

    // 이미지 로드 리스너 설정
    public void setImageLoadListener(Consumer<BufferedImage> listener) {
        this.imageLoadListener = listener;
    }
}
