import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Triangulate {

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("please supply filename as first argument and ____ as second argument");
            System.out.println("alternatively, type 'help' for more information");
            return;
        }

        if (args[0].equals("help")) {
            return;
        }

        BufferedImage inImg = ImageIO.read(new File(args[0]));

        triangulate(inImg);
    }

    public static void triangulate(BufferedImage inImg) throws IOException {
        ImageData imgData = new ImageData(inImg);

        ImageData.Map map = p -> p.setPixel(new Pixel(p.getLum(), 255));
        imgData.applyMap(map);

        // int[] kernel = {
        //     -1, -1, -1,
        //     -1, 8, -1,
        //     -1, -1, -1
        // };

        // int[] kernel = {
        //     1, -2, 1,
        //     -2, 5, -2,
        //     1, -2, 1
        // };

        int[] kernel = {
            1, 4, 7, 4, 1,
            4, 16, 26, 16, 4,
            7, 26, 41, 26, 7,
            4, 16, 26, 16, 4,
            1, 4, 7, 4, 1
        };

        double[] normalizedKernel = normalizeKernel(kernel);

        Pixel[] res = new Pixel[imgData.getWidth() * imgData.getHeight()];

        ImageData.KernelMap edgeMap = (pixels, index) -> {
            Pixel newPixel = new Pixel(0, 255);
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] != null) {
                    newPixel.setPixel(newPixel.add(pixels[i].multScalar(normalizedKernel[i])));
                }
            }
            newPixel.clamp();
            res[index] = newPixel;
        };
        imgData.applyKernelMap(edgeMap, (int) Math.sqrt(kernel.length));

        ImageData.IndexedMap copyMap = (p, index) -> p.setPixel(res[index]);
        imgData.applyIndexedMap(copyMap);

        BufferedImage outImg = new BufferedImage(
            inImg.getWidth(),
            inImg.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        imgData.toImage(outImg);
        File outFile = new File("outTriangulate.png");
        ImageIO.write(outImg, "png", outFile);
    }

    private static double[] normalizeKernel(int[] kernel) {
        int sum = 0;
        double[] res = new double[kernel.length];
        for (int i = 0; i < kernel.length; i++) {
            sum += kernel[i];
        }
        for (int i = 0; i < kernel.length; i++) {
            res[i] = (double) kernel[i] / ((sum > 0) ? sum : 1);
        }
        return res;
    }
}
