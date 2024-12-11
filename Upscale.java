import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Upscale {

    public static void main(String[] args) throws IOException {
        // Đọc ảnh đầu vào
        File inputFile = new File("D:\\office 1\\lesson\\leetcode\\images.jpg");
        BufferedImage inputImage = ImageIO.read(inputFile);

        // Kích thước khối ban đầu N1 x N2
        int N1 = inputImage.getHeight();
        int N2 = inputImage.getWidth();

        // Tỉ lệ upscale (scale factor)
        double scaleFactor = 1.3; // Giá trị tùy chỉnh, ví dụ 2.0 để upscale 2 lần

        // Chuyển ảnh sang mảng grayscale
        double[][] image = toGrayscale(inputImage);

        // Bước 1: Tính DCT 2D tiến
        double[][] dctCoefficients = forwardDCT2D(image, N1, N2);

        // Bước 2: Tăng mẫu trong miền DCT
        double[][] upsampledDCT = upsampleDCT(dctCoefficients, N1, N2, scaleFactor);

        // Bước 3: Lọc thông thấp
        int newN1 = (int) (scaleFactor * N1);
        int newN2 = (int) (scaleFactor * N2);
        double[][] lowPassCoefficients = applyLowPassFilter(upsampledDCT, newN1, newN2);

        // Bước 4: Tính DCT 2D ngược
        double[][] upscaledImage = inverseDCT2D(lowPassCoefficients, newN1, newN2);

        // Chuyển mảng ngược thành ảnh
        BufferedImage outputImage = toBufferedImage(upscaledImage);
        File outputFile = new File("upscaled_image.png");
        ImageIO.write(outputImage, "png", outputFile);

        System.out.println("Succesfull upscaled_image.png");
    }

    // Hàm chuyển ảnh sang grayscale
    private static double[][] toGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] grayscale = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                grayscale[y][x] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        return grayscale;
    }

    // Hàm DCT 2D tiến
    private static double[][] forwardDCT2D(double[][] image, int N1, int N2) {
        double[][] dct = new double[N1][N2];
        for (int u = 0; u < N1; u++) {
            for (int v = 0; v < N2; v++) {
                double sum = 0.0;
                for (int x = 0; x < N1; x++) {
                    for (int y = 0; y < N2; y++) {
                        sum += image[x][y] *
                                Math.cos(((2 * x + 1) * u * Math.PI) / (2 * N1)) *
                                Math.cos(((2 * y + 1) * v * Math.PI) / (2 * N2));
                    }
                }
                double alphaU = (u == 0) ? Math.sqrt(1.0 / N1) : Math.sqrt(2.0 / N1);
                double alphaV = (v == 0) ? Math.sqrt(1.0 / N2) : Math.sqrt(2.0 / N2);
                dct[u][v] = alphaU * alphaV * sum;
            }
        }
        return dct;
    }

    // Hàm tăng mẫu trong miền DCT
    private static double[][] upsampleDCT(double[][] dct, int N1, int N2, double scaleFactor) {
        int newN1 = (int) (scaleFactor * N1);
        int newN2 = (int) (scaleFactor * N2);
        double[][] upsampled = new double[newN1][newN2];

        for (int u = 0; u < N1; u++) {
            for (int v = 0; v < N2; v++) {
                upsampled[u][v] = dct[u][v];
            }
        }
        return upsampled;
    }

    // Hàm lọc thông thấp
    private static double[][] applyLowPassFilter(double[][] dct, int N1, int N2) {
        double[][] filtered = new double[N1][N2];
        int cutoffX = N1 / 2; // Cắt tần số cao ở cả hai chiều
        int cutoffY = N2 / 2;

        for (int u = 0; u < N1; u++) {
            for (int v = 0; v < N2; v++) {
                if (u < cutoffX && v < cutoffY) {
                    filtered[u][v] = dct[u][v];
                } else {
                    filtered[u][v] = 0;
                }
            }
        }
        return filtered;
    }

    // Hàm DCT 2D ngược
    private static double[][] inverseDCT2D(double[][] dct, int N1, int N2) {
        double[][] image = new double[N1][N2];
        for (int x = 0; x < N1; x++) {
            for (int y = 0; y < N2; y++) {
                double sum = 0.0;
                for (int u = 0; u < N1; u++) {
                    for (int v = 0; v < N2; v++) {
                        double alphaU = (u == 0) ? Math.sqrt(1.0 / N1) : Math.sqrt(2.0 / N1);
                        double alphaV = (v == 0) ? Math.sqrt(1.0 / N2) : Math.sqrt(2.0 / N2);
                        sum += alphaU * alphaV * dct[u][v] *
                                Math.cos(((2 * x + 1) * u * Math.PI) / (2 * N1)) *
                                Math.cos(((2 * y + 1) * v * Math.PI) / (2 * N2));
                    }
                }
                image[x][y] = sum;
            }
        }
        return image;
    }

    // Hàm chuyển từ mảng thành ảnh
    private static BufferedImage toBufferedImage(double[][] image) {
        int height = image.length;
        int width = image[0].length;
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (int) Math.round(image[y][x]);
                value = Math.min(255, Math.max(0, value)); // Đảm bảo giá trị nằm trong [0, 255]
                int rgb = (value << 16) | (value << 8) | value;
                bufferedImage.setRGB(x, y, rgb);
            }
        }
        return bufferedImage;
    }
}