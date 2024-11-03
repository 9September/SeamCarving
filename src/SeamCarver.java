import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SeamCarver {
    private EnergyCalculator energyCalculator = new EnergyCalculator();

    // 기존의 시임 카빙 알고리즘을 사용하는 너비 조정 메서드
    public BufferedImage resizeWidthStandard(BufferedImage image, int deltaWidth, Consumer<Integer> progressCallback) {
        int sign = Integer.signum(deltaWidth);
        deltaWidth = Math.abs(deltaWidth);

        BufferedImage resultImage = image;

        if (sign < 0) {
            // Width decrease: Remove seams
            for (int i = 0; i < deltaWidth; i++) {
                double[][] energy = energyCalculator.computeEnergy(resultImage);
                int[] seam = findVerticalSeam(energy);
                resultImage = removeVerticalSeam(resultImage, seam);
                progressCallback.accept(1); // Increment progress by 1 per seam
            }
        } else if (sign > 0) {
            // Width increase: Insert seams one by one
            for (int i = 0; i < deltaWidth; i++) {
                double[][] energy = energyCalculator.computeEnergy(resultImage);
                int[] seam = findVerticalSeam(energy);
                resultImage = insertVerticalSeam(resultImage, seam);
                progressCallback.accept(1); // Increment progress by 1 per seam
            }
        }

        return resultImage;
    }


    // 시임 삽입 위치를 강제로 지정하는 너비 조정 메서드
    public BufferedImage resizeWidthForced(BufferedImage image, int deltaWidth, Consumer<Integer> progressCallback) {
        int sign = Integer.signum(deltaWidth);
        deltaWidth = Math.abs(deltaWidth);

        BufferedImage resultImage = image;

        for (int i = 0; i < deltaWidth; i++) {
            if (sign > 0) {
                // 너비 증가: 좌우 가장자리에서 시임 삽입
                int position = (i % 2 == 0) ? 0 : resultImage.getWidth() - 1;
                resultImage = insertVerticalSeamAtPosition(resultImage, position);
            } else {
                // 너비 감소: 좌우 가장자리에서 시임 제거
                int position = (i % 2 == 0) ? 0 : resultImage.getWidth() - 1;
                resultImage = removeVerticalSeamAtPosition(resultImage, position);
            }

            // 진행 상황 업데이트
            progressCallback.accept(1);
        }

        return resultImage;
    }

    // 높이 조정 메서드들도 동일하게 구현
    public BufferedImage resizeHeightStandard(BufferedImage image, int deltaHeight, Consumer<Integer> progressCallback) {
        int sign = Integer.signum(deltaHeight);
        deltaHeight = Math.abs(deltaHeight);

        BufferedImage resultImage = image;

        if (sign < 0) {
            // Height decrease: Remove seams
            for (int i = 0; i < deltaHeight; i++) {
                double[][] energy = energyCalculator.computeEnergy(resultImage);
                int[] seam = findHorizontalSeam(energy);
                resultImage = removeHorizontalSeam(resultImage, seam);
                progressCallback.accept(1); // Increment by 1 per seam
            }
        } else if (sign > 0) {
            // Height increase: Insert seams one by one
            for (int i = 0; i < deltaHeight; i++) {
                double[][] energy = energyCalculator.computeEnergy(resultImage);
                int[] seam = findHorizontalSeam(energy);
                resultImage = insertHorizontalSeam(resultImage, seam);
                progressCallback.accept(1); // Increment by 1 per seam
            }
        }

        return resultImage;
    }


    public BufferedImage resizeHeightForced(BufferedImage image, int deltaHeight, Consumer<Integer> progressCallback) {
        int sign = Integer.signum(deltaHeight);
        deltaHeight = Math.abs(deltaHeight);

        BufferedImage resultImage = image;

        for (int i = 0; i < deltaHeight; i++) {
            if (sign > 0) {
                // 높이 증가: 상하 가장자리에서 시임 삽입
                int position = (i % 2 == 0) ? 0 : resultImage.getHeight() - 1;
                resultImage = insertHorizontalSeamAtPosition(resultImage, position);
            } else {
                // 높이 감소: 상하 가장자리에서 시임 제거
                int position = (i % 2 == 0) ? 0 : resultImage.getHeight() - 1;
                resultImage = removeHorizontalSeamAtPosition(resultImage, position);
            }
            progressCallback.accept(1);
        }

        return resultImage;
    }


    public BufferedImage insertVerticalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width + 1, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            int newX = 0;
            for (int x = 0; x < width; x++) {
                output.setRGB(newX++, y, image.getRGB(x, y));
                if (x == seam[y]) {
                    // Insert a new pixel by averaging the current and next pixel
                    int rgb1 = image.getRGB(x, y);
                    int rgb2 = (x < width - 1) ? image.getRGB(x + 1, y) : image.getRGB(x, y);
                    int newRGB = averageColor(rgb1, rgb2);
                    output.setRGB(newX++, y, newRGB);
                }
            }
        }

        return output;
    }


    public BufferedImage insertHorizontalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height + 1, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            int newY = 0;
            for (int y = 0; y < height; y++) {
                output.setRGB(x, newY++, image.getRGB(x, y));
                if (y == seam[x]) {
                    // Insert a new pixel by averaging the current and next pixel
                    int rgb1 = image.getRGB(x, y);
                    int rgb2 = (y < height - 1) ? image.getRGB(x, y + 1) : image.getRGB(x, y);
                    int newRGB = averageColor(rgb1, rgb2);
                    output.setRGB(x, newY++, newRGB);
                }
            }
        }

        return output;
    }


    public BufferedImage insertVerticalSeamAtPosition(BufferedImage image, int position) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width + 1, height, image.getType());

        for (int y = 0; y < height; y++) {
            int newX = 0;
            for (int x = 0; x <= width; x++) {
                if (x == position) {
                    // 시임 삽입
                    int rgb1 = image.getRGB(Math.min(x, width - 1), y);
                    int rgb2 = image.getRGB(Math.max(x - 1, 0), y);
                    int newRGB = averageColor(rgb1, rgb2);
                    output.setRGB(newX++, y, newRGB);
                }
                if (x < width) {
                    output.setRGB(newX++, y, image.getRGB(x, y));
                }
            }
        }

        return output;
    }

    public BufferedImage removeVerticalSeamAtPosition(BufferedImage image, int position) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width - 1, height, image.getType());

        for (int y = 0; y < height; y++) {
            int newX = 0;
            for (int x = 0; x < width; x++) {
                if (x != position) {
                    output.setRGB(newX++, y, image.getRGB(x, y));
                }
            }
        }

        return output;
    }

    public BufferedImage insertHorizontalSeamAtPosition(BufferedImage image, int position) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height + 1, image.getType());

        for (int x = 0; x < width; x++) {
            int newY = 0;
            for (int y = 0; y <= height; y++) {
                if (y == position) {
                    // 시임 삽입
                    int rgb1 = image.getRGB(x, Math.min(y, height - 1));
                    int rgb2 = image.getRGB(x, Math.max(y - 1, 0));
                    int newRGB = averageColor(rgb1, rgb2);
                    output.setRGB(x, newY++, newRGB);
                }
                if (y < height) {
                    output.setRGB(x, newY++, image.getRGB(x, y));
                }
            }
        }

        return output;
    }

    public BufferedImage removeHorizontalSeamAtPosition(BufferedImage image, int position) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height - 1, image.getType());

        for (int x = 0; x < width; x++) {
            int newY = 0;
            for (int y = 0; y < height; y++) {
                if (y != position) {
                    output.setRGB(x, newY++, image.getRGB(x, y));
                }
            }
        }

        return output;
    }

    public int[] findVerticalSeam(double[][] energy) {
        int height = energy.length;
        int width = energy[0].length;
        double[][] dp = new double[height][width];
        int[][] backtrack = new int[height][width];

        // 첫 번째 행 초기화
        System.arraycopy(energy[0], 0, dp[0], 0, width);

        // DP와 백트랙 배열 채우기
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

        // 최소 에너지 시임 찾기
        double minTotalEnergy = Double.POSITIVE_INFINITY;
        int minIndex = -1;
        for (int x = 0; x < width; x++) {
            if (dp[height - 1][x] < minTotalEnergy) {
                minTotalEnergy = dp[height - 1][x];
                minIndex = x;
            }
        }

        // 시임 경로 백트랙
        int[] seam = new int[height];
        seam[height - 1] = minIndex;
        for (int y = height - 1; y > 0; y--) {
            seam[y - 1] = backtrack[y][seam[y]];
        }
        return seam;
    }

    public int[] findHorizontalSeam(double[][] energy) {
        int height = energy.length;
        int width = energy[0].length;
        double[][] dp = new double[height][width];
        int[][] backtrack = new int[height][width];

        // 첫 번째 열 초기화
        for (int y = 0; y < height; y++) {
            dp[y][0] = energy[y][0];
        }

        // DP와 백트랙 배열 채우기
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

        // 최소 에너지 시임 찾기
        double minTotalEnergy = Double.POSITIVE_INFINITY;
        int minIndex = -1;
        for (int y = 0; y < height; y++) {
            if (dp[y][width - 1] < minTotalEnergy) {
                minTotalEnergy = dp[y][width - 1];
                minIndex = y;
            }
        }

        // 시임 경로 백트랙
        int[] seam = new int[width];
        seam[width - 1] = minIndex;
        for (int x = width - 1; x > 0; x--) {
            seam[x - 1] = backtrack[seam[x]][x];
        }
        return seam;
    }

    public BufferedImage removeVerticalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width - 1, height, BufferedImage.TYPE_INT_ARGB);

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

    public BufferedImage removeHorizontalSeam(BufferedImage image, int[] seam) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height - 1, BufferedImage.TYPE_INT_ARGB);

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

    private List<int[]> mapSeamsToOriginal(List<int[]> seams) {
        List<int[]> mappedSeams = new ArrayList<>();
        for (int[] seam : seams) {
            mappedSeams.add(seam.clone());
        }
        return mappedSeams;
    }

    private List<int[]> adjustSeamsForInsertion(List<int[]> seams, int size) {
        int numSeams = seams.size();

        // 시임들을 각 픽셀 위치에 따라 오름차순으로 정렬합니다.
        // Vertical Seams: 오름차순으로 정렬하여 왼쪽에서 오른쪽으로 삽입
        // Horizontal Seams: 오름차순으로 정렬하여 위에서 아래로 삽입
        // 이를 위해 각 시임의 최소 인덱스를 기준으로 정렬
        seams.sort((a, b) -> {
            int minA = Arrays.stream(a).min().orElse(0);
            int minB = Arrays.stream(b).min().orElse(0);
            return Integer.compare(minA, minB);
        });

        // shifts 배열의 크기를 충분히 크게 설정
        int[] shifts = new int[size + numSeams * 2];

        List<int[]> adjustedSeams = new ArrayList<>();
        for (int[] seam : seams) {
            int[] adjustedSeam = new int[seam.length];
            for (int i = 0; i < seam.length; i++) {
                int originalIdx = seam[i];
                if (originalIdx < 0 || originalIdx >= shifts.length) {
                    throw new IllegalArgumentException("Seam index out of bounds: " + originalIdx);
                }
                int shift = shifts[originalIdx];
                int adjustedIdx = originalIdx + shift;

                // 인덱스가 배열의 크기를 초과하지 않도록 제한
                if (adjustedIdx >= shifts.length) {
                    adjustedIdx = shifts.length - 1;
                }

                adjustedSeam[i] = adjustedIdx;

                // shifts 배열 업데이트 (해당 위치 이후로 시임 삽입으로 인한 이동)
                for (int j = originalIdx; j < shifts.length; j++) {
                    shifts[j]++;
                }

                // 로그 출력
                System.out.println("Original Index: " + originalIdx + ", Shift: " + shift + ", Adjusted Index: " + adjustedIdx);
            }
            adjustedSeams.add(adjustedSeam);
        }

        return adjustedSeams;
    }






    private int averageColor(int rgb1, int rgb2) {
        // Extract ARGB components from the first color
        int a1 = (rgb1 >> 24) & 0xff;
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = rgb1 & 0xff;

        // Extract ARGB components from the second color
        int a2 = (rgb2 >> 24) & 0xff;
        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >> 8) & 0xff;
        int b2 = rgb2 & 0xff;

        // Compute the average of each component
        int a = (a1 + a2) / 2;
        int r = (r1 + r2) / 2;
        int g = (g1 + g2) / 2;
        int b = (b1 + b2) / 2;

        // If the image does not support alpha, set alpha to 255
        a = 255;

        // Combine the averaged components back into a single integer
        int newRGB = (a << 24) | (r << 16) | (g << 8) | b;

        return newRGB;
    }



}
