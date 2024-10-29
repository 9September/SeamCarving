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
        int sign = Integer.signum(deltaWidth);
        deltaWidth = Math.abs(deltaWidth);

        BufferedImage resultImage = image;

        if (sign > 0) {
            // 너비 증가: 시임 삽입
            List<int[]> seams = new ArrayList<>();
            for (int i = 0; i < deltaWidth; i++) {
                double[][] energy = computeEnergy(resultImage);
                int[] seam = findVerticalSeam(energy);
                seams.add(seam);
                resultImage = addVerticalSeam(resultImage, seam);
            }
            // 시임 위치를 균등하게 분포시킵니다.
            resultImage = distributeVerticalSeams(resultImage, seams);
        } else {
            // 너비 감소: 시임 제거
            for (int i = 0; i < deltaWidth; i++) {
                double[][] energy = computeEnergy(resultImage);
                int[] seam = findVerticalSeam(energy);
                resultImage = removeVerticalSeam(resultImage, seam);
            }
        }

        return resultImage;
    }

    private BufferedImage resizeHeight(BufferedImage image, int deltaHeight) {
        int sign = Integer.signum(deltaHeight);
        deltaHeight = Math.abs(deltaHeight);

        BufferedImage resultImage = image;

        if (sign > 0) {
            // 높이 증가: 시임 삽입
            List<int[]> seams = new ArrayList<>();
            for (int i = 0; i < deltaHeight; i++) {
                double[][] energy = computeEnergy(resultImage);
                int[] seam = findHorizontalSeam(energy);
                seams.add(seam);
                resultImage = addHorizontalSeam(resultImage, seam);
            }
            // 시임 위치를 균등하게 분포시킵니다.
            resultImage = distributeHorizontalSeams(resultImage, seams);
        } else {
            // 높이 감소: 시임 제거
            for (int i = 0; i < deltaHeight; i++) {
                double[][] energy = computeEnergy(resultImage);
                int[] seam = findHorizontalSeam(energy);
                resultImage = removeHorizontalSeam(resultImage, seam);
            }
        }

        return resultImage;
    }

    private BufferedImage addVerticalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width + 1, height, image.getType());

        for (int y = 0; y < height; y++) {
            int newX = 0;
            for (int x = 0; x < width; x++) {
                output.setRGB(newX++, y, image.getRGB(x, y));
                if (x == seam[y]) {
                    // 시임을 삽입합니다.
                    int newRGB = averageColor(image, x, y);
                    output.setRGB(newX++, y, newRGB);
                }
            }
        }

        return output;
    }

    private BufferedImage addHorizontalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height + 1, image.getType());

        for (int x = 0; x < width; x++) {
            int newY = 0;
            for (int y = 0; y < height; y++) {
                output.setRGB(x, newY++, image.getRGB(x, y));
                if (y == seam[x]) {
                    // 시임을 삽입합니다.
                    int newRGB = averageColor(image, x, y);
                    output.setRGB(x, newY++, newRGB);
                }
            }
        }

        return output;
    }

    private BufferedImage distributeVerticalSeams(BufferedImage image, List<int[]> seams) {
        int numSeams = seams.size();
        if (numSeams == 0) return image;

        // 시임들의 위치를 조정하여 균등하게 분포시킵니다.
        seams = sortAndShiftSeams(seams);

        BufferedImage resultImage = image;

        for (int[] seam : seams) {
            resultImage = addVerticalSeam(resultImage, seam);
        }

        return resultImage;
    }

    private BufferedImage distributeHorizontalSeams(BufferedImage image, List<int[]> seams) {
        int numSeams = seams.size();
        if (numSeams == 0) return image;

        // 시임들의 위치를 조정하여 균등하게 분포시킵니다.
        seams = sortAndShiftSeams(seams);

        BufferedImage resultImage = image;

        for (int[] seam : seams) {
            resultImage = addHorizontalSeam(resultImage, seam);
        }

        return resultImage;
    }

    private List<int[]> sortAndShiftSeams(List<int[]> seams) {
        int height = seams.get(0).length;

        // 시임들의 위치를 정렬합니다.
        seams.sort(Comparator.comparingInt(seam -> seam[0]));

        // 시임 위치 조정
        int[][] shiftMap = new int[height][seams.size()];

        for (int i = 0; i < seams.size(); i++) {
            int[] seam = seams.get(i);
            for (int y = 0; y < height; y++) {
                seam[y] += i;
            }
        }

        return seams;
    }


    // Compute the energy map of the image
    // 기본 사임 카빙 알고리즘 -> 에너지 적은 것을 기준으로 확장 => 특정 모서리에서만 확장
    // 따라서 에너지 맵 계산 시 가장자리에 낮은 에너지 값을 부여할 예정
    public double[][] computeEnergy(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] energy = new double[height][width];

        // 이미지 중앙 좌표 계산
        double centerX = width / 2.0;
        double centerY = height / 2.0;

        // 최대 거리 계산 (이미지 대각선 길이의 절반)
        double maxDistance = Math.sqrt(centerX * centerX + centerY * centerY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double e = computeEnergyAtPixel(image, x, y);

                // 픽셀의 중앙으로부터의 거리 계산
                double dx = x - centerX;
                double dy = y - centerY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                // 위치에 따른 가중치 계산 (중앙부에 높은 에너지 부여)
                double positionWeight = 1.0 + (distance / maxDistance) * 2.0; // 가중치 조절 가능

                // 에너지에 가중치 적용
                energy[y][x] = e * positionWeight;
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
                    int newRGB = averageColor(image, x, y);
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
                    int newRGB = averageColor(image, x, y);
                    output.setRGB(x, newY++, newRGB);
                }
            }
        }
        return output;
    }

    // 주변 픽셀들의 평균을 사용
    private int averageColor(BufferedImage image, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 주변 좌표 계산
        int left = Math.max(x - 1, 0);
        int right = Math.min(x + 1, width - 1);
        int up = Math.max(y - 1, 0);
        int down = Math.min(y + 1, height - 1);

        // 주변 픽셀들의 RGB 값 추출
        Color c1 = new Color(image.getRGB(left, y), true);
        Color c2 = new Color(image.getRGB(right, y), true);
        Color c3 = new Color(image.getRGB(x, up), true);
        Color c4 = new Color(image.getRGB(x, down), true);

        // 각 컴포넌트의 합 계산
        int red = c1.getRed() + c2.getRed() + c3.getRed() + c4.getRed();
        int green = c1.getGreen() + c2.getGreen() + c3.getGreen() + c4.getGreen();
        int blue = c1.getBlue() + c2.getBlue() + c3.getBlue() + c4.getBlue();
        int alpha = c1.getAlpha() + c2.getAlpha() + c3.getAlpha() + c4.getAlpha();

        // 평균 계산
        red /= 4;
        green /= 4;
        blue /= 4;
        alpha /= 4;

        return new Color(red, green, blue, alpha).getRGB();
    }

    // 가중 평균을 사용한 색상 결정 - 중앙 픽셀에 더 높은 가중치를 주고, 주변 픽셀에는 낮은 가중치 부여
    private int weightedAverageColor(BufferedImage image, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 주변 좌표 계산
        int left = Math.max(x - 1, 0);
        int right = Math.min(x + 1, width - 1);
        int up = Math.max(y - 1, 0);
        int down = Math.min(y + 1, height - 1);

        // 중심 픽셀
        Color center = new Color(image.getRGB(x, y), true);
        // 주변 픽셀
        Color leftC = new Color(image.getRGB(left, y), true);
        Color rightC = new Color(image.getRGB(right, y), true);
        Color upC = new Color(image.getRGB(x, up), true);
        Color downC = new Color(image.getRGB(x, down), true);

        // 가중치 설정
        double centerWeight = 0.4;
        double sideWeight = 0.15;
        double upDownWeight = 0.15;
        double totalWeight = centerWeight + 2 * sideWeight + 2 * upDownWeight;

        // 각 컴포넌트의 가중 합 계산
        double red = center.getRed() * centerWeight +
                leftC.getRed() * sideWeight +
                rightC.getRed() * sideWeight +
                upC.getRed() * upDownWeight +
                downC.getRed() * upDownWeight;
        double green = center.getGreen() * centerWeight +
                leftC.getGreen() * sideWeight +
                rightC.getGreen() * sideWeight +
                upC.getGreen() * upDownWeight +
                downC.getGreen() * upDownWeight;
        double blue = center.getBlue() * centerWeight +
                leftC.getBlue() * sideWeight +
                rightC.getBlue() * sideWeight +
                upC.getBlue() * upDownWeight +
                downC.getBlue() * upDownWeight;
        double alpha = center.getAlpha() * centerWeight +
                leftC.getAlpha() * sideWeight +
                rightC.getAlpha() * sideWeight +
                upC.getAlpha() * upDownWeight +
                downC.getAlpha() * upDownWeight;

        // 평균 계산
        red /= totalWeight;
        green /= totalWeight;
        blue /= totalWeight;
        alpha /= totalWeight;

        // 0~255 범위로 클램핑
        int r = Math.min(255, Math.max(0, (int) red));
        int g = Math.min(255, Math.max(0, (int) green));
        int b = Math.min(255, Math.max(0, (int) blue));
        int a = Math.min(255, Math.max(0, (int) alpha));

        return new Color(r, g, b, a).getRGB();
    }




    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
