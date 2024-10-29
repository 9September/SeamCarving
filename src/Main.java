import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.*;
import java.util.List;

public class Main extends JFrame {
    private JPanel mainPanel;
    private BufferedImage originalImage;
    private BufferedImage resizedImage;
    private JLabel imageLabel;
    private JTextField widthField;
    private JTextField heightField;
    private JButton convertButton;
    private JButton downloadButton;
    private JProgressBar progressBar;

    public Main() {
        setTitle("Seam Carving Resizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 메인 패널 생성
        mainPanel = new JPanel(new BorderLayout());
        setContentPane(mainPanel); // 프레임의 Content Pane을 메인 패널로 설정

        // 이미지 표시를 위한 패널 생성 (기존 코드와 동일)
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);

        JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        imagePanel.add(imageLabel, gbc);

        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 컨트롤 패널
        JPanel controlPanel = new JPanel(new FlowLayout());

        JButton loadButton = new JButton("Load Image");
        controlPanel.add(loadButton);

        controlPanel.add(new JLabel("Width:"));
        widthField = new JTextField(5);
        controlPanel.add(widthField);

        controlPanel.add(new JLabel("Height:"));
        heightField = new JTextField(5);
        controlPanel.add(heightField);

        convertButton = new JButton("Convert");
        convertButton.setEnabled(false);
        controlPanel.add(convertButton);

        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);
        controlPanel.add(downloadButton);

        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(200, 20));
        controlPanel.add(progressBar);

        add(controlPanel, BorderLayout.SOUTH);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        // 액션 리스너
        loadButton.addActionListener(e -> loadImage());
        convertButton.addActionListener(e -> convertImage());
        downloadButton.addActionListener(e -> downloadImage());

        // 드래그 & 드롭 기능을 메인 패널에 설정
        mainPanel.setTransferHandler(new TransferHandler() {
            // TransferHandler 구현 (기존 코드와 동일)
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
                        loadImageFromFile(file);
                        return true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Method to load the image
    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image Files", "jpg", "jpeg", "png", "bmp");
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadImageFromFile(chooser.getSelectedFile());
        }
    }

    private void loadImageFromFile(File file) {
        try {
            originalImage = ImageIO.read(file);
            resizedImage = originalImage;
            imageLabel.setIcon(new ImageIcon(originalImage));
            widthField.setText(String.valueOf(originalImage.getWidth()));
            heightField.setText(String.valueOf(originalImage.getHeight()));
            convertButton.setEnabled(true);
            downloadButton.setEnabled(false);
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "이미지를 로드하는 중 오류가 발생했습니다.");
        }
    }

    // Method to convert the image
    private void convertImage() {
        if (originalImage != null) {
            try {
                int targetWidth = Integer.parseInt(widthField.getText());
                int targetHeight = Integer.parseInt(heightField.getText());

                if (targetWidth <= 0 || targetHeight <= 0) {
                    JOptionPane.showMessageDialog(this, "Width and height must be positive integers.");
                    return;
                }

                // 변환 버튼 비활성화
                convertButton.setEnabled(false);

                // 진행 바 초기화
                progressBar.setValue(0);
                progressBar.setVisible(true);

                // 이미지 변환 작업 실행
                ImageResizeTask task = new ImageResizeTask(originalImage, targetWidth, targetHeight);
                task.execute();

                resizedImage = seamCarve(originalImage, targetWidth, targetHeight);

                imageLabel.setIcon(new ImageIcon(resizedImage));
                downloadButton.setEnabled(true);
                pack();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Please enter valid integers for width and height.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred during resizing.");
            }
        }
    }

    private class ImageResizeTask extends SwingWorker<BufferedImage, Integer> {
        private BufferedImage image;
        private int targetWidth;
        private int targetHeight;

        public ImageResizeTask(BufferedImage image, int targetWidth, int targetHeight) {
            this.image = image;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            BufferedImage tempImage = image;

            int currentWidth = tempImage.getWidth();
            int currentHeight = tempImage.getHeight();

            int deltaWidth = targetWidth - currentWidth;
            int deltaHeight = targetHeight - currentHeight;

            int totalSteps = Math.abs(deltaWidth) + Math.abs(deltaHeight);
            int completedSteps = 0;

            // 너비 조정
            if (deltaWidth != 0) {
                int direction = Integer.signum(deltaWidth);
                for (int i = 0; i < Math.abs(deltaWidth); i++) {
                    tempImage = resizeWidthStep(tempImage, direction);
                    completedSteps++;
                    int progress = (int) ((completedSteps / (double) totalSteps) * 100);
                    setProgress(progress);
                }
            }

            // 높이 조정
            if (deltaHeight != 0) {
                int direction = Integer.signum(deltaHeight);
                for (int i = 0; i < Math.abs(deltaHeight); i++) {
                    tempImage = resizeHeightStep(tempImage, direction);
                    completedSteps++;
                    int progress = (int) ((completedSteps / (double) totalSteps) * 100);
                    setProgress(progress);
                }
            }

            return tempImage;
        }

        @Override
        protected void process(List<Integer> chunks) {
            int value = chunks.get(chunks.size() - 1);
            progressBar.setValue(value);
        }

        @Override
        protected void done() {
            try {
                resizedImage = get();
                imageLabel.setIcon(new ImageIcon(resizedImage));
                downloadButton.setEnabled(true);
                convertButton.setEnabled(true);
                progressBar.setValue(100);
                JOptionPane.showMessageDialog(Main.this, "이미지 변환이 완료되었습니다.");
                pack();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(Main.this, "An error occurred during resizing: " + ex.getMessage());
                convertButton.setEnabled(true);
            }
        }
    }

    private BufferedImage resizeWidthStep(BufferedImage image, int direction) {
        if (direction > 0) {
            // 너비 증가
            double[][] energy = computeEnergy(image);
            int[] seam = findVerticalSeam(energy);
            image = insertVerticalSeam(image, seam);
        } else if (direction < 0) {
            // 너비 감소
            double[][] energy = computeEnergy(image);
            int[] seam = findVerticalSeam(energy);
            image = removeVerticalSeam(image, seam);
        }
        return image;
    }

    private BufferedImage resizeHeightStep(BufferedImage image, int direction) {
        if (direction > 0) {
            // 높이 증가
            double[][] energy = computeEnergy(image);
            int[] seam = findHorizontalSeam(energy);
            image = insertHorizontalSeam(image, seam);
        } else if (direction < 0) {
            // 높이 감소
            double[][] energy = computeEnergy(image);
            int[] seam = findHorizontalSeam(energy);
            image = removeHorizontalSeam(image, seam);
        }
        return image;
    }


    // Method to download the image
    private void downloadImage() {
        if (resizedImage != null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("resized_image.png"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File outputfile = chooser.getSelectedFile();
                    String formatName = "png";
                    String fileName = outputfile.getName();
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex >= 0) {
                        formatName = fileName.substring(dotIndex + 1);
                    }
                    ImageIO.write(resizedImage, formatName, outputfile);
                    JOptionPane.showMessageDialog(this, "이미지가 성공적으로 저장되었습니다.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "이미지를 저장하는 중 오류가 발생했습니다.");
                }
            }
        }
    }

    // Seam carving method to resize image to target dimensions
    private BufferedImage seamCarve(BufferedImage image, int targetWidth, int targetHeight) {
        BufferedImage tempImage = image;

        int currentWidth = tempImage.getWidth();
        int currentHeight = tempImage.getHeight();

        int deltaWidth = targetWidth - currentWidth;
        int deltaHeight = targetHeight - currentHeight;

        // 너비 조정
        if (deltaWidth != 0) {
            tempImage = resizeWidth(tempImage, deltaWidth);
        }

        // 높이 조정
        if (deltaHeight != 0) {
            tempImage = resizeHeight(tempImage, deltaHeight);
        }

        return tempImage;
    }

    private BufferedImage resizeWidth(BufferedImage image, int deltaWidth) {
        deltaWidth = Math.abs(deltaWidth);

        List<int[]> seams = new ArrayList<>();
        BufferedImage tempImage = image;

        // 시임 계산 및 제거
        for (int i = 0; i < deltaWidth; i++) {
            double[][] energy = computeEnergy(tempImage);
            int[] seam = findVerticalSeam(energy);
            seams.add(seam);
            tempImage = removeVerticalSeam(tempImage, seam);
        }

        // 시임 위치 조정
        seams = adjustSeamsForInsertion(seams, image.getWidth());

        // 시임을 원본 이미지에 삽입
        BufferedImage resultImage = image;
        for (int[] seam : seams) {
            resultImage = insertVerticalSeam(resultImage, seam);
        }

        return resultImage;
    }



    private BufferedImage resizeHeight(BufferedImage image, int deltaHeight) {
        deltaHeight = Math.abs(deltaHeight);

        List<int[]> seams = new ArrayList<>();
        BufferedImage tempImage = image;

        // 시임 계산 및 제거
        for (int i = 0; i < deltaHeight; i++) {
            double[][] energy = computeEnergy(tempImage);
            int[] seam = findHorizontalSeam(energy);
            seams.add(seam);
            tempImage = removeHorizontalSeam(tempImage, seam);
        }

        // 시임 위치 조정
        seams = adjustSeamsForInsertion(seams, image.getHeight());

        // 시임을 원본 이미지에 삽입
        BufferedImage resultImage = image;
        for (int[] seam : seams) {
            resultImage = insertHorizontalSeam(resultImage, seam);
        }

        return resultImage;
    }


    // Compute the energy map of the image
    public double[][] computeEnergy(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] energy = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                energy[y][x] = computeEnergyAtPixel(image, x, y);
            }
        }
        return energy;
    }

    private double computeEnergyAtPixel(BufferedImage image, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();

        int xLeft = (x == 0) ? x : x - 1;
        int xRight = (x == width - 1) ? x : x + 1;
        int yUp = (y == 0) ? y : y - 1;
        int yDown = (y == height - 1) ? y : y + 1;

        Color left = new Color(image.getRGB(xLeft, y));
        Color right = new Color(image.getRGB(xRight, y));
        Color up = new Color(image.getRGB(x, yUp));
        Color down = new Color(image.getRGB(x, yDown));

        double deltaX = Math.pow(left.getRed() - right.getRed(), 2)
                + Math.pow(left.getGreen() - right.getGreen(), 2)
                + Math.pow(left.getBlue() - right.getBlue(), 2);

        double deltaY = Math.pow(up.getRed() - down.getRed(), 2)
                + Math.pow(up.getGreen() - down.getGreen(), 2)
                + Math.pow(up.getBlue() - down.getBlue(), 2);

        return Math.sqrt(deltaX + deltaY);
    }

    // Find the vertical seam with the least energy
    public int[] findVerticalSeam(double[][] energy) {
        int height = energy.length;
        int width = energy[0].length;
        double[][] dp = new double[height][width];
        int[][] backtrack = new int[height][width];

        // Initialize the first row of dp
        System.arraycopy(energy[0], 0, dp[0], 0, width);

        // Populate dp and backtrack arrays
        for (int y = 1; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double minEnergy = dp[y - 1][x];
                int idx = x;

                if (x > 0 && dp[y - 1][x - 1] < minEnergy) {
                    minEnergy = dp[y - 1][x - 1];
                    idx = x - 1;
                }
                if (x < width - 1 && dp[y - 1][x + 1] < minEnergy) {
                    minEnergy = dp[y - 1][x + 1];
                    idx = x + 1;
                }

                dp[y][x] = energy[y][x] + minEnergy;
                backtrack[y][x] = idx;
            }
        }

        // Find the minimum energy seam
        double minTotalEnergy = Double.POSITIVE_INFINITY;
        int minIndex = -1;
        for (int x = 0; x < width; x++) {
            if (dp[height - 1][x] < minTotalEnergy) {
                minTotalEnergy = dp[height - 1][x];
                minIndex = x;
            }
        }

        // Backtrack to find the seam path
        int[] seam = new int[height];
        seam[height - 1] = minIndex;
        for (int y = height - 1; y > 0; y--) {
            seam[y - 1] = backtrack[y][seam[y]];
        }
        return seam;
    }

    // Find the horizontal seam with the least energy
    public int[] findHorizontalSeam(double[][] energy) {
        int height = energy.length;
        int width = energy[0].length;
        double[][] dp = new double[height][width];
        int[][] backtrack = new int[height][width];

        // Initialize the first column of dp
        for (int y = 0; y < height; y++) {
            dp[y][0] = energy[y][0];
        }

        // Populate dp and backtrack arrays
        for (int x = 1; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double minEnergy = dp[y][x - 1];
                int idx = y;

                if (y > 0 && dp[y - 1][x - 1] < minEnergy) {
                    minEnergy = dp[y - 1][x - 1];
                    idx = y - 1;
                }
                if (y < height - 1 && dp[y + 1][x - 1] < minEnergy) {
                    minEnergy = dp[y + 1][x - 1];
                    idx = y + 1;
                }

                dp[y][x] = energy[y][x] + minEnergy;
                backtrack[y][x] = idx;
            }
        }

        // Find the minimum energy seam
        double minTotalEnergy = Double.POSITIVE_INFINITY;
        int minIndex = -1;
        for (int y = 0; y < height; y++) {
            if (dp[y][width - 1] < minTotalEnergy) {
                minTotalEnergy = dp[y][width - 1];
                minIndex = y;
            }
        }

        // Backtrack to find the seam path
        int[] seam = new int[width];
        seam[width - 1] = minIndex;
        for (int x = width - 1; x > 0; x--) {
            seam[x - 1] = backtrack[seam[x]][x];
        }
        return seam;
    }

    // Remove a vertical seam
    public BufferedImage removeVerticalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width - 1, height, image.getType());

        for (int y = 0; y < height; y++) {
            int newX = 0;
            for (int x = 0; x < width; x++) {
                if (x != seam[y]) {
                    output.setRGB(newX++, y, image.getRGB(x, y));
                }
            }
        }
        return output;
    }

    // Remove a horizontal seam
    public BufferedImage removeHorizontalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height - 1, image.getType());

        for (int x = 0; x < width; x++) {
            int newY = 0;
            for (int y = 0; y < height; y++) {
                if (y != seam[x]) {
                    output.setRGB(x, newY++, image.getRGB(x, y));
                }
            }
        }
        return output;
    }

    // Insert a vertical seam
    public BufferedImage insertVerticalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width + 1, height, image.getType());

        for (int y = 0; y < height; y++) {
            int newX = 0;
            for (int x = 0; x < width; x++) {
                output.setRGB(newX++, y, image.getRGB(x, y));
                if (x == seam[y]) {
                    int rgb1 = image.getRGB(x, y);
                    int rgb2 = (x + 1 < width) ? image.getRGB(x + 1, y) : rgb1;
                    int newRGB = averageColor(rgb1, rgb2);
                    output.setRGB(newX++, y, newRGB);
                }
            }
        }
        return output;
    }



    // Insert a horizontal seam
    public BufferedImage insertHorizontalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height + 1, image.getType());

        for (int x = 0; x < width; x++) {
            int newY = 0;
            for (int y = 0; y < height; y++) {
                output.setRGB(x, newY++, image.getRGB(x, y));
                if (y == seam[x]) {
                    int rgb1 = image.getRGB(x, y);
                    int rgb2 = (y + 1 < height) ? image.getRGB(x, y + 1) : rgb1;
                    int newRGB = averageColor(rgb1, rgb2);
                    output.setRGB(x, newY++, newRGB);
                }
            }
        }
        return output;
    }

    private List<int[]> adjustSeamsForInsertion(List<int[]> seams, int size) {
        List<int[]> adjustedSeams = new ArrayList<>();
        int totalSize = size + seams.size();
        int[] shift = new int[totalSize];

        for (int[] seam : seams) {
            int[] adjustedSeam = new int[seam.length];
            for (int y = 0; y < seam.length; y++) {
                int pos = seam[y];
                if (pos < shift.length) {
                    adjustedSeam[y] = pos + shift[pos];
                } else {
                    // 인덱스가 범위를 벗어나면 조정
                    adjustedSeam[y] = shift.length - 1;
                }
            }
            for (int y = 0; y < seam.length; y++) {
                int pos = adjustedSeam[y];
                if (pos < shift.length) {
                    shift[pos]++;
                }
            }
            adjustedSeams.add(adjustedSeam);
        }
        return adjustedSeams;
    }





    private List<int[]> mapSeamsToOriginal(List<int[]> seams, int width, int height) {
        List<int[]> mappedSeams = new ArrayList<>();
        boolean[][] isSeamPixel = new boolean[height][width];
        for (int[] seam : seams) {
            for (int y = 0; y < height; y++) {
                isSeamPixel[y][seam[y]] = true;
            }
        }

        for (int[] seam : seams) {
            int[] mappedSeam = new int[seam.length];
            for (int y = 0; y < seam.length; y++) {
                int count = 0;
                for (int x = 0; x < seam[y]; x++) {
                    if (isSeamPixel[y][x]) {
                        count++;
                    }
                }
                mappedSeam[y] = seam[y] + count;
            }
            mappedSeams.add(mappedSeam);
        }
        return mappedSeams;
    }


    private int averageColor(int rgb1, int rgb2) {
        Color c1 = new Color(rgb1);
        Color c2 = new Color(rgb2);
        int red = (c1.getRed() + c2.getRed()) / 2;
        int green = (c1.getGreen() + c2.getGreen()) / 2;
        int blue = (c1.getBlue() + c2.getBlue()) / 2;
        return new Color(red, green, blue).getRGB();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
